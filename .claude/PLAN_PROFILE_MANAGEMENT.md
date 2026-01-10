# Profile Management UI Migration Plan

## Goal
Create a unified "Profile Management" screen with a card-based UI for managing all profiles in one place.

## Final State Vision

### Profile Management Screen
- **Card-based layout**: Each profile displayed as a card
- **Active profile highlighting**: Different color for currently active profile
- **Card content**:
  - Profile name
  - If active with modifications: percentage, timeshift, remaining time
  - Following profile (what activates when temp expires)
- **Card actions**:
  - **Edit** → opens ProfileEditor
  - **Show** → opens ProfileViewer
  - **Activate** → triggers profile switch (with duration/percentage/timeshift options)
- **Screen actions**:
  - Add new profile (creates new card)
  - Clone profile (duplicates card)
  - Remove profile (removes card)

### ProfileEditor (Simplified)
- Only edits single profile values (DIA, IC, ISF, Basal, Target)
- No longer handles profile list management (moved to Profile Management Screen)

### Architecture
```
Profile Management Screen (NEW)
├── List of ProfileCards
│   └── Each card shows: name, active state, percentage, timeshift, remaining time
├── Add/Clone/Remove profile actions
└── Card actions: Edit, Show, Activate

ProfileEditor (EXISTING - simplified)
└── Edits single profile: DIA, IC, ISF, Basal, Target tabs

ProfileViewer (EXISTING)
└── Displays profile details (read-only)
```

## Strategy
1. **Extract logic first**: Move profile manipulation logic from `ProfilePlugin` to a new `LocalProfileManager` class
2. **ProfilePlugin becomes bridge**: After extraction, ProfilePlugin stays only as a thin bridge for old Activity-based UI
3. **Build unified screen**: Create new Compose UI using extracted logic and existing migrated components

## Current State

### Already Migrated (Reusable)
| Component | Location | Purpose |
|-----------|----------|---------|
| `ProfileEditorScreen` | `plugins/main/profile/ProfileScreen.kt` | Full profile editing with tabs (DIA, IC, ISF, Basal, Target) |
| `ProfileEditorViewModel` | `plugins/main/profile/ProfileEditorViewModel.kt` | State management for profile editing |
| `ProfileViewerScreen` | `ui/compose/profileViewer/ProfileViewerScreen.kt` | Display profile in single/comparison modes |
| `ProfileSwitchScreen` | `ui/compose/profileSwitch/ProfileSwitchScreen.kt` | View profile switch history (PS & EPS) |
| `ProfileSwitchViewModel` | `ui/compose/profileSwitch/viewmodels/ProfileSwitchViewModel.kt` | State for profile switch history |
| Graph composables | `core/graph/*Compose.kt` | Basal, IC, ISF, Target profile graphs |

### NOT Migrated (Needs Work)
| Component | Location | Purpose |
|-----------|----------|---------|
| `ProfileSwitchDialog` | `ui/dialogs/ProfileSwitchDialog.kt` | Activate profile with duration/percentage/timeshift/TT |
| Profile activation flow | scattered | Creating a new profile switch |

## Phase 0: Extract LocalProfileManager

### Logic to Extract from ProfilePlugin → LocalProfileManager

**State/Data:**
- `profiles: ArrayList<ProfileSource.SingleProfile>` - profile storage
- `currentProfileIndex: Int` - current selected profile index
- `isEdited: Boolean` - edit state tracking
- `rawProfile: ProfileStore?` - converted profile store

**Getters:**
- `numOfProfiles: Int` - count of profiles
- `currentProfile(): SingleProfile?` - get current profile
- `profile: ProfileStore?` - get profile store
- `profileName: String` - get formatted profile name

**Validation:**
- `isValidEditState(activity: FragmentActivity?): Boolean` - validate current profile

**Profile Operations:**
- `getEditedProfile(): PureProfile?` - get current profile as PureProfile
- `loadSettings()` - load profiles from preferences
- `storeSettings(activity, timestamp)` - save profiles to preferences
- `loadFromStore(store: ProfileStore)` - import from ProfileStore
- `copyFrom(pureProfile, newName): SingleProfile` - copy a profile
- `addNewProfile()` - create new empty profile
- `cloneProfile()` - clone current profile
- `addProfile(p: SingleProfile)` - add existing profile
- `removeCurrentProfile()` - delete current profile

**Internal:**
- `createProfileStore(): ProfileStore` - convert profiles to ProfileStore
- `createAndStoreConvertedProfile()` - trigger conversion
- `isExistingName(name): Boolean` - check for duplicate names

### What Stays in ProfilePlugin
- Plugin registration (`PluginDescription`)
- `onStart()` → delegates to `LocalProfileManager.loadSettings()`
- `fragmentClass` reference for old UI
- Implements `ProfileSource` by delegating to `LocalProfileManager`

### LocalProfileManager Interface

```kotlin
interface LocalProfileManager {
    // State
    val profiles: List<ProfileSource.SingleProfile>
    var currentProfileIndex: Int
    var isEdited: Boolean

    // Getters
    val numOfProfiles: Int
    val profile: ProfileStore?
    val profileName: String
    fun currentProfile(): ProfileSource.SingleProfile?

    // Validation
    fun isValidEditState(): Boolean
    fun getValidationErrors(): List<String>

    // Profile operations
    fun getEditedProfile(): PureProfile?
    fun loadSettings()
    fun storeSettings(timestamp: Long)
    fun loadFromStore(store: ProfileStore)
    fun copyFrom(pureProfile: PureProfile, newName: String): ProfileSource.SingleProfile
    fun addNewProfile()
    fun cloneProfile()
    fun addProfile(profile: ProfileSource.SingleProfile)
    fun removeCurrentProfile()

    // Events
    fun notifyProfileChanged()
}
```

