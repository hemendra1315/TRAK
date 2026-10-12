package app.trak.core.model.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Acceptance test A2. */
class NumericRulesTest {

    @Test
    fun `missing counts stay missing, never zero`() {
        assertEquals(ValidationResult.Valid<Int?>(null), NumericRules.count("deliveries", null))
        assertEquals(ValidationResult.Valid<Int?>(null), NumericRules.minutes("minutes", null))
    }

    @Test
    fun `negative counts are rejected and zero is accepted`() {
        assertEquals(ErrorCode.NEGATIVE, errorOf(NumericRules.count("deliveries", -1)))
        assertEquals(ValidationResult.Valid<Int?>(0), NumericRules.count("deliveries", 0))
    }

    @Test
    fun `minutes must be within one day`() {
        assertEquals(ValidationResult.Valid<Int?>(1440), NumericRules.minutes("m", 1440))
        assertEquals(ErrorCode.OUT_OF_RANGE, errorOf(NumericRules.minutes("m", 1441)))
        assertEquals(ErrorCode.NEGATIVE, errorOf(NumericRules.minutes("m", -5)))
    }

    @Test
    fun `legal balls can never exceed deliveries`() {
        assertEquals(ErrorCode.EXCEEDS_TOTAL, errorOf(NumericRules.legalBalls(deliveries = 6, legalBalls = 7)))
        assertEquals(ValidationResult.Valid<Int?>(6), NumericRules.legalBalls(deliveries = 8, legalBalls = 6))
        assertEquals(ValidationResult.Valid<Int?>(6), NumericRules.legalBalls(deliveries = 6, legalBalls = 6))
    }

    @Test
    fun `legal balls are never inferred from deliveries`() {
        assertEquals(ValidationResult.Valid<Int?>(null), NumericRules.legalBalls(deliveries = 36, legalBalls = null))
    }

    @Test
    fun `legal balls without deliveries are allowed but must not be negative`() {
        assertTrue(NumericRules.legalBalls(deliveries = null, legalBalls = 12).isValid)
        assertEquals(ErrorCode.NEGATIVE, errorOf(NumericRules.legalBalls(deliveries = null, legalBalls = -1)))
    }

    @Test
    fun `activity minutes cannot exceed session minutes`() {
        assertEquals(ErrorCode.EXCEEDS_TOTAL, errorOf(NumericRules.activityMinutes(sessionMinutes = 90, activityMinutesTotal = 100)))
        assertTrue(NumericRules.activityMinutes(sessionMinutes = 90, activityMinutesTotal = 90).isValid)
        assertTrue(NumericRules.activityMinutes(sessionMinutes = null, activityMinutesTotal = 30).isValid)
    }

    @Test
    fun `drill balls cannot exceed the entry's balls faced`() {
        assertEquals(ErrorCode.EXCEEDS_TOTAL, errorOf(NumericRules.drillBalls(entryBallsFaced = 60, drillBallsTotal = 61)))
        assertTrue(NumericRules.drillBalls(entryBallsFaced = 60, drillBallsTotal = 45).isValid)
    }

    private fun errorOf(result: ValidationResult<*>): ErrorCode =
        (result as ValidationResult.Invalid).errors.single().code
}
