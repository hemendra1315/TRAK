package app.trak

import androidx.compose.material3.Text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.trak.core.designsystem.theme.ThemeMode
import app.trak.core.designsystem.theme.TrakTheme
import app.trak.main.MainScaffold
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Acceptance test A13: five tabs, honest empty states with no numbers. */
@RunWith(AndroidJUnit4::class)
class MainShellUiTest {
    @get:Rule val compose = createComposeRule()

    private val anyDigit = SemanticsMatcher("text contains a digit") { node ->
        node.config.getOrNull(SemanticsProperties.Text)?.any { t -> t.text.any(Char::isDigit) } == true
    }

    private fun setShell(mode: ThemeMode) = compose.setContent {
        TrakTheme(mode) {
            MainScaffold(
                homeName = "Arjun",
                profileContent = { Text("Profile content") },
                editProfileContent = {},
                settingsContent = {},
            )
        }
    }

    @Test
    fun allFiveTabsAreReachableAndShowNoNumbers() {
        setShell(ThemeMode.DARK)
        compose.onNodeWithTag("screen_home").assertIsDisplayed()
        compose.onAllNodes(anyDigit).assertCountEquals(0)

        compose.onNodeWithTag("tab_log").performClick()
        compose.onNodeWithTag("screen_journal").assertIsDisplayed()
        // The journal header shows today's date; that is the only place digits may appear in Phase 1.

        compose.onNodeWithTag("tab_progress").performClick()
        compose.onNodeWithTag("screen_progress").assertIsDisplayed()
        compose.onAllNodes(anyDigit).assertCountEquals(0)

        compose.onNodeWithTag("tab_recovery").performClick()
        compose.onNodeWithTag("screen_recovery").assertIsDisplayed()
        compose.onAllNodes(anyDigit).assertCountEquals(0)

        compose.onNodeWithTag("tab_profile").performClick()
        compose.onAllNodesWithText("Profile content").assertCountEquals(1)

        compose.onNodeWithTag("tab_home").performClick()
        compose.onNodeWithTag("screen_home").assertIsDisplayed()
    }

    @Test
    fun groundModeRendersTheSameShell() {
        setShell(ThemeMode.GROUND)
        compose.onNodeWithTag("tab_progress").performClick()
        compose.onNodeWithTag("screen_progress").assertIsDisplayed()
    }
}
