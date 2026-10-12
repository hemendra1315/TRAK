package app.trak.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * The only place the Supabase client is built. Everything outside :core:data talks to
 * repository interfaces, so supabase-kt can be replaced without touching the UI.
 *
 * [publishableKey] must be the public client key. A service-role key must never reach the app.
 */
fun createTrakSupabaseClient(
    url: String,
    publishableKey: String,
    loadSessionFromStorage: Boolean = true,
): SupabaseClient = createSupabaseClient(supabaseUrl = url, supabaseKey = publishableKey) {
    install(Auth) {
        autoLoadFromStorage = loadSessionFromStorage
        alwaysAutoRefresh = loadSessionFromStorage
    }
    install(Postgrest)
}
