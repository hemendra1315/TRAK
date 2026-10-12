package app.trak

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.trak.core.designsystem.theme.TrakTheme
import app.trak.ui.TypedDeleteDialog
import app.trak.ui.UnsyncedChangesDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Acceptance test A14 (UI): unsynced changes are never discarded without a typed confirmation. */
@RunWith(AndroidJUnit4::class)
class SignOutDialogsUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun unsyncedDialogShowsCountAndOffersKeep() {
        var kept = false
        compose.setContent {
            TrakTheme {
                UnsyncedChangesDialog(count = 1, onSyncNow = {}, onKeepAndSignOut = { kept = true }, onDelete = {}, onDismiss = {})
            }
        }
        compose.onNodeWithText("1 change on this phone hasn't reached the server yet.").assertIsDisplayed()
        compose.onNodeWithText("Sync now").assertIsDisplayed()
        compose.onNodeWithText("Keep on this phone and sign out").performClick()
        assertTrue(kept)
    }

    @Test
    fun deleteStaysDisabledUntilDeleteIsTyped() {
        var confirmed = 0
        compose.setContent {
            TrakTheme {
                var typed by remember { mutableStateOf("") }
                TypedDeleteDialog("Delete?", "Type DELETE", "Delete and sign out", typed, false,
                    onTypedChange = { typed = it }, onConfirm = { confirmed++ }, onDismiss = {})
            }
        }
        val button = compose.onNodeWithTag("delete_confirm_button")
        button.assertIsNotEnabled()
        compose.onNodeWithTag("delete_confirm_field").performTextInput("delete")
        button.assertIsNotEnabled()
        compose.onNodeWithTag("delete_confirm_field").performTextClearance()
        compose.onNodeWithTag("delete_confirm_field").performTextInput("DELETE")
        button.assertIsEnabled()
        button.performClick()
        assertEquals(1, confirmed)
    }
}
