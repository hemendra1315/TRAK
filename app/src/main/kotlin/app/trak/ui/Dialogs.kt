package app.trak.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import app.trak.R
import app.trak.core.data.session.SessionCoordinator
import app.trak.core.designsystem.component.TrakButton
import app.trak.core.designsystem.component.TrakButtonStyle
import app.trak.core.designsystem.component.TrakTextField
import app.trak.core.designsystem.theme.Spacing

/** Shown when signing out with unsynced changes. "Sync now" is the default action (decision S1). */
@Composable
fun UnsyncedChangesDialog(
    count: Int,
    onSyncNow: () -> Unit,
    onKeepAndSignOut: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_unsynced"),
        title = { Text(stringResource(R.string.unsynced_title)) },
        text = { Text(pluralStringResource(R.plurals.unsynced_body, count, count)) },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                TrakButton(stringResource(R.string.sync_now), onClick = onSyncNow)
                TrakButton(stringResource(R.string.keep_and_sign_out), onClick = onKeepAndSignOut, style = TrakButtonStyle.SECONDARY)
                TrakButton(stringResource(R.string.delete_unsynced), onClick = onDelete, style = TrakButtonStyle.TEXT)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Deleting unsynced data needs the player to type the confirmation word; the button stays disabled until then. */
@Composable
fun TypedDeleteDialog(
    title: String,
    body: String,
    actionLabel: String,
    typed: String,
    wrong: Boolean,
    onTypedChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_delete"),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(body)
                TrakTextField(
                    value = typed,
                    onValueChange = onTypedChange,
                    label = stringResource(R.string.delete_confirm_label),
                    errorText = if (wrong) stringResource(R.string.delete_confirm_label) else null,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.testTag("delete_confirm_field"),
                )
            }
        },
        confirmButton = {
            TrakButton(
                text = actionLabel,
                onClick = onConfirm,
                enabled = typed.trim() == SessionCoordinator.CONFIRMATION_WORD,
                style = TrakButtonStyle.TEXT,
                modifier = Modifier.testTag("delete_confirm_button"),
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
