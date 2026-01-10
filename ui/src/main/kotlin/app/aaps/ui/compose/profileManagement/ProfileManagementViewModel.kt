package app.aaps.ui.compose.profileManagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aaps.core.data.model.GlucoseUnit
import app.aaps.core.data.model.PS
import app.aaps.core.data.time.T
import app.aaps.core.interfaces.logging.AAPSLogger
import app.aaps.core.interfaces.logging.LTag
import app.aaps.core.interfaces.plugin.ActivePlugin
import app.aaps.core.interfaces.profile.LocalProfileManager
import app.aaps.core.interfaces.profile.ProfileErrorType
import app.aaps.core.interfaces.profile.ProfileFunction
import app.aaps.core.interfaces.profile.ProfileSource
import app.aaps.core.interfaces.profile.ProfileUtil
import app.aaps.core.interfaces.profile.ProfileValidationError
import app.aaps.core.interfaces.profile.PureProfile
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.interfaces.rx.AapsSchedulers
import app.aaps.core.interfaces.rx.bus.RxBus
import app.aaps.core.interfaces.rx.events.EventLocalProfileChanged
import app.aaps.core.interfaces.rx.events.EventProfileStoreChanged
import app.aaps.core.interfaces.utils.DateUtil
import app.aaps.core.interfaces.utils.DecimalFormatter
import app.aaps.core.objects.extensions.pureProfileFromJson
import app.aaps.core.objects.profile.ProfileSealed
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.TimeZone
import javax.inject.Inject

/**
 * ViewModel for ProfileManagementScreen managing profile list and operations.
 */
