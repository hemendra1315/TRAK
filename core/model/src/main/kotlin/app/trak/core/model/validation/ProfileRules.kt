package app.trak.core.model.validation

object ProfileRules {
    const val DISPLAY_NAME_MAX = 60
    const val FIELD_DISPLAY_NAME = "displayName"

    /**
     * Trims surrounding whitespace and checks length 1..60 (measured in Unicode code points,
     * matching Postgres char_length on the server).
     */
    fun displayName(raw: String): ValidationResult<String> {
        val trimmed = raw.trim()
        val length = trimmed.codePointCount(0, trimmed.length)
        return when {
            length == 0 -> invalid(ErrorCode.REQUIRED)
            length > DISPLAY_NAME_MAX -> invalid(ErrorCode.TOO_LONG)
            else -> ValidationResult.Valid(trimmed)
        }
    }

    private fun invalid(code: ErrorCode) =
        ValidationResult.Invalid(listOf(ValidationError(FIELD_DISPLAY_NAME, code)))
}
