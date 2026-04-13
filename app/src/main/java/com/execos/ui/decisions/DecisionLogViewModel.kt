package com.execos.ui.decisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.DecisionItem
import com.execos.data.remote.AiService
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.DecisionRepository
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

data class DecisionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val decisions: List<DecisionItem> = emptyList(),
    val aiBusy: Boolean = false,
    val draft: DecisionItem = DecisionItem(date = Dates.todayIso()),
    val editorOpen: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class DecisionLogViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val decisionRepository: DecisionRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val aiBusy = MutableStateFlow(false)
    private val draft = MutableStateFlow(DecisionItem(date = Dates.todayIso()))
    private val editorOpen = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)

    private val listFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else decisionRepository.observeDecisions(u)
    }

    private data class DecisionCore(
        val error: String?,
        val uid: String?,
        val list: List<DecisionItem>,
        val aiBusy: Boolean,
    )

    val uiState: StateFlow<DecisionUiState> = combine(
        combine(authError, uid, listFlow, aiBusy) { err, u, list, ai ->
            DecisionCore(err, u, list, ai)
        },
        draft,
        editorOpen,
        flash,
    ) { core, d, open, msg ->
        DecisionUiState(
            loading = core.error == null && core.uid == null,
            error = core.error,
            decisions = core.list,
            aiBusy = core.aiBusy,
            draft = d,
            editorOpen = open,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DecisionUiState(),
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

    fun openNew() {
        draft.value = DecisionItem(date = Dates.todayIso())
        editorOpen.value = true
    }

    fun openEdit(item: DecisionItem) {
        draft.value = item
        editorOpen.value = true
    }

    fun dismissEditor() {
        editorOpen.value = false
    }

    fun updateDraft(transform: (DecisionItem) -> DecisionItem) {
        draft.value = transform(draft.value)
    }

    fun saveDraft() {
        val u = uid.value ?: return
        val d = draft.value
        if (d.title.isBlank()) {
            flash.value = "Title is required."
            return
        }
        viewModelScope.launch {
            try {
                decisionRepository.saveDecision(u, d)
                editorOpen.value = false
            } catch (e: Exception) {
                flash.value = e.message ?: "Save failed"
            }
        }
    }

    fun runAiOnDraft() {
        val d = draft.value
        if (d.title.isBlank()) {
            flash.value = "Add a title before running AI."
            return
        }
        viewModelScope.launch {
            aiBusy.value = true
            val result = aiService.decisionEvaluation(
                title = d.title,
                context = d.context,
                options = d.options,
                decision = d.finalDecision,
                confidence = d.confidence,
            )
            aiBusy.value = false
            result.fold(
                onSuccess = { text ->
                    draft.value = d.copy(aiAnalysis = text)
                },
                onFailure = { e ->
                    flash.value = e.message ?: "AI request failed"
                },
            )
        }
    }
}
