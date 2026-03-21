package com.example.trainingdashboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trainingdashboard.notification.ReminderScheduler
import com.example.trainingdashboard.ui.components.CompletionBanner
import com.example.trainingdashboard.ui.components.DayHeader
import com.example.trainingdashboard.ui.components.ExerciseCard
import com.example.trainingdashboard.ui.components.PermissionBanner
import com.example.trainingdashboard.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Training Dashboard") },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Reminder settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionBanner()

                DayHeader(dayNumber = state.dayNumber)

                state.exercises.forEach { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onToggle = { viewModel.toggleExercise(exercise.name) },
                        onUpdateCount = { count ->
                            viewModel.updateExerciseCount(exercise.name, count)
                        }
                    )
                }

                CompletionBanner(
                    visible = state.allCompleted,
                    dayNumber = state.dayNumber,
                    streak = state.streak
                )
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            state = state,
            onDismiss = { showSettings = false },
            onSave = { dayText, morningH, morningM, afternoonH, afternoonM, eveningH, eveningM, adaptiveEnabled ->
                val newDay = dayText.toIntOrNull()
                if (newDay != null && newDay >= 1) {
                    viewModel.setCurrentDay(newDay)
                }
                viewModel.updateReminderTime(morningH, morningM)
                viewModel.updateAfternoonNudgeTime(afternoonH, afternoonM)
                viewModel.updateEveningInterruptTime(eveningH, eveningM)
                viewModel.setAdaptiveTimingEnabled(adaptiveEnabled)

                ReminderScheduler.schedule(context, morningH, morningM)
                ReminderScheduler.scheduleAfternoonNudge(context, afternoonH, afternoonM)
                ReminderScheduler.scheduleEveningInterrupt(context, eveningH, eveningM)
                if (adaptiveEnabled) {
                    ReminderScheduler.scheduleAdaptiveTiming(context)
                } else {
                    ReminderScheduler.cancelAdaptiveTiming(context)
                }

                showSettings = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsDialog(
    state: com.example.trainingdashboard.viewmodel.DashboardUiState,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit
) {
    var dayText by remember { mutableStateOf(state.dayNumber.toString()) }
    var adaptiveEnabled by remember { mutableStateOf(state.adaptiveTimingEnabled) }

    var morningHour by remember { mutableStateOf(state.reminderHour) }
    var morningMinute by remember { mutableStateOf(state.reminderMinute) }
    var afternoonHour by remember { mutableStateOf(state.afternoonNudgeHour) }
    var afternoonMinute by remember { mutableStateOf(state.afternoonNudgeMinute) }
    var eveningHour by remember { mutableStateOf(state.eveningInterruptHour) }
    var eveningMinute by remember { mutableStateOf(state.eveningInterruptMinute) }

    var editingTimePicker by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Current Day
                Text(text = "Current Day", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dayText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            dayText = value
                        }
                    },
                    label = { Text("Day number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Morning Reminder
                TimeSettingRow(
                    label = "Morning Reminder",
                    subtitle = if (adaptiveEnabled) "Auto-adjusted by adaptive timing" else null,
                    hour = morningHour,
                    minute = morningMinute,
                    onClick = { editingTimePicker = "morning" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Afternoon Nudge
                TimeSettingRow(
                    label = "Afternoon Nudge",
                    hour = afternoonHour,
                    minute = afternoonMinute,
                    onClick = { editingTimePicker = "afternoon" }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Evening Interrupt
                TimeSettingRow(
                    label = "Evening Interrupt",
                    hour = eveningHour,
                    minute = eveningMinute,
                    onClick = { editingTimePicker = "evening" }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Adaptive Timing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Adaptive Timing", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "Auto-adjust morning reminder based on when you train",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = adaptiveEnabled,
                        onCheckedChange = { adaptiveEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    dayText,
                    morningHour, morningMinute,
                    afternoonHour, afternoonMinute,
                    eveningHour, eveningMinute,
                    adaptiveEnabled
                )
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Time picker dialog — shown when any time row is tapped
    editingTimePicker?.let { which ->
        val initialH = when (which) { "morning" -> morningHour; "afternoon" -> afternoonHour; else -> eveningHour }
        val initialM = when (which) { "morning" -> morningMinute; "afternoon" -> afternoonMinute; else -> eveningMinute }
        val pickerState = rememberTimePickerState(initialHour = initialH, initialMinute = initialM)
        val title = when (which) { "morning" -> "Morning Reminder"; "afternoon" -> "Afternoon Nudge"; else -> "Evening Interrupt" }

        AlertDialog(
            onDismissRequest = { editingTimePicker = null },
            title = { Text(title) },
            text = { TimePicker(state = pickerState) },
            confirmButton = {
                TextButton(onClick = {
                    when (which) {
                        "morning" -> { morningHour = pickerState.hour; morningMinute = pickerState.minute }
                        "afternoon" -> { afternoonHour = pickerState.hour; afternoonMinute = pickerState.minute }
                        "evening" -> { eveningHour = pickerState.hour; eveningMinute = pickerState.minute }
                    }
                    editingTimePicker = null
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { editingTimePicker = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TimeSettingRow(
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val timeText = String.format("%02d:%02d", hour, minute)

    OutlinedTextField(
        value = timeText,
        onValueChange = {},
        label = { Text(label) },
        supportingText = subtitle?.let { { Text(it) } },
        readOnly = true,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        interactionSource = remember { MutableInteractionSource() }
            .also { interactionSource ->
                LaunchedEffect(interactionSource) {
                    interactionSource.interactions.collect { interaction ->
                        if (interaction is PressInteraction.Release) {
                            onClick()
                        }
                    }
                }
            }
    )
}
