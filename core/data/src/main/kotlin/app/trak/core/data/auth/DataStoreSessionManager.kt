package app.trak.core.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.exception.NoSessionFoundException
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Stores the Supabase session in an app-private DataStore file ("session.preferences_pb") that the
 * backup rules exclude from cloud backup and device transfer (review fix B4).
 *
 * Contract (supabase-kt 3.8.0): loadSession() throws NoSessionFoundException when nothing is stored.
 * A stored value that can't be decoded is treated as no session rather than crashing sign-in.
 */
class DataStoreSessionManager(
    private val store: DataStore<Preferences>,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) : SessionManager {

    override suspend fun saveSession(session: UserSession) {
        val encoded = json.encodeToString(UserSession.serializer(), session)
        store.edit { it[KEY] = encoded }
    }

    override suspend fun loadSession(): UserSession {
        val raw = store.data.first()[KEY] ?: throw NoSessionFoundException()
        return runCatching { json.decodeFromString(UserSession.serializer(), raw) }
            .getOrElse { throw NoSessionFoundException() }
    }

    override suspend fun deleteSession() {
        store.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("user_session")
    }
}
