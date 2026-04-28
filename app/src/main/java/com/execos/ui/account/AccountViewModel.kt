package com.execos.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.execos.BuildConfig
import com.execos.data.repo.AuthRepository
import com.execos.data.strava.StravaOAuth
import com.execos.data.strava.StravaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val currentEmail: String? = null,
    val email: String = "",
    val password: String = "",
    val busy: Boolean = false,
    val stravaConnected: Boolean = false,
    val lastSync: String? = null,
    val message: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val stravaOAuth: StravaOAuth,
    private val stravaRepository: StravaRepository,
) : ViewModel() {
    private val email = MutableStateFlow("")
    private val password = MutableStateFlow("")
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val currentEmail = MutableStateFlow<String?>(null)
    private val stravaConnected = MutableStateFlow(false)
    private val lastSync = MutableStateFlow<String?>(null)

    private val authIntents = Channel<android.content.Intent>(capacity = Channel.BUFFERED)
    val stravaAuthIntents = authIntents.receiveAsFlow()

    private data class AccountCore(
        val currentEmail: String?,
        val stravaConnected: Boolean,
        val lastSync: String?,
    )

    private data class AccountFields(
        val email: String,
        val password: String,
    )

    private data class AccountFlags(
        val busy: Boolean,
        val message: String?,
    )

    val uiState: StateFlow<AccountUiState> = combine(
        combine(currentEmail, stravaConnected, lastSync) { ce, sc, ls ->
            AccountCore(ce, sc, ls)
        },
        combine(email, password) { e, p ->
            AccountFields(e, p)
        },
        combine(busy, message) { b, m ->
            AccountFlags(b, m)
        },
    ) { core, fields, flags ->
        AccountUiState(
            currentEmail = core.currentEmail,
            email = fields.email,
            password = fields.password,
            busy = flags.busy,
            stravaConnected = core.stravaConnected,
            lastSync = core.lastSync,
            message = flags.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountUiState(),
    )

    init {
        refreshCurrent()
        refreshStrava()
    }

    fun consumeMessage() {
        message.value = null
    }

    fun setEmail(v: String) {
        email.value = v
    }

    fun setPassword(v: String) {
        password.value = v
    }

    fun refreshCurrent() {
        viewModelScope.launch {
            currentEmail.value = authRepository.currentUserEmail()
        }
    }

    fun refreshStrava() {
        viewModelScope.launch {
            stravaConnected.value = stravaRepository.isConnected()
        }
    }

    fun register() {
        val e = email.value
        val p = password.value.toCharArray()
        viewModelScope.launch {
            busy.value = true
            try {
                authRepository.register(e, p).fold(
                    onSuccess = {
                        message.value = "Account created."
                        password.value = ""
                        refreshCurrent()
                        refreshStrava()
                    },
                    onFailure = { message.value = it.message ?: "Register failed" },
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun login() {
        val e = email.value
        val p = password.value.toCharArray()
        viewModelScope.launch {
            busy.value = true
            try {
                authRepository.login(e, p).fold(
                    onSuccess = {
                        message.value = "Signed in."
                        password.value = ""
                        refreshCurrent()
                        refreshStrava()
                    },
                    onFailure = { message.value = it.message ?: "Login failed" },
                )
            } finally {
                busy.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            busy.value = true
            try {
                authRepository.logout()
                message.value = "Signed out."
                refreshCurrent()
                refreshStrava()
            } finally {
                busy.value = false
            }
        }
    }

    /**
     * OAuth needs an Activity context to launch the browser.
     */
    fun connectStrava() {
        viewModelScope.launch {
            if (BuildConfig.STRAVA_CLIENT_ID.isBlank() || BuildConfig.STRAVA_CLIENT_SECRET.isBlank()) {
                message.value = "Strava is not configured. Add STRAVA_CLIENT_ID and STRAVA_CLIENT_SECRET in local.properties."
                return@launch
            }
            val req = stravaOAuth.buildAuthRequest()
            val intent = stravaOAuth.authorizationService().getAuthorizationRequestIntent(req)
            authIntents.trySend(intent)
        }
    }

    fun handleStravaRedirect(data: android.content.Intent?) {
        viewModelScope.launch {
            if (data == null) {
                message.value = "Strava sign-in cancelled."
                return@launch
            }
            val resp = net.openid.appauth.AuthorizationResponse.fromIntent(data)
            val ex = net.openid.appauth.AuthorizationException.fromIntent(data)
            if (ex != null) {
                message.value = ex.errorDescription ?: "Strava auth failed"
                return@launch
            }
            val code = resp?.authorizationCode
            if (code.isNullOrBlank()) {
                message.value = "Missing Strava auth code"
                return@launch
            }
            busy.value = true
            try {
                stravaRepository.exchangeCodeAndStore(code)
                stravaConnected.value = true
                message.value = "Strava connected."
            } catch (e: Exception) {
                message.value = "Strava connect failed: ${e.message ?: "unknown error"}"
            } finally {
                busy.value = false
            }
        }
    }

    fun syncStrava() {
        viewModelScope.launch {
            busy.value = true
            try {
                val n = stravaRepository.syncRecentActivities(daysBack = 30)
                lastSync.value = "Synced $n activities (last 30 days)."
                message.value = lastSync.value
            } catch (e: Exception) {
                message.value = e.message ?: "Sync failed"
            } finally {
                busy.value = false
            }
        }
    }

    fun disconnectStrava() {
        viewModelScope.launch {
            busy.value = true
            try {
                stravaRepository.disconnect()
                stravaConnected.value = false
                lastSync.value = null
                message.value = "Strava disconnected."
            } finally {
                busy.value = false
            }
        }
    }
}

