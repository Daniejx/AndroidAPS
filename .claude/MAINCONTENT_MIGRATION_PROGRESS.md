# Migration Progress: ProfileSource.SingleProfile → LocalProfileManager

## Status: COMPLETE ✓
Build verified successful.

## Task Summary
Moved `SingleProfile` class from `ProfileSource` interface to `LocalProfileManager` interface, and replaced most `activePlugin.activeProfileSource` usages with direct `LocalProfileManager` injection.

## Completed Changes

### 1. SingleProfile Class Move
- **From:** `core/interfaces/.../ProfileSource.kt`
- **To:** `core/interfaces/.../LocalProfileManager.kt`
- Updated all references from `ProfileSource.SingleProfile` to `LocalProfileManager.SingleProfile`

### 2. Files Updated to Use LocalProfileManager Directly

#### Core/Implementation:
- `implementation/.../LocalProfileManagerImpl.kt` - Updated references

#### Plugins/Main:
- `plugins/main/.../SmsCommunicatorPlugin.kt` - Added LocalProfileManager injection
- `plugins/main/.../ActionsFragment.kt` - Added LocalProfileManager injection
- `plugins/main/.../ProfilePlugin.kt` - Updated SingleProfile references

#### Plugins/APS:
- `plugins/aps/.../AutotunePlugin.kt` - Added LocalProfileManager injection
- `plugins/aps/.../AutotuneFragment.kt` - Added LocalProfileManager injection

#### Plugins/Automation:
- `plugins/automation/.../InputProfileName.kt` - Changed constructor param
- `plugins/automation/.../ActionProfileSwitch.kt` - Added LocalProfileManager injection
- `plugins/automation/.../ActionRunAutotune.kt` - Added LocalProfileManager injection

#### Plugins/Sync:
- `plugins/sync/.../DataSyncSelectorXdripImpl.kt` - Added LocalProfileManager injection
- `plugins/sync/.../DataSyncSelectorV3.kt` - Added LocalProfileManager injection
- `plugins/sync/.../DataSyncSelectorV1.kt` - Added LocalProfileManager injection
- `plugins/sync/.../NsIncomingDataProcessor.kt` - Added LocalProfileManager injection
- `plugins/sync/.../NSClientAddUpdateWorker.kt` - Added LocalProfileManager injection
- `plugins/sync/nsclientV3/extensions/ProfileSwitchExtension.kt` - Changed function param
- `plugins/sync/nsclient/extensions/ProfileSwitchExtension.kt` - Changed function param

#### UI:
- `ui/.../WizardDialog.kt` - Added LocalProfileManager injection
- `ui/.../ProfileSwitchDialog.kt` - Added LocalProfileManager injection
- `ui/.../ProfileSwitchScreen.kt` - Changed function param
- `ui/.../TreatmentsScreen.kt` - Updated call site
- `ui/.../TreatmentsViewModel.kt` - Added LocalProfileManager
- `ui/.../ProfileManagementViewModel.kt` - Updated usages
- `ui/.../ProfileEditorViewModel.kt` - Updated usages
- `ui/.../ProfileHelperViewModel.kt` - Added LocalProfileManager injection
- `ui/.../ProfileManagementScreen.kt` - Updated import

## Intentionally Unchanged (Must Keep activePlugin.activeProfileSource)

1. **ProfileFunctionImpl.kt**
   - Reason: Works with ANY profile source, not specifically local
   - Also: Adding LocalProfileManager here creates circular DI dependency

2. **SWDefinition.kt**
   - Reason: Casts to `PluginBase` for plugin UI/fragment functionality
   - Lines: 336, 341

3. **MainActivity.kt**
   - Reason: Uses `javaClass.simpleName` for Fabric analytics logging
   - Line: 414

4. **Test files** (may need separate update):
   - SmsCommunicatorPluginTest.kt
   - ActionsTestBase.kt
   - LoopTest.kt
   - CompatDbHelperTest.kt
   - ProfileSwitchExtensionKtTest.kt

## Key Pattern
Before:
```kotlin
val profile = activePlugin.activeProfileSource.profile
activePlugin.activeProfileSource.addProfile(...)
activePlugin.activeProfileSource.copyFrom(...)
```

After:
```kotlin
val profile = localProfileManager.profile
localProfileManager.addProfile(...)
localProfileManager.copyFrom(...)
```

## Circular Dependency Note
ProfileFunctionImpl cannot inject LocalProfileManager because:
- LocalProfileManagerImpl already injects ProfileFunction
- This would create: ProfileFunction → LocalProfileManager → ProfileFunction cycle
- ProfileFunctionImpl correctly uses `activePlugin.activeProfileSource` abstraction
