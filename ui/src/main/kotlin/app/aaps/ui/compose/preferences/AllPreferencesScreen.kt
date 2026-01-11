package app.aaps.ui.compose.preferences

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.automation.Automation
import app.aaps.core.interfaces.autotune.Autotune
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.maintenance.Maintenance
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.profile.ProfileUtil
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.smsCommunicator.SmsCommunicator
import app.aaps.core.keys.BooleanKey
import app.aaps.core.keys.IntKey
import app.aaps.core.keys.StringKey
import app.aaps.core.keys.interfaces.PreferenceVisibilityContext
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.keys.interfaces.withEntries
import app.aaps.core.ui.compose.AapsTopAppBar
import app.aaps.core.ui.compose.preference.LocalCheckPassword
import app.aaps.core.ui.compose.preference.LocalHashPassword
import app.aaps.core.ui.compose.preference.LocalSnackbarHostState
import app.aaps.core.ui.compose.preference.LocalVisibilityContext
import app.aaps.core.ui.compose.preference.PreferenceSubScreenDef
import app.aaps.core.ui.compose.preference.ProvidePreferenceTheme
import app.aaps.core.ui.compose.preference.addPreferenceContent
import app.aaps.core.ui.compose.preference.rememberPreferenceSectionState
import app.aaps.core.ui.compose.preference.verticalScrollIndicators

