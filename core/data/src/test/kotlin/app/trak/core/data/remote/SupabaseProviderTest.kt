package app.trak.core.data.remote

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Build-spike check: the supabase-kt 3.8.0 client, its Auth and Postgrest plugins and the Ktor
 * OkHttp engine resolve and construct together. No network call is made.
 */
class SupabaseProviderTest {

    @Test
    fun `client builds with auth and postgrest plugins`() {
        val client = createTrakSupabaseClient(
            url = "https://spike-test.supabase.co",
            publishableKey = "sb_publishable_spikeTestKeyNotReal0000",
            loadSessionFromStorage = false,
        )
        assertNotNull(client.auth)
        assertNotNull(client.postgrest)
    }
}
