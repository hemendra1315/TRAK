package app.trak.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trak.R
import app.trak.core.designsystem.component.TrakButton
import app.trak.core.designsystem.component.TrakButtonStyle
import app.trak.core.designsystem.component.TrakTextField
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak

@Composable
fun SignInRoute(vm: SignInViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    SignInContent(
        state = state,
        onEmailChange = vm::onEmailChange,
        onSendCode = vm::sendCode,
        onCodeChange = vm::onCodeChange,
        onVerify = vm::verify,
        onResend = vm::resend,
        onBack = vm::backToEmail,
    )
}

@Composable
fun SignInContent(
    state: SignInUiState,
    onEmailChange: (String) -> Unit,
    onSendCode: () -> Unit,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
        Text(stringResource(R.string.tagline), color = Trak.colors.textSecondary)

        val errorText = state.error?.let { errorMessage(it, state.errorDetail) }
        when (state.step) {
            SignInStep.EMAIL -> {
                Text(stringResource(R.string.sign_in_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.l))
                Text(stringResource(R.string.sign_in_body), color = Trak.colors.textSecondary)
                TrakTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = stringResource(R.string.email_label),
                    errorText = errorText,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Send,
                    onImeAction = onSendCode,
                )
                TrakButton(stringResource(R.string.send_code), onClick = onSendCode, loading = state.busy)
            }
            SignInStep.CODE -> {
                Text(stringResource(R.string.code_title), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = Spacing.l))
                Text(stringResource(R.string.code_body, state.email), color = Trak.colors.textSecondary)
                TrakTextField(
                    value = state.code,
                    onValueChange = onCodeChange,
                    label = stringResource(R.string.code_label),
                    errorText = errorText,
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                    onImeAction = onVerify,
                )
                TrakButton(stringResource(R.string.verify), onClick = onVerify, loading = state.busy)
                TrakButton(
                    text = if (state.resendSeconds > 0) stringResource(R.string.resend_in, state.resendSeconds)
                    else stringResource(R.string.resend_code),
                    onClick = onResend,
                    style = TrakButtonStyle.TEXT,
                    enabled = state.resendSeconds == 0 && !state.busy,
                )
                TrakButton(stringResource(R.string.use_different_email), onClick = onBack, style = TrakButtonStyle.TEXT)
            }
        }
    }
}

@Composable
private fun errorMessage(error: SignInError, detail: String?): String = when (error) {
    SignInError.EMAIL_INVALID -> stringResource(R.string.error_email_invalid)
    SignInError.CODE_INVALID -> stringResource(R.string.error_code_invalid)
    SignInError.CODE_WRONG -> stringResource(R.string.error_code_wrong)
    SignInError.RATE_LIMITED -> stringResource(R.string.error_rate_limited)
    SignInError.NETWORK -> stringResource(R.string.error_network)
    SignInError.NOT_CONFIGURED -> stringResource(R.string.not_configured)
    SignInError.OTHER -> stringResource(R.string.error_generic, detail ?: "")
}
