package app.trak.core.data.auth

import app.trak.core.model.UserId
import kotlinx.coroutines.flow.Flow

/** Authentication state as the app needs it. Identity always comes from the auth session. */
sealed interface AuthState {
    data object Initializing : AuthState

    /**
     * No usable session. [byUser] is true after an explicit sign-out. When false the session ended on
     * its own (expired or revoked): local data is kept untouched (test A20).
     */
    data class SignedOut(val byUser: Boolean) : AuthState

    /** [offline] is true when the stored session is known but couldn't be refreshed (no network). */
    data class SignedIn(val userId: UserId, val email: String?, val offline: Boolean = false) : AuthState
}

sealed interface AuthResult {
    data object Success : AuthResult
    data object InvalidOrExpiredCode : AuthResult
    data object RateLimited : AuthResult
    data object Network : AuthResult
    /** The app was built without Supabase settings (see README / local.properties.example). */
    data object NotConfigured : AuthResult
    data class Error(val message: String) : AuthResult
}

/** Email-code sign-in (decision D1). Implemented over supabase-kt; replaceable without UI changes. */
interface AuthGateway {
    val state: Flow<AuthState>
    suspend fun sendCode(email: String): AuthResult
    suspend fun verifyCode(email: String, code: String): AuthResult

    /** Ends the session. Works offline: the local session is cleared even if the server can't be reached. */
    suspend fun signOut()
}
