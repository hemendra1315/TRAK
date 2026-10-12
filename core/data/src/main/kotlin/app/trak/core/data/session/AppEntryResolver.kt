package app.trak.core.data.session

import app.trak.core.data.auth.AuthGateway
import app.trak.core.data.auth.AuthState
import app.trak.core.data.profile.ProfileRepository
import app.trak.core.data.profile.RestoreResult
import app.trak.core.model.UserId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest

/** Where the app should be, derived only from local state and the auth session. */
sealed interface AppEntryState {
    data object Loading : AppEntryState
    data class SignedOut(val byUser: Boolean) : AppEntryState
    /** Another account's unsynced data is on this phone (review fix B3). */
    data class BlockedByOtherAccount(val userId: UserId, val unsyncedCount: Int) : AppEntryState
    /** New device, no local profile, and the server can't be reached to check for one. */
    data class ProfileCheckOffline(val userId: UserId) : AppEntryState
    data class NeedsOnboarding(val userId: UserId) : AppEntryState
    data class Ready(val userId: UserId, val offline: Boolean) : AppEntryState
}

/**
 * Routing rules (plan section 7, steps 8–10):
 * no session → sign-in; another account's unsynced data → guard; no local profile → restore from the
 * server, else onboarding; otherwise the main tabs. A known session works offline (local-first).
 * Onboarding is never offered while the server can't be checked, so a new device can't overwrite a
 * profile that already exists on the server.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class AppEntryResolver @Inject constructor(
    private val auth: AuthGateway,
    private val coordinator: SessionCoordinator,
    private val profiles: ProfileRepository,
) {
    private val refreshes = MutableStateFlow(0)

    /** Re-evaluate after the guard deleted data or the player asked to retry. */
    fun refresh() { refreshes.value++ }

    val state: Flow<AppEntryState> = combine(auth.state, refreshes) { a, _ -> a }
        .transformLatest { authState ->
            when (authState) {
                AuthState.Initializing -> emit(AppEntryState.Loading)
                is AuthState.SignedOut -> emit(AppEntryState.SignedOut(authState.byUser))
                is AuthState.SignedIn -> {
                    emit(AppEntryState.Loading)
                    emitAll(resolveSignedIn(authState.userId, authState.offline))
                }
            }
        }
        .distinctUntilChanged()

    private suspend fun resolveSignedIn(userId: UserId, offline: Boolean): Flow<AppEntryState> {
        val decision = coordinator.onSignedIn(userId)
        if (decision is AccessDecision.BlockedByOtherAccount) {
            return MutableStateFlow(AppEntryState.BlockedByOtherAccount(userId, decision.unsyncedCount))
        }
        when (profiles.restoreFromServer(userId)) {
            RestoreResult.Offline -> return MutableStateFlow(AppEntryState.ProfileCheckOffline(userId))
            RestoreResult.HasLocalProfile -> profiles.requestSync(userId)
            RestoreResult.Restored, RestoreResult.NotFoundOnServer -> Unit
        }
        return profiles.observe(userId).map { local ->
            if (local == null) AppEntryState.NeedsOnboarding(userId) else AppEntryState.Ready(userId, offline)
        }
    }
}
