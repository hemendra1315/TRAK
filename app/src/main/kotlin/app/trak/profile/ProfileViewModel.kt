package app.trak.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trak.core.data.profile.LocalProfile
import app.trak.core.data.profile.ProfileRepository
import app.trak.core.data.network.NetworkMonitor
import app.trak.core.data.profile.SaveResult
import app.trak.core.data.profile.SyncOutcome
import app.trak.core.data.session.SessionCoordinator
import app.trak.core.data.session.SignOutCheck
import app.trak.core.data.settings.UserSettings
import app.trak.core.model.AppTheme
import app.trak.core.model.BattingHand
import app.trak.core.model.BowlingStyle
import app.trak.core.model.PrimaryRole
import app.trak.core.model.Profile
import app.trak.core.model.ProfileInput
import app.trak.core.model.UserId
import app.trak.core.model.WeightUnit
import app.trak.core.model.validation.ValidationError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Sign-out dialog state (decision S1: unsynced changes are never discarded silently). */
sealed interface SignOutUi {
    data object None : SignOutUi
    data class Unsynced(val count: Int) : SignOutUi
    data class ConfirmDelete(val typed: String = "", val wrong: Boolean = false) : SignOutUi
}

/** Result of tapping Sync now that the status line alone can't show. */
enum class SyncNotice {
    /** The attempt didn't reach the server (offline or session not usable). Data is still on the phone. */
    NOT_REACHED,
}

/**
 * Sync now must never look like it worked when it didn't. Synced and refused outcomes are already
 * shown by the status line (SYNCED, or FAILED with the server's reason); only "still pending after
 * an attempt" needs an extra message, otherwise the tap would appear to do nothing.
 */
fun syncNoticeFor(outcome: SyncOutcome): SyncNotice? = when (outcome) {
    SyncOutcome.StillPending -> SyncNotice.NOT_REACHED
    SyncOutcome.Synced, SyncOutcome.AdoptedServerCopy, SyncOutcome.NothingToSync, is SyncOutcome.Failed -> null
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val coordinator: SessionCoordinator,
    private val settings: UserSettings,
    network: NetworkMonitor,
) : ViewModel() {

    private val userId = MutableStateFlow<UserId?>(null)

    init {
        // Retry a pending profile automatically whenever the phone comes back online (test B).
        viewModelScope.launch {
            userId.filterNotNull().collectLatest { id -> profiles.syncWhenOnline(id, network.isOnline) }
        }
    }

    val profile: StateFlow<LocalProfile?> = userId.filterNotNull()
        .flatMapLatest { profiles.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val theme: StateFlow<AppTheme> = settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DARK)

    private val _editErrors = MutableStateFlow<List<ValidationError>>(emptyList())
    val editErrors: StateFlow<List<ValidationError>> = _editErrors.asStateFlow()

    private val _syncNotice = MutableStateFlow<SyncNotice?>(null)
    val syncNotice: StateFlow<SyncNotice?> = _syncNotice.asStateFlow()

    private val _signOut = MutableStateFlow<SignOutUi>(SignOutUi.None)
    val signOut: StateFlow<SignOutUi> = _signOut.asStateFlow()

    fun bind(id: UserId) { userId.value = id }

    /** Edit profile: returns true when saved locally (sync follows in the background). */
    fun saveProfile(
        name: String,
        role: PrimaryRole?,
        hand: BattingHand?,
        styles: Set<BowlingStyle>,
        onSaved: () -> Unit,
    ) = update(onSaved) { it.copy(displayName = name, primaryRole = role, battingHand = hand, bowlingStyles = styles) }

    fun setWeekStart(day: DayOfWeek) = update { it.copy(weekStart = day) }
    fun setWeightUnit(unit: WeightUnit) = update { it.copy(weightUnit = unit) }
    fun setTheme(theme: AppTheme) = viewModelScope.launch { settings.setTheme(theme) }

    fun syncNow() {
        val id = userId.value ?: return
        viewModelScope.launch {
            _syncNotice.value = null
            _syncNotice.value = syncNoticeFor(profiles.sync(id, includeFailed = true))
        }
    }

    private fun update(onSaved: () -> Unit = {}, change: (ProfileInput) -> ProfileInput) {
        val id = userId.value ?: return
        val current = profile.value?.profile ?: return
        viewModelScope.launch {
            when (val r = profiles.save(id, change(current.toInput()))) {
                is SaveResult.Invalid -> _editErrors.value = r.errors
                SaveResult.Saved -> {
                    _editErrors.value = emptyList()
                    profiles.requestSync(id)
                    onSaved()
                }
            }
        }
    }

    // --- Sign-out (decision S1) ---

    fun requestSignOut() = viewModelScope.launch {
        when (val check = coordinator.signOut()) {
            SignOutCheck.SignedOut -> _signOut.value = SignOutUi.None
            is SignOutCheck.HasUnsyncedChanges -> _signOut.value = SignOutUi.Unsynced(check.count)
        }
    }

    fun dismissSignOut() { _signOut.value = SignOutUi.None }

    fun syncThenRetrySignOut() {
        val id = userId.value ?: return
        viewModelScope.launch {
            profiles.sync(id, includeFailed = true)
            requestSignOut()
        }
    }

    fun keepAndSignOut() = viewModelScope.launch {
        _signOut.value = SignOutUi.None
        coordinator.signOutKeepingData()
    }

    fun startDelete() { _signOut.value = SignOutUi.ConfirmDelete() }

    fun onDeleteTyped(text: String) = _signOut.update { if (it is SignOutUi.ConfirmDelete) it.copy(typed = text, wrong = false) else it }

    fun confirmDelete() = viewModelScope.launch {
        val s = _signOut.value as? SignOutUi.ConfirmDelete ?: return@launch
        if (coordinator.deleteUnsyncedAndSignOut(s.typed)) _signOut.value = SignOutUi.None
        else _signOut.value = s.copy(wrong = true)
    }
}

private fun Profile.toInput() = ProfileInput(
    displayName = displayName,
    primaryRole = primaryRole,
    battingHand = battingHand,
    bowlingStyles = bowlingStyles,
    weekStart = weekStart,
    weightUnit = weightUnit,
    timeZoneId = timeZone.id,
    adultConfirmedAt = adultConfirmedAt,
)
