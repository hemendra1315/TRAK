package app.trak.guard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.trak.R
import app.trak.core.data.session.SessionCoordinator
import app.trak.core.designsystem.component.TrakButton
import app.trak.core.designsystem.component.TrakButtonStyle
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak
import app.trak.core.model.UserId
import app.trak.ui.TypedDeleteDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GuardUi(val confirming: Boolean = false, val typed: String = "", val wrong: Boolean = false)

/**
 * Review fix B3: the signed-in account can't see or continue over another account's unsynced data.
 * It can sign out (the data stays for its owner) or delete it with a typed confirmation.
 */
@HiltViewModel
class AccountGuardViewModel @Inject constructor(private val coordinator: SessionCoordinator) : ViewModel() {
    private val _ui = MutableStateFlow(GuardUi())
    val ui: StateFlow<GuardUi> = _ui.asStateFlow()

    fun signOut() = viewModelScope.launch { coordinator.signOutKeepingData() }
    fun startDelete() { _ui.value = GuardUi(confirming = true) }
    fun cancel() { _ui.value = GuardUi() }
    fun onTyped(t: String) { _ui.value = _ui.value.copy(typed = t, wrong = false) }

    fun confirm(newOwner: UserId, onResolved: () -> Unit) = viewModelScope.launch {
        if (coordinator.deleteOtherAccountData(_ui.value.typed, newOwner)) {
            _ui.value = GuardUi()
            onResolved()
        } else {
            _ui.value = _ui.value.copy(wrong = true)
        }
    }
}

@Composable
fun AccountGuardRoute(userId: UserId, unsyncedCount: Int, onResolved: () -> Unit, vm: AccountGuardViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Spacing.l).testTag("screen_guard"),
        verticalArrangement = Arrangement.spacedBy(Spacing.m, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.guard_title), style = MaterialTheme.typography.titleLarge)
        Text(pluralStringResource(R.plurals.guard_body, unsyncedCount, unsyncedCount), color = Trak.colors.textSecondary)
        TrakButton(stringResource(R.string.guard_sign_out), onClick = { vm.signOut() })
        TrakButton(stringResource(R.string.guard_delete), onClick = vm::startDelete, style = TrakButtonStyle.TEXT)
    }
    if (ui.confirming) {
        TypedDeleteDialog(
            title = stringResource(R.string.delete_confirm_title),
            body = stringResource(R.string.delete_confirm_body),
            actionLabel = stringResource(R.string.guard_delete_action),
            typed = ui.typed,
            wrong = ui.wrong,
            onTypedChange = vm::onTyped,
            onConfirm = { vm.confirm(userId, onResolved) },
            onDismiss = vm::cancel,
        )
    }
}
