package app.trak.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak

@Composable
fun LoadingView(modifier: Modifier = Modifier, label: String = "Loading") {
    Box(modifier.fillMaxSize().semantics { liveRegion = LiveRegionMode.Polite }, contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
        Text(label, modifier = Modifier.padding(top = Spacing.xxl), color = Trak.colors.textSecondary)
    }
}

/**
 * Honest empty state: one sentence of what will appear here, and at most one action.
 * Never shows sample or placeholder numbers.
 */
@Composable
fun EmptyView(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = Trak.colors.textSecondary, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            TrakButton(text = actionLabel, onClick = onAction, style = TrakButtonStyle.SECONDARY)
        }
    }
}

@Composable
fun ErrorView(message: String, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.s, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        if (onRetry != null) TrakButton(text = "Try again", onClick = onRetry, style = TrakButtonStyle.SECONDARY)
    }
}

/** Thin banner shown while offline. Saving still works; it says so plainly. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, text: String = "Offline. Changes are saved on this phone and will sync later.") {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = Trak.colors.textPrimary,
        modifier = modifier
            .fillMaxWidth()
            .background(Trak.colors.surfaceRaised)
            .padding(horizontal = Spacing.m, vertical = Spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
