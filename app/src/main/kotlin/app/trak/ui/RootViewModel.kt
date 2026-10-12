package app.trak.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.trak.core.data.session.AppEntryResolver
import app.trak.core.data.session.AppEntryState
import app.trak.core.data.settings.UserSettings
import app.trak.core.model.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Holds where the app should be (from local state + auth) and the theme. No logic of its own. */
@HiltViewModel
class RootViewModel @Inject constructor(
    private val resolver: AppEntryResolver,
    settings: UserSettings,
) : ViewModel() {

    val entry: StateFlow<AppEntryState> =
        resolver.state.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppEntryState.Loading)

    val theme: StateFlow<AppTheme> =
        settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, AppTheme.DARK)

    fun retry() = resolver.refresh()
}
