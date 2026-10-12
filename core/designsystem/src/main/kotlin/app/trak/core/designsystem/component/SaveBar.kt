package app.trak.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak

/**
 * Sticky bottom bar holding the screen's one Save action. It rides above the keyboard
 * (imePadding) so saving never requires closing the keyboard or scrolling.
 * Place it in a Scaffold's bottomBar.
 */
@Composable
fun SaveBar(
    label: String,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    saving: Boolean = false,
    hint: String? = null,
) {
    Surface(color = MaterialTheme.colorScheme.background, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.navigationBarsPadding().imePadding()) {
            HorizontalDivider(color = Trak.colors.divider)
            Column(Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s)) {
                if (hint != null) {
                    Text(hint, style = MaterialTheme.typography.bodyMedium, color = Trak.colors.textSecondary,
                        modifier = Modifier.padding(bottom = Spacing.xs))
                }
                TrakButton(text = label, onClick = onSave, enabled = enabled, loading = saving)
            }
        }
    }
}
