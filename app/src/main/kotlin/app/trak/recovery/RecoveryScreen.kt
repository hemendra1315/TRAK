package app.trak.recovery

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.trak.R
import app.trak.core.designsystem.component.EmptyView

/** Phase 1: honest empty state. Phase 3 shows today and the previous 7 days (decision E6). */
@Composable
fun RecoveryScreen() {
    EmptyView(
        title = stringResource(R.string.recovery_empty_title),
        body = stringResource(R.string.recovery_empty_body),
        modifier = Modifier.testTag("screen_recovery"),
    )
}
