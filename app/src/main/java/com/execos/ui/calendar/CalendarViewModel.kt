package com.execos.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.DecisionItem
import com.execos.data.model.ReflectionItem
import com.execos.data.model.TaskItem
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.DecisionRepository
import com.execos.data.repo.ReflectionRepository
import com.execos.data.repo.TaskRepository
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
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class CalendarUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val monthLabel: String = "",
    val yearMonth: YearMonth = YearMonth.now(),
    val selectedDateIso: String = Dates.todayIso(),
    val dayTaskCount: Map<String, Int> = emptyMap(),
    val dayDecisionCount: Map<String, Int> = emptyMap(),
    val dayReflectionCount: Map<String, Int> = emptyMap(),
    val selectedDayPlannedGoals: List<String> = emptyList(),
    val selectedDayAchievedGoals: List<String> = emptyList(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val decisionRepository: DecisionRepository,
    private val reflectionRepository: ReflectionRepository,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)

    private val yearMonth = MutableStateFlow(YearMonth.now())
    private val selectedDateIso = MutableStateFlow(Dates.todayIso())

    private val monthRange = yearMonth.combine(uid) { ym, u -> ym to u }

    private val tasksFlow = monthRange.flatMapLatest { (ym, u) ->
        if (u == null) flowOf(emptyList())
        else taskRepository.observeTasksInRange(u, ym.atDay(1).toString(), ym.atEndOfMonth().toString())
    }

    private val decisionsFlow = monthRange.flatMapLatest { (ym, u) ->
        if (u == null) flowOf(emptyList())
        else decisionRepository.observeDecisionsInRange(u, ym.atDay(1).toString(), ym.atEndOfMonth().toString())
    }

    // Reflection repository currently exposes "recent" only; we approximate per-month by filtering recent.
    private val reflectionsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else reflectionRepository.observeRecent(u, limit = 200)
    }

    private val selectedDayTasksFlow = selectedDateIso.combine(uid) { d, u -> d to u }.flatMapLatest { (d, u) ->
        if (u == null) flowOf(emptyList())
        else taskRepository.observeTasksForDate(u, d)
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        authError,
        yearMonth,
        selectedDateIso,
        tasksFlow,
        decisionsFlow,
        reflectionsFlow,
        selectedDayTasksFlow,
    ) { parts ->
        val err = parts[0] as String?
        val ym = parts[1] as YearMonth
        val selected = parts[2] as String
        val tasks = parts[3] as? List<TaskItem> ?: emptyList()
        val decisions = parts[4] as? List<DecisionItem> ?: emptyList()
        val reflections = parts[5] as? List<ReflectionItem> ?: emptyList()
        val selectedDayTasks = parts[6] as? List<TaskItem> ?: emptyList()
        val selectedPlanned = selectedDayTasks
            .filterNot { it.completed }
            .map { it.title }
            .filter { it.isNotBlank() }
        val selectedAchieved = selectedDayTasks
            .filter { it.completed }
            .map { it.title }
            .filter { it.isNotBlank() }

        val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        val taskCount = tasks.groupBy { it.date }.mapValues { it.value.size }
        val decisionCount = decisions.groupBy { it.date }.mapValues { it.value.size }
        val reflectionCount = reflections
            .filter { r ->
                runCatching {
                    val d = LocalDate.parse(r.date, DateTimeFormatter.ISO_LOCAL_DATE)
                    !d.isBefore(monthStart) && !d.isAfter(monthEnd)
                }.getOrDefault(false)
            }
            .groupBy { it.date }
            .mapValues { it.value.size }

        CalendarUiState(
            loading = err == null && uid.value == null,
            error = err,
            monthLabel = ym.format(monthFmt),
            yearMonth = ym,
            selectedDateIso = selected,
            dayTaskCount = taskCount,
            dayDecisionCount = decisionCount,
            dayReflectionCount = reflectionCount,
            selectedDayPlannedGoals = selectedPlanned,
            selectedDayAchievedGoals = selectedAchieved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(),
    )

    init {
        viewModelScope.launch {
            authRepository.ensureSignedIn().fold(
                onSuccess = { uid.value = it },
                onFailure = { authError.value = it.message ?: "Sign-in failed" },
            )
        }
    }

    fun prevMonth() {
        yearMonth.value = yearMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        yearMonth.value = yearMonth.value.plusMonths(1)
    }

    fun selectDate(iso: String) {
        selectedDateIso.value = iso
    }
}

