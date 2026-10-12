package app.trak.core.model.validation

/**
 * Numeric rules reused by every log form. A `null` input always means "not recorded" and is
 * valid; these rules never turn a missing value into zero.
 */
object NumericRules {
    const val MINUTES_PER_DAY = 1440

    /** Deliveries, balls faced, reps, etc.: whole numbers ≥ 0 when recorded. */
    fun count(field: String, value: Int?): ValidationResult<Int?> = when {
        value == null -> valid(null)
        value < 0 -> invalid(field, ErrorCode.NEGATIVE)
        else -> valid(value)
    }

    /** A duration in minutes, 0..1440 when recorded. */
    fun minutes(field: String, value: Int?): ValidationResult<Int?> = when {
        value == null -> valid(null)
        value < 0 -> invalid(field, ErrorCode.NEGATIVE)
        value > MINUTES_PER_DAY -> invalid(field, ErrorCode.OUT_OF_RANGE)
        else -> valid(value)
    }

    /**
     * Legal balls can never exceed deliveries (wides and no-balls make deliveries ≥ legal balls).
     * Only checked when both are recorded; legal balls are never inferred from deliveries.
     */
    fun legalBalls(deliveries: Int?, legalBalls: Int?): ValidationResult<Int?> =
        partOfWhole(FIELD_LEGAL_BALLS, part = legalBalls, whole = deliveries)

    /** Activity minutes split a session's elapsed time; they cannot add up to more than it. */
    fun activityMinutes(sessionMinutes: Int?, activityMinutesTotal: Int?): ValidationResult<Int?> =
        partOfWhole(FIELD_ACTIVITY_MINUTES, part = activityMinutesTotal, whole = sessionMinutes)

    /** Drill balls are a breakdown of an entry's balls faced; they cannot exceed it. */
    fun drillBalls(entryBallsFaced: Int?, drillBallsTotal: Int?): ValidationResult<Int?> =
        partOfWhole(FIELD_DRILL_BALLS, part = drillBallsTotal, whole = entryBallsFaced)

    const val FIELD_LEGAL_BALLS = "legalBalls"
    const val FIELD_ACTIVITY_MINUTES = "activityMinutes"
    const val FIELD_DRILL_BALLS = "drillBalls"

    private fun partOfWhole(field: String, part: Int?, whole: Int?): ValidationResult<Int?> = when {
        part == null -> valid(null)
        part < 0 -> invalid(field, ErrorCode.NEGATIVE)
        whole != null && part > whole -> invalid(field, ErrorCode.EXCEEDS_TOTAL)
        else -> valid(part)
    }
}
