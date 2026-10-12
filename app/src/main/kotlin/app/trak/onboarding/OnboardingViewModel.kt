package app.trak.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trak.core.data.TimeSource
import app.trak.core.data.profile.ProfileRepository
import app.trak.core.data.profile.SaveResult
import app.trak.core.model.PrimaryRole
import app.trak.core.model.ProfileInput
import app.trak.core.model.UserId
import app.trak.core.model.validation.ValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val name: String = "",
    val role: PrimaryRole? = null,
    val adultConfirmed: Boolean = false,
    val saving: Boolean = false,
    val errors: List<ValidationError> = emptyList(),
) {
    /** Continue is enabled only when the three required answers are present (test A12). */
    val canContinue: Boolean get() = name.isNotBlank() && role != null && adultConfirmed && !saving
}

/**
 * Onboarding asks only name, main role and the 18+ confirmation. Saving writes locally first; when
 * the local profile exists the root moves to the main tabs on its own.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val clock: TimeSource,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, errors = emptyList()) }
    fun onRoleChange(v: PrimaryRole?) = _state.update { it.copy(role = v, errors = emptyList()) }
    fun onAdultChange(v: Boolean) = _state.update { it.copy(adultConfirmed = v, errors = emptyList()) }

    fun save(userId: UserId) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val input = ProfileInput(
                displayName = s.name,
                primaryRole = s.role,
                timeZoneId = deviceZoneId(),
                adultConfirmedAt = if (s.adultConfirmed) clock.now() else null,
            )
            when (val result = profiles.save(userId, input)) {
                is SaveResult.Invalid -> _state.update { it.copy(saving = false, errors = result.errors) }
                SaveResult.Saved -> profiles.requestSync(userId)
            }
        }
    }
}

/** The device's region time zone, or UTC when the device reports a raw offset. */
internal fun deviceZoneId(): String =
    ZoneId.systemDefault().id.takeIf { it in ZoneId.getAvailableZoneIds() } ?: "UTC"
