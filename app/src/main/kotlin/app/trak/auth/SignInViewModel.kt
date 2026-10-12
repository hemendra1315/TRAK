package app.trak.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trak.core.data.auth.AuthGateway
import app.trak.core.data.auth.AuthResult
import app.trak.core.model.validation.AuthRules
import app.trak.core.model.validation.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SignInStep { EMAIL, CODE }

enum class SignInError { EMAIL_INVALID, CODE_INVALID, CODE_WRONG, RATE_LIMITED, NETWORK, NOT_CONFIGURED, OTHER }

data class SignInUiState(
    val step: SignInStep = SignInStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val busy: Boolean = false,
    val error: SignInError? = null,
    val errorDetail: String? = null,
    val resendSeconds: Int = 0,
)

/** Email-code sign-in. On success the auth state changes and the root routes onward. */
@HiltViewModel
class SignInViewModel @Inject constructor(private val auth: AuthGateway) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()
    private var countdown: Job? = null

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, error = null) }
    fun onCodeChange(value: String) = _state.update { it.copy(code = value.filter(Char::isDigit).take(6), error = null) }
    fun backToEmail() = _state.update { it.copy(step = SignInStep.EMAIL, code = "", error = null) }

    fun sendCode() {
        val email = when (val v = AuthRules.email(_state.value.email)) {
            is ValidationResult.Invalid -> return _state.update { it.copy(error = SignInError.EMAIL_INVALID) }
            is ValidationResult.Valid -> v.value
        }
        run(onSuccess = {
            _state.update { it.copy(step = SignInStep.CODE, email = email, code = "") }
            startCountdown()
        }) { auth.sendCode(email) }
    }

    fun verify() {
        val code = when (val v = AuthRules.code(_state.value.code)) {
            is ValidationResult.Invalid -> return _state.update { it.copy(error = SignInError.CODE_INVALID) }
            is ValidationResult.Valid -> v.value
        }
        run(onSuccess = {}) { auth.verifyCode(_state.value.email, code) }
    }

    fun resend() {
        if (_state.value.resendSeconds > 0) return
        run(onSuccess = { startCountdown() }) { auth.sendCode(_state.value.email) }
    }

    private fun run(onSuccess: () -> Unit, call: suspend () -> AuthResult) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val result = call()
            _state.update { it.copy(busy = false) }
            when (result) {
                AuthResult.Success -> onSuccess()
                AuthResult.InvalidOrExpiredCode -> _state.update { it.copy(error = SignInError.CODE_WRONG) }
                AuthResult.RateLimited -> _state.update { it.copy(error = SignInError.RATE_LIMITED) }
                AuthResult.Network -> _state.update { it.copy(error = SignInError.NETWORK) }
                AuthResult.NotConfigured -> _state.update { it.copy(error = SignInError.NOT_CONFIGURED) }
                is AuthResult.Error -> _state.update { it.copy(error = SignInError.OTHER, errorDetail = result.message) }
            }
        }
    }

    private fun startCountdown() {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            for (s in RESEND_SECONDS downTo 0) {
                _state.update { it.copy(resendSeconds = s) }
                if (s > 0) delay(1_000)
            }
        }
    }

    private companion object {
        const val RESEND_SECONDS = 60
    }
}
