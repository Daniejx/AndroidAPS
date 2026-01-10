package app.aaps.ui.compose.profileManagement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aaps.core.data.configuration.Constants
import app.aaps.core.interfaces.resources.ResourceHelper
import app.aaps.core.ui.compose.NumberInputRow
import app.aaps.core.ui.compose.OkCancelDialog
import app.aaps.ui.R

/**
 * Full screen for activating a profile with optional percentage, timeshift, and duration.
 *
 * @param profileName Name of the profile to activate
 * @param currentPercentage Current active percentage (for reuse button)
 * @param currentTimeshiftHours Current active timeshift in hours (for reuse button)
 * @param hasReuseValues Whether reuse button should be shown
 * @param showNotesField Whether to show the notes input field (based on BooleanKey.OverviewShowNotesInDialogs)
 * @param rh ResourceHelper for string resources
 * @param onNavigateBack Callback to navigate back
 * @param onActivate Callback when profile is activated with (duration, percentage, timeshift, withTT, notes)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileActivationScreen(
    profileName: String,
    currentPercentage: Int = 100,
    currentTimeshiftHours: Int = 0,
    hasReuseValues: Boolean = false,
    showNotesField: Boolean = true,
    rh: ResourceHelper,
    onNavigateBack: () -> Unit,
    onActivate: (durationMinutes: Int, percentage: Int, timeshiftHours: Int, withTT: Boolean, notes: String) -> Unit
) {
    var duration by remember { mutableDoubleStateOf(0.0) }
    var percentage by remember { mutableDoubleStateOf(100.0) }
    var timeshift by remember { mutableDoubleStateOf(0.0) }
    var withTT by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    // TT option only visible when duration > 0 and percentage < 100
    val showTTOption = duration > 0 && percentage < 100

    // Build confirmation message
    val confirmationMessage = buildString {
        append(rh.gs(app.aaps.core.ui.R.string.profile))
        append(": ")
        append(profileName)
        if (duration > 0) {
            append("<br/>")
            append(rh.gs(app.aaps.core.ui.R.string.duration))
            append(": ")
            append(rh.gs(app.aaps.core.ui.R.string.format_mins, duration.toInt()))
        }
        if (percentage.toInt() != 100) {
            append("<br/>")
            append(rh.gs(app.aaps.core.ui.R.string.percent))
            append(": ")
            append("${percentage.toInt()}%")
        }
        if (timeshift.toInt() != 0) {
            append("<br/>")
            append(rh.gs(R.string.timeshift_label))
            append(": ")
            append(rh.gs(app.aaps.core.ui.R.string.format_hours, timeshift))
        }
        if (showTTOption && withTT) {
            append("<br/>")
            append(rh.gs(app.aaps.core.ui.R.string.temporary_target))
            append(": ")
            append(rh.gs(app.aaps.core.ui.R.string.activity))
        }
        if (notes.isNotBlank()) {
            append("<br/>")
            append(rh.gs(app.aaps.core.ui.R.string.notes_label))
            append(": ")
            append(notes)
        }
    }

    // Confirmation dialog
    if (showConfirmDialog) {
        OkCancelDialog(
            title = rh.gs(app.aaps.core.ui.R.string.careportal_profileswitch),
            message = confirmationMessage,
            onConfirm = {
                showConfirmDialog = false
                onActivate(
                    duration.toInt(),
                    percentage.toInt(),
                    timeshift.toInt(),
                    showTTOption && withTT,
                    notes
                )
            },
            onDismiss = { showConfirmDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.activate_label))
                        Text(
                            text = profileName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(app.aaps.core.ui.R.string.back)
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { showConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.activate_label))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Duration input
            NumberInputRow(
                label = stringResource(app.aaps.core.ui.R.string.duration),
                value = duration,
                onValueChange = { duration = it },
                minValue = 0.0,
                maxValue = Constants.MAX_PROFILE_SWITCH_DURATION,
                step = 10.0,
                unitLabel = stringResource(app.aaps.core.keys.R.string.units_min),
                summary = if (duration == 0.0) stringResource(R.string.permanent_profile_switch) else null
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Percentage input
            NumberInputRow(
                label = stringResource(R.string.percentage_label),
                value = percentage,
                onValueChange = { percentage = it },
                minValue = Constants.CPP_MIN_PERCENTAGE.toDouble(),
                maxValue = Constants.CPP_MAX_PERCENTAGE.toDouble(),
                step = 5.0,
                unitLabel = stringResource(app.aaps.core.keys.R.string.units_percent)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Timeshift input
            NumberInputRow(
                label = stringResource(R.string.timeshift_label),
                value = timeshift,
                onValueChange = { timeshift = it },
                minValue = Constants.CPP_MIN_TIMESHIFT.toDouble(),
                maxValue = Constants.CPP_MAX_TIMESHIFT.toDouble(),
                step = 1.0,
                unitLabel = stringResource(app.aaps.core.keys.R.string.units_hours)
            )

            // Reuse button
            if (hasReuseValues && (currentPercentage != 100 || currentTimeshiftHours != 0)) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = {
                        percentage = currentPercentage.toDouble()
                        timeshift = currentTimeshiftHours.toDouble()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(rh.gs(R.string.reuse_profile_pct_hours, currentPercentage, currentTimeshiftHours))
                }
            }

            // Temporary Target switch (only when duration > 0 and percentage < 100)
            if (showTTOption) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(app.aaps.core.ui.R.string.temporary_target),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = stringResource(app.aaps.core.ui.R.string.activity),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = withTT,
                        onCheckedChange = { withTT = it }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Notes (conditional based on BooleanKey.OverviewShowNotesInDialogs)
            if (showNotesField) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(app.aaps.core.ui.R.string.notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
