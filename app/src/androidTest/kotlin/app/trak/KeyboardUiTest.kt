package app.trak

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.trak.core.designsystem.component.SaveBar
import app.trak.core.designsystem.component.TrakTextField
import app.trak.core.designsystem.theme.TrakTheme
import app.trak.main.EditProfileRoute
import app.trak.main.MainScaffold
import app.trak.onboarding.OnboardingContent
import app.trak.onboarding.OnboardingUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Acceptance test A12, keyboard parts: "keyboard Next moves focus" and "Save stays visible with the
 * keyboard open". Tests 4 and 5 need the device's soft keyboard to actually appear; if it never does
 * (for example an emulator with a hardware keyboard), they are reported as skipped, never as passed.
 */
@RunWith(AndroidJUnit4::class)
class KeyboardUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    // 1. Name is the only text field in onboarding. Its key is Done: it closes the keyboard (focus leaves
    //    the field) and does not submit, because role and the 18+ confirmation may still be missing.
    @Test
    fun onboardingNameDoneLeavesFieldWithoutSubmitting() {
        var submitted = false
        compose.setContent { TrakTheme { StatefulOnboarding(onContinue = { submitted = true }) } }
        val name = compose.onNode(hasSetTextAction())
        name.performClick()
        name.performTextInput("Arjun")
        name.assertIsFocused()
        name.performImeAction()
        name.assertIsNotFocused()
        assertFalse("Done must not submit the form", submitted)
    }

    // 2. Shared text field: Next moves focus to the next field (keyboard stays up); the last field's Done
    //    leaves the form. This is the default every future multi-field form inherits.
    @Test
    fun nextMovesFocusToTheNextFieldAndDoneLeavesTheLast() {
        compose.setContent {
            TrakTheme {
                Column {
                    var a by remember { mutableStateOf("") }
                    var b by remember { mutableStateOf("") }
                    TrakTextField(a, { a = it }, label = "First")
                    TrakTextField(b, { b = it }, label = "Second",
                        imeAction = ImeAction.Done)
                }
            }
        }
        val fields = compose.onAllNodes(hasSetTextAction())
        fields[0].performClick()
        fields[0].assertIsFocused()
        fields[0].performImeAction()
        fields[1].assertIsFocused()
        fields[0].assertIsNotFocused()
    }

    // 3. Small screen (320 x 480 dp) and 200% font: Continue is laid out inside the screen and reachable.
    @Test
    fun continueStaysOnScreenOnASmallScreenAtDoubleFontSize() {
        compose.setContent {
            TrakTheme {
                val d = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                    Box(Modifier.size(320.dp, 480.dp).testTag("small_screen")) {
                        StatefulOnboarding(onContinue = {})
                    }
                }
            }
        }
        val screen = compose.onNodeWithTag("small_screen").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithText("Continue").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Continue bottom ${button.bottom} is below the screen ${screen.bottom}", button.bottom <= screen.bottom + 1f)
        assertTrue("Continue top ${button.top} is above the screen ${screen.top}", button.top >= screen.top - 1f)
    }

    // 4. Onboarding with the real keyboard open, edge-to-edge like MainActivity, at 200% font:
    //    Continue sits entirely above the keyboard.
    @Test
    fun continueIsAboveTheKeyboardAtDoubleFontSize() {
        prepareEdgeToEdge()
        compose.setContent {
            TrakTheme {
                val d = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                    StatefulOnboarding(onContinue = {})
                }
            }
        }
        compose.onNode(hasSetTextAction()).performClick()
        val keyboardTop = showKeyboardAndGetItsTop()
        val button = compose.onNodeWithText("Continue").fetchSemanticsNode().boundsInWindow
        assertTrue("Continue bottom ${button.bottom} is under the keyboard top $keyboardTop", button.bottom <= keyboardTop + 1f)
    }

    // 5. A Save bar on a screen inside the five-tab shell (Edit profile) sits directly on the keyboard:
    //    not hidden by it, and not floating a tab bar's height above it (regression for double insets).
    @Test
    fun saveInsideTheShellSitsDirectlyOnTheKeyboard() {
        prepareEdgeToEdge()
        compose.setContent {
            TrakTheme {
                MainScaffold(
                    homeName = "Arjun",
                    // Profile immediately opens Edit profile, the shell's nested screen with a Save bar.
                    profileContent = { nav -> LaunchedEffect(Unit) { nav.navigate(EditProfileRoute) } },
                    editProfileContent = {
                        Scaffold(bottomBar = { SaveBar("Save", onSave = {}) }) { p ->
                            Column(Modifier.padding(p)) {
                                var v by remember { mutableStateOf("") }
                                TrakTextField(v, { v = it }, label = "Name")
                            }
                        }
                    },
                    settingsContent = {},
                )
            }
        }
        compose.onNodeWithTag("tab_profile").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Save").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performClick()
        val keyboardTop = showKeyboardAndGetItsTop()
        val button = compose.onNodeWithText("Save").fetchSemanticsNode().boundsInWindow
        val gap = keyboardTop - button.bottom
        val maxGapPx = with(compose.density) { 24.dp.toPx() }
        assertTrue("Save bottom ${button.bottom} is under the keyboard top $keyboardTop", gap >= -1f)
        assertTrue("Save floats ${gap}px above the keyboard (max $maxGapPx): insets counted twice", gap <= maxGapPx)
    }

    // --- helpers ---

    @Composable
    private fun StatefulOnboarding(onContinue: () -> Unit) {
        var state by remember { mutableStateOf(OnboardingUiState()) }
        OnboardingContent(
            state = state,
            onNameChange = { state = state.copy(name = it) },
            onRoleChange = { state = state.copy(role = it) },
            onAdultChange = { state = state.copy(adultConfirmed = it) },
            onContinue = onContinue,
        )
    }

    /** Same window setup as MainActivity: edge-to-edge and adjustResize. */
    private fun prepareEdgeToEdge() {
        compose.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
            @Suppress("DEPRECATION")
            activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    /** Asks for the soft keyboard and returns its top edge in window pixels; skips the test if it never shows. */
    private fun showKeyboardAndGetItsTop(): Float {
        compose.activityRule.scenario.onActivity { activity ->
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .show(WindowInsetsCompat.Type.ime())
        }
        fun imeBottom(): Int {
            var bottom = 0
            compose.activityRule.scenario.onActivity { activity ->
                bottom = ViewCompat.getRootWindowInsets(activity.window.decorView)
                    ?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
            }
            return bottom
        }
        val shown = runCatching { compose.waitUntil(10_000) { imeBottom() > 0 } }.isSuccess
        assumeTrue("The soft keyboard did not appear on this device; keyboard layout not checked", shown)
        // Let the IME animation finish and the layout settle.
        var last = -1
        compose.waitUntil(5_000) { val now = imeBottom(); (now == last).also { last = now } }
        compose.waitForIdle()
        var windowHeight = 0
        compose.activityRule.scenario.onActivity { windowHeight = it.window.decorView.height }
        return (windowHeight - imeBottom()).toFloat()
    }
}
