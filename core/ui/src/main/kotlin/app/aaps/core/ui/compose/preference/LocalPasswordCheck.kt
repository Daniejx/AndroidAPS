package app.aaps.core.ui.compose.preference

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf
import app.aaps.core.interfaces.protection.PasswordCheck
import app.aaps.core.keys.interfaces.PreferenceVisibilityContext

/**
 * CompositionLocal for providing PreferenceVisibilityContext to preference composables.
 * Used by AdaptivePreferenceList and PreferenceContentExtensions to evaluate visibility conditions.
 */
val LocalVisibilityContext = compositionLocalOf<PreferenceVisibilityContext?> { null }

/**
 * CompositionLocal for password verification function.
 * Used by password preference dialogs to verify entered passwords.
 * Signature: (enteredPassword: String, storedHash: String) -> Boolean
 */
val LocalCheckPassword = compositionLocalOf<((String, String) -> Boolean)?> { null }

/**
 * CompositionLocal for password hashing function.
 * Used by password preference dialogs to hash passwords before storing.
 * Signature: (password: String) -> String
 */
val LocalHashPassword = compositionLocalOf<((String) -> String)?> { null }

/**
 * CompositionLocal for SnackbarHostState.
 * Used by preference dialogs to show feedback messages.
 */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState?> { null }
