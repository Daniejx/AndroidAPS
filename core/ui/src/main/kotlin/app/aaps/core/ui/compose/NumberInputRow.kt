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
import java.text.DecimalFormat
import kotlin.math.roundToInt

/**
 * Composable that displays a numeric input with label, current value, and Material 3 slider.
 * Used for inputting numeric parameters with optional unit display and formatting.
 *
 * **Layout:**
 * ```
 * Label                    42 min
 * Optional summary text
 * ──────○────────────────
 * ```
 *
 * The component displays:
 * - Top row: Label (left, labelLarge) and current value with unit (right, titleMedium bold in primary color, clickable)
 * - Optional: Summary text below label (bodySmall, onSurfaceVariant)
 * - Bottom: Material 3 Slider with +/- buttons
 * - Clicking on value opens a dialog for direct input
 *
 * @param label Display label for the input (e.g., "Duration", "Percentage")
 * @param value Current numeric value
 * @param onValueChange Callback invoked when slider value changes, receives new value as Double
 * @param minValue Minimum allowed value for the slider range
 * @param maxValue Maximum allowed value for the slider range
 * @param step Step increment for slider (determines number of discrete positions)
 * @param unitLabel Optional unit label displayed after value (e.g., "min", "%", "h")
 * @param decimalPlaces Number of decimal places for value display (0 = integer, default)
 * @param summary Optional summary/description text shown below the label
 * @param valueFormatResId Optional resource ID for formatted value display (e.g., R.string.format_mins for "%1$d min")
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
    unitLabel: String = "",
    decimalPlaces: Int = 0,
    summary: String? = null,
    valueFormatResId: Int? = null,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    val valueFormat = remember(decimalPlaces) {
        if (decimalPlaces == 0) DecimalFormat("0")
        else DecimalFormat("0.${"0".repeat(decimalPlaces)}")
    }

    // Format the displayed value
    val displayText = when {
        valueFormatResId != null -> {
            if (decimalPlaces == 0) {
                stringResource(valueFormatResId, value.roundToInt())
            } else {
                stringResource(valueFormatResId, value)
            }
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
        if (summary != null) {
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
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
            summary = summary,
            unitLabel = unitLabel,
            valueFormat = valueFormat,
            onValueConfirm = onValueChange,
            onDismiss = { showDialog = false }
        )
    }
}
