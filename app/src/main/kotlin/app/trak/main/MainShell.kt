package app.trak.main

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.trak.R
import app.trak.core.model.UserId
import app.trak.home.HomeScreen
import app.trak.log.JournalScreen
import app.trak.profile.EditProfileScreen
import app.trak.profile.ProfileScreen
import app.trak.profile.ProfileViewModel
import app.trak.profile.SettingsScreen
import app.trak.progress.ProgressScreen
import app.trak.recovery.RecoveryScreen
import app.trak.ui.TrakIcons
import kotlin.reflect.KClass

private data class Tab(val route: Any, val routeClass: KClass<*>, val label: Int, val icon: ImageVector, val tag: String)

private val Tabs = listOf(
    Tab(HomeRoute, HomeRoute::class, R.string.tab_home, TrakIcons.Home, "tab_home"),
    Tab(JournalRoute(), JournalRoute::class, R.string.tab_log, TrakIcons.Log, "tab_log"),
    Tab(ProgressRoute, ProgressRoute::class, R.string.tab_progress, TrakIcons.Progress, "tab_progress"),
    Tab(RecoveryRoute, RecoveryRoute::class, R.string.tab_recovery, TrakIcons.Recovery, "tab_recovery"),
    Tab(ProfileRoute, ProfileRoute::class, R.string.tab_profile, TrakIcons.Profile, "tab_profile"),
)

@Composable
fun MainShell(userId: UserId, profileVm: ProfileViewModel = hiltViewModel()) {
    LaunchedEffect(userId) { profileVm.bind(userId) }
    val profile by profileVm.profile.collectAsStateWithLifecycle()
    MainScaffold(
        homeName = profile?.profile?.displayName,
        profileContent = { nav ->
            ProfileScreen(profileVm, onEdit = { nav.navigate(EditProfileRoute) }, onSettings = { nav.navigate(SettingsRoute) })
        },
        editProfileContent = { nav -> EditProfileScreen(profileVm, onDone = { nav.popBackStack() }) },
        settingsContent = { SettingsScreen(profileVm) },
    )
}

/** The five-tab scaffold. Content lambdas are injected so UI tests can run it without Hilt. */
@Composable
fun MainScaffold(
    homeName: String?,
    profileContent: @Composable (NavHostController) -> Unit,
    editProfileContent: @Composable (NavHostController) -> Unit,
    settingsContent: @Composable () -> Unit,
) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tabs.forEach { tab ->
                    val selected = backStack?.destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                        alwaysShowLabel = true,
                        modifier = Modifier.testTag(tab.tag),
                    )
                }
            }
        },
    ) { padding ->
        // consumeWindowInsets: the shell has already reserved the status bar and the bottom navigation
        // (which includes the gesture/button bar). Without this, a nested screen's SaveBar would add the
        // navigation-bar inset a second time and float above the keyboard by the height of the tab bar.
        NavHost(
            nav,
            startDestination = HomeRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<HomeRoute> { HomeScreen(name = homeName) }
            composable<JournalRoute> { entry -> JournalScreen(initialDate = entry.toRoute<JournalRoute>().date) }
            composable<ProgressRoute> { ProgressScreen() }
            composable<RecoveryRoute> { RecoveryScreen() }
            composable<ProfileRoute> { profileContent(nav) }
            composable<EditProfileRoute> { editProfileContent(nav) }
            composable<SettingsRoute> { settingsContent() }
        }
    }
}
