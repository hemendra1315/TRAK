package app.trak.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.trak.R
import app.trak.core.designsystem.component.ChipOption
import app.trak.core.designsystem.component.ChoiceChips
import app.trak.core.designsystem.component.TrakTextField
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak
import app.trak.core.model.PrimaryRole
import app.trak.core.model.validation.ErrorCode
import app.trak.core.model.validation.ProfileRules
import app.trak.core.model.validation.ValidationError

/** Name + main role, shared by onboarding and edit profile so the questions are asked one way. */
@Composable
fun NameAndRoleFields(
    name: String,
    onNameChange: (String) -> Unit,
    role: PrimaryRole?,
    onRoleChange: (PrimaryRole?) -> Unit,
    errors: List<ValidationError>,
) {
    val nameError = errors.firstOrNull { it.field == ProfileRules.FIELD_DISPLAY_NAME }?.code?.let {
        if (it == ErrorCode.TOO_LONG) stringResource(R.string.error_name_too_long) else stringResource(R.string.error_name_required)
    }
    TrakTextField(
        value = name,
        onValueChange = onNameChange,
        label = stringResource(R.string.display_name_label),
        errorText = nameError,
    )
    Spacer(Modifier.height(Spacing.s))
    SectionLabel(stringResource(R.string.primary_role_label))
    ChoiceChips(
        options = PrimaryRole.entries.map { ChipOption(it, stringResource(it.label())) },
        selected = setOfNotNull(role),
        onSelectionChange = { onRoleChange(it.firstOrNull()) },
    )
    if (errors.any { it.field == "primaryRole" }) {
        Text(stringResource(R.string.error_role_required), color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SectionLabel(text: String, hint: String? = null) {
    Column {
        Text(text, style = MaterialTheme.typography.titleMedium)
        if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = Trak.colors.textSecondary)
    }
}
