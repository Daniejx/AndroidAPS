# MainContent Migration to :ui Module

## Goal

Move `MainContent` composable from `app` module to `:ui` module to minimize app module code and improve
recompilation times.

## Current State

**File:** `app/src/main/kotlin/app/aaps/ComposeMainActivity.kt`
**Lines:** 100-360 (~260 lines)

### Progress Summary
- ✅ **Phase 1:** ProtectionCheck abstraction - `requestProtection()` without Activity parameter
- ✅ **Phase 1.5:** Password dialogs extracted, ToastUtils migrated to Snackbar
- ⏳ **Phase 0:** Compose dialogs for UiInteraction (blocking MainContent migration)
- ⏳ **Phases 2-6:** Activity navigation, AppRoute move, SkinProvider, ViewModels, AppNavHost

### MainContent Structure

```
MainContent()
├── NavHost with 9 routes
│   ├── Main (115 lines of callbacks)
│   ├── Profile
│   ├── ProfileActivation
│   ├── ProfileEditor
│   ├── Treatments
│   ├── Stats
│   ├── ProfileHelper
│   ├── Preferences
│   └── PluginPreferences
└── Activity-bound operations throughout
```

---

## Blocking Dependencies

### 1. UiInteraction (FragmentManager-based dialogs)

**Current:** 15 methods require `FragmentManager` parameter

| Method                    | Used In MainContent |
|---------------------------|---------------------|
| `runTempTargetDialog`     | Yes                 |
| `runTempBasalDialog`      | Yes                 |
| `runExtendedBolusDialog`  | Yes                 |
| `runFillDialog`           | Yes                 |
| `runCareDialog`           | Yes (7 event types) |
| `runSiteRotationDialog`   | Yes                 |
| `runWizardDialog`         | No (Overview)       |
| `runLoopDialog`           | No (Overview)       |
| `runProfileSwitchDialog`  | No (Overview)       |
| `runTreatmentDialog`      | No                  |
| `runInsulinDialog`        | No                  |
| `runCalibrationDialog`    | No                  |
| `runCarbsDialog`          | No                  |
| `runBolusProgressDialog`  | No                  |

**Blocker:** Cannot call from `:ui` module without FragmentManager

### 2. ProtectionCheck (Activity-based)

**Current:** `protectionCheck.queryProtection(activity, type, callback)`

**Used for:**

- Profile management access
- Preferences access
- Bolus-related actions (temp target, temp basal, extended bolus, fill, profile activation)

**Blocker:** Requires `Activity` parameter

### 3. Activity Navigation

**Current:** Direct `startActivity(Intent(this@ComposeMainActivity, ...))`

| Target Activity        | Module               |
|------------------------|----------------------|
| `HistoryBrowseActivity`| app                  |
| `MainActivity`         | app                  |
| `SetupWizardActivity`  | :plugins:configuration |
| `SingleFragmentActivity`| :plugins:configuration |
| `uiInteraction.tddStatsActivity` | via interface |
| `uiInteraction.historyBrowseActivity` | via interface |

**Blocker:** Cannot reference app module activities from `:ui`

### 4. AppRoute

**Current:** `app.aaps.compose.navigation.AppRoute` in app module

**Blocker:** Minor - just needs to move to `:ui`

### 5. SkinProvider

**Current:** `skinProvider.list` from `:plugins:main`

**Used:** `AllPreferencesScreen` skin entries

**Blocker:** `:ui` doesn't depend on `:plugins:main`

### 6. Injected ViewModels

**Current:** 7 ViewModels `@Inject`ed into `ComposeMainActivity`

**Blocker:** Need alternative provision mechanism

---

## Migration Phases

### Phase 0: Prerequisites - UiInteraction Compose Dialogs

**Goal:** Eliminate `FragmentManager` dependency from dialog methods

#### Step 0.1: Create Compose Dialog State Management

```kotlin
// :core:interfaces - new
interface DialogManager {
    val currentDialog: StateFlow<DialogState?>
    fun showDialog(dialog: DialogState)
    fun dismissDialog()
}

sealed class DialogState {
    data class TempTarget(...) : DialogState()
    data class TempBasal(...) : DialogState()
    data class ExtendedBolus(...) : DialogState()
    data class Fill(...) : DialogState()
    data class CarePortal(val eventType: EventType, @StringRes val title: Int) : DialogState()
    data class SiteRotation(...) : DialogState()
    // ... other dialogs
}
```

