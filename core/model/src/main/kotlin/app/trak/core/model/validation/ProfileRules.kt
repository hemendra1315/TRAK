package app.trak.core.model.validation

import java.time.DateTimeException
import java.time.ZoneId

object ProfileRules {
    const val DISPLAY_NAME_MAX = 60
    const val FIELD_DISPLAY_NAME = "displayName"
    const val FIELD_TIME_ZONE = "timeZone"
    const val FIELD_WEEK_START = "weekStart"

    /**
     * Trims surrounding whitespace and checks length 1..60 (measured in Unicode code points,
     * matching Postgres char_length on the server).
     */
    fun displayName(raw: String): ValidationResult<String> {
        val trimmed = raw.trim()
        val length = trimmed.codePointCount(0, trimmed.length)
        return when {
            length == 0 -> invalid(FIELD_DISPLAY_NAME, ErrorCode.REQUIRED)
            length > DISPLAY_NAME_MAX -> invalid(FIELD_DISPLAY_NAME, ErrorCode.TOO_LONG)
            else -> valid(trimmed)
        }
    }

    /**
     * Accepts region-based IANA ids only (e.g. "Asia/Kolkata"), not raw offsets, so a player's
     * day boundaries follow daylight-saving rules. The server also rejects unknown zones.
     */
    fun timeZone(raw: String): ValidationResult<ZoneId> {
        val id = raw.trim()
        if (id.isEmpty()) return invalid(FIELD_TIME_ZONE, ErrorCode.REQUIRED)
        if (id !in ZoneId.getAvailableZoneIds()) return invalid(FIELD_TIME_ZONE, ErrorCode.INVALID_TIME_ZONE)
        return try {
            valid(ZoneId.of(id))
        } catch (e: DateTimeException) {
            invalid(FIELD_TIME_ZONE, ErrorCode.INVALID_TIME_ZONE)
        }
    }

    /** ISO day number: 1 = Monday … 7 = Sunday. */
    fun weekStart(isoDay: Int): ValidationResult<Int> =
        if (isoDay in 1..7) valid(isoDay) else invalid(FIELD_WEEK_START, ErrorCode.OUT_OF_RANGE)
}
