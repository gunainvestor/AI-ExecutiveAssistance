package com.execos.ui.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.TaskItem
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.DecisionRepository
import com.execos.data.repo.ReflectionRepository
import com.execos.data.repo.TaskRepository
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

data class CalendarDayDetailsUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val dateIso: String = "",
    val tasksCount: Int = 0,
    val decisionsCount: Int = 0,
    val reflectionsCount: Int = 0,
    val plannedGoals: List<String> = emptyList(),
    val achievedGoals: List<String> = emptyList(),
)

@HiltViewModel
class CalendarDayDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val decisionRepository: DecisionRepository,
    private val reflectionRepository: ReflectionRepository,
) : ViewModel() {
    private val dateIso = MutableStateFlow(savedStateHandle.get<String>("dateIso").orEmpty())
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)

    private val dayScope = combine(uid, dateIso) { u, d -> u to d }

    private val tasksFlow = dayScope.flatMapLatest { (u, d) ->
        if (u == null || d.isBlank()) flowOf(emptyList()) else taskRepository.observeTasksForDate(u, d)
    }

    private val decisionsFlow = dayScope.flatMapLatest { (u, d) ->
        if (u == null || d.isBlank()) flowOf(emptyList())
        else decisionRepository.observeDecisionsInRange(u, d, d)
    }

    private val reflectionsFlow = dayScope.flatMapLatest { (u, d) ->
        if (u == null || d.isBlank()) flowOf(emptyList())
        else reflectionRepository.observeRecent(u, limit = 200)
    }

    private data class Header(
        val error: String?,
        val uid: String?,
        val dateIso: String,
    )

    private data class DailyData(
        val tasks: List<TaskItem>,
        val decisionsCount: Int,
        val reflectionsCount: Int,
    )

    private val headerFlow = combine(authError, uid, dateIso) { err, userId, selectedDate ->
        Header(error = err, uid = userId, dateIso = selectedDate)
    }

    private val dailyDataFlow = combine(tasksFlow, decisionsFlow, reflectionsFlow, dateIso) { tasks, decisions, reflections, selectedDate ->
        val dayReflections = reflections.filter { it.date == selectedDate }
        DailyData(
            tasks = tasks,
            decisionsCount = decisions.size,
            reflectionsCount = dayReflections.size,
        )
    }

    val uiState: StateFlow<CalendarDayDetailsUiState> = combine(headerFlow, dailyDataFlow) { header, daily ->
        val planned = daily.tasks.filterNot { it.completed }.map(TaskItem::title).filter { it.isNotBlank() }
        val achieved = daily.tasks.filter { it.completed }.map(TaskItem::title).filter { it.isNotBlank() }

        CalendarDayDetailsUiState(
            loading = header.error == null && header.uid == null,
            error = header.error,
            dateIso = header.dateIso,
            tasksCount = daily.tasks.size,
            decisionsCount = daily.decisionsCount,
            reflectionsCount = daily.reflectionsCount,
            plannedGoals = planned,
            achievedGoals = achieved,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarDayDetailsUiState(),
    )

    init {
        viewModelScope.launch {
            authRepository.ensureSignedIn().fold(
                onSuccess = { uid.value = it },
                onFailure = { authError.value = it.message ?: "Sign-in failed" },
            )
        }
    }
}
