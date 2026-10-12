package app.trak.core.model.validation

import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileRulesTest {

    @Test
    fun `empty and blank names are required`() {
        assertEquals(ErrorCode.REQUIRED, errorOf(ProfileRules.displayName("")))
        assertEquals(ErrorCode.REQUIRED, errorOf(ProfileRules.displayName("   ")))
    }

    @Test
    fun `one and sixty characters are accepted`() {
        assertEquals(ValidationResult.Valid("A"), ProfileRules.displayName("A"))
        val sixty = "a".repeat(60)
        assertEquals(ValidationResult.Valid(sixty), ProfileRules.displayName(sixty))
    }

    @Test
    fun `sixty one characters are too long`() {
        assertEquals(ErrorCode.TOO_LONG, errorOf(ProfileRules.displayName("a".repeat(61))))
    }

    @Test
    fun `surrounding whitespace is trimmed before checking`() {
        assertEquals(ValidationResult.Valid("Arjun"), ProfileRules.displayName("  Arjun  "))
        val sixtyPadded = "  " + "a".repeat(60) + "  "
        assertEquals(ValidationResult.Valid("a".repeat(60)), ProfileRules.displayName(sixtyPadded))
    }

    @Test
    fun `length counts code points, not UTF-16 units`() {
        // 60 emoji = 120 UTF-16 units but 60 code points, matching Postgres char_length.
        val sixtyEmoji = "🏏".repeat(60)
        assertEquals(ValidationResult.Valid(sixtyEmoji), ProfileRules.displayName(sixtyEmoji))
    }

    private fun errorOf(result: ValidationResult<String>): ErrorCode =
        (result as ValidationResult.Invalid).errors.single().code
}
