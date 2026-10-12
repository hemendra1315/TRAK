package app.trak.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance test A5: WCAG 2.x contrast for every token pair, both themes.
 * Text ≥ 4.5:1; borders, icons and the accent used as a control colour ≥ 3:1 (text use ≥ 4.5:1).
 */
class ContrastTest {

    private fun channel(c: Float): Double =
        if (c <= 0.04045f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun ratio(a: Color, b: Color): Double {
        val la = luminance(a); val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private fun assertContrast(name: String, fg: Color, bg: Color, minimum: Double) {
        val r = ratio(fg, bg)
        assertTrue("$name is ${"%.2f".format(r)}:1, needs ≥ $minimum:1", r >= minimum)
    }

    private fun checkTheme(themeName: String, t: TrakColorTokens) {
        val backgrounds = mapOf("background" to t.background, "surface" to t.surface, "surfaceRaised" to t.surfaceRaised)
        val text = mapOf(
            "textPrimary" to t.textPrimary, "textSecondary" to t.textSecondary, "textTertiary" to t.textTertiary,
            "accent" to t.accent, "warning" to t.warning, "error" to t.error,
        )
        for ((bgName, bg) in backgrounds) {
            for ((fgName, fg) in text) assertContrast("$themeName $fgName on $bgName", fg, bg, 4.5)
            assertContrast("$themeName border on $bgName", t.border, bg, 3.0)
        }
        assertContrast("$themeName onAccent on accent", t.onAccent, t.accent, 4.5)
    }

    @Test fun `dark theme meets WCAG contrast`() = checkTheme("Dark", TrakColors.Dark)

    @Test fun `ground mode meets WCAG contrast`() = checkTheme("Ground", TrakColors.Ground)

    @Test
    fun `no surface is green tinted`() {
        // Neutral surfaces: red, green and blue channels are equal (blueprint palette replaced, decision C1).
        for (t in listOf(TrakColors.Dark, TrakColors.Ground)) {
            for (c in listOf(t.background, t.surface, t.surfaceRaised)) {
                assertTrue("surface $c must be neutral", c.red == c.green && c.green == c.blue)
            }
        }
    }
}
