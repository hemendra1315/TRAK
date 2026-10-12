package app.trak.core.data.remote

import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.postgrest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Build-spike check: supabase-kt 3.8.0 with Auth, Postgrest, the Ktor OkHttp engine and a
 * TRAK-supplied SessionManager construct together on the JVM. No network call is made.
 *
 * Evidence (CI, 2026-10-12): without a supplied SessionManager, construction fails with
 * "Failed to create default settings for SettingsSessionManager".
 */
class SupabaseProviderTest {

    private class InMemorySessionManager : SessionManager {
        var session: UserSession? = null
        override suspend fun saveSession(session: UserSession) { this.session = session }
        override suspend fun loadSession(): UserSession? = session
        override suspend fun deleteSession() { session = null }
    }

    @Test
    fun `client builds with auth, postgrest and a TRAK session manager`() {
        val client = createTrakSupabaseClient(
            url = "https://spike-test.supabase.co",
            publishableKey = "sb_publishable_spikeTestKeyNotReal0000",
            sessionManager = InMemorySessionManager(),
            autoRefresh = false,
        )
        assertNotNull(client.auth)
        assertNotNull(client.postgrest)
    }
}
