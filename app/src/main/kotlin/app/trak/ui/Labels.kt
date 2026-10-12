package app.trak.ui

import androidx.annotation.StringRes
import app.trak.R
import app.trak.core.designsystem.theme.ThemeMode
import app.trak.core.model.AppTheme
import app.trak.core.model.BattingHand
import app.trak.core.model.BowlingStyle
import app.trak.core.model.PrimaryRole
import app.trak.core.model.WeightUnit
import java.time.DayOfWeek

@StringRes fun PrimaryRole.label(): Int = when (this) {
    PrimaryRole.BATTER -> R.string.role_batter
    PrimaryRole.BOWLER -> R.string.role_bowler
    PrimaryRole.ALL_ROUNDER -> R.string.role_all_rounder
    PrimaryRole.WICKETKEEPER -> R.string.role_wicketkeeper
    PrimaryRole.WICKETKEEPER_BATTER -> R.string.role_wicketkeeper_batter
    PrimaryRole.OTHER -> R.string.role_other
}

@StringRes fun BattingHand.label(): Int = when (this) {
    BattingHand.RIGHT -> R.string.hand_right
    BattingHand.LEFT -> R.string.hand_left
}

@StringRes fun BowlingStyle.label(): Int = when (this) {
    BowlingStyle.FAST -> R.string.style_fast
    BowlingStyle.MEDIUM -> R.string.style_medium
    BowlingStyle.OFF_SPIN -> R.string.style_off_spin
    BowlingStyle.LEG_SPIN -> R.string.style_leg_spin
    BowlingStyle.LEFT_ARM_ORTHODOX -> R.string.style_left_arm_orthodox
    BowlingStyle.WRIST_SPIN_OTHER -> R.string.style_wrist_spin_other
    BowlingStyle.OTHER -> R.string.style_other
}

@StringRes fun WeightUnit.label(): Int = when (this) {
    WeightUnit.KG -> R.string.unit_kg
    WeightUnit.LB -> R.string.unit_lb
}

@StringRes fun AppTheme.label(): Int = when (this) {
    AppTheme.DARK -> R.string.theme_dark
    AppTheme.GROUND -> R.string.theme_ground
    AppTheme.SYSTEM -> R.string.theme_system
}

/** Week-start choices offered in Settings (the profile accepts any ISO day). */
val WeekStartChoices = listOf(DayOfWeek.MONDAY to R.string.day_monday, DayOfWeek.SUNDAY to R.string.day_sunday,
    DayOfWeek.SATURDAY to R.string.day_saturday)

fun AppTheme.toThemeMode(): ThemeMode = when (this) {
    AppTheme.DARK -> ThemeMode.DARK
    AppTheme.GROUND -> ThemeMode.GROUND
    AppTheme.SYSTEM -> ThemeMode.SYSTEM
}
