package app.trak.core.model.validation

object AuthRules {
    const val FIELD_EMAIL = "email"
    const val FIELD_CODE = "code"
    const val CODE_LENGTH = 6

    // Pragmatic check: something@something.tld, no spaces. The server is the final authority.
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")

    fun email(raw: String): ValidationResult<String> {
        val e = raw.trim().lowercase()
        return when {
            e.isEmpty() -> invalid(FIELD_EMAIL, ErrorCode.REQUIRED)
            e.length > 254 || !EMAIL.matches(e) -> invalid(FIELD_EMAIL, ErrorCode.OUT_OF_RANGE)
            else -> valid(e)
        }
    }

    /** Six digits. Spaces a user may paste in ("123 456") are removed. */
    fun code(raw: String): ValidationResult<String> {
        val c = raw.filterNot { it.isWhitespace() }
        return when {
            c.isEmpty() -> invalid(FIELD_CODE, ErrorCode.REQUIRED)
            c.length != CODE_LENGTH || !c.all { it in '0'..'9' } -> invalid(FIELD_CODE, ErrorCode.OUT_OF_RANGE)
            else -> valid(c)
        }
    }
}
