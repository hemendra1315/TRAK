package app.trak.core.data.remote

import io.github.jan.supabase.auth.MemorySessionManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Build-spike check: supabase-kt 3.8.0 with Auth, Postgrest, the Ktor OkHttp engine and a
 * supplied SessionManager construct together on the JVM. No network call is made.
 *
 * Evidence (CI, 2026-10-12):
 * - Without a supplied SessionManager, construction fails with
 *   "Failed to create default settings for SettingsSessionManager".
 * - SessionManager.loadSession() returns a non-null UserSession and must throw
 *   NoSessionFoundException when nothing is stored (supabase-kt 3.8.0 source).
 */
class SupabaseProviderTest {

    @Test
    fun `client builds with auth, postgrest and a supplied session manager`() {
        val client = createTrakSupabaseClient(
            url = "https://spike-test.supabase.co",
            publishableKey = "sb_publishable_spikeTestKeyNotReal0000",
            sessionManager = MemorySessionManager(),
            autoRefresh = false,
            lifecycleCallbacks = false,
        )
        assertNotNull(client.auth)
        assertNotNull(client.postgrest)
    }
}
