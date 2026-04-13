package com.execos.ui.energy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.data.model.EnergyEntry
import com.execos.data.repo.AuthRepository
import com.execos.data.repo.EnergyRepository
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

data class EnergyUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val today: String = Dates.todayIso(),
    val morning: Int = 3,
    val evening: Int = 3,
    val history: List<EnergyEntry> = emptyList(),
    val saveBusy: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class EnergyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val energyRepository: EnergyRepository,
) : ViewModel() {
    private val uid = MutableStateFlow<String?>(null)
    private val authError = MutableStateFlow<String?>(null)
    private val morning = MutableStateFlow(3)
    private val evening = MutableStateFlow(3)
    private val saveBusy = MutableStateFlow(false)
    private val flash = MutableStateFlow<String?>(null)
    private val today = Dates.todayIso()

    private val historyFlow = uid.flatMapLatest { u ->
        if (u == null) flowOf(emptyList())
        else energyRepository.observeRecent(u)
    }

    private data class EnergyCore(
        val error: String?,
        val uid: String?,
        val history: List<EnergyEntry>,
    )

    val uiState: StateFlow<EnergyUiState> = combine(
        combine(authError, uid, historyFlow) { err, u, hist ->
            EnergyCore(err, u, hist)
        },
        morning,
        evening,
        saveBusy,
        flash,
    ) { core, m, e, sBusy, msg ->
        EnergyUiState(
            loading = core.error == null && core.uid == null,
            error = core.error,
            today = today,
            morning = m,
            evening = e,
            history = core.history,
            saveBusy = sBusy,
            message = msg,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EnergyUiState(),
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

    fun setMorning(v: Int) {
        morning.value = v.coerceIn(1, 5)
    }

    fun setEvening(v: Int) {
        evening.value = v.coerceIn(1, 5)
    }

    fun saveToday() {
        val u = uid.value ?: return
        viewModelScope.launch {
            saveBusy.value = true
            try {
                energyRepository.saveEnergy(
                    u,
                    EnergyEntry(
                        morningScore = morning.value,
                        eveningScore = evening.value,
                        date = today,
                    ),
                )
                flash.value = "Energy logged."
            } catch (e: Exception) {
                flash.value = e.message ?: "Save failed"
            } finally {
                saveBusy.value = false
            }
        }
    }
}
