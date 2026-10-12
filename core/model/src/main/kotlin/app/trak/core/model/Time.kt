package app.trak.core.model

import java.time.LocalDate
import java.time.ZoneId

/**
 * A calendar day as the player lived it: the local date plus the time zone it was recorded in.
 * Records that belong to a day store both, so travelling never moves a session to another day.
 */
data class LocalDay(val date: LocalDate, val zone: ZoneId)
