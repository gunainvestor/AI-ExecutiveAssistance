package com.execos.ui.home

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

data class HomeUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val todayTasks: List<TaskItem> = emptyList(),
    val weekGoals: List<GoalItem> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val taskRepository: TaskRepository,
    private val goalRepository: GoalRepository,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val today = Dates.todayIso()
    private val weekStart = Dates.weekStartIso()

    val uiState: StateFlow<HomeUiState> = combine(
        authError,
        uid,
        uid.flatMapLatest { u ->
            if (u == null) flowOf(emptyList())
            else taskRepository.observeTasksForDate(u, today)
        },
        uid.flatMapLatest { u ->
            if (u == null) flowOf(emptyList())
            else goalRepository.observeGoals(u, GoalPeriod.WEEK, weekStart)
        },
    ) { err, u, tasks, goals ->
        HomeUiState(
            loading = err == null && u == null,
            error = err,
            todayTasks = tasks,
            weekGoals = goals,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
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
