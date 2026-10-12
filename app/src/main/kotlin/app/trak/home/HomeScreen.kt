package app.trak.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trak.R
import app.trak.core.designsystem.component.EmptyView
import app.trak.core.designsystem.theme.Spacing

/** Phase 1 Home: a greeting and an honest empty state. No numbers until real data exists. */
@Composable
fun HomeScreen(name: String?) {
    Column(Modifier.fillMaxSize().testTag("screen_home")) {
        if (name != null) {
            Text(
                stringResource(R.string.home_greeting, name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            )
        }
        EmptyView(
            title = stringResource(R.string.home_empty_title),
            body = stringResource(R.string.home_empty_body),
        )
    }
}
