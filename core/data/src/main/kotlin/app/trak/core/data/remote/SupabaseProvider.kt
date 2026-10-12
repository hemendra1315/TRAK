package app.trak.core.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.CodeVerifierCache
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
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
 * storage location and backup exclusion are owned by our implementation. The PKCE
 * [codeVerifierCache] is likewise supplied; email-code sign-in does not use PKCE, so an
 * in-memory cache is sufficient.
 */
fun createTrakSupabaseClient(
    url: String,
    publishableKey: String,
    sessionManager: SessionManager,
    codeVerifierCache: CodeVerifierCache = MemoryCodeVerifierCache(),
    autoRefresh: Boolean = true,
    /** Android focus/lifecycle hooks for auto-refresh. Disabled in plain JVM tests (no main looper). */
    lifecycleCallbacks: Boolean = true,
): SupabaseClient = createSupabaseClient(supabaseUrl = url, supabaseKey = publishableKey) {
    install(Auth) {
        this.sessionManager = sessionManager
        this.codeVerifierCache = codeVerifierCache
        autoLoadFromStorage = true
        alwaysAutoRefresh = autoRefresh
        enableLifecycleCallbacks = lifecycleCallbacks
    }
    install(Postgrest)
}

/** Build-time client settings (from local.properties or CI environment). */
data class SupabaseConfig(val url: String, val publishableKey: String) {
    val isConfigured: Boolean get() = url.isNotBlank() && publishableKey.isNotBlank()
}

/**
 * Holds the single client. [client] is null when the build has no Supabase settings, so the app
 * still runs (local features work; sign-in reports "not configured") instead of crashing.
 */
class SupabaseClientHolder(
    config: SupabaseConfig,
    sessionManager: SessionManager,
    lifecycleCallbacks: Boolean = true,
) {
    val client: SupabaseClient? =
        if (config.isConfigured) {
            createTrakSupabaseClient(config.url, config.publishableKey, sessionManager, lifecycleCallbacks = lifecycleCallbacks)
        } else {
            null
        }
}
