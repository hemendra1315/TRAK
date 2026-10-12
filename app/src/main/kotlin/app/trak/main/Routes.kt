package app.trak.main

import kotlinx.serialization.Serializable

/** Type-safe routes. Navigation stays Home, Log, Progress, Recovery, Profile in every phase. */
@Serializable data object HomeRoute

/**
 * The Log tab opens the daily journal for a date (ISO yyyy-MM-dd; null = today).
 * Phase 2 fills this route with the single-page journal; the route itself does not change.
 */
@Serializable data class JournalRoute(val date: String? = null)

@Serializable data object ProgressRoute
@Serializable data object RecoveryRoute
@Serializable data object ProfileRoute
@Serializable data object EditProfileRoute
@Serializable data object SettingsRoute
