package app.aaps.core.ui.compose

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aaps.core.ui.R

/**
 * Sealed class representing different types of snackbar messages.
 * Each type has appropriate styling (color, icon) applied automatically.
 *
 * @property message The text message to display
 */
sealed class SnackbarMessage(open val message: String) {

    data class Error(override val message: String) : SnackbarMessage(message)
    data class Warning(override val message: String) : SnackbarMessage(message)
    data class Info(override val message: String) : SnackbarMessage(message)
    data class Success(override val message: String) : SnackbarMessage(message)
}

/**
 * Color scheme for snackbar messages.
 * Provides consistent colors for different message types.
 *
 * @property errorContainer Background color for error snackbars
 * @property onErrorContainer Text/icon color for error snackbars
 * @property warningContainer Background color for warning snackbars
 * @property onWarningContainer Text/icon color for warning snackbars
 * @property infoContainer Background color for info snackbars
 * @property onInfoContainer Text/icon color for info snackbars
 * @property successContainer Background color for success snackbars
 * @property onSuccessContainer Text/icon color for success snackbars
 */
data class SnackbarColors(
    val errorContainer: Color,
    val onErrorContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
    val successContainer: Color,
    val onSuccessContainer: Color
)

/**
 * Light mode snackbar colors.
 */
internal val LightSnackbarColors = SnackbarColors(
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    warningContainer = Color(0xFFFFE082),
    onWarningContainer = Color(0xFF3E2723),
    infoContainer = Color(0xFFBBDEFB),
    onInfoContainer = Color(0xFF0D47A1),
    successContainer = Color(0xFFC8E6C9),
    onSuccessContainer = Color(0xFF1B5E20)
)

/**
 * Dark mode snackbar colors.
 */
internal val DarkSnackbarColors = SnackbarColors(
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    warningContainer = Color(0xFF5D4037),
    onWarningContainer = Color(0xFFFFE082),
    infoContainer = Color(0xFF1565C0),
    onInfoContainer = Color(0xFFBBDEFB),
    successContainer = Color(0xFF2E7D32),
    onSuccessContainer = Color(0xFFC8E6C9)
)

/**
 * CompositionLocal providing snackbar colors based on current theme.
 */
internal val LocalSnackbarColors = compositionLocalOf { LightSnackbarColors }

/**
 * Snackbar host that displays typed messages with appropriate styling.
 * Supports Error, Warning, Info, and Success message types with distinct colors and icons.
 *
 * **Usage:**
 * ```kotlin
 * // In ViewModel
 * data class UiState(
 *     val snackbarMessage: SnackbarMessage? = null
 * )
 *
 * fun showError(msg: String) {
 *     _uiState.update { it.copy(snackbarMessage = SnackbarMessage.Error(msg)) }
 * }
 *
 * // In Composable
 * AapsSnackbarHost(
 *     message = uiState.snackbarMessage,
 *     onDismiss = { viewModel.clearMessage() },
 *     modifier = Modifier.align(Alignment.BottomCenter)
 * )
 * ```
 *
 * @param message The message to display, or null if no message
 * @param onDismiss Callback when message is dismissed
 * @param modifier Modifier for the snackbar host
 */
@Composable
fun AapsSnackbarHost(
    message: SnackbarMessage?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = AapsTheme.snackbarColors

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(
                message = it.message,
                withDismissAction = true
            )
            onDismiss()
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier
    ) { snackbarData ->
        val (containerColor, contentColor, icon) = when (message) {
            is SnackbarMessage.Error   -> Triple(colors.errorContainer, colors.onErrorContainer, Icons.Default.Error)
            is SnackbarMessage.Warning -> Triple(colors.warningContainer, colors.onWarningContainer, Icons.Default.Warning)
            is SnackbarMessage.Info    -> Triple(colors.infoContainer, colors.onInfoContainer, Icons.Default.Info)
            is SnackbarMessage.Success -> Triple(colors.successContainer, colors.onSuccessContainer, Icons.Default.CheckCircle)
            null                       -> Triple(MaterialTheme.colorScheme.inverseSurface, MaterialTheme.colorScheme.inverseOnSurface, Icons.Default.Info)
        }

        Snackbar(
            containerColor = containerColor,
            contentColor = contentColor,
            dismissAction = {
                TextButton(onClick = { snackbarHostState.currentSnackbarData?.dismiss() }) {
                    Text(
                        text = stringResource(R.string.dismiss),
                        color = contentColor
                    )
                }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = snackbarData.visuals.message,
                    color = contentColor
                )
            }
        }
    }
}
