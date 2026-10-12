package app.trak.core.model

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** Codes match the server check constraints in supabase/migrations/*_profiles.sql. */
enum class PrimaryRole(val code: String) {
    BATTER("batter"),
    BOWLER("bowler"),
    ALL_ROUNDER("all_rounder"),
    WICKETKEEPER("wicketkeeper"),
    WICKETKEEPER_BATTER("wicketkeeper_batter"),
    OTHER("other");

    companion object {
        fun fromCode(code: String): PrimaryRole? = entries.firstOrNull { it.code == code }
    }
}

enum class BattingHand(val code: String) {
    RIGHT("right"),
    LEFT("left");

    companion object {
        fun fromCode(code: String?): BattingHand? = entries.firstOrNull { it.code == code }
    }
}

enum class BowlingStyle(val code: String) {
    FAST("fast"),
    MEDIUM("medium"),
    OFF_SPIN("off_spin"),
    LEG_SPIN("leg_spin"),
    LEFT_ARM_ORTHODOX("left_arm_orthodox"),
    WRIST_SPIN_OTHER("wrist_spin_other"),
    OTHER("other");

    companion object {
        fun fromCode(code: String): BowlingStyle? = entries.firstOrNull { it.code == code }
    }
}

enum class WeightUnit(val code: String) {
    KG("kg"),
    LB("lb");

    companion object {
        fun fromCode(code: String): WeightUnit? = entries.firstOrNull { it.code == code }
    }
}

/** Local sync bookkeeping for a row (plan section 5, Phase 1 profile sync rules). */
enum class SyncState { PENDING, SYNCED, FAILED }

/**
 * The player's profile. Optional details (batting hand, bowling styles) are null/empty when
 * not recorded; nothing is defaulted to a guessed value.
 */
data class Profile(
    val userId: UserId,
    val displayName: String,
    val primaryRole: PrimaryRole,
    val battingHand: BattingHand?,
    val bowlingStyles: Set<BowlingStyle>,
    val weekStart: DayOfWeek,
    val weightUnit: WeightUnit,
    val timeZone: ZoneId,
    val adultConfirmedAt: Instant,
    val clientModifiedAt: Instant,
)
