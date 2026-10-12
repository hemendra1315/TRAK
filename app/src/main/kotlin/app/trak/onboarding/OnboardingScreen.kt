package app.trak.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trak.R
import app.trak.core.designsystem.component.SaveBar
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak
import app.trak.core.model.PrimaryRole
import app.trak.core.model.UserId
import app.trak.ui.NameAndRoleFields

@Composable
fun OnboardingRoute(userId: UserId, vm: OnboardingViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        onNameChange = vm::onNameChange,
        onRoleChange = vm::onRoleChange,
        onAdultChange = vm::onAdultChange,
        onContinue = { vm.save(userId) },
    )
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onRoleChange: (PrimaryRole?) -> Unit,
    onAdultChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SaveBar(
                label = stringResource(R.string.continue_label),
                onSave = onContinue,
                enabled = state.canContinue,
                saving = state.saving,
                modifier = Modifier.testTag("onboarding_continue"),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.onboarding_body), color = Trak.colors.textSecondary)
            NameAndRoleFields(
                name = state.name,
                onNameChange = onNameChange,
                role = state.role,
                onRoleChange = onRoleChange,
                errors = state.errors,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Spacing.minTouchTarget)
                    .toggleable(value = state.adultConfirmed, role = Role.Checkbox, onValueChange = onAdultChange)
                    .testTag("adult_confirmation"),
            ) {
                Checkbox(checked = state.adultConfirmed, onCheckedChange = null)
                Column(Modifier.padding(start = Spacing.xs)) {
                    Text(stringResource(R.string.adult_confirmation), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.adult_explainer), style = MaterialTheme.typography.bodyMedium,
                        color = Trak.colors.textSecondary)
                }
            }
        }
    }
}