#### Step 0.2: Create Compose Dialog Components

**Location:** `:ui/src/main/kotlin/app/aaps/ui/compose/dialogs/`

| Dialog               | Priority | Complexity |
|----------------------|----------|------------|
| TempTargetDialog     | High     | Medium     |
| TempBasalDialog      | High     | Medium     |
| ExtendedBolusDialog  | High     | Low        |
| FillDialog           | High     | Low        |
| CarePortalDialog     | High     | Medium     |
| SiteRotationDialog   | High     | Medium     |
| WizardDialog         | Medium   | High       |
| LoopDialog           | Medium   | Medium     |
| ProfileSwitchDialog  | Medium   | Medium     |
| TreatmentDialog      | Low      | Low        |
| InsulinDialog        | Low      | Low        |
| CalibrationDialog    | Low      | Low        |
| CarbsDialog          | Low      | Medium     |
| BolusProgressDialog  | Low      | Medium     |

#### Step 0.3: Add Compose Dialog Methods to UiInteraction

```kotlin
// :core:interfaces - extend UiInteraction
interface UiInteraction {
    // Existing FragmentManager methods (keep for backward compat)
    fun runTempTargetDialog(fragmentManager: FragmentManager)

    // NEW: Compose-friendly methods (no FragmentManager)
    fun showTempTargetDialog()
    fun showTempBasalDialog()
    fun showExtendedBolusDialog()
    fun showFillDialog()
    fun showCareDialog(eventType: EventType, @StringRes title: Int)
    fun showSiteRotationDialog()

    // Dialog state for Compose observation
    val dialogState: StateFlow<DialogState?>
    fun dismissDialog()
}
```

#### Step 0.4: Implement in UiInteractionImpl

```kotlin
// :app or appropriate module
class UiInteractionImpl @Inject constructor(
    private val dialogManager: DialogManager
) : UiInteraction {

    override val dialogState: StateFlow<DialogState?> = dialogManager.currentDialog

    override fun showTempTargetDialog() {
        dialogManager.showDialog(DialogState.TempTarget(...))
    }

    // FragmentManager methods delegate to Compose versions
    override fun runTempTargetDialog(fragmentManager: FragmentManager) {
        showTempTargetDialog()
    }
}
```

---

### Phase 1: ProtectionCheck Abstraction ✅ COMPLETED

**Goal:** Enable protection queries without Activity reference at call sites

#### Completed Work

1. **Added types to :core:interfaces**
   - `ProtectionResult` enum (GRANTED, DENIED, CANCELLED)
   - `ProtectionRequest` data class with id, protection, type, titleRes, onResult callback

2. **Extended ProtectionCheck interface**
   - `requestProtection(protection, onResult)` - Compose-friendly, no activity parameter
   - `pendingRequest: StateFlow<ProtectionRequest?>` - for ProtectionHost observation
   - `completeRequest(requestId, result)` - called by ProtectionHost after dialog

3. **Created ProtectionHost composable** (:core:ui/compose/ProtectionHost.kt)
   - Observes `pendingRequest` StateFlow
   - Shows appropriate dialog based on `ProtectionType`
   - Uses pure Compose dialogs from PasswordDialogs.kt
   - Biometric handled via `showBiometric` function parameter (platform requires Activity)

4. **Migrated all Compose call sites**
   - All 10 `queryProtection` calls in ComposeMainActivity migrated to `requestProtection`
   - No Activity parameter needed at call sites
   - Fragment-based code unchanged (backward compatible)

5. **Bypassed PasswordCheckImpl workarounds**
   - ProtectionHost uses QueryPasswordDialog directly
   - No more Android Dialog with ComposeView wrapper for protection dialogs

#### Original Analysis

Current flow in `ProtectionCheckImpl`:
```
queryProtection(activity, protection, ok, cancel, fail)
    ├─► NONE → ok?.run()                              ✅ No activity needed
    ├─► BIOMETRIC → BiometricPrompt(activity, ...)    ❌ Needs FragmentActivity (platform limit)
    ├─► MASTER_PASSWORD → passwordCheck.queryPassword ✅ Only needs Context
    ├─► CUSTOM_PASSWORD → passwordCheck.queryPassword ✅ Only needs Context
    └─► CUSTOM_PIN → passwordCheck.queryPassword      ✅ Only needs Context
```

