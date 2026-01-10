package app.aaps.core.interfaces.profile

/**
 * Interface for managing local profiles.
 * Provides methods for profile CRUD operations, persistence, and state management.
 *
 * This interface is used by the new Compose UI for profile management.
 * Legacy UI uses ProfilePlugin directly which implements ProfileSource.
 */
interface LocalProfileManager {

    /**
     * List of all profiles in the store.
     */
    val profiles: List<ProfileSource.SingleProfile>

    /**
     * Number of profiles in the store.
     */
    val numOfProfiles: Int

    /**
     * Index of the currently selected profile.
     */
    var currentProfileIndex: Int

    /**
     * Whether the current profile has unsaved changes.
     */
    var isEdited: Boolean

    /**
     * The profile store containing all profiles.
     */
    val profile: ProfileStore?

    /**
     * Formatted name of the current profile (includes basal sum).
     */
    val profileName: String

    /**
     * Get the currently selected profile.
     *
     * @return The current SingleProfile or null if no profiles exist
     */
    fun currentProfile(): ProfileSource.SingleProfile?

    /**
     * Get the currently edited profile as a PureProfile.
     * Used for validation and activation.
     *
     * @return PureProfile of the current profile or null
     */
    fun getEditedProfile(): PureProfile?

    /**
     * Validate the current profile.
     * Returns list of validation error messages, empty if valid.
     *
     * @return List of error messages, empty if profile is valid
     */
    fun validateProfile(): List<String>

    /**
     * Check if the current profile is valid.
     *
     * @return true if valid, false otherwise
     */
    fun isValid(): Boolean = validateProfile().isEmpty()

    /**
     * Load profiles from SharedPreferences.
     */
    fun loadSettings()

    /**
     * Save profiles to SharedPreferences.
     *
     * @param timestamp Timestamp of the change
     */
    fun storeSettings(timestamp: Long)

    /**
     * Import profiles from a ProfileStore.
     * Validates profiles before importing.
     *
     * @param store ProfileStore to import from
     */
    fun loadFromStore(store: ProfileStore)

    /**
     * Create a SingleProfile from a PureProfile.
     *
     * @param pureProfile Source profile
     * @param newName Name for the new profile
     * @return New SingleProfile
     */
    fun copyFrom(pureProfile: PureProfile, newName: String): ProfileSource.SingleProfile

    /**
     * Create a new empty profile with default values.
     * Selects the new profile as current.
     */
    fun addNewProfile()

    /**
     * Clone the current profile.
     * Creates a copy with " copy" appended to the name.
     * Selects the cloned profile as current.
     */
    fun cloneProfile()

    /**
     * Add an existing profile to the store.
     * Selects the added profile as current.
     *
     * @param profile Profile to add
     */
    fun addProfile(profile: ProfileSource.SingleProfile)

    /**
     * Remove the currently selected profile.
     * If this was the last profile, creates a new default profile.
     * Selects index 0 after removal.
     */
    fun removeCurrentProfile()

    /**
     * Notify listeners that profile data has changed.
     * Sends EventLocalProfileChanged via RxBus.
     */
    fun notifyProfileChanged()
}
