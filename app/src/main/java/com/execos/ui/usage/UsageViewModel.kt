package com.execos.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.execos.data.remote.AiService
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.GoalRepository
import com.execos.data.repo.TaskRepository
import com.execos.data.model.GoalPeriod
import com.execos.data.usage.AppUsageItem
import com.execos.data.usage.DeviceUsageRepository
import com.execos.util.Dates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsageUiState(
    val hasAccess: Boolean = false,
    val loading: Boolean = true,
    val error: String? = null,
    val items: List<AppUsageItem> = emptyList(),
    val aiBusy: Boolean = false,
    val aiText: String = "",
    val aiPlan: UsageAiPlan? = null,
)

data class UsageAiPlan(
    val emojiHeadline: String = "",
    val realityCheck: String = "",
    val focusGoal: String = "",
    val focusHorizon: String = "",
    val timeCouldHaveBeen: List<UsageAiItem> = emptyList(),
    val next2Hours: List<UsageAiItem> = emptyList(),
)

data class UsageAiItem(
    val emoji: String = "",
    val title: String = "",
    val minutes: Int = 0,
    val goal: String = "",
)

@HiltViewModel
class UsageViewModel @Inject constructor(
    private val deviceUsageRepository: DeviceUsageRepository,
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val hasAccess = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)
    private val items = MutableStateFlow<List<AppUsageItem>>(emptyList())
    private val aiBusy = MutableStateFlow(false)
    private val aiText = MutableStateFlow("")
    private val aiPlan = MutableStateFlow<UsageAiPlan?>(null)
    private val gson = Gson()

    val uiState: StateFlow<UsageUiState> = combine(
        hasAccess,
        loading,
        error,
        items,
        aiBusy,
        aiText,
        aiPlan,
    ) { parts ->
        UsageUiState(
            hasAccess = parts[0] as Boolean,
            loading = parts[1] as Boolean,
            error = parts[2] as String?,
            items = parts[3] as List<AppUsageItem>,
            aiBusy = parts[4] as Boolean,
            aiText = parts[5] as String,
            aiPlan = parts[6] as UsageAiPlan?,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UsageUiState(),
    )

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            val access = deviceUsageRepository.hasUsageAccess()
            hasAccess.value = access
            if (!access) {
                items.value = emptyList()
                aiText.value = ""
                aiPlan.value = null
                loading.value = false
                return@launch
            }
            deviceUsageRepository.getSocialAndBrowserUsage().fold(
                onSuccess = { items.value = it },
                onFailure = { error.value = it.message ?: "Could not read device usage" },
            )
            loading.value = false
        }
    }

    fun generateAiCoach() {
        val list = items.value
        val minutesToday = list.sumOf { it.minutesToday }
        val minutesWeek = list.sumOf { it.minutesThisWeek }
        viewModelScope.launch {
            aiBusy.value = true
            error.value = null
            val uid = authRepository.ensureSignedIn().getOrNull()
            val priorities = if (uid == null) {
                emptyList()
            } else {
                runCatching {
                    taskRepository.observeTasksForDate(uid, Dates.todayIso()).first()
                        .sortedWith(compareByDescending<com.execos.data.model.TaskItem> { it.impactScore }.thenBy { it.title })
                        .take(3)
                        .map { it.title }
                }.getOrElse { emptyList() }
            }

            val goalsPack = if (uid == null) {
                GoalsPack()
            } else {
                runCatching {
                    val weekGoals = goalRepository.observeGoals(uid, GoalPeriod.WEEK, Dates.weekStartIso()).first()
                        .sortedBy { it.rank }
                        .map { it.title }
                    val monthGoals = goalRepository.observeGoals(uid, GoalPeriod.MONTH, Dates.monthKey()).first()
                        .sortedBy { it.rank }
                        .map { it.title }
                    val quarterGoals = goalRepository.observeGoals(uid, GoalPeriod.QUARTER, Dates.quarterKey()).first()
                        .sortedBy { it.rank }
                        .map { it.title }
                    GoalsPack(weekGoals = weekGoals, monthGoals = monthGoals, quarterGoals = quarterGoals)
                }.getOrElse { GoalsPack() }
            }

            aiService.usageCoach(
                minutesToday = minutesToday,
                minutesThisWeek = minutesWeek,
                priorityTitles = priorities,
                weekGoals = goalsPack.weekGoals,
                monthGoals = goalsPack.monthGoals,
                quarterGoals = goalsPack.quarterGoals,
            ).fold(
                onSuccess = { raw ->
                    aiText.value = raw
                    aiPlan.value = parsePlanOrNull(raw)
                },
                onFailure = { error.value = it.message ?: "AI failed" },
            )
            aiBusy.value = false
        }
    }

    private fun parsePlanOrNull(raw: String): UsageAiPlan? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            val plan = gson.fromJson(trimmed, UsageAiPlan::class.java)
            if (plan.emojiHeadline.isBlank()) null else plan
        } catch (_: JsonSyntaxException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private data class GoalsPack(
        val weekGoals: List<String> = emptyList(),
        val monthGoals: List<String> = emptyList(),
        val quarterGoals: List<String> = emptyList(),
    )
}

