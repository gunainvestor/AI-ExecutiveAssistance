package com.execos.ui.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.ReflectionItem
import com.execos.data.remote.AiService
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.ReflectionRepository
import com.execos.util.Dates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReflectionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val input: String = "",
    val aiOutput: String = "",
    val busy: Boolean = false,
    val recent: List<ReflectionItem> = emptyList(),
    val message: String? = null,
)

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val reflectionRepository: ReflectionRepository,
    private val aiService: AiService,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val input = MutableStateFlow("")
    private val aiOut = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)

    private val recentFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else reflectionRepository.observeRecent(u)
    }

    private data class ReflectionCore(
        val error: String?,
        val uid: String?,
        val recent: List<ReflectionItem>,
    )

    val uiState: StateFlow<ReflectionUiState> = combine(
        combine(authError, uid, recentFlow) { err, u, recent ->
            ReflectionCore(err, u, recent)
        },
        input,
        aiOut,
        busy,
        flash,
    ) { core, inp, out, b, msg ->
        ReflectionUiState(
            loading = core.error == null && core.uid == null,
            error = core.error,
            input = inp,
            aiOutput = out,
            busy = b,
            recent = core.recent,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReflectionUiState(),
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

    fun setInput(value: String) {
        input.value = value
    }

    fun generate() {
        val text = input.value.trim()
        if (text.isBlank()) {
            flash.value = "Describe what you did today."
            return
        }
        viewModelScope.launch {
            busy.value = true
            aiOut.value = ""
            try {
                aiService.reflectionInsightsStream(text)
                    .catch { e ->
                        flash.value = e.message ?: "AI failed"
                    }
                    .collect { chunk ->
                        aiOut.value += chunk
                    }
            } finally {
                busy.value = false
            }
        }
    }

    fun save() {
        val u = uid.value
        if (u == null) {
            flash.value = "Not signed in."
            return
        }
        val text = input.value.trim()
        val ai = aiOut.value.trim()
        if (text.isBlank() && ai.isBlank()) {
            flash.value = "Nothing to save."
            return
        }
        viewModelScope.launch {
            try {
                reflectionRepository.saveReflection(
                    u,
                    ReflectionItem(
                        textInput = text,
                        aiOutput = ai,
                        date = Dates.todayIso(),
                    ),
                )
                flash.value = "Saved reflection."
                input.value = ""
                aiOut.value = ""
            } catch (e: Exception) {
                flash.value = e.message ?: "Save failed"
            }
        }
    }
}
