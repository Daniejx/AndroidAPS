package app.aaps.core.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.aaps.core.keys.R as KeysR
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * Composable that displays a numeric input with label, current value, and Material 3 slider.
 * Used for inputting numeric parameters with optional unit display and formatting.
 *
 * **Layout:**
 * ```
 * Label                    2h 10m
 * ──────○────────────────
 * ```
 *
 * The component displays:
 * - Top row: Label (left, labelLarge) and current value with unit (right, titleMedium bold in primary color, clickable)
 * - Bottom: Material 3 Slider with +/- buttons
 * - Clicking on value opens a dialog for direct input
 * - Special formatting for minutes: when unitLabelResId is units_min and value >= 60, displays as "Xh Ym"
 *
 * @param label Display label for the input (e.g., "Duration", "Percentage")
 * @param value Current numeric value
 * @param onValueChange Callback invoked when slider value changes, receives new value as Double
 * @param minValue Minimum allowed value for the slider range
 * @param maxValue Maximum allowed value for the slider range
 * @param step Step increment for slider (determines number of discrete positions)
 * @param unitLabelResId Resource ID for unit label (e.g., R.string.units_min, R.string.units_percent)
 * @param decimalPlaces Number of decimal places for value display (0 = integer, default)
 * @param modifier Modifier for the root Column container
 */
@Composable
fun NumberInputRow(
    label: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    minValue: Double,
    maxValue: Double,
    step: Double,
    unitLabelResId: Int = 0,
    decimalPlaces: Int = 0,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val valueFormat = remember(decimalPlaces) {
        if (decimalPlaces == 0) DecimalFormat("0")
        else DecimalFormat("0.${"0".repeat(decimalPlaces)}")
    }

    // Resolve unit label string
    val unitLabel = if (unitLabelResId != 0) stringResource(unitLabelResId) else ""

    // Check if this is minutes input for special formatting
    val isMinutesUnit = unitLabelResId == KeysR.string.units_min
    val intValue = value.roundToInt()

    // Format the displayed value
    val displayText = when {
        // Special formatting for minutes >= 60 as "Xh Ym"
        isMinutesUnit && intValue >= 60 -> {
            val hours = intValue / 60
            val mins = intValue % 60
            stringResource(app.aaps.core.ui.R.string.format_hour_minute, hours, mins)
        }
        unitLabel.isNotEmpty() -> "${valueFormat.format(value)} $unitLabel"
        else -> valueFormat.format(value)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDialog = true }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        SliderWithButtons(
            value = value,
            onValueChange = onValueChange,
            valueRange = minValue..maxValue,
            step = step,
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showDialog) {
        ValueInputDialog(
            currentValue = value,
            valueRange = minValue..maxValue,
            step = step,
            label = label,
            unitLabel = unitLabel,
            valueFormat = valueFormat,
            onValueConfirm = onValueChange,
            onDismiss = { showDialog = false }
        )
    }
}