**Key insight:** `PasswordCheckImpl` already uses Compose dialogs! Only `BiometricPrompt` truly needs `FragmentActivity`.

#### Step 1.1: Add Types to :core:interfaces

```kotlin
// :core:interfaces/protection/ProtectionCheck.kt

enum class ProtectionResult {
    GRANTED,
    DENIED,
    CANCELLED
}

data class ProtectionRequest(
    val id: Long,                    // Unique ID to track request
    val protection: Protection,
    val type: ProtectionType,        // BIOMETRIC, PASSWORD, PIN, NONE
    val titleRes: Int,
    val onResult: (ProtectionResult) -> Unit
)
```

#### Step 1.2: Extend ProtectionCheck Interface

```kotlin
interface ProtectionCheck {
    // Existing (keep for backward compatibility with Fragment code)
    fun queryProtection(activity: FragmentActivity, protection: Protection, ok: Runnable?, cancel: Runnable?, fail: Runnable?)

    // NEW: Compose-friendly - no activity parameter at call site
    fun requestProtection(protection: Protection, onResult: (ProtectionResult) -> Unit)

    // For ProtectionHost composable to observe
    val pendingRequest: StateFlow<ProtectionRequest?>

    // Called by ProtectionHost when biometric needs activity
    fun handleBiometricWithActivity(activity: FragmentActivity, request: ProtectionRequest)

    // Called by ProtectionHost after password/PIN dialog completes
    fun completeRequest(requestId: Long, result: ProtectionResult)
}
```

#### Step 1.3: Update ProtectionCheckImpl

```kotlin
class ProtectionCheckImpl @Inject constructor(...) : ProtectionCheck {

    private val _pendingRequest = MutableStateFlow<ProtectionRequest?>(null)
    override val pendingRequest: StateFlow<ProtectionRequest?> = _pendingRequest.asStateFlow()

    private var requestIdCounter = 0L

    override fun requestProtection(protection: Protection, onResult: (ProtectionResult) -> Unit) {
        // Fast path: no protection needed
        if (!isLocked(protection)) {
            onOk(protection)
            onResult(ProtectionResult.GRANTED)
            return
        }

        val type = ProtectionType.entries[preferences.get(protectionTypeResourceIDs[protection.ordinal])]
        val titleRes = when (type) {
            ProtectionType.CUSTOM_PIN -> titlePinResourceIDs[protection.ordinal]
            else -> titlePassResourceIDs[protection.ordinal]
        }

        _pendingRequest.value = ProtectionRequest(
            id = ++requestIdCounter,
            protection = protection,
            type = type,
            titleRes = titleRes,
            onResult = { result ->
                if (result == ProtectionResult.GRANTED) onOk(protection)
                _pendingRequest.value = null
                onResult(result)
            }
        )
    }

    override fun handleBiometricWithActivity(activity: FragmentActivity, request: ProtectionRequest) {
        BiometricCheck.biometricPrompt(
            activity,
            request.titleRes,
            ok = { request.onResult(ProtectionResult.GRANTED) },
            cancel = { request.onResult(ProtectionResult.CANCELLED) },
            fail = { request.onResult(ProtectionResult.DENIED) },
            passwordCheck
        )
    }

    override fun completeRequest(requestId: Long, result: ProtectionResult) {
        _pendingRequest.value?.let { request ->
            if (request.id == requestId) {
                request.onResult(result)
            }
        }
    }
}
```

#### Step 1.4: Create ProtectionHost Composable in :core:ui

