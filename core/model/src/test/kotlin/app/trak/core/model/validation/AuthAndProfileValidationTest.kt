package app.trak.core.model.validation

import app.trak.core.model.PrimaryRole
import app.trak.core.model.ProfileInput
import app.trak.core.model.ProfileValidation
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthAndProfileValidationTest {

    @Test
    fun `email is trimmed, lowercased and checked`() {
        assertEquals(ValidationResult.Valid("arjun@club.in"), AuthRules.email("  Arjun@Club.IN "))
        assertEquals(ErrorCode.REQUIRED, codes(AuthRules.email("")).single())
        assertEquals(ErrorCode.OUT_OF_RANGE, codes(AuthRules.email("arjun@club")).single())
        assertEquals(ErrorCode.OUT_OF_RANGE, codes(AuthRules.email("ar jun@club.in")).single())
    }

    @Test
    fun `code must be six digits, pasted spaces allowed`() {
        assertEquals(ValidationResult.Valid("123456"), AuthRules.code("123 456"))
        assertEquals(ErrorCode.OUT_OF_RANGE, codes(AuthRules.code("12345")).single())
        assertEquals(ErrorCode.OUT_OF_RANGE, codes(AuthRules.code("12345a")).single())
        assertEquals(ErrorCode.REQUIRED, codes(AuthRules.code(" ")).single())
    }

    private val base = ProfileInput(
        displayName = "Arjun",
        primaryRole = PrimaryRole.ALL_ROUNDER,
        timeZoneId = "Asia/Kolkata",
        adultConfirmedAt = Instant.parse("2026-10-12T10:00:00Z"),
    )

    @Test
    fun `onboarding needs only name, role, zone and the 18+ confirmation`() {
        val result = ProfileValidation.validate(base)
        assertTrue(result.isValid)
        val p = (result as ValidationResult.Valid).value
        assertEquals(null, p.battingHand)        // optional, not recorded
        assertTrue(p.bowlingStyles.isEmpty())    // optional, not recorded
    }

    @Test
    fun `missing role and missing 18+ confirmation are both reported`() {
        val result = ProfileValidation.validate(base.copy(primaryRole = null, adultConfirmedAt = null))
        val fields = (result as ValidationResult.Invalid).errors.map { it.field }.toSet()
        assertEquals(setOf(ProfileValidation.FIELD_PRIMARY_ROLE, ProfileValidation.FIELD_ADULT), fields)
    }

    @Test
    fun `invalid name and zone are reported together`() {
        val result = ProfileValidation.validate(base.copy(displayName = " ", timeZoneId = "Mars/Base"))
        assertEquals(setOf(ErrorCode.REQUIRED, ErrorCode.INVALID_TIME_ZONE), codes(result).toSet())
    }

    private fun codes(r: ValidationResult<*>) = (r as ValidationResult.Invalid).errors.map { it.code }
}