/**
 * Screen for displaying all preferences from all plugins.
 * Maintains the same ordering as the legacy MyPreferenceFragment.
 *
 * Plugins are looked up via their interfaces through ActivePlugin, eliminating
 * direct dependencies on specific plugin implementations.
 *
 * @param activePlugin ActivePlugin instance for accessing plugins by interface
 * @param preferences Preferences instance for built-in settings
 * @param config Config instance
 * @param rh ResourceHelper instance
 * @param checkPassword Function to verify passwords: (enteredPassword, storedHash) -> Boolean
 * @param hashPassword Function to hash passwords before storing: (password) -> String
 * @param visibilityContext PreferenceVisibilityContext for visibility control
 * @param profileUtil ProfileUtil instance
 * @param skinEntries Map of skin class names to their display names for the skin preference
 * @param onBackClick Callback when back button is clicked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPreferencesScreen(
    activePlugin: ActivePlugin,
    preferences: Preferences,
    config: Config,
    rh: ResourceHelper,
    checkPassword: (password: String, hash: String) -> Boolean,
    hashPassword: (password: String) -> String,
    visibilityContext: PreferenceVisibilityContext,
    profileUtil: ProfileUtil,
    skinEntries: Map<String, String>,
    onBackClick: () -> Unit
) {
    // Look up plugins by interface
    val smsCommunicatorPlugin = activePlugin.getSpecificPluginsListByInterface(SmsCommunicator::class.java).firstOrNull()
    val automationPlugin = activePlugin.getSpecificPluginsListByInterface(Automation::class.java).firstOrNull()
    val autotunePlugin = activePlugin.getSpecificPluginsListByInterface(Autotune::class.java).firstOrNull()
    val maintenancePlugin = activePlugin.getSpecificPluginsListByInterface(Maintenance::class.java).firstOrNull()

    // Built-in preference screens
    val generalPreferences = PreferenceSubScreenDef(
        key = "general",
        titleResId = app.aaps.core.ui.R.string.configbuilder_general,
        items = listOf(
            StringKey.GeneralUnits,
            StringKey.GeneralLanguage,
            BooleanKey.GeneralSimpleMode,
            StringKey.GeneralPatientName,
            StringKey.GeneralSkin.withEntries(skinEntries),
            StringKey.GeneralDarkMode
        ),
        iconResId = app.aaps.core.ui.R.drawable.ic_settings
    )
    val protectionPreferences = PreferenceSubScreenDef(
        key = "protection",
        titleResId = app.aaps.core.ui.R.string.protection,
        items = listOf(
            // Master Password
            StringKey.ProtectionMasterPassword,
            // Settings Protection
            IntKey.ProtectionTypeSettings,
            StringKey.ProtectionSettingsPassword,  // Visibility defined on StringKey
            StringKey.ProtectionSettingsPin,       // Visibility defined on StringKey
            // Application Protection
            IntKey.ProtectionTypeApplication,
            StringKey.ProtectionApplicationPassword,
            StringKey.ProtectionApplicationPin,
            // Bolus Protection
            IntKey.ProtectionTypeBolus,
            StringKey.ProtectionBolusPassword,
            StringKey.ProtectionBolusPin,
            // Protection Timeout
            IntKey.ProtectionTimeout
        ),
        iconResId = app.aaps.core.objects.R.drawable.ic_header_key
    )
    val pumpPreferences = PreferenceSubScreenDef(
        key = "pump",
        titleResId = app.aaps.core.ui.R.string.pump,
        items = listOf(
            BooleanKey.PumpBtWatchdog
        ),
        iconResId = app.aaps.core.ui.R.drawable.ic_settings
    )
    val alertsPreferences = PreferenceSubScreenDef(
        key = "alerts",
        titleResId = app.aaps.core.ui.R.string.localalertsettings_title,
        items = listOf(
            BooleanKey.AlertMissedBgReading,
            IntKey.AlertsStaleDataThreshold,
            BooleanKey.AlertPumpUnreachable,
            IntKey.AlertsPumpUnreachableThreshold,
            BooleanKey.AlertCarbsRequired,
            BooleanKey.AlertUrgentAsAndroidNotification,
            BooleanKey.AlertIncreaseVolume
        ),
        iconResId = app.aaps.core.objects.R.drawable.ic_cp_announcement
    )

    // Helper function to get preference content if plugin is enabled
    fun getPreferenceContentIfEnabled(plugin: PluginBase?, enabledCondition: Boolean = true): Any? {
        if (plugin == null) return null
        // Check simple mode visibility
        if (preferences.simpleMode && !plugin.pluginDescription.preferencesVisibleInSimpleMode && !config.isDev()) {
            return null
        }
        // Check if plugin is enabled
        if (!enabledCondition || !plugin.isEnabled()) {
            return null
        }
        // Only PreferenceSubScreenDef is supported
        return when (val content = plugin.getPreferenceScreenContent()) {
            is PreferenceSubScreenDef -> content
            else                      -> null
        }
    }

    // Build plugin preference screens in the same order as MyPreferenceFragment
    val pluginContents = buildList {
        // 1. Overview plugin (always enabled)
        getPreferenceContentIfEnabled(activePlugin.activeOverview as PluginBase)?.let { add(it) }

        // 2. Safety plugin (always enabled)
        getPreferenceContentIfEnabled(activePlugin.activeSafety as PluginBase)?.let { add(it) }

        // 3. BG Source plugin
        getPreferenceContentIfEnabled(activePlugin.activeBgSource as PluginBase)?.let { add(it) }

        // 4. LOOP type plugins (enabled only if APS is configured)
        activePlugin.getSpecificPluginsList(PluginType.LOOP).forEach { plugin ->
            getPreferenceContentIfEnabled(plugin, config.APS)?.let { add(it) }
        }

        // 5. APS plugin (enabled only if APS is configured)
        getPreferenceContentIfEnabled(activePlugin.activeAPS as PluginBase, config.APS)?.let { add(it) }

        // 6. Sensitivity plugin
        getPreferenceContentIfEnabled(activePlugin.activeSensitivity as PluginBase)?.let { add(it) }

        // 7. Pump plugin
        getPreferenceContentIfEnabled(activePlugin.activePump as PluginBase)?.let { add(it) }

        // 8. Insulin plugin
        getPreferenceContentIfEnabled(activePlugin.activeInsulin as PluginBase)?.let { add(it) }

        // 9. SYNC type plugins
        activePlugin.getSpecificPluginsList(PluginType.SYNC).forEach { plugin ->
            getPreferenceContentIfEnabled(plugin)?.let { add(it) }
        }

        // 10. SMS Communicator plugin (found via interface)
        getPreferenceContentIfEnabled(smsCommunicatorPlugin)?.let { add(it) }

        // 11. Automation plugin (found via interface)
        getPreferenceContentIfEnabled(automationPlugin)?.let { add(it) }

        // 12. Autotune plugin (found via interface)
        getPreferenceContentIfEnabled(autotunePlugin)?.let { add(it) }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    CompositionLocalProvider(
        LocalCheckPassword provides checkPassword,
        LocalHashPassword provides hashPassword,
        LocalSnackbarHostState provides snackbarHostState,
        LocalVisibilityContext provides visibilityContext
    ) {
        ProvidePreferenceTheme {
            Scaffold(
                topBar = {
                    AapsTopAppBar(
                        title = {
                            Text(
                                text = stringResource(app.aaps.core.ui.R.string.settings),
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(app.aaps.core.ui.R.string.back)
                                )
                            }
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                val listState = rememberLazyListState()
                val sectionState = rememberPreferenceSectionState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScrollIndicators(listState),
                    state = listState
                ) {
                    // Built-in: General settings (first)
                    addPreferenceContent(generalPreferences, sectionState, preferences, config, profileUtil)

                    // Built-in: Protection settings
                    addPreferenceContent(protectionPreferences, sectionState, preferences, config, profileUtil)

                    // Plugin preferences (in fixed order, only enabled plugins)
                    pluginContents.forEach { content ->
                        addPreferenceContent(content, sectionState, preferences, config, profileUtil)
                    }

                    // Built-in: Pump settings
                    addPreferenceContent(pumpPreferences, sectionState, preferences, config, profileUtil)

                    // Built-in: Alerts settings
                    addPreferenceContent(alertsPreferences, sectionState, preferences, config, profileUtil)

                    // Maintenance plugin (found via interface, always last)
                    getPreferenceContentIfEnabled(maintenancePlugin)?.let { content ->
                        addPreferenceContent(content, sectionState, preferences, config, profileUtil)
                    }
                }
            }
        }
    }
}
