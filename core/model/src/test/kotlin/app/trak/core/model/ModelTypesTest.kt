package app.trak.core.model

import app.trak.core.model.validation.ErrorCode
import app.trak.core.model.validation.ProfileRules
import app.trak.core.model.validation.ValidationResult
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

/** Acceptance test A4 plus id, enum-code and time-zone checks. */
class ModelTypesTest {

    @Test
    fun `rating5 accepts 1 to 5 only`() {
        assertNull(Rating5.of(0))
        assertNull(Rating5.of(6))
        assertEquals(1, Rating5.of(1)?.value)
        assertEquals(5, Rating5.of(5)?.value)
    }

    @Test
    fun `rpe10 accepts 1 to 10 only`() {
        assertNull(Rpe10.of(0))
        assertNull(Rpe10.of(11))
        assertEquals(10, Rpe10.of(10)?.value)
    }

    @Test
    fun `entity ids are random lowercase UUIDs`() {
        val a = EntityId.random()
        val b = EntityId.random()
        assertNotEquals(a, b)
        assertEquals(a.value.lowercase(), a.value)
    }

    @Test
    fun `ids reject non-UUID values`() {
        assertThrows(IllegalArgumentException::class.java) { UserId("not-a-uuid") }
        assertThrows(IllegalArgumentException::class.java) { EntityId("123") }
    }

    @Test
    fun `enum codes match the server check constraints`() {
        assertEquals(
            listOf("batter", "bowler", "all_rounder", "wicketkeeper", "wicketkeeper_batter", "other"),
            PrimaryRole.entries.map { it.code },
        )
        assertEquals(
            listOf("fast", "medium", "off_spin", "leg_spin", "left_arm_orthodox", "wrist_spin_other", "other"),
            BowlingStyle.entries.map { it.code },
        )
        assertEquals(PrimaryRole.ALL_ROUNDER, PrimaryRole.fromCode("all_rounder"))
        assertNull(PrimaryRole.fromCode("captain"))
    }

    @Test
    fun `time zones must be region ids`() {
        assertEquals(ValidationResult.Valid(ZoneId.of("Asia/Kolkata")), ProfileRules.timeZone("Asia/Kolkata"))
        assertEquals(ErrorCode.INVALID_TIME_ZONE, (ProfileRules.timeZone("+05:30") as ValidationResult.Invalid).errors.single().code)
        assertEquals(ErrorCode.INVALID_TIME_ZONE, (ProfileRules.timeZone("Not/AZone") as ValidationResult.Invalid).errors.single().code)
        assertEquals(ErrorCode.REQUIRED, (ProfileRules.timeZone(" ") as ValidationResult.Invalid).errors.single().code)
    }

    @Test
    fun `week start is an ISO day`() {
        assertEquals(ValidationResult.Valid(1), ProfileRules.weekStart(1))
        assertEquals(ErrorCode.OUT_OF_RANGE, (ProfileRules.weekStart(0) as ValidationResult.Invalid).errors.single().code)
        assertEquals(ErrorCode.OUT_OF_RANGE, (ProfileRules.weekStart(8) as ValidationResult.Invalid).errors.single().code)
    }
}
