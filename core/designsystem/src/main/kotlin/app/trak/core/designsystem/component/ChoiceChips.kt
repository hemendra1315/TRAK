package app.trak.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.trak.core.designsystem.theme.Spacing

/** One option in a chip group. [key] is stable and stored; [label] is display text. */
data class ChipOption<K>(val key: K, val label: String)

/**
 * Tap-to-select chips that wrap onto new lines. Use [multiSelect] for sets (e.g. bowling styles),
 * single selection otherwise. In single mode, tapping the selected chip clears it, so optional
 * questions can always go back to "not recorded".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <K> ChoiceChips(
    options: List<ChipOption<K>>,
    selected: Set<K>,
    onSelectionChange: (Set<K>) -> Unit,
    modifier: Modifier = Modifier,
    multiSelect: Boolean = false,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        options.forEach { option ->
            val isSelected = option.key in selected
            FilterChip(
                selected = isSelected,
                onClick = {
                    val next = when {
                        multiSelect && isSelected -> selected - option.key
                        multiSelect -> selected + option.key
                        isSelected -> emptySet()
                        else -> setOf(option.key)
                    }
                    onSelectionChange(next)
                },
                label = { Text(option.label) },
                modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
            )
        }
    }
}
