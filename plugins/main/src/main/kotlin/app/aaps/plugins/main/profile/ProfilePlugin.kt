package app.aaps.plugins.main.profile

import androidx.fragment.app.FragmentActivity
import app.aaps.core.data.plugin.PluginType
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.plugin.PluginBaseWithPreferences
import app.aaps.core.interfaces.plugin.PluginDescription
import app.aaps.core.interfaces.profile.LocalProfileManager
import app.aaps.core.interfaces.profile.ProfileSource
import app.aaps.core.interfaces.profile.ProfileStore
import app.aaps.core.interfaces.profile.PureProfile
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.ui.UiInteraction
import app.aaps.core.keys.ProfileComposedBooleanKey
import app.aaps.core.keys.ProfileComposedDoubleKey
import app.aaps.core.keys.ProfileComposedStringKey
import app.aaps.core.keys.ProfileIntKey
import app.aaps.core.keys.interfaces.Preferences
import app.aaps.core.ui.toast.ToastUtils
import app.aaps.plugins.main.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePlugin @Inject constructor(
    aapsLogger: AAPSLogger,
    rh: ResourceHelper,
    preferences: Preferences,
    private val localProfileManager: LocalProfileManager,
    private val uiInteraction: UiInteraction
) : PluginBaseWithPreferences(
    pluginDescription = PluginDescription()
        .mainType(PluginType.PROFILE)
        .fragmentClass(ProfileFragment::class.java.name)
        .enableByDefault(true)
        .simpleModePosition(PluginDescription.Position.TAB)
        .pluginIcon(app.aaps.core.objects.R.drawable.ic_local_profile)
        .pluginName(app.aaps.core.ui.R.string.localprofile)
        .shortName(R.string.localprofile_shortname)
        .description(R.string.description_profile_local)
        .visibleByDefault(true)
        .setDefault(),
    ownPreferences = listOf(
        ProfileComposedStringKey::class.java, ProfileComposedDoubleKey::class.java, ProfileComposedBooleanKey::class.java, ProfileIntKey::class.java
    ),
    aapsLogger, rh, preferences
), ProfileSource {

    override fun onStart() {
        super.onStart()
        localProfileManager.loadSettings()
    }

    // Delegate to LocalProfileManager
    var isEdited: Boolean
        get() = localProfileManager.isEdited
        set(value) { localProfileManager.isEdited = value }

    val numOfProfiles: Int
        get() = localProfileManager.numOfProfiles

    override var currentProfileIndex: Int
        get() = localProfileManager.currentProfileIndex
        set(value) { localProfileManager.currentProfileIndex = value }

    override fun currentProfile(): ProfileSource.SingleProfile? = localProfileManager.currentProfile()

    override val profile: ProfileStore?
        get() = localProfileManager.profile

    override val profileName: String
        get() = localProfileManager.profileName

    /**
     * Validate profile and show Toast for first error (legacy UI).
     * New Compose UI should use localProfileManager.validateProfile() directly.
     */
    @Synchronized
    fun isValidEditState(activity: FragmentActivity?): Boolean {
        val errors = localProfileManager.validateProfile()
        if (errors.isNotEmpty()) {
            // Show first error as Toast (legacy behavior)
            // Filter out dot warning - it's shown separately in storeSettings
            val criticalErrors = errors.filter { it != rh.gs(app.aaps.core.ui.R.string.profile_name_contains_dot) }
            if (criticalErrors.isNotEmpty()) {
                ToastUtils.errorToast(activity, criticalErrors.first())
                return false
            }
        }
        return true
    }

    @Synchronized
    fun getEditedProfile(): PureProfile? = localProfileManager.getEditedProfile()

    @Synchronized
    override fun storeSettings(activity: FragmentActivity?, timestamp: Long) {
        localProfileManager.storeSettings(timestamp)
        // Show dot warning dialog for legacy UI
        val errors = localProfileManager.validateProfile()
        if (errors.any { it == rh.gs(app.aaps.core.ui.R.string.profile_name_contains_dot) }) {
            activity?.let {
                uiInteraction.showOkDialog(context = it, title = "", message = rh.gs(app.aaps.core.ui.R.string.profile_name_contains_dot))
            }
        }
    }

    @Synchronized
    override fun loadFromStore(store: ProfileStore) = localProfileManager.loadFromStore(store)

    override fun copyFrom(pureProfile: PureProfile, newName: String): ProfileSource.SingleProfile =
        localProfileManager.copyFrom(pureProfile, newName)

    override fun addProfile(p: ProfileSource.SingleProfile) = localProfileManager.addProfile(p)

    fun addNewProfile() = localProfileManager.addNewProfile()

    fun cloneProfile() = localProfileManager.cloneProfile()

    fun removeCurrentProfile() = localProfileManager.removeCurrentProfile()

    fun loadSettings() = localProfileManager.loadSettings()
}
