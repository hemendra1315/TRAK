package app.trak.core.model.validation

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

object TimeRangeRules {
    const val FIELD_SLEEP = "sleep"
    const val MAX_SLEEP_MINUTES = 18 * 60

    /**
     * Time in bed for a sleep that ends on [wakeDate] at [wakeTime] in [zone].
     * If the bedtime is later in the clock than the wake time, it belongs to the previous day
     * (crossing midnight). Minutes are real elapsed time, so daylight-saving changes are counted
     * correctly. Valid range: 1..1080 minutes. This is "time in bed (reported)", not confirmed sleep.
     */
    fun timeInBedMinutes(
        wakeDate: LocalDate,
        bedTime: LocalTime,
        wakeTime: LocalTime,
        zone: ZoneId,
    ): ValidationResult<Int> {
        if (bedTime == wakeTime) return invalid(FIELD_SLEEP, ErrorCode.INVALID_TIME_RANGE)
        val bedDate = if (bedTime.isAfter(wakeTime)) wakeDate.minusDays(1) else wakeDate
        val bed = ZonedDateTime.of(bedDate, bedTime, zone)
        val wake = ZonedDateTime.of(wakeDate, wakeTime, zone)
        val minutes = Duration.between(bed, wake).toMinutes()
        return if (minutes in 1..MAX_SLEEP_MINUTES.toLong()) valid(minutes.toInt())
        else invalid(FIELD_SLEEP, ErrorCode.INVALID_TIME_RANGE)
    }
}
