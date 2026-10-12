package app.trak.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Theme modes offered in Settings. Ground Mode is the high-contrast light theme for outdoor use. */
enum class ThemeMode { DARK, GROUND, SYSTEM }

val LocalTrakColors = staticCompositionLocalOf { TrakColors.Dark }

private fun TrakColorTokens.toMaterial() = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        secondary = textSecondary, onSecondary = background,
        background = background, onBackground = textPrimary,
        surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceRaised, onSurfaceVariant = textSecondary,
        surfaceContainer = surface, surfaceContainerHigh = surfaceRaised, surfaceContainerHighest = surfaceRaised,
        surfaceContainerLow = surface, surfaceContainerLowest = background,
        outline = border, outlineVariant = divider,
        error = error, onError = background,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        secondary = textSecondary, onSecondary = background,
        background = background, onBackground = textPrimary,
        surface = surface, onSurface = textPrimary,
        surfaceVariant = surfaceRaised, onSurfaceVariant = textSecondary,
        surfaceContainer = surface, surfaceContainerHigh = surfaceRaised, surfaceContainerHighest = surfaceRaised,
        surfaceContainerLow = surface, surfaceContainerLowest = background,
        outline = border, outlineVariant = divider,
        error = error, onError = background,
    )
}

private val TrakShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun TrakTheme(mode: ThemeMode = ThemeMode.DARK, content: @Composable () -> Unit) {
    val tokens = when (mode) {
        ThemeMode.DARK -> TrakColors.Dark
        ThemeMode.GROUND -> TrakColors.Ground
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) TrakColors.Dark else TrakColors.Ground
    }
    CompositionLocalProvider(LocalTrakColors provides tokens) {
        MaterialTheme(
            colorScheme = tokens.toMaterial(),
            typography = TrakTypography,
            shapes = TrakShapes,
            content = content,
        )
    }
}

/** Access TRAK tokens that Material's scheme doesn't carry (tertiary text, warning). */
object Trak {
    val colors: TrakColorTokens
        @Composable get() = LocalTrakColors.current
}