class ProfileManagementViewModel @Inject constructor(
    private val localProfileManager: LocalProfileManager,
    private val profileFunction: ProfileFunction,
    private val rxBus: RxBus,
    private val aapsSchedulers: AapsSchedulers,
    val rh: ResourceHelper,
    val dateUtil: DateUtil,
    private val aapsLogger: AAPSLogger,
    private val activePlugin: ActivePlugin,
    val profileUtil: ProfileUtil,
    val decimalFormatter: DecimalFormatter
) : ViewModel() {

    private val disposable = CompositeDisposable()
    private val _uiState = MutableStateFlow(ProfileManagementUiState())
    val uiState: StateFlow<ProfileManagementUiState> = _uiState.asStateFlow()

    init {
        loadData()
        observeProfileChanges()
    }

    override fun onCleared() {
        super.onCleared()
        disposable.clear()
    }

    /**
     * Load profiles from LocalProfileManager and active profile state
     */
    fun loadData() {
        viewModelScope.launch {
            try {
                val profiles = localProfileManager.profiles
                val currentIndex = localProfileManager.currentProfileIndex
                val activeProfileSwitch = profileFunction.getRequestedProfile()
                val activeProfileName = activeProfileSwitch?.profileName

                // Calculate remaining time for active profile
                val remainingTime = activeProfileSwitch?.let { ps ->
                    if (ps.duration > 0) {
                        val endTime = ps.timestamp + ps.duration
                        val now = dateUtil.now()
                        if (endTime > now) endTime - now else 0L
                    } else null
                }

                // Calculate basal sum for each profile
                val basalSums = profiles.map { singleProfile ->
                    toPureProfile(singleProfile)?.let { pureProfile ->
                        ProfileSealed.Pure(pureProfile, activePlugin).baseBasalSum()
                    } ?: 0.0
                }

                // Validate each profile with structured errors
                val profileErrors = profiles.indices.map { index ->
                    val savedIndex = localProfileManager.currentProfileIndex
                    localProfileManager.currentProfileIndex = index
                    val errors = localProfileManager.validateProfileStructured()
                        .filter { it.type != ProfileErrorType.NAME || it.message != rh.gs(app.aaps.core.ui.R.string.profile_name_contains_dot) }
                    localProfileManager.currentProfileIndex = savedIndex
                    errors
                }

                // Get selected profile as ProfileSealed for viewer
                val selectedProfile = if (currentIndex in profiles.indices) {
                    toPureProfile(profiles[currentIndex])?.let { pureProfile ->
                        ProfileSealed.Pure(pureProfile, activePlugin)
                    }
                } else null

                _uiState.update {
                    it.copy(
                        profiles = profiles,
                        currentProfileIndex = currentIndex,
                        activeProfileName = activeProfileName,
                        activeProfileSwitch = activeProfileSwitch,
                        remainingTimeMs = remainingTime,
                        basalSums = basalSums,
                        profileErrors = profileErrors,
                        selectedProfile = selectedProfile,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                aapsLogger.error(LTag.UI, "Failed to load profiles", e)
                _uiState.update {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    /**
     * Convert SingleProfile to PureProfile
     */
    private fun toPureProfile(singleProfile: ProfileSource.SingleProfile): PureProfile? {
        val profile = JSONObject().apply {
            put("dia", singleProfile.dia)
            put("carbratio", singleProfile.ic)
            put("sens", singleProfile.isf)
            put("basal", singleProfile.basal)
            put("target_low", singleProfile.targetLow)
            put("target_high", singleProfile.targetHigh)
            put("units", if (singleProfile.mgdl) GlucoseUnit.MGDL.asText else GlucoseUnit.MMOL.asText)
            put("timezone", TimeZone.getDefault().id)
        }
        return pureProfileFromJson(profile, dateUtil)
    }

    /**
     * Subscribe to profile change events
     */
    private fun observeProfileChanges() {
        disposable += rxBus
            .toObservable(EventLocalProfileChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ loadData() }, { aapsLogger.error(LTag.UI, "Error observing profile changes", it) })

        disposable += rxBus
            .toObservable(EventProfileStoreChanged::class.java)
            .observeOn(aapsSchedulers.main)
            .subscribe({ loadData() }, { aapsLogger.error(LTag.UI, "Error observing profile store changes", it) })
    }

    /**
     * Select a profile by index
     */
    fun selectProfile(index: Int) {
        if (index in 0 until localProfileManager.numOfProfiles) {
            localProfileManager.currentProfileIndex = index
            loadData()
        }
    }

    /**
     * Add a new empty profile
     */
    fun addNewProfile() {
        localProfileManager.addNewProfile()
        localProfileManager.notifyProfileChanged()
        loadData()
    }

    /**
     * Clone the profile at the given index
     */
    fun cloneProfile(index: Int) {
        val previousIndex = localProfileManager.currentProfileIndex
        localProfileManager.currentProfileIndex = index
        localProfileManager.cloneProfile()
        localProfileManager.currentProfileIndex = previousIndex
        localProfileManager.notifyProfileChanged()
        loadData()
    }

    /**
     * Remove the profile at the given index
     */
    fun removeProfile(index: Int) {
        val previousIndex = localProfileManager.currentProfileIndex
        localProfileManager.currentProfileIndex = index
        localProfileManager.removeCurrentProfile()
        // Adjust index if needed
        if (previousIndex >= localProfileManager.numOfProfiles) {
            localProfileManager.currentProfileIndex = localProfileManager.numOfProfiles - 1
        } else if (previousIndex > index) {
            localProfileManager.currentProfileIndex = previousIndex - 1
        } else {
            localProfileManager.currentProfileIndex = previousIndex
        }
        localProfileManager.notifyProfileChanged()
        loadData()
    }

    /**
     * Format remaining time for display
     */
    fun formatRemainingTime(remainingMs: Long): String {
        val hours = T.msecs(remainingMs).hours().toInt()
        val mins = T.msecs(remainingMs).mins().toInt() % 60
        return if (hours > 0) {
            "${hours}h ${mins}m"
        } else {
            "${mins}m"
        }
    }

    // Profile viewer formatting helpers
    fun getIcList(profile: ProfileSealed): String = profile.getIcList(rh, dateUtil)
    fun getIsfList(profile: ProfileSealed): String = profile.getIsfList(rh, dateUtil)
    fun getBasalList(profile: ProfileSealed): String = profile.getBasalList(rh, dateUtil)
    fun getTargetList(profile: ProfileSealed): String = profile.getTargetList(rh, dateUtil)
    fun formatDia(dia: Double): String = rh.gs(app.aaps.core.ui.R.string.format_hours, dia)
    fun formatBasalSum(basalSum: Double): String = decimalFormatter.to2Decimal(basalSum) + " " + rh.gs(app.aaps.core.ui.R.string.insulin_unit_shortname)
}

/**
 * UI state for ProfileManagementScreen
 */
data class ProfileManagementUiState(
    val profiles: List<ProfileSource.SingleProfile> = emptyList(),
    val currentProfileIndex: Int = 0,
    val activeProfileName: String? = null,
    val activeProfileSwitch: PS? = null,
    val remainingTimeMs: Long? = null,
    val basalSums: List<Double> = emptyList(),
    val profileErrors: List<List<ProfileValidationError>> = emptyList(),
    val selectedProfile: ProfileSealed? = null,
    val isLoading: Boolean = true
)
