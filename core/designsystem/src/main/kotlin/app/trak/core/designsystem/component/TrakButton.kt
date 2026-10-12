package app.trak.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.trak.core.designsystem.theme.Spacing

enum class TrakButtonStyle { PRIMARY, SECONDARY, TEXT }

/**
 * The one button used across TRAK. Primary is 56 dp tall and full width by default.
 * While [loading], the label is replaced by a spinner and clicks are ignored.
 */
@Composable
fun TrakButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: TrakButtonStyle = TrakButtonStyle.PRIMARY,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val sized = modifier
        .heightIn(min = if (style == TrakButtonStyle.TEXT) Spacing.minTouchTarget else Spacing.primaryButtonHeight)
        .semantics { if (loading) stateDescription = "Working" }
    val content: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
    val active = enabled && !loading
    when (style) {
        TrakButtonStyle.PRIMARY -> Button(onClick = onClick, enabled = active, modifier = sized.fillMaxWidth()) { content() }
        TrakButtonStyle.SECONDARY -> OutlinedButton(onClick = onClick, enabled = active, modifier = sized.fillMaxWidth()) { content() }
        TrakButtonStyle.TEXT -> TextButton(onClick = onClick, enabled = active, modifier = sized) { content() }
    }
}