### Files to Create (Phase 0)
```
core/interfaces/src/main/kotlin/app/aaps/core/interfaces/profile/LocalProfileManager.kt  # Interface
plugins/main/src/main/kotlin/app/aaps/plugins/main/profile/LocalProfileManagerImpl.kt    # Implementation
```

---

## Implementation Phases

### Phase 0: Extract LocalProfileManager ✅ COMPLETE
- [x] Created Snackbar infrastructure in `core/ui/compose/Snackbar.kt`
- [x] Created `LocalProfileManager` interface in `core/interfaces`
- [x] Created `LocalProfileManagerImpl` in `implementation/profile/` (moved from plugins/main)
- [x] Moved profile keys to `core/keys/` (ProfileComposedStringKey, ProfileComposedBooleanKey, ProfileComposedDoubleKey, ProfileIntKey)
- [x] Moved validation strings to `core/ui/res/values/strings.xml`
- [x] Updated `ProfilePlugin` to delegate to `LocalProfileManager`
- [x] Added Dagger binding in `ImplementationModule`
- [x] Compile verified

### Phase 1: Profile Management Screen
- [ ] Create `ProfileManagementScreen` composable
  - [ ] Scaffold with TopAppBar
  - [ ] LazyColumn of ProfileCards
  - [ ] FAB or toolbar action for "Add Profile"
- [ ] Create `ProfileManagementViewModel`
  - [ ] Observes LocalProfileManager for profile list
  - [ ] Observes ProfileFunction for active profile state
  - [ ] Handles add/clone/remove profile actions

### Phase 2: ProfileCard Component
- [ ] Create `ProfileCard` composable
  - [ ] Profile name display
  - [ ] Active state highlighting (different color)
  - [ ] When active: percentage, timeshift, remaining time
  - [ ] Following profile (when temp expires)
  - [ ] Action buttons: Edit, Show, Activate
- [ ] Create `ProfileCardState` data class

### Phase 3: Profile Activation (Compose)
- [ ] Create activation UI (bottom sheet or dialog)
  - [ ] Duration input (0 = permanent)
  - [ ] Percentage slider (30-200%)
  - [ ] Timeshift slider (-12 to +12 hours)
  - [ ] Optional TT checkbox
  - [ ] Reuse button (when current has custom %)
- [ ] Wire to ProfileFunction.createProfileSwitch()

### Phase 4: Simplify ProfileEditor
- [ ] Remove profile list management from ProfileEditorScreen
  - [ ] Remove ProfileHeader with add/clone/delete
  - [ ] Remove profile dropdown
- [ ] ProfileEditor receives profile index/name as parameter
- [ ] Only handles editing single profile values

### Phase 5: Integration & Navigation
- [ ] Wire Edit action → ProfileEditor
- [ ] Wire Show action → ProfileViewer
- [ ] Wire Activate action → Activation UI
- [ ] Update ProfilePlugin to show new screen
- [ ] Handle navigation back from sub-screens

### Phase 6: Testing & Polish
- [ ] Test all flows
- [ ] Handle protection checks
- [ ] Verify legacy code still works (ProfileFragment, ProfileSwitchDialog)

---

## Files Created (Phase 0)

```
core/interfaces/src/.../profile/LocalProfileManager.kt     # Interface ✅
implementation/src/.../profile/LocalProfileManagerImpl.kt  # Implementation ✅
core/keys/src/.../ProfileComposedStringKey.kt              # Keys ✅
core/keys/src/.../ProfileComposedBooleanKey.kt             # Keys ✅
core/keys/src/.../ProfileComposedDoubleKey.kt              # Keys ✅
core/keys/src/.../ProfileIntKey.kt                         # Keys ✅
core/ui/src/.../compose/Snackbar.kt                        # Snackbar infrastructure ✅
```

## Files to Create (Remaining Phases)

```
ui/src/.../compose/profileManagement/
├── ProfileManagementScreen.kt                             # Main screen
├── ProfileManagementViewModel.kt                          # ViewModel
├── ProfileCard.kt                                         # Card composable
└── ProfileActivationSheet.kt                              # Activation bottom sheet
```

## Files to Modify

```
plugins/main/profile/ProfilePlugin.kt
  → Delegate to LocalProfileManager, keep as bridge

plugins/main/profile/ProfileEditorViewModel.kt
  → Use LocalProfileManager instead of ProfilePlugin

plugins/main/profile/ProfileScreen.kt
  → Simplify: remove profile list management
```

## Legacy Code (Keep For Now)

```
ui/dialogs/ProfileSwitchDialog.kt          # Keep working
ui/res/layout/dialog_profileswitch.xml     # Keep working
plugins/main/profile/ProfileFragment.kt    # Keep working (old Activity UI)
```

## Dependencies

- LocalProfileManager (new)
- ProfileFunction (for active state & activation)
- ProfileEditorViewModel (existing, modified)
- ProfileViewer (existing)
- ProtectionCheck (for security)

## Notes

- Follow existing Compose patterns in the codebase
- **Final state: Pure Material 3 Expressive** design language
- Use Material 3 components consistently
- Keep backward compatibility during migration
- ProfilePlugin remains for old Activity-based UI bridge
- **IMPORTANT: Avoid adding new module dependencies** - can significantly slow compilation. Prefer inlining constants or other solutions. Always discuss before adding dependencies.