```kotlin
// :core:ui/compose/ProtectionHost.kt

@Composable
fun ProtectionHost(
    protectionCheck: ProtectionCheck,
    passwordCheck: PasswordCheck,
    preferences: Preferences,
    cryptoUtil: CryptoUtil
) {
    val request by protectionCheck.pendingRequest.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    request?.let { req ->
        when (req.type) {
            ProtectionType.NONE -> {
                // Should not happen, but handle gracefully
                LaunchedEffect(req.id) {
                    protectionCheck.completeRequest(req.id, ProtectionResult.GRANTED)
                }
            }

            ProtectionType.BIOMETRIC -> {
                // Biometric needs activity - delegate back
                LaunchedEffect(req.id) {
                    activity?.let {
                        protectionCheck.handleBiometricWithActivity(it, req)
                    } ?: run {
                        // Fallback if no activity (shouldn't happen in normal flow)
                        protectionCheck.completeRequest(req.id, ProtectionResult.CANCELLED)
                    }
                }
            }

            ProtectionType.MASTER_PASSWORD -> {
                PasswordProtectionDialog(
                    titleRes = req.titleRes,
                    passwordKey = StringKey.ProtectionMasterPassword,
                    pinInput = false,
                    preferences = preferences,
                    cryptoUtil = cryptoUtil,
                    onResult = { result ->
                        protectionCheck.completeRequest(req.id, result)
                    }
                )
            }

            ProtectionType.CUSTOM_PASSWORD -> {
                val passwordKey = getPasswordKeyForProtection(req.protection)
                PasswordProtectionDialog(
                    titleRes = req.titleRes,
                    passwordKey = passwordKey,
                    pinInput = false,
                    preferences = preferences,
                    cryptoUtil = cryptoUtil,
                    onResult = { result ->
                        protectionCheck.completeRequest(req.id, result)
                    }
                )
            }

            ProtectionType.CUSTOM_PIN -> {
                val pinKey = getPinKeyForProtection(req.protection)
                PasswordProtectionDialog(
                    titleRes = req.titleRes,
                    passwordKey = pinKey,
                    pinInput = true,
                    preferences = preferences,
                    cryptoUtil = cryptoUtil,
                    onResult = { result ->
                        protectionCheck.completeRequest(req.id, result)
                    }
                )
            }
        }
    }
}

@Composable
private fun PasswordProtectionDialog(
    titleRes: Int,
    passwordKey: StringPreferenceKey,
    pinInput: Boolean,
    preferences: Preferences,
    cryptoUtil: CryptoUtil,
    onResult: (ProtectionResult) -> Unit
) {
    // Reuse existing dialog composables from PasswordCheckImpl
    // or extract them to shared location
}
```

#### Step 1.5: Integrate in ComposeMainActivity

```kotlin
class ComposeMainActivity : DaggerAppCompatActivityWithResult() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(...) {
                AapsTheme {
                    // Add ProtectionHost at root - handles all protection dialogs
                    ProtectionHost(
                        protectionCheck = protectionCheck,
                        passwordCheck = passwordCheck,
                        preferences = preferences,
                        cryptoUtil = cryptoUtil
                    )

                    MainContent()  // No longer needs activity for protection!
                }
            }
        }
    }
}
```

#### Step 1.6: Migrate Call Sites in MainContent

```kotlin
// BEFORE (activity-dependent)
onTempTargetClick = {
    protectionCheck.queryProtection(
        this@ComposeMainActivity,
        ProtectionCheck.Protection.BOLUS,
        UIRunnable { uiInteraction.runTempTargetDialog(supportFragmentManager) }
    )
}

// AFTER (Compose-friendly)
onTempTargetClick = {
    protectionCheck.requestProtection(ProtectionCheck.Protection.BOLUS) { result ->
        if (result == ProtectionResult.GRANTED) {
            uiInteraction.runTempTargetDialog(supportFragmentManager)
        }
    }
}
```

#### Benefits
- No `activity` parameter at 28+ call sites in MainContent
- Pure Compose dialogs for password/PIN (already implemented in PasswordCheckImpl)
- Biometric still works via activity obtained from LocalContext
- Backward compatible - existing Fragment code unchanged
- Can be used from :ui module without activity reference

---

### Phase 1.5: Password Dialog & Preference Cleanup ✅ COMPLETED

**Goal:** Clean up password-related code, extract reusable dialogs, migrate to native Compose patterns

#### Completed Work

1. **Extracted password dialogs to shared location** (:core:ui/compose/PasswordDialogs.kt)
   - `QueryPasswordDialog` - for verifying existing password/PIN
   - `SetPasswordDialog` - for setting/changing password/PIN (with confirmation)
   - `QueryAnyPasswordDialog` - for generic password entry with explanation/warning
   - Removed ~270 lines of private composables from PasswordCheckImpl

