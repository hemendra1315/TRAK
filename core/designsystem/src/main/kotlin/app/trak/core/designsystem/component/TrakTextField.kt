package app.trak.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Text field with a visible label and inline error. [errorText] non-null shows the error under the
 * field and announces it to TalkBack. [imeAction] Next/Done drives keyboard-friendly entry.
 *
 * When [onImeAction] is null the keyboard key does the platform default: Next moves focus to the
 * next field without closing the keyboard, Done closes the keyboard. Pass a handler only when the key
 * should do something else (send a code, verify, leave the field).
 */
@Composable
fun TrakTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = errorText != null,
        supportingText = (errorText ?: supportingText)?.let { msg ->
            { Text(msg, style = MaterialTheme.typography.bodyMedium) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = {
            if (onImeAction != null) onImeAction() else defaultKeyboardAction(imeAction)
        }),
        singleLine = singleLine,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (errorText != null) error(errorText) },
    )
}
