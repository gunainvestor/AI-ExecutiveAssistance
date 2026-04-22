package com.execos.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.GoalItem
import com.execos.data.model.GoalPeriod
import com.execos.data.model.TaskItem
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.GoalRepository
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
import javax.inject.Inject

data class FocusUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val date: String = Dates.todayIso(),
    val tasks: List<TaskItem> = emptyList(),
    val yearGoals: List<GoalItem> = emptyList(),
    val quarterGoals: List<GoalItem> = emptyList(),
    val monthGoals: List<GoalItem> = emptyList(),
    val weekGoals: List<GoalItem> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class FocusPlannerViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val dateFlow = MutableStateFlow(Dates.todayIso())
    private val busy = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)

    private val tasksFlow = combine(uid, dateFlow) { u, d -> u to d }
        .flatMapLatest { (u, d) ->
            if (u == null) flowOf(emptyList())
            else taskRepository.observeTasksForDate(u, d)
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
    private val weekGoalsFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList()) else goalRepository.observeGoals(u, GoalPeriod.WEEK, Dates.weekStartIso())
    }

    private data class FocusCore(
        val error: String?,
        val uid: String?,
        val date: String,
        val busy: Boolean,
        val message: String?,
    )

    private data class GoalsPack(
        val tasks: List<TaskItem>,
        val yearGoals: List<GoalItem>,
        val quarterGoals: List<GoalItem>,
        val monthGoals: List<GoalItem>,
        val weekGoals: List<GoalItem>,
    )

    val uiState: StateFlow<FocusUiState> = combine(
        combine(authError, uid, dateFlow, busy, flash) { err, u, date, b, msg ->
            FocusCore(err, u, date, b, msg)
        },
        combine(tasksFlow, yearGoalsFlow, quarterGoalsFlow, monthGoalsFlow, weekGoalsFlow) { tasks, yGoals, qGoals, mGoals, wGoals ->
            GoalsPack(tasks, yGoals, qGoals, mGoals, wGoals)
        },
    ) { core, goals ->
        FocusUiState(
            loading = core.error == null && core.uid == null,
            error = core.error,
            date = core.date,
            tasks = goals.tasks,
            yearGoals = goals.yearGoals,
            quarterGoals = goals.quarterGoals,
            monthGoals = goals.monthGoals,
            weekGoals = goals.weekGoals,
            busy = core.busy,
            message = core.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FocusUiState(),
    )

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

    fun addTask() {
        val u = uid.value ?: return
        val current = uiState.value.tasks
        if (current.size >= 3) {
            flash.value = "You can only track 3 priorities per day."
            return
        }
        viewModelScope.launch {
            busy.value = true
            try {
                taskRepository.saveTask(
                    u,
                    TaskItem(
                        title = "New priority",
                        impactScore = 3,
                        notes = "",
                        completed = false,
                        date = dateFlow.value,
                    ),
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun updateTask(task: TaskItem) {
        val u = uid.value ?: return
        viewModelScope.launch {
            try {
                taskRepository.saveTask(u, task)
            } catch (e: Exception) {
                flash.value = e.message ?: "Could not save task"
            }
        }
    }

    fun toggleComplete(task: TaskItem) {
        val u = uid.value ?: return
        viewModelScope.launch {
            busy.value = true
            try {
                taskRepository.saveTask(u, task.copy(completed = !task.completed))
            } finally {
                busy.value = false
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        val u = uid.value ?: return
        if (task.id.isBlank()) return
        viewModelScope.launch {
            busy.value = true
            try {
                taskRepository.deleteTask(u, task.id)
            } finally {
                busy.value = false
            }
        }
    }
}