2. **Created CompositionLocals for password operations** (:core:ui/compose/preference/LocalPasswordCheck.kt)
   - `LocalCheckPassword` - `(enteredPassword: String, storedHash: String) -> Boolean`
   - `LocalHashPassword` - `(password: String) -> String`
   - `LocalSnackbarHostState` - for native Compose feedback
   - Deprecated `LocalPasswordCheck` (legacy PasswordCheck interface)

3. **Updated password preference composables**
   - `AdaptivePasswordPreferenceItem` - uses SetPasswordDialog directly + Snackbar
   - `AdaptiveMasterPasswordPreferenceItem` - uses QueryPasswordDialog + SetPasswordDialog + Snackbar
   - `AdaptivePreferenceItem` - uses LocalCheckPassword/LocalHashPassword

4. **Updated AllPreferencesScreen**
   - Signature changed: accepts `checkPassword` and `hashPassword` functions
   - Provides LocalCheckPassword, LocalHashPassword, LocalSnackbarHostState
   - Added SnackbarHost to Scaffold

5. **Migrated ToastUtils to native Snackbar** (in password preferences)
   - All ToastUtils calls replaced with `snackbarHostState?.showSnackbar()`
   - Uses coroutine scope for async snackbar display

#### Files Modified
- :core:ui/compose/PasswordDialogs.kt (NEW)
- :core:ui/compose/preference/LocalPasswordCheck.kt (extended)
- :core:ui/compose/preference/AdaptivePasswordPreference.kt
- :core:ui/compose/preference/AdaptiveMasterPasswordPreference.kt
- :core:ui/compose/preference/AdaptivePreferenceItem.kt
- :ui/compose/preferences/AllPreferencesScreen.kt
- :implementation/protection/PasswordCheckImpl.kt (simplified)
- :app/ComposeMainActivity.kt (passes cryptoUtil functions)

---

### Phase 2: Activity Navigation Abstraction

**Goal:** Abstract activity launches behind interface

#### Step 2.1: Extend UiInteraction for Navigation

```kotlin
// :core:interfaces - extend UiInteraction
interface UiInteraction {
    // Existing activity class references (keep)
    val historyBrowseActivity: Class<*>
    val tddStatsActivity: Class<*>

    // NEW: Direct navigation methods (no Intent construction needed)
    fun navigateToHistoryBrowser()
    fun navigateToTddStats()
    fun navigateToSetupWizard()
    fun navigateToSingleFragment(pluginIndex: Int)
    fun navigateToClassicUi()
    fun exitApp(reason: String, source: Sources)
}
```

#### Step 2.2: Implement Navigation in UiInteractionImpl

```kotlin
class UiInteractionImpl @Inject constructor(
    private val activityProvider: ActivityProvider,
    private val configBuilder: ConfigBuilder
) : UiInteraction {

    override fun navigateToHistoryBrowser() {
        activityProvider.currentActivity?.let { activity ->
            activity.startActivity(Intent(activity, historyBrowseActivity))
        }
    }

    override fun exitApp(reason: String, source: Sources) {
        activityProvider.currentActivity?.finish()
        configBuilder.exitApp(reason, source, false)
    }
}
```

---

### Phase 3: Move AppRoute to :ui

**Goal:** Navigation routes available in :ui module

#### Step 3.1: Move File

```
FROM: app/src/main/kotlin/app/aaps/compose/navigation/AppRoute.kt
TO:   ui/src/main/kotlin/app/aaps/ui/compose/navigation/AppRoute.kt
```

#### Step 3.2: Update Package

```kotlin
package app.aaps.ui.compose.navigation

sealed class AppRoute(val route: String) { ... }
```

#### Step 3.3: Update Imports in app module

---

### Phase 4: SkinProvider Abstraction

**Goal:** Skin list available without :plugins:main dependency

#### Option A: Move to :core:interfaces

```kotlin
// :core:interfaces
interface SkinProvider {
    val skins: List<SkinInfo>
}

data class SkinInfo(
    val className: String,
    @StringRes val description: Int
)
```

#### Option B: Pass as Parameter

```kotlin
// In ComposeMainActivity - pass skin entries to AppNavHost
val skinEntries = skinProvider.list.associate { ... }
AppNavHost(..., skinEntries = skinEntries)
```

