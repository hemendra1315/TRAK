package app.trak.core.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.trak.core.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Device settings (not synced): theme. Week start and units belong to the profile so they follow
 * the player across devices.
 */
class UserSettings(private val store: DataStore<Preferences>) {

    val theme: Flow<AppTheme> = store.data.map { prefs ->
        prefs[THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.DARK
    }

    suspend fun setTheme(theme: AppTheme) {
        store.edit { it[THEME] = theme.name }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
    }
}
