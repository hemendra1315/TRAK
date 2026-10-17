package app.trak

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.trak.core.designsystem.theme.TrakTheme
import app.trak.core.model.PrimaryRole
import app.trak.onboarding.OnboardingContent
import app.trak.onboarding.OnboardingUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Acceptance test A12: onboarding needs only name, role and the 18+ confirmation. */
@RunWith(AndroidJUnit4::class)
class OnboardingUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun continueIsEnabledOnlyWhenNameRoleAndAdultAreGiven() {
        var submitted: OnboardingUiState? = null
        compose.setContent {
            TrakTheme {
                var state by remember { mutableStateOf(OnboardingUiState()) }
                OnboardingContent(
                    state = state,
                    onNameChange = { state = state.copy(name = it) },
                    onRoleChange = { state = state.copy(role = it) },
                    onAdultChange = { state = state.copy(adultConfirmed = it) },
                    onContinue = { submitted = state },
                )
            }
        }
        val cont = compose.onNodeWithText("Continue")
        cont.assertIsNotEnabled()

        // As a player would: type the name, then Done closes the keyboard (on a small screen the open
        // keyboard leaves room only for the field and the Continue bar).
        val name = compose.onNode(hasSetTextAction())
        name.performClick()
        name.performTextInput("Arjun")
        name.performImeAction()
        cont.assertIsNotEnabled()

        compose.onNodeWithText("Bowler").performScrollTo().performClick()
        cont.assertIsNotEnabled()

        compose.onNodeWithTag("adult_confirmation").performScrollTo().performClick()
        cont.assertIsEnabled()

        cont.performClick()
        assertEquals("Arjun", submitted?.name)
        assertEquals(PrimaryRole.BOWLER, submitted?.role)
        assertEquals(true, submitted?.adultConfirmed)
    }

    @Test
    fun tappingTheSelectedRoleClearsIt() {
        compose.setContent {
            TrakTheme {
                var state by remember { mutableStateOf(OnboardingUiState(name = "A", adultConfirmed = true)) }
                OnboardingContent(state, { state = state.copy(name = it) }, { state = state.copy(role = it) },
                    { state = state.copy(adultConfirmed = it) }, {})
            }
        }
        compose.onNodeWithText("Batter").performScrollTo().performClick()
        compose.onNodeWithText("Continue").assertIsEnabled()
        compose.onNodeWithText("Batter").performScrollTo().performClick()
        compose.onNodeWithText("Continue").assertIsNotEnabled()
    }
}