**Recommendation:** Option B (simpler, no interface changes needed)

---

### Phase 5: ViewModel Provision Strategy

**Goal:** ViewModels accessible in :ui module

#### Option A: Pass as Parameters

```kotlin
// :ui module
@Composable
fun AppNavHost(
    viewModels: AppViewModels,  // data class with all VMs
    ...
)

data class AppViewModels(
    val main: MainViewModel,
    val actions: ActionsViewModel,
    val treatments: TreatmentsViewModel,
    val stats: StatsViewModel,
    val profileHelper: ProfileHelperViewModel,
    val profileEditor: ProfileEditorViewModel,
    val profileManagement: ProfileManagementViewModel
)
```

#### Option B: Hilt ViewModel Integration

ViewModels already in :ui, use `hiltViewModel()` in Compose

```kotlin
// In :ui NavHost
composable(AppRoute.Treatments.route) {
    val viewModel: TreatmentsViewModel = hiltViewModel()
    TreatmentsScreen(viewModel = viewModel, ...)
}
```

**Recommendation:** Option B if using Hilt, Option A otherwise

---

### Phase 6: Create AppNavHost in :ui

**Goal:** Move NavHost declaration to :ui module

#### Step 6.1: Create Callbacks Interface

```kotlin
// :ui module
interface AppNavigationCallbacks {
    // Protection
    fun withBoluProtection(action: () -> Unit)
    fun withPreferencesProtection(action: () -> Unit)

    // Navigation
    fun navigateToHistoryBrowser()
    fun navigateToTddStats()
    fun navigateToSetupWizard()
    fun navigateToSingleFragment(pluginIndex: Int)
    fun switchToClassicUi()
    fun exitApp()

    // Dialogs (after Phase 0)
    fun showTempTargetDialog()
    fun showTempBasalDialog()
    fun showExtendedBolusDialog()
    fun showFillDialog()
    fun showCareDialog(eventType: UiInteraction.EventType, @StringRes title: Int)
    fun showSiteRotationDialog()

    // Misc
    fun runAlarm(comment: String, title: String, soundId: Int)
}
```

#### Step 6.2: Create AppNavHost Composable

```kotlin
// :ui module - new file
// ui/src/main/kotlin/app/aaps/ui/compose/navigation/AppNavHost.kt

@Composable
fun AppNavHost(
    navController: NavHostController,
    callbacks: AppNavigationCallbacks,
    mainViewModel: MainViewModel,
    actionsViewModel: ActionsViewModel,
    // ... other VMs or use hiltViewModel()
    skinEntries: Map<String, String>,
    preferences: Preferences,
    // ... other dependencies
) {
    NavHost(
        navController = navController,
        startDestination = AppRoute.Main.route
    ) {
        // All composable() declarations move here
        // ~200 lines
    }
}
```

#### Step 6.3: Simplify ComposeMainActivity

```kotlin
// app module - simplified
class ComposeMainActivity : DaggerAppCompatActivityWithResult() {

    @Inject lateinit var uiInteraction: UiInteraction
    @Inject lateinit var protectionCheck: ProtectionCheck
    // ... minimal injections

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()
            val callbacks = remember { createCallbacks(navController) }

            CompositionLocalProvider(...) {
                AapsTheme {
                    AppNavHost(
                        navController = navController,
                        callbacks = callbacks,
                        skinEntries = skinProvider.list.associate { ... },
                        ...
                    )
                }
            }
        }
    }

    private fun createCallbacks(navController: NavController): AppNavigationCallbacks {
        return object : AppNavigationCallbacks {
            override fun withBolusProtection(action: () -> Unit) {
                protectionCheck.queryProtection(this@ComposeMainActivity, Protection.BOLUS, UIRunnable(action))
            }
            // ... ~50 lines of callback implementations
        }
    }
}
```

---

