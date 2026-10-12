package app.trak.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * The only place the Supabase client is built. Everything outside :core:data talks to
 * repository interfaces, so supabase-kt can be replaced without touching the UI.
 *
 * [publishableKey] must be the public client key. A service-role key must never reach the app.
 *
 * [sessionManager] is always supplied by TRAK (review fix B4): the library default
 * (SettingsSessionManager) depends on platform settings that TRAK does not control, so session
 * storage location and backup exclusion are owned by our implementation.
 */
fun createTrakSupabaseClient(
    url: String,
    publishableKey: String,
    sessionManager: SessionManager,
    autoRefresh: Boolean = true,
): SupabaseClient = createSupabaseClient(supabaseUrl = url, supabaseKey = publishableKey) {
    install(Auth) {
        this.sessionManager = sessionManager
        autoLoadFromStorage = true
        alwaysAutoRefresh = autoRefresh
    }
    install(Postgrest)
}
