package app.trak.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trak.R
import app.trak.core.data.profile.LocalProfile
import app.trak.core.designsystem.component.ChipOption
import app.trak.core.designsystem.component.ChoiceChips
import app.trak.core.designsystem.component.LoadingView
import app.trak.core.designsystem.component.SaveBar
import app.trak.core.designsystem.component.TrakButton
import app.trak.core.designsystem.component.TrakButtonStyle
import app.trak.core.designsystem.theme.Spacing
import app.trak.core.designsystem.theme.Trak
import app.trak.core.model.AppTheme
import app.trak.core.model.BattingHand
import app.trak.core.model.BowlingStyle
import app.trak.core.model.SyncState
import app.trak.core.model.WeightUnit
import app.trak.ui.NameAndRoleFields
import app.trak.ui.SectionLabel
import app.trak.ui.TypedDeleteDialog
import app.trak.ui.UnsyncedChangesDialog
import app.trak.ui.WeekStartChoices
import app.trak.ui.label

@Composable
fun ProfileScreen(vm: ProfileViewModel, onEdit: () -> Unit, onSettings: () -> Unit) {
    val local by vm.profile.collectAsStateWithLifecycle()
    val signOut by vm.signOut.collectAsStateWithLifecycle()
    val p = local ?: return LoadingView()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l)
            .testTag("screen_profile"),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(p.profile.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(p.profile.primaryRole.label()), color = Trak.colors.textSecondary)
        SyncStatusLine(p)
        HorizontalDivider(color = Trak.colors.divider)
        TrakButton(stringResource(R.string.edit_profile), onClick = onEdit, style = TrakButtonStyle.SECONDARY)
        TrakButton(stringResource(R.string.settings), onClick = onSettings, style = TrakButtonStyle.SECONDARY)
        TrakButton(stringResource(R.string.sign_out), onClick = { vm.requestSignOut() }, style = TrakButtonStyle.TEXT,
            modifier = Modifier.testTag("sign_out"))
    }
    when (val s = signOut) {
        SignOutUi.None -> Unit
        is SignOutUi.Unsynced -> UnsyncedChangesDialog(
            count = s.count,
            onSyncNow = vm::syncThenRetrySignOut,
            onKeepAndSignOut = { vm.keepAndSignOut() },
            onDelete = vm::startDelete,
            onDismiss = vm::dismissSignOut,
        )
        is SignOutUi.ConfirmDelete -> TypedDeleteDialog(
            title = stringResource(R.string.delete_confirm_title),
            body = stringResource(R.string.delete_confirm_body),
            actionLabel = stringResource(R.string.delete_confirm_action),
            typed = s.typed,
            wrong = s.wrong,
            onTypedChange = { vm.onDeleteTyped(it) },
            onConfirm = { vm.confirmDelete() },
            onDismiss = vm::dismissSignOut,
        )
    }
}

@Composable
private fun SyncStatusLine(p: LocalProfile) {
    val text = when (p.syncState) {
        SyncState.SYNCED -> stringResource(R.string.sync_status_synced)
        SyncState.PENDING -> stringResource(R.string.sync_status_pending)
        SyncState.FAILED -> stringResource(R.string.sync_status_failed, p.syncError ?: "")
    }
    Text(text, style = MaterialTheme.typography.bodyMedium,
        color = if (p.syncState == SyncState.FAILED) MaterialTheme.colorScheme.error else Trak.colors.textSecondary,
        modifier = Modifier.testTag("sync_status"))
}

@Composable
fun EditProfileScreen(vm: ProfileViewModel, onDone: () -> Unit) {
    val local by vm.profile.collectAsStateWithLifecycle()
    val errors by vm.editErrors.collectAsStateWithLifecycle()
    val p = local?.profile ?: return LoadingView()
    var name by rememberSaveable { mutableStateOf(p.displayName) }
    var role by rememberSaveable { mutableStateOf(p.primaryRole) }
    var hand by rememberSaveable { mutableStateOf(p.battingHand) }
    var styles by rememberSaveable { mutableStateOf(p.bowlingStyles.toList()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            SaveBar(stringResource(R.string.save), onSave = {
                vm.saveProfile(name, role, hand, styles.toSet(), onSaved = onDone)
            })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
            verticalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Text(stringResource(R.string.edit_profile), style = MaterialTheme.typography.headlineSmall)
            NameAndRoleFields(name, { name = it }, role, { r -> r?.let { role = it } }, errors)
            SectionLabel(stringResource(R.string.batting_hand_label), stringResource(R.string.optional))
            ChoiceChips(
                options = BattingHand.entries.map { ChipOption(it, stringResource(it.label())) },
                selected = setOfNotNull(hand),
                onSelectionChange = { hand = it.firstOrNull() },
            )
            SectionLabel(stringResource(R.string.bowling_styles_label), stringResource(R.string.optional))
            ChoiceChips(
                options = BowlingStyle.entries.map { ChipOption(it, stringResource(it.label())) },
                selected = styles.toSet(),
                onSelectionChange = { styles = it.toList() },
                multiSelect = true,
            )
        }
    }
}

@Composable
fun SettingsScreen(vm: ProfileViewModel) {
    val local by vm.profile.collectAsStateWithLifecycle()
    val theme by vm.theme.collectAsStateWithLifecycle()
    val p = local ?: return LoadingView()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenHorizontal, vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.m),
    ) {
        Text(stringResource(R.string.settings), style = MaterialTheme.typography.headlineSmall)
        SectionLabel(stringResource(R.string.theme_label))
        ChoiceChips(
            options = AppTheme.entries.map { ChipOption(it, stringResource(it.label())) },
            selected = setOf(theme),
            onSelectionChange = { sel -> sel.firstOrNull()?.let { vm.setTheme(it) } },
        )
        SectionLabel(stringResource(R.string.week_start_label))
        ChoiceChips(
            options = WeekStartChoices.map { (day, label) -> ChipOption(day, stringResource(label)) },
            selected = setOf(p.profile.weekStart),
            onSelectionChange = { sel -> sel.firstOrNull()?.let { vm.setWeekStart(it) } },
        )
        SectionLabel(stringResource(R.string.weight_unit_label))
        ChoiceChips(
            options = WeightUnit.entries.map { ChipOption(it, stringResource(it.label())) },
            selected = setOf(p.profile.weightUnit),
            onSelectionChange = { sel -> sel.firstOrNull()?.let { vm.setWeightUnit(it) } },
        )
        HorizontalDivider(color = Trak.colors.divider)
        SyncStatusLine(p)
        TrakButton(stringResource(R.string.sync_now), onClick = vm::syncNow, style = TrakButtonStyle.SECONDARY,
            enabled = p.syncState != SyncState.SYNCED)
    }
}
