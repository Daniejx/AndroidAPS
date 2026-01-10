package app.aaps.ui.compose.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.model.EPS
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.configuration.Config
import app.aaps.core.interfaces.configuration.ConfigBuilder
import app.aaps.core.interfaces.db.PersistenceLayer
import app.aaps.core.interfaces.db.observeChanges
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.plugin.PluginBase
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.ui.IconsProvider
import app.aaps.core.interfaces.utils.fabric.FabricPrivacy
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.objects.profile.ProfileSealed
import app.aaps.ui.compose.alertDialogs.AboutDialogData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val activePlugin: ActivePlugin,
    private val configBuilder: ConfigBuilder,
    private val config: Config,
    private val preferences: Preferences,
    private val profileFunction: ProfileFunction,
    private val persistenceLayer: PersistenceLayer,
    private val fabricPrivacy: FabricPrivacy,
    private val iconsProvider: IconsProvider,
    private val rh: ResourceHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val versionName: String get() = config.VERSION_NAME
    val appIcon: Int get() = iconsProvider.getIcon()

    init {
        loadDrawerCategories()
        observeProfileChanges()
    }

    private fun observeProfileChanges() {
        // Update profile state when profile changes (using Flow from PersistenceLayer)
        persistenceLayer.observeChanges<EPS>()
            .onEach { refreshProfileState() }
            .launchIn(viewModelScope)
    }

    private fun loadDrawerCategories() {
        viewModelScope.launch {
            val categories = buildDrawerCategories()
            val profile = profileFunction.getProfile()
            val isModified = profile?.let {
                if (it is ProfileSealed.EPS) {
                    it.value.originalPercentage != 100 || it.value.originalTimeshift != 0L || it.value.originalDuration != 0L
                } else false
            } ?: false

            _uiState.update { state ->
                state.copy(
                    drawerCategories = categories,
                    isSimpleMode = preferences.simpleMode,
                    isProfileLoaded = profile != null,
                    profileName = profileFunction.getProfileNameWithRemainingTime(),
                    isProfileModified = isModified
                )
            }
        }
    }

    private fun buildDrawerCategories(): List<DrawerCategory> {
        val categories = mutableListOf<DrawerCategory>()

        // Insulin (if APS or PUMPCONTROL or engineering mode)
        if (config.APS || config.PUMPCONTROL || config.isEngineeringMode()) {
            activePlugin.getSpecificPluginsVisibleInList(PluginType.INSULIN).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.INSULIN,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_insulin,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.INSULIN)
                    )
                )
            }
        }

        // BG Source, Smoothing, Pump (if not AAPSCLIENT)
        if (!config.AAPSCLIENT) {
            activePlugin.getSpecificPluginsVisibleInList(PluginType.BGSOURCE).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.BGSOURCE,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_bgsource,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.BGSOURCE)
                    )
                )
            }

            activePlugin.getSpecificPluginsVisibleInList(PluginType.SMOOTHING).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.SMOOTHING,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_smoothing,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.SMOOTHING)
                    )
                )
            }

            activePlugin.getSpecificPluginsVisibleInList(PluginType.PUMP).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.PUMP,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_pump,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.PUMP)
                    )
                )
            }
        }

        // Sensitivity (if APS or PUMPCONTROL or engineering mode)
        if (config.APS || config.PUMPCONTROL || config.isEngineeringMode()) {
            activePlugin.getSpecificPluginsVisibleInList(PluginType.SENSITIVITY).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.SENSITIVITY,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_sensitivity,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.SENSITIVITY)
                    )
                )
            }
        }

        // APS, Loop, Constraints (if APS mode)
        if (config.APS) {
            activePlugin.getSpecificPluginsVisibleInList(PluginType.APS).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.APS,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_aps,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.APS)
                    )
                )
            }

            activePlugin.getSpecificPluginsVisibleInList(PluginType.LOOP).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.LOOP,
                        titleRes = app.aaps.core.ui.R.string.configbuilder_loop,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.LOOP)
                    )
                )
            }

            activePlugin.getSpecificPluginsVisibleInList(PluginType.CONSTRAINTS).takeIf { it.isNotEmpty() }?.let { plugins ->
                categories.add(
                    DrawerCategory(
                        type = PluginType.CONSTRAINTS,
                        titleRes = app.aaps.core.ui.R.string.constraints,
                        plugins = plugins,
                        isMultiSelect = DrawerCategory.isMultiSelect(PluginType.CONSTRAINTS)
                    )
                )
            }
        }

        // Sync
        activePlugin.getSpecificPluginsVisibleInList(PluginType.SYNC).takeIf { it.isNotEmpty() }?.let { plugins ->
            categories.add(
                DrawerCategory(
                    type = PluginType.SYNC,
                    titleRes = app.aaps.core.ui.R.string.configbuilder_sync,
                    plugins = plugins,
                    isMultiSelect = DrawerCategory.isMultiSelect(PluginType.SYNC)
                )
            )
        }

        // General
        activePlugin.getSpecificPluginsVisibleInList(PluginType.GENERAL).takeIf { it.isNotEmpty() }?.let { plugins ->
            categories.add(
                DrawerCategory(
                    type = PluginType.GENERAL,
                    titleRes = app.aaps.core.ui.R.string.configbuilder_general,
                    plugins = plugins,
                    isMultiSelect = DrawerCategory.isMultiSelect(PluginType.GENERAL)
                )
            )
        }

        return categories
    }

    // Drawer state
    fun openDrawer() {
        _uiState.update { it.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        _uiState.update { it.copy(isDrawerOpen = false) }
    }

    // Category sheet state
    fun showCategorySheet(category: DrawerCategory) {
        _uiState.update { it.copy(selectedCategoryForSheet = category) }
    }

    fun dismissCategorySheet() {
        _uiState.update { it.copy(selectedCategoryForSheet = null) }
    }

    // About dialog state
    fun setShowAboutDialog(show: Boolean) {
        _uiState.update { it.copy(showAboutDialog = show) }
    }

    // Navigation state
    fun setNavDestination(destination: MainNavDestination) {
        _uiState.update { it.copy(currentNavDestination = destination) }
    }

    // Profile state
    fun refreshProfileState() {
        val profile = profileFunction.getProfile()
        val isModified = profile?.let {
            if (it is ProfileSealed.EPS) {
                it.value.originalPercentage != 100 || it.value.originalTimeshift != 0L || it.value.originalDuration != 0L
            } else false
        } ?: false

        _uiState.update {
            it.copy(
                isProfileLoaded = profile != null,
                profileName = profileFunction.getProfileNameWithRemainingTime(),
                isProfileModified = isModified
            )
        }
    }

    // Plugin toggle
    fun togglePluginEnabled(plugin: PluginBase, type: PluginType, enabled: Boolean) {
        configBuilder.performPluginSwitch(plugin, enabled, type)
        val categories = buildDrawerCategories()
        val currentSheet = _uiState.value.selectedCategoryForSheet
        val updatedSheet = currentSheet?.let { sheet ->
            categories.find { it.type == sheet.type }
        }
        _uiState.update { state ->
            state.copy(
                drawerCategories = categories,
                selectedCategoryForSheet = updatedSheet,
                pluginStateVersion = state.pluginStateVersion + 1
            )
        }
    }

    // Category click handling
    fun handleCategoryClick(category: DrawerCategory, onPluginClick: (PluginBase) -> Unit) {
        if (category.enabledCount == 1) {
            category.enabledPlugins.firstOrNull()?.let { plugin ->
                onPluginClick(plugin)
            }
        } else {
            showCategorySheet(category)
        }
    }

    // Build about dialog data
    fun buildAboutDialogData(appName: String): AboutDialogData {
        var message = "Build: ${config.BUILD_VERSION}\n"
        message += "Flavor: ${config.FLAVOR}${config.BUILD_TYPE}\n"
        message += "${rh.gs(app.aaps.core.ui.R.string.configbuilder_nightscoutversion_label)} ${activePlugin.activeNsClient?.detectedNsVersion() ?: rh.gs(app.aaps.core.ui.R.string.not_available_full)}"
        if (config.isEngineeringMode()) message += "\n${rh.gs(app.aaps.core.ui.R.string.engineering_mode_enabled)}"
        if (config.isUnfinishedMode()) message += "\nUnfinished mode enabled"
        if (!fabricPrivacy.fabricEnabled()) message += "\n${rh.gs(app.aaps.core.ui.R.string.fabric_upload_disabled)}"
        message += rh.gs(app.aaps.core.ui.R.string.about_link_urls)

        return AboutDialogData(
            title = "$appName ${config.VERSION}",
            message = message,
            icon = iconsProvider.getIcon()
        )
    }
}
