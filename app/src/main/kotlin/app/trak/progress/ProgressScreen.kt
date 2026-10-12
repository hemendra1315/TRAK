package app.trak.progress

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trak.R
import app.trak.core.designsystem.component.EmptyView

/** Phase 1: honest empty state. Phase 4 adds graphs computed only in :analytics. */
@Composable
fun ProgressScreen() {
    EmptyView(
        title = stringResource(R.string.progress_empty_title),
        body = stringResource(R.string.progress_empty_body),
        modifier = Modifier.testTag("screen_progress"),
    )
}