## Final Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        app module                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ ComposeMainActivity (~100 lines)                      │  │
│  │  - Creates callbacks                                  │  │
│  │  - Provides CompositionLocals                         │  │
│  │  - Calls AppNavHost                                   │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ uses
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                         :ui module                          │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ AppNavHost (~200 lines)                               │  │
│  │  - NavHost with all routes                            │  │
│  │  - Screen compositions                                │  │
│  │  - Uses AppNavigationCallbacks                        │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ AppRoute (sealed class)                               │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ AppNavigationCallbacks (interface)                    │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ Compose Dialogs (TempTarget, TempBasal, etc.)         │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ uses
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    :core:interfaces                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ UiInteraction (extended)                              │  │
│  │  - showTempTargetDialog() (no FragmentManager)        │  │
│  │  - dialogState: StateFlow<DialogState?>               │  │
│  │  - navigateToHistoryBrowser()                         │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ ProtectionCheck (extended)                            │  │
│  │  - queryProtection without Activity param             │  │
│  ├───────────────────────────────────────────────────────┤  │
│  │ DialogState (sealed class)                            │  │
│  │ ActivityProvider (interface)                          │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## Dependency Elimination Summary

| Dependency              | Current                          | Target                              | Phase | Status |
|-------------------------|----------------------------------|-------------------------------------|-------|--------|
| FragmentManager dialogs | `runXxxDialog(fragmentManager)`  | `showXxxDialog()` + Compose dialog  | 0     | TODO   |
| ProtectionCheck         | `queryProtection(activity, ...)` | `requestProtection(type, callback)` | 1     | ✅ DONE |
| PasswordCheck workarounds | Android Dialog wrapper         | Pure Compose dialogs                | 1.5   | ✅ DONE |
| ToastUtils in Compose   | `ToastUtils.xxxToast()`          | Native Snackbar                     | 1.5   | ✅ DONE |
| Activity navigation     | `startActivity(Intent(...))`     | `uiInteraction.navigateTo...()`     | 2     | TODO   |
| AppRoute                | In app module                    | In :ui module                       | 3     | TODO   |
| SkinProvider            | Direct injection                 | Passed as parameter                 | 4     | TODO   |
| ViewModels              | @Inject in Activity              | hiltViewModel() or parameters       | 5     | TODO   |

---

## Implementation Order

```
Phase 0 ──► Phase 1 ──► Phase 1.5 ──► Phase 2 ──► Phase 3 ──► Phase 4 ──► Phase 5 ──► Phase 6
   │           │            │            │           │           │           │           │
   │           │            │            │           │           │           │           └─► AppNavHost in :ui
   │           │            │            │           │           │           └─► ViewModel provision
   │           │            │            │           │           └─► SkinProvider abstraction
   │           │            │            │           └─► Move AppRoute
   │           │            │            └─► Activity navigation abstraction
   │           │            └─► Password cleanup & Snackbar ✅ DONE
   │           └─► ProtectionCheck abstraction ✅ DONE
   └─► Compose dialogs (biggest effort)
```

**Critical Path:** Phase 0 (Compose dialogs) is the largest effort and blocks MainContent migration.
**Progress:** Phase 1 and 1.5 completed - ProtectionCheck no longer needs Activity at call sites.

---

## Metrics

| Metric                     | Before  | After   |
|----------------------------|---------|---------|
| MainContent in app         | 260 lines | 0 lines |
| ComposeMainActivity total  | 470 lines | ~150 lines |
| AppNavHost in :ui          | 0 lines | ~200 lines |
| New interfaces             | 0       | 2-3     |
| New Compose dialogs        | 0       | 6-14    |

---

## Risks & Mitigations

| Risk                                    | Mitigation                                      |
|-----------------------------------------|-------------------------------------------------|
| Compose dialogs behavior differs        | Test extensively, keep Fragment versions temporarily |
| Breaking existing dialog flows          | Maintain backward compat in UiInteraction       |
| ViewModel scope issues                  | Use proper Compose lifecycle integration        |
| Protection dialogs need Activity        | ActivityProvider pattern                        |

---

## Open Questions

1. **Dialog state management:** Global `DialogManager` vs per-screen state?
2. **Hilt integration:** Use `hiltViewModel()` in :ui or pass VMs from app?
3. **Fragment dialog deprecation timeline:** Keep Fragment versions how long?
4. ~~**Protection biometric prompts:** These inherently need Activity - acceptable to keep in app?~~
   - ✅ RESOLVED: Biometric handled via `showBiometric` function parameter passed to ProtectionHost.
     Activity obtained from LocalContext, passed to BiometricCheck.biometricPrompt().
     No platform workaround needed - BiometricPrompt requires FragmentActivity by design.
