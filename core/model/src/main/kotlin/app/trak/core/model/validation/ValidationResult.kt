package app.trak.core.model.validation

/**
 * Result of validating user input. Errors are codes, not display strings, so the UI layer
 * owns wording and translation.
 */
sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>
    data class Invalid(val errors: List<ValidationError>) : ValidationResult<Nothing>
}

/** A single field-level problem. */
data class ValidationError(val field: String, val code: ErrorCode)

enum class ErrorCode {
    REQUIRED,
    TOO_LONG,
    NEGATIVE,
    OUT_OF_RANGE,
    EXCEEDS_TOTAL,
}
