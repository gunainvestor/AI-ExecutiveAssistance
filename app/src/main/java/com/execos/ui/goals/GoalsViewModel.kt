package com.execos.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.GoalItem
import com.execos.data.model.GoalPeriod
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.GoalRepository
import com.execos.util.Dates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val yearKey: String = Dates.yearKey(),
    val quarterKey: String = Dates.quarterKey(),
    val monthKey: String = Dates.monthKey(),
    val weekKey: String = Dates.weekStartIso(),
    val yearGoals: List<GoalItem> = emptyList(),
    val quarterGoals: List<GoalItem> = emptyList(),
    val monthGoals: List<GoalItem> = emptyList(),
    val weekGoals: List<GoalItem> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val busy = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)

    private val yearKey = MutableStateFlow(Dates.yearKey())
    private val quarterKey = MutableStateFlow(Dates.quarterKey())
    private val monthKey = MutableStateFlow(Dates.monthKey())
    private val weekKey = MutableStateFlow(Dates.weekStartIso())

    private val yearGoals = combine(uid, yearKey) { u, k -> u to k }.flatMapLatest { (u, k) ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.YEAR, k)
    }
    private val quarterGoals = combine(uid, quarterKey) { u, k -> u to k }.flatMapLatest { (u, k) ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.QUARTER, k)
    }
    private val monthGoals = combine(uid, monthKey) { u, k -> u to k }.flatMapLatest { (u, k) ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.MONTH, k)
    }
    private val weekGoals = combine(uid, weekKey) { u, k -> u to k }.flatMapLatest { (u, k) ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.WEEK, k)
    }

    val uiState: StateFlow<GoalsUiState> = combine(
        combine(authError, uid, busy, flash) { err, u, b, m -> Quad(err, u, b, m) },
        combine(yearKey, quarterKey, monthKey, weekKey) { y, q, m, w -> Keys(y, q, m, w) },
        combine(yearGoals, quarterGoals, monthGoals, weekGoals) { y, q, m, w -> GoalBuckets(y, q, m, w) },
    ) { core, keys, buckets ->
        GoalsUiState(
            loading = core.err == null && core.uid == null,
            error = core.err,
            yearKey = keys.year,
            quarterKey = keys.quarter,
            monthKey = keys.month,
            weekKey = keys.week,
            yearGoals = buckets.year,
            quarterGoals = buckets.quarter,
            monthGoals = buckets.month,
            weekGoals = buckets.week,
            busy = core.busy,
            message = core.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalsUiState())

    init {
        viewModelScope.launch {
            authRepository.ensureSignedIn().fold(
                onSuccess = { uid.value = it },
                onFailure = { authError.value = it.message ?: "Sign-in failed" },
            )
        }
    }

    fun consumeMessage() {
        flash.value = null
    }

    fun addGoal(periodType: String) {
        val u = uid.value ?: return
        val (key, existing) = currentKeyAndList(periodType)
        if (existing.size >= 3) {
            flash.value = "Top 3 limit reached for $periodType."
            return
        }
        val nextRank = (existing.maxOfOrNull { it.rank } ?: 0) + 1
        viewModelScope.launch {
            busy.value = true
            try {
                goalRepository.saveGoal(
                    u,
                    GoalItem(
                        periodType = periodType,
                        periodKey = key,
                        rank = nextRank,
                        title = "New goal",
                    ),
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun updateGoal(goal: GoalItem, title: String) {
        val u = uid.value ?: return
        viewModelScope.launch {
            goalRepository.saveGoal(u, goal.copy(title = title))
        }
    }

    fun deleteGoal(goal: GoalItem) {
        val u = uid.value ?: return
        if (goal.id.isBlank()) return
        viewModelScope.launch {
            goalRepository.deleteGoal(u, goal.id)
        }
    }

    private fun currentKeyAndList(periodType: String): Pair<String, List<GoalItem>> {
        val s = uiState.value
        return when (periodType) {
            GoalPeriod.YEAR -> s.yearKey to s.yearGoals
            GoalPeriod.QUARTER -> s.quarterKey to s.quarterGoals
            GoalPeriod.MONTH -> s.monthKey to s.monthGoals
            else -> s.weekKey to s.weekGoals
        }
    }

    private data class Quad(
        val err: String?,
        val uid: String?,
        val busy: Boolean,
        val message: String?,
    )

    private data class Keys(
        val year: String,
        val quarter: String,
        val month: String,
        val week: String,
    )

    private data class GoalBuckets(
        val year: List<GoalItem>,
        val quarter: List<GoalItem>,
        val month: List<GoalItem>,
        val week: List<GoalItem>,
    )
}

