package app.trak.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Theme modes offered in Settings. Ground Mode is the high-contrast light theme for outdoor use. */
enum class ThemeMode { DARK, GROUND, SYSTEM }

/**
 * Colour tokens. Neutral near-black surfaces with one teal accent (decision D2).
 * Contrast ratios are documented in the Phase 1 plan and asserted by ContrastTest (step 4).
 */
object TrakColors {
    object Dark {
        val background = Color(0xFF0A0A0A)
        val surface = Color(0xFF111111)
        val surfaceRaised = Color(0xFF161616)
        val textPrimary = Color(0xFFF5F5F5)
        val textSecondary = Color(0xFFA3A3A3)
        val accent = Color(0xFF2DD4BF)
        val onAccent = Color(0xFF0A0A0A)
        val error = Color(0xFFF87171)
    }

    object Ground {
        val background = Color(0xFFFFFFFF)
        val surface = Color(0xFFF2F2F2)
        val surfaceRaised = Color(0xFFE8E8E8)
        val textPrimary = Color(0xFF000000)
        val textSecondary = Color(0xFF3D3D3D)
        val accent = Color(0xFF0F766E)
        val onAccent = Color(0xFFFFFFFF)
        val error = Color(0xFFB91C1C)
    }
}

private val DarkScheme = darkColorScheme(
    primary = TrakColors.Dark.accent,
    onPrimary = TrakColors.Dark.onAccent,
    background = TrakColors.Dark.background,
    onBackground = TrakColors.Dark.textPrimary,
    surface = TrakColors.Dark.surface,
    onSurface = TrakColors.Dark.textPrimary,
    surfaceVariant = TrakColors.Dark.surfaceRaised,
    onSurfaceVariant = TrakColors.Dark.textSecondary,
    error = TrakColors.Dark.error,
)

private val GroundScheme = lightColorScheme(
    primary = TrakColors.Ground.accent,
    onPrimary = TrakColors.Ground.onAccent,
    background = TrakColors.Ground.background,
    onBackground = TrakColors.Ground.textPrimary,
    surface = TrakColors.Ground.surface,
    onSurface = TrakColors.Ground.textPrimary,
    surfaceVariant = TrakColors.Ground.surfaceRaised,
    onSurfaceVariant = TrakColors.Ground.textSecondary,
    error = TrakColors.Ground.error,
)

@Composable
fun TrakTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val useDark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.GROUND -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(colorScheme = if (useDark) DarkScheme else GroundScheme, content = content)
}
