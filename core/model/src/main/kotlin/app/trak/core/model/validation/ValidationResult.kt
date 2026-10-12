package app.trak.core.model.validation

/**
 * Result of validating user input. Errors are codes, not display strings, so the UI layer
 * owns wording and translation.
 */
sealed interface ValidationResult<out T> {
    data class Valid<T>(val value: T) : ValidationResult<T>
    data class Invalid(val errors: List<ValidationError>) : ValidationResult<Nothing>

    val isValid: Boolean get() = this is Valid
}

/** A single field-level problem. */
data class ValidationError(val field: String, val code: ErrorCode)

enum class ErrorCode {
    /** A required value is missing or blank. */
    REQUIRED,
    TOO_LONG,
    NEGATIVE,
    OUT_OF_RANGE,
    /** A part is larger than the whole it belongs to (e.g. legal balls > deliveries). */
    EXCEEDS_TOTAL,
    /** An end time that is not after the start, or an implausibly long span. */
    INVALID_TIME_RANGE,
    /** Not a recognised IANA time zone. */
    INVALID_TIME_ZONE,
}

internal fun <T> valid(value: T): ValidationResult<T> = ValidationResult.Valid(value)
internal fun invalid(field: String, code: ErrorCode): ValidationResult.Invalid =
    ValidationResult.Invalid(listOf(ValidationError(field, code)))
