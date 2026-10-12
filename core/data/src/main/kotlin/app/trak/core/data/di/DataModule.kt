package app.trak.core.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.trak.core.data.ApplicationScope
import app.trak.core.data.BuildConfig
import app.trak.core.data.TimeSource
import app.trak.core.data.auth.AuthGateway
import app.trak.core.data.auth.DataStoreSessionManager
import app.trak.core.data.auth.SupabaseAuthGateway
import app.trak.core.data.db.TrakDatabase
import app.trak.core.data.db.dao.ProfileDao
import app.trak.core.data.profile.ProfileRemote
import app.trak.core.data.profile.SupabaseProfileRemote
import app.trak.core.data.remote.SupabaseClientHolder
import app.trak.core.data.remote.SupabaseConfig
import app.trak.core.data.session.AccountStore
import app.trak.core.data.session.RoomAccountStore
import app.trak.core.data.settings.UserSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.auth.SessionManager
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SettingsStore
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class SessionStore

/**
 * File names here are referenced by the app's backup exclusion rules
 * (res/xml/data_extraction_rules.xml); change both together.
 */
object DataFiles {
    const val SETTINGS = "settings"
    const val SESSION = "session"
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidesModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): TrakDatabase =
        // No fallbackToDestructiveMigration: schema changes always ship an explicit, tested Migration.
        Room.databaseBuilder(context, TrakDatabase::class.java, TrakDatabase.NAME).build()

    @Provides fun profileDao(db: TrakDatabase): ProfileDao = db.profileDao()

    @Provides @Singleton @SettingsStore
    fun settingsStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(DataFiles.SETTINGS) }

    @Provides @Singleton @SessionStore
    fun sessionStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(DataFiles.SESSION) }

    @Provides @Singleton
    fun userSettings(@SettingsStore store: DataStore<Preferences>) = UserSettings(store)

    @Provides @Singleton
    fun sessionManager(@SessionStore store: DataStore<Preferences>): SessionManager = DataStoreSessionManager(store)

    @Provides @Singleton
    fun supabaseConfig() = SupabaseConfig(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY)

    @Provides @Singleton
    fun supabaseClientHolder(config: SupabaseConfig, sessionManager: SessionManager) =
        SupabaseClientHolder(config, sessionManager)

    @Provides @Singleton
    fun timeSource(): TimeSource = TimeSource.System

    @Provides @Singleton @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindsModule {
    @Binds abstract fun authGateway(impl: SupabaseAuthGateway): AuthGateway
    @Binds abstract fun profileRemote(impl: SupabaseProfileRemote): ProfileRemote
    @Binds abstract fun accountStore(impl: RoomAccountStore): AccountStore
}
