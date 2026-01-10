package app.aaps.core.ui.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Color scheme for general UI elements across the app.
 * Provides consistent color coding for common elements like IOB, COB, etc.
 *
 * **Usage:**
 * - Treatment screens
 * - Overview screen
 * - General UI elements
 *
 * **Color Assignment:**
 * - activeInsulinText: Blue - Active Insulin On Board (IOB) text color
 * - calculator: Green - Bolus calculator icon and related elements
 * - future: Green - Future/scheduled items color
 * - invalidated: Red - Invalid/deleted items color
 * - inProgress: Amber - Items with temporary modifications (e.g., profile with percentage/timeshift)
 * - onInProgress: Dark text color for use on inProgress background
 *
 * Colors match the existing theme attribute colors for consistency with the rest of the app.
 *
 * @property activeInsulinText Color for active insulin (IOB) text
 * @property calculator Color for calculator icon and elements
 * @property futureRecord Color for future/scheduled items
 * @property invalidatedRecord Color for invalid/deleted items
 * @property inProgress Color for items with temporary modifications (matches ribbonWarningColor)
 * @property onInProgress Text color for use on inProgress background (matches ribbonTextWarningColor)
 */
data class GeneralColors(
    val activeInsulinText: Color,
    val calculator: Color,
    val futureRecord: Color,
    val invalidatedRecord: Color,
    val statusNormal: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val inProgress: Color,
    val onInProgress: Color
)

/**
 * Light mode color scheme for general elements.
 * Colors match the light theme values from colors.xml.
 */
internal val LightGeneralColors = GeneralColors(
    activeInsulinText = Color(0xFF1E88E5),  // iob color
    calculator = Color(0xFF66BB6A),          // colorCalculatorButton
    futureRecord = Color(0xFF66BB6A),        // green for scheduled/future items
    invalidatedRecord = Color(0xFFE53935),   // red for invalid/deleted items
    statusNormal = Color(0xFF4CAF50),        // green for normal status
    statusWarning = Color(0xFFFFC107),       // amber for warning status
    statusCritical = Color(0xFFF44336),      // red for critical status
    inProgress = Color(0xFFF4D700),          // ribbonWarning - amber for modified/temporary items
    onInProgress = Color(0xFF303030)         // ribbonTextWarning - dark text on amber background
)

/**
 * Dark mode color scheme for general elements.
 * Colors match the dark theme values from colors.xml (night folder).
 */
internal val DarkGeneralColors = GeneralColors(
    activeInsulinText = Color(0xFF1E88E5),  // iob color (same in both modes)
    calculator = Color(0xFF67E86A),          // colorCalculatorButton (night)
    futureRecord = Color(0xFF6AE86D),        // green for scheduled/future items (night)
    invalidatedRecord = Color(0xFFEF5350),   // red for invalid/deleted items (night)
    statusNormal = Color(0xFF81C784),        // lighter green for dark mode
    statusWarning = Color(0xFFFFD54F),       // lighter amber for dark mode
    statusCritical = Color(0xFFE57373),      // lighter red for dark mode
    inProgress = Color(0xFFF4D700),          // ribbonWarning - same in both modes
    onInProgress = Color(0xFF303030)         // ribbonTextWarning - same in both modes
)

/**
 * CompositionLocal providing general colors based on current theme (light/dark).
 * Accessed via AapsTheme.generalColors in composables.
 */
internal val LocalGeneralColors = compositionLocalOf { LightGeneralColors }
