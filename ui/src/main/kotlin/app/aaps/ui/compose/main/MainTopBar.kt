package app.aaps.ui.compose.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import app.aaps.core.ui.compose.AapsTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

sealed class MainMenuItem(val id: String) {
    data object Preferences : MainMenuItem("preferences")
    data object PluginPreferences : MainMenuItem("plugin_preferences")
    data object Treatments : MainMenuItem("treatments")
    data object HistoryBrowser : MainMenuItem("history_browser")
    data object SetupWizard : MainMenuItem("setup_wizard")
    data object Stats : MainMenuItem("stats")
    data object ProfileHelper : MainMenuItem("profile_helper")
    data object About : MainMenuItem("about")
    data object Exit : MainMenuItem("exit")
}

@Composable
fun MainTopBar(
    onMenuClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AapsTopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(app.aaps.core.ui.R.string.open_navigation)
                )
            }
        },
        actions = {
            IconButton(onClick = onPreferencesClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(app.aaps.core.ui.R.string.settings)
                )
            }
        },
        modifier = modifier
    )
}
