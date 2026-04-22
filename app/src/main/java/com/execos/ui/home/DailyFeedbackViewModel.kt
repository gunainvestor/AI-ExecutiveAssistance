package com.execos.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.repo.DailyFeedback
import com.execos.data.repo.DailyFeedbackStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyFeedbackViewModel @Inject constructor(
    private val store: DailyFeedbackStore,
) : ViewModel() {
    val popup: StateFlow<DailyFeedback?> =
        store.todayUnseenFeedback.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    fun markShown(feedback: DailyFeedback) {
        viewModelScope.launch {
            store.markShown(feedback.date)
        }
    }
}

