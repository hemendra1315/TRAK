package app.trak.core.model

import app.trak.core.model.validation.ErrorCode
import app.trak.core.model.validation.ProfileRules
import app.trak.core.model.validation.ValidationError
import app.trak.core.model.validation.ValidationResult
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** What the onboarding and edit-profile forms submit. Validated into a [ValidatedProfile]. */
data class ProfileInput(
    val displayName: String,
    val primaryRole: PrimaryRole?,
    val battingHand: BattingHand? = null,
    val bowlingStyles: Set<BowlingStyle> = emptySet(),
    val weekStart: DayOfWeek = DayOfWeek.MONDAY,
    val weightUnit: WeightUnit = WeightUnit.KG,
    val timeZoneId: String,
    /** When the player confirmed they are 18 or older; null = not confirmed (decision D5). */
    val adultConfirmedAt: Instant?,
)

data class ValidatedProfile(
    val displayName: String,
    val primaryRole: PrimaryRole,
    val battingHand: BattingHand?,
    val bowlingStyles: Set<BowlingStyle>,
    val weekStart: DayOfWeek,
    val weightUnit: WeightUnit,
    val timeZone: ZoneId,
    val adultConfirmedAt: Instant,
)

object ProfileValidation {
    const val FIELD_PRIMARY_ROLE = "primaryRole"
    const val FIELD_ADULT = "adultConfirmed"

    fun validate(input: ProfileInput): ValidationResult<ValidatedProfile> {
        val errors = mutableListOf<ValidationError>()
        val name = ProfileRules.displayName(input.displayName)
        val zone = ProfileRules.timeZone(input.timeZoneId)
        if (name is ValidationResult.Invalid) errors += name.errors
        if (zone is ValidationResult.Invalid) errors += zone.errors
        if (input.primaryRole == null) errors += ValidationError(FIELD_PRIMARY_ROLE, ErrorCode.REQUIRED)
        if (input.adultConfirmedAt == null) errors += ValidationError(FIELD_ADULT, ErrorCode.REQUIRED)
        if (errors.isNotEmpty()) return ValidationResult.Invalid(errors)
        return ValidationResult.Valid(
            ValidatedProfile(
                displayName = (name as ValidationResult.Valid).value,
                primaryRole = input.primaryRole!!,
                battingHand = input.battingHand,
                bowlingStyles = input.bowlingStyles,
                weekStart = input.weekStart,
                weightUnit = input.weightUnit,
                timeZone = (zone as ValidationResult.Valid).value,
                adultConfirmedAt = input.adultConfirmedAt!!,
            ),
        )
    }
}
