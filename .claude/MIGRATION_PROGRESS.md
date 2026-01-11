# Migration Progress: ProfileSource.SingleProfile → LocalProfileManager

## Status Summary (Saved for Compaction)

| Component | Status |
|-----------|--------|
| **Main App Build** | PASSING |
| **Migration-related Tests** | FIXED |
| **Pre-existing Test Failures** | IN PROGRESS - pump/* tests remaining |

---

## COMPLETED: Migration Work

### 1. SingleProfile Class Move
- Moved from `ProfileSource` interface to `LocalProfileManager` interface
- All references updated

### 2. Production Code Migration (23+ files)
All production code updated to use `LocalProfileManager` directly:
- core/implementation, plugins/main, plugins/aps, plugins/automation
- plugins/sync, ui modules

### 3. ProfileFragment.kt Updated
- Uses `localProfileManager` for data operations
- Uses `profilePlugin` for UI operations (isValidEditState, storeSettings with dialogs)

### 4. Migration-related Test Updates
- `TestBaseWithProfile.kt` - Added `@Mock lateinit var localProfileManager: LocalProfileManager`
- `SmsCommunicatorPluginTest.kt` - Uses base class mock, fixed constructor order, added `sp`
- `ActionsTestBase.kt` - Uses `localProfileManager` from base class
- `ActionProfileSwitchTest.kt` - Updated `InputProfileName` constructor
- `InputProfileNameTest.kt` - Updated `InputProfileName` constructor
- `AutotunePluginTest.kt` - Added `localProfileManager` to constructor

---

## COMPLETED: Pre-existing Test Fixes

### plugins/automation (3 files)
- [x] `AutomationPluginTest.kt` - Added `sp` parameter
- [x] `BolusTimerImplTest.kt` - Added `sp` parameter
- [x] `CarbTimerImplTest.kt` - Added `sp` parameter

### plugins/aps (1 file)
- [x] `LoopPluginTest.kt` - Added `sp` parameter

### plugins/sensitivity (3 files)
- [x] `SensitivityAAPSPluginTest.kt` - Added `config` parameter
- [x] `SensitivityOref1PluginTest.kt` - Added `config` parameter
- [x] `SensitivityWeightedAveragePluginTest.kt` - Fixed (only SensitivityAAPSPlugin needs `config`)

---

## COMPLETED: Additional Pre-existing Test Fixes

### plugins/constraints (2 files)
- [x] `ConstraintsCheckerImplTest.kt` - Added `sp` mock, updated SafetyPlugin/DanaRPlugin/DanaRSPlugin/InsightPlugin with `config`
- [x] `SafetyPluginTest.kt` - Added `sp` mock to SafetyPlugin constructor

### plugins/main (1 file)
- [x] `OverviewPluginTest.kt` - Added `visibilityContext`, `uel` mocks and `profileUtil` to constructor

### plugins/source (12 files)
- [x] All source plugin tests - Removed `uiInteraction`, added `config` parameter

### plugins/sync (16 files)
- [x] NSClientV3Plugin worker tests (7 files) - Added `sp` parameter
- [x] `OpenHumansUploaderPluginTest.kt` - Added `config` parameter
- [x] `TidepoolPluginTest.kt` - Added `config` parameter
- [x] `WearPluginTest.kt` - Added `sp` parameter
- [x] `NSClientPluginTest.kt` - Added `config` parameter
- [x] `DataSyncSelectorV3Test.kt` - Added `localProfileManager` parameter
- [x] `ProfileSwitchExtensionKtTest.kt` - Uses inherited `localProfileManager`
- [x] `NsIncomingDataProcessorTest.kt` - Replaced `profileSource` with `localProfileManager`
- [x] `GarminPluginTest.kt` - Added `config` parameter

---

## IN PROGRESS: pump/* tests

### pump/danar, pump/danars, pump/insight, etc.
- Need to verify compilation and fix any remaining constructor mismatches

---

## Files Intentionally Unchanged

1. **ProfileFunctionImpl.kt** - Works with ANY profile source + circular DI
2. **SWDefinition.kt** - Needs `PluginBase` cast for UI
3. **MainActivity.kt** - Uses `javaClass.simpleName` for analytics

---

## Resume Instructions

To continue fixing remaining tests:
1. Check production class constructor order
2. Add missing `sp` or `config` parameters to test constructor calls
3. Add corresponding `@Mock` declarations
4. Compile module to verify: `./gradlew.bat :module:compileFullDebugUnitTestKotlin --quiet --no-daemon`