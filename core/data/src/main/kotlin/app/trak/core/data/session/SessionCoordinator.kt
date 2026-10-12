package app.trak.core.data.session

import app.trak.core.data.auth.AuthGateway
import app.trak.core.model.UserId
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AccessDecision {
    /** The signed-in account owns (or now owns) the local data. */
    data object Proceed : AccessDecision

    /**
     * Another account's unsynced changes are on this phone. The new account sees none of it and
     * can't continue until that account syncs them, or someone deletes them with a typed confirmation.
     */
    data class BlockedByOtherAccount(val unsyncedCount: Int) : AccessDecision
}

sealed interface SignOutCheck {
    data object SignedOut : SignOutCheck
    /** Nothing was done. The UI must offer Sync now, Keep on this phone, or Delete (typed). */
    data class HasUnsyncedChanges(val count: Int) : SignOutCheck
}

/**
 * Account safety rules (plan section 5, review fix B3, decision S1).
 * Nothing in this class deletes unsynced data except the two explicit, typed-confirmation paths.
 */
@Singleton
class SessionCoordinator @Inject constructor(
    private val auth: AuthGateway,
    private val account: AccountStore,
) {
    suspend fun onSignedIn(userId: UserId): AccessDecision {
        val owner = account.owner()
        if (owner == userId.value) return AccessDecision.Proceed
        if (owner == null) {
            account.setOwner(userId.value)
            return AccessDecision.Proceed
        }
        val unsynced = account.unsyncedCount()
        if (unsynced > 0) return AccessDecision.BlockedByOtherAccount(unsynced)
        // Another account's fully synced data: it is safe on the server, so clear it before
        // this account's data loads.
        account.wipeUserData()
        account.setOwner(userId.value)
        return AccessDecision.Proceed
    }

    /** Signs out and clears local data only when nothing is unsynced. */
    suspend fun signOut(): SignOutCheck {
        val unsynced = account.unsyncedCount()
        if (unsynced > 0) return SignOutCheck.HasUnsyncedChanges(unsynced)
        auth.signOut()
        account.wipeUserData()
        return SignOutCheck.SignedOut
    }

    /** Ends the session but keeps unsynced data on the phone, locked to its owner. */
    suspend fun signOutKeepingData() {
        auth.signOut()
    }

    /** Deletes unsynced changes and signs out. Requires the player to type [CONFIRMATION_WORD]. */
    suspend fun deleteUnsyncedAndSignOut(typedConfirmation: String): Boolean {
        if (!isConfirmed(typedConfirmation)) return false
        auth.signOut()
        account.wipeUserData()
        return true
    }

    /**
     * Used from the "another account's changes" screen: deletes them so [newOwner] can continue.
     * Requires the typed confirmation.
     */
    suspend fun deleteOtherAccountData(typedConfirmation: String, newOwner: UserId): Boolean {
        if (!isConfirmed(typedConfirmation)) return false
        account.wipeUserData()
        account.setOwner(newOwner.value)
        return true
    }

    private fun isConfirmed(typed: String) = typed.trim() == CONFIRMATION_WORD

    companion object {
        const val CONFIRMATION_WORD = "DELETE"
    }
}
