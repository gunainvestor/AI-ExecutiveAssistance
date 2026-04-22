package com.execos.ui.weekly

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.DecisionItem
import com.execos.data.model.GoalItem
import com.execos.data.model.GoalPeriod
import com.execos.data.model.TaskItem
import com.execos.data.model.WeeklyReviewItem
import com.execos.data.remote.AiService
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.DecisionRepository
import com.execos.data.repo.GoalRepository
import com.execos.data.repo.TaskRepository
import com.execos.data.repo.WeeklyReviewRepository
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WeeklyUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val weekStart: String = Dates.weekStartIso(),
    val weekTasks: List<TaskItem> = emptyList(),
    val weekDecisions: List<DecisionItem> = emptyList(),
    val yearGoals: List<GoalItem> = emptyList(),
    val quarterGoals: List<GoalItem> = emptyList(),
    val monthGoals: List<GoalItem> = emptyList(),
    val weekGoals: List<GoalItem> = emptyList(),
    val completedTasks: List<TaskItem> = emptyList(),
    val wins: String = "",
    val mistakes: String = "",
    val learnings: String = "",
    val aiSummary: String = "",
    val aiBusy: Boolean = false,
    val saveBusy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class WeeklyReviewViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val decisionRepository: DecisionRepository,
    private val weeklyReviewRepository: WeeklyReviewRepository,
    private val goalRepository: GoalRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val weekStart = MutableStateFlow(Dates.weekStartIso())
    private val wins = MutableStateFlow("")
    private val mistakes = MutableStateFlow("")
    private val learnings = MutableStateFlow("")
    private val aiSummary = MutableStateFlow("")
    private val aiBusy = MutableStateFlow(false)
    private val saveBusy = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)
    private var lastAppliedWeek: String? = null

    private val reviewsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else weeklyReviewRepository.observeReviews(u)
    }

    private val tasksFlow = combine(uid, weekStart) { u, ws ->
        u to ws
    }.flatMapLatest { (u, ws) ->
        if (u == null) flowOf(emptyList())
        else {
            val end = Dates.weekEndIso(ws)
            taskRepository.observeTasksInRange(u, ws, end)
        }
    }

    private val decisionsFlow = combine(uid, weekStart) { u, ws ->
        u to ws
    }.flatMapLatest { (u, ws) ->
        if (u == null) flowOf(emptyList())
        else {
            val end = Dates.weekEndIso(ws)
            decisionRepository.observeDecisionsInRange(u, ws, end)
        }
    }
    private val yearGoalsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.YEAR, Dates.yearKey())
    }
    private val quarterGoalsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.QUARTER, Dates.quarterKey())
    }
    private val monthGoalsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.MONTH, Dates.monthKey())
    }
    private val weekGoalsFlow = combine(uid, weekStart) { u, ws -> u to ws }.flatMapLatest { (u, ws) ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.WEEK, ws)
    }

    private data class WeeklyCore(
        val error: String?,
        val uid: String?,
        val weekStart: String,
        val tasks: List<TaskItem>,
        val decisions: List<DecisionItem>,
        val yearGoals: List<GoalItem>,
        val quarterGoals: List<GoalItem>,
        val monthGoals: List<GoalItem>,
        val weekGoals: List<GoalItem>,
    )

    private data class WeeklyFields(
        val wins: String,
        val mistakes: String,
        val learnings: String,
        val aiSummary: String,
    )

    private data class WeeklyFlags(
        val aiBusy: Boolean,
        val saveBusy: Boolean,
        val message: String?,
    )

    val uiState: StateFlow<WeeklyUiState> = combine(
        combine(
            combine(authError, uid, weekStart, tasksFlow, decisionsFlow) { err, u, ws, tasks, decs ->
                Quint(err, u, ws, tasks, decs)
            },
            combine(yearGoalsFlow, quarterGoalsFlow, monthGoalsFlow, weekGoalsFlow) { yGoals, qGoals, mGoals, wGoals ->
                GoalsPack(yGoals, qGoals, mGoals, wGoals)
            },
        ) { head, gp ->
            WeeklyCore(
                error = head.err,
                uid = head.uid,
                weekStart = head.weekStart,
                tasks = head.tasks,
                decisions = head.decisions,
                yearGoals = gp.yearGoals,
                quarterGoals = gp.quarterGoals,
                monthGoals = gp.monthGoals,
                weekGoals = gp.weekGoals,
            )
        },
        combine(wins, mistakes, learnings, aiSummary) { w, m, l, ai ->
            WeeklyFields(w, m, l, ai)
        },
        combine(aiBusy, saveBusy, flash) { aBusy, sBusy, msg ->
            WeeklyFlags(aBusy, sBusy, msg)
        },
    ) { core, fields, flags ->
        WeeklyUiState(
            loading = core.error == null && core.uid == null,
            error = core.error,
            weekStart = core.weekStart,
            weekTasks = core.tasks,
            weekDecisions = core.decisions,
            yearGoals = core.yearGoals,
            quarterGoals = core.quarterGoals,
            monthGoals = core.monthGoals,
            weekGoals = core.weekGoals,
            completedTasks = core.tasks.filter { it.completed },
            wins = fields.wins,
            mistakes = fields.mistakes,
            learnings = fields.learnings,
            aiSummary = fields.aiSummary,
            aiBusy = flags.aiBusy,
            saveBusy = flags.saveBusy,
            message = flags.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = WeeklyUiState(),
    )

    private data class Quint(
        val err: String?,
        val uid: String?,
        val weekStart: String,
        val tasks: List<TaskItem>,
        val decisions: List<DecisionItem>,
    )

    private data class GoalsPack(
        val yearGoals: List<GoalItem>,
        val quarterGoals: List<GoalItem>,
        val monthGoals: List<GoalItem>,
        val weekGoals: List<GoalItem>,
    )

    init {
        viewModelScope.launch {
            authRepository.ensureSignedIn().fold(
                onSuccess = { uid.value = it },
                onFailure = { authError.value = it.message ?: "Sign-in failed" },
            )
        }
        viewModelScope.launch {
            combine(weekStart, reviewsFlow) { ws, revs -> ws to revs }
                .collect { (ws, revs) ->
                    if (lastAppliedWeek != ws) {
                        lastAppliedWeek = ws
                        val match = revs.find { it.weekStart == ws }
                        wins.value = match?.wins.orEmpty()
                        mistakes.value = match?.mistakes.orEmpty()
                        learnings.value = match?.learnings.orEmpty()
                        aiSummary.value = match?.aiSummary.orEmpty()
                    }
                }
        }
    }

    fun consumeMessage() {
        flash.value = null
    }

    fun setWins(v: String) {
        wins.value = v
    }

    fun setMistakes(v: String) {
        mistakes.value = v
    }

    fun setLearnings(v: String) {
        learnings.value = v
    }

    fun shiftWeek(deltaWeeks: Long) {
        val iso = DateTimeFormatter.ISO_LOCAL_DATE
        val nextMonday = LocalDate.parse(weekStart.value, iso).plusWeeks(deltaWeeks)
        weekStart.value = nextMonday.format(iso)
    }

    fun save() {
        val u = uid.value ?: return
        viewModelScope.launch {
            saveBusy.value = true
            try {
                weeklyReviewRepository.saveReview(
                    u,
                    WeeklyReviewItem(
                        weekStart = weekStart.value,
                        wins = wins.value,
                        mistakes = mistakes.value,
                        learnings = learnings.value,
                        aiSummary = aiSummary.value.takeIf { it.isNotBlank() },
                    ),
                )
                flash.value = "Weekly review saved."
            } catch (e: Exception) {
                flash.value = e.message ?: "Save failed"
            } finally {
                saveBusy.value = false
            }
        }
    }

    fun runAiSummary() {
        val s = uiState.value
        viewModelScope.launch {
            aiBusy.value = true
            val tasksSummary = s.completedTasks.joinToString("; ") { t ->
                "${t.title} (impact ${t.impactScore})"
            }.ifBlank { "None logged" }
            val decSummary = s.weekDecisions.joinToString("; ") { d ->
                d.title
            }.ifBlank { "None logged" }
            val plannedGoalsSummary = buildString {
                append("Quarter: ")
                append(s.quarterGoals.joinToString(" | ") { it.title }.ifBlank { "none" })
                append("; Month: ")
                append(s.monthGoals.joinToString(" | ") { it.title }.ifBlank { "none" })
                append("; Week: ")
                append(s.weekGoals.joinToString(" | ") { it.title }.ifBlank { "none" })
            }
            val result = aiService.weeklySummary(
                completedTasksSummary = tasksSummary,
                decisionsSummary = decSummary,
                plannedGoalsSummary = plannedGoalsSummary,
                wins = s.wins,
                mistakes = s.mistakes,
                learnings = s.learnings,
            )
            aiBusy.value = false
            result.fold(
                onSuccess = {
                    aiSummary.value = it
                },
                onFailure = {
                    flash.value = it.message ?: "AI failed"
                },
            )
        }
    }
}
