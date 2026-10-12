package app.trak.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.trak.R
import app.trak.auth.SignInRoute
import app.trak.core.data.session.AppEntryState
import app.trak.core.designsystem.component.EmptyView
import app.trak.core.designsystem.component.LoadingView
import app.trak.guard.AccountGuardRoute
import app.trak.main.MainShell
import app.trak.onboarding.OnboardingRoute

/** Top-level switch driven entirely by [AppEntryState]. */
@Composable
fun RootScreen(vm: RootViewModel) {
    val entry by vm.entry.collectAsStateWithLifecycle()
    when (val s = entry) {
        AppEntryState.Loading -> LoadingView(label = stringResource(R.string.loading))
        is AppEntryState.SignedOut -> SignInRoute()
        is AppEntryState.BlockedByOtherAccount -> AccountGuardRoute(s.userId, s.unsyncedCount, onResolved = vm::retry)
        is AppEntryState.ProfileCheckOffline -> EmptyView(
            title = stringResource(R.string.profile_check_offline_title),
            body = stringResource(R.string.profile_check_offline_body),
            actionLabel = stringResource(R.string.try_again),
            onAction = vm::retry,
        )
        is AppEntryState.NeedsOnboarding -> OnboardingRoute(s.userId)
        is AppEntryState.Ready -> MainShell(s.userId)
    }
}
