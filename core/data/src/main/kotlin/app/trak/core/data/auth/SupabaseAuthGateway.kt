package app.trak.core.data.auth

import app.trak.core.data.remote.SupabaseClientHolder
import app.trak.core.model.UserId
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class SupabaseAuthGateway @Inject constructor(
    private val holder: SupabaseClientHolder,
    private val sessionManager: SessionManager,
) : AuthGateway {

    override val state: Flow<AuthState> =
        holder.client?.auth?.sessionStatus?.map { status ->
            when (status) {
                is SessionStatus.Authenticated -> {
                    val user = status.session.user
                    if (user != null) AuthState.SignedIn(UserId(user.id), user.email) else storedSessionState()
                }
                is SessionStatus.NotAuthenticated -> AuthState.SignedOut(byUser = status.isSignOut)
                SessionStatus.Initializing -> AuthState.Initializing
                // Couldn't refresh (usually offline). The stored session still identifies the player,
                // so a local-first app keeps working instead of forcing a sign-in.
                is SessionStatus.RefreshFailure -> storedSessionState(offline = true)
            }
        } ?: flowOf(AuthState.SignedOut(byUser = false))

    private suspend fun storedSessionState(offline: Boolean = false): AuthState {
        val user = sessionManager.loadSessionOrNull()?.user ?: return AuthState.SignedOut(byUser = false)
        return AuthState.SignedIn(UserId(user.id), user.email, offline)
    }

    override suspend fun sendCode(email: String): AuthResult = call {
        auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
    }

    override suspend fun verifyCode(email: String, code: String): AuthResult = call {
        auth.verifyEmailOtp(type = OtpType.Email.EMAIL, email = email, token = code)
    }

    override suspend fun signOut() {
        val client = holder.client ?: return
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            // Offline or server error: still end the session on this device.
            client.auth.clearSession()
        }
    }

    private suspend fun call(block: suspend io.github.jan.supabase.SupabaseClient.() -> Unit): AuthResult {
        val client = holder.client ?: return AuthResult.NotConfigured
        return try {
            client.block()
            AuthResult.Success
        } catch (e: RestException) {
            when (e.statusCode) {
                429 -> AuthResult.RateLimited
                400, 401, 403, 422 -> AuthResult.InvalidOrExpiredCode
                in 500..599 -> AuthResult.Network
                else -> AuthResult.Error(e.error)
            }
        } catch (e: HttpRequestException) {
            AuthResult.Network
        } catch (e: IOException) {
            AuthResult.Network
        }
    }
}
