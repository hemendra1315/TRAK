package app.trak.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * TRAK colour tokens. Near-black neutral surfaces, one teal accent (decision D2), no green-tinted
 * backgrounds. Every text/background and border/background pair is asserted by ContrastTest
 * (text ≥ 4.5:1, borders and icons ≥ 3:1) for both themes.
 */
@Immutable
data class TrakColorTokens(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val onAccent: Color,
    /** Decorative dividers only; never used as an input or control boundary. */
    val divider: Color,
    /** Input and control boundaries (≥ 3:1 against every surface). */
    val border: Color,
    val warning: Color,
    val error: Color,
    val isDark: Boolean,
)

object TrakColors {
    val Dark = TrakColorTokens(
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF111111),
        surfaceRaised = Color(0xFF161616),
        textPrimary = Color(0xFFF5F5F5),
        textSecondary = Color(0xFFA3A3A3),
        textTertiary = Color(0xFF8A8A8A),
        accent = Color(0xFF2DD4BF),
        onAccent = Color(0xFF0A0A0A),
        divider = Color(0xFF262626),
        border = Color(0xFF6B6B6B),
        warning = Color(0xFFF5B83D),
        error = Color(0xFFF87171),
        isDark = true,
    )

    /** Ground Mode: high-contrast light theme for direct sunlight at the ground. */
    val Ground = TrakColorTokens(
        background = Color(0xFFFFFFFF),
        surface = Color(0xFFF2F2F2),
        surfaceRaised = Color(0xFFE8E8E8),
        textPrimary = Color(0xFF000000),
        textSecondary = Color(0xFF3D3D3D),
        textTertiary = Color(0xFF575757),
        accent = Color(0xFF115E59),
        onAccent = Color(0xFFFFFFFF),
        divider = Color(0xFFD4D4D4),
        border = Color(0xFF767676),
        warning = Color(0xFF92400E),
        error = Color(0xFFB91C1C),
        isDark = false,
    )
}
