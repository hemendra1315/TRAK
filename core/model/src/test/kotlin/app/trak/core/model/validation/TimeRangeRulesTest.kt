package app.trak.core.model.validation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

/** Acceptance test A3. */
class TimeRangeRulesTest {

    private val kolkata = ZoneId.of("Asia/Kolkata")
    private val london = ZoneId.of("Europe/London")

    private fun minutes(date: String, bed: String, wake: String, zone: ZoneId = kolkata) =
        TimeRangeRules.timeInBedMinutes(LocalDate.parse(date), LocalTime.parse(bed), LocalTime.parse(wake), zone)

    @Test
    fun `bed 22_30 to wake 06_30 crosses midnight and is 480 minutes`() {
        assertEquals(ValidationResult.Valid(480), minutes("2026-10-12", "22:30", "06:30"))
    }

    @Test
    fun `afternoon nap on the same day`() {
        assertEquals(ValidationResult.Valid(45), minutes("2026-10-12", "14:00", "14:45"))
    }

    @Test
    fun `bed equal to wake is rejected`() {
        assertEquals(ErrorCode.INVALID_TIME_RANGE, errorOf(minutes("2026-10-12", "06:30", "06:30")))
    }

    @Test
    fun `more than 18 hours is rejected`() {
        // 11:00 -> 06:00 next day = 19 hours
        assertEquals(ErrorCode.INVALID_TIME_RANGE, errorOf(minutes("2026-10-12", "11:00", "06:00")))
        // 12:00 -> 06:00 next day = exactly 18 hours, allowed
        assertEquals(ValidationResult.Valid(1080), minutes("2026-10-12", "12:00", "06:00"))
    }

    @Test
    fun `clocks going back in London add an hour of real time`() {
        // BST ends 2026-10-25 02:00 -> 01:00. 23:00 to 07:00 on the wall is 9 real hours.
        assertEquals(ValidationResult.Valid(540), minutes("2026-10-25", "23:00", "07:00", london))
    }

    @Test
    fun `clocks going forward in London remove an hour of real time`() {
        // BST starts 2026-03-29 01:00 -> 02:00. 23:00 to 07:00 on the wall is 7 real hours.
        assertEquals(ValidationResult.Valid(420), minutes("2026-03-29", "23:00", "07:00", london))
    }

    private fun errorOf(result: ValidationResult<*>): ErrorCode =
        (result as ValidationResult.Invalid).errors.single().code
}
