package com.example.trainingdashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trainingdashboard.ui.components.CompletionBanner
import com.example.trainingdashboard.ui.components.DayHeader
import com.example.trainingdashboard.ui.components.ExerciseCard
import com.example.trainingdashboard.ui.components.PermissionBanner
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticBackground
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainer
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainerHigh
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant
import com.example.trainingdashboard.viewmodel.DashboardViewModel
import com.example.trainingdashboard.viewmodel.ExerciseState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel = viewModel(factory = DashboardViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var selectedExercise by remember { mutableStateOf<ExerciseState?>(null) }

    // If an exercise is selected, show the LogRepsScreen
    val currentExercise = selectedExercise
    if (currentExercise != null) {
        // Re-read the latest state for this exercise
        val latestExercise = state.exercises.find { it.name == currentExercise.name } ?: currentExercise
        LogRepsScreen(
            exercise = latestExercise,
            onUpdateCount = { count ->
                viewModel.updateExerciseCount(latestExercise.name, count)
            },
            onDone = { selectedExercise = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KineticBackground)
    ) {
        if (!state.isLoading) {
            val totalTarget = state.exercises.sumOf { it.targetCount }
            val totalCompleted = state.exercises.sumOf { it.completedCount }
            val progressPercent = if (totalTarget > 0) {
                (totalCompleted * 100 / totalTarget).coerceIn(0, 100)
            } else 0

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: "KINETIC" + settings gear
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KINETIC",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KineticGreen
                    )
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                PermissionBanner()

                DayHeader(
                    dayNumber = state.dayNumber,
                    goalLevel = state.goalLevel,
                    progressPercent = progressPercent
                )

                // TODAY'S GOALS label
                Text(
                    text = "TODAY'S GOALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                state.exercises.forEach { exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        onToggle = { viewModel.toggleExercise(exercise.name) },
                        onLogReps = { selectedExercise = exercise }
                    )
                }

                CompletionBanner()
            }
        }
    }

    if (showSettings) {
        SettingsBottomSheet(
            state = state,
            onDismiss = { showSettings = false },
            onSave = { goalLevelText, morningH, morningM, afternoonH, afternoonM, eveningH, eveningM, adaptiveEnabled ->
                val newLevel = goalLevelText.toIntOrNull()
                if (newLevel != null && newLevel >= 1) {
                    viewModel.setGoalLevel(newLevel)
                }
                viewModel.updateReminderTime(morningH, morningM)
                viewModel.updateAfternoonNudgeTime(afternoonH, afternoonM)
                viewModel.updateEveningInterruptTime(eveningH, eveningM)
                viewModel.setAdaptiveTimingEnabled(adaptiveEnabled)

                showSettings = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    state: com.example.trainingdashboard.viewmodel.DashboardUiState,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int, Int, Int, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var adaptiveEnabled by remember { mutableStateOf(state.adaptiveTimingEnabled) }

    var morningHour by remember { mutableStateOf(state.reminderHour) }
    var morningMinute by remember { mutableStateOf(state.reminderMinute) }
    var afternoonHour by remember { mutableStateOf(state.afternoonNudgeHour) }
    var afternoonMinute by remember { mutableStateOf(state.afternoonNudgeMinute) }
    var eveningHour by remember { mutableStateOf(state.eveningInterruptHour) }
    var eveningMinute by remember { mutableStateOf(state.eveningInterruptMinute) }

    var editingTimePicker by remember { mutableStateOf<String?>(null) }
    var showDayDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KineticBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(KineticOnSurfaceVariant, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: "SETTINGS" with neon underline bar
            Text(
                text = "SETTINGS",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 36.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(4.dp)
                    .background(KineticGreen, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Morning Reminder row
            TimeSettingRow(
                icon = Icons.Default.WbSunny,
                label = "Morning Reminder",
                hour = morningHour,
                minute = morningMinute,
                onClick = { editingTimePicker = "morning" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Afternoon Nudge row
            TimeSettingRow(
                icon = Icons.Default.LightMode,
                label = "Afternoon Nudge",
                hour = afternoonHour,
                minute = afternoonMinute,
                onClick = { editingTimePicker = "afternoon" }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Evening Interrupt row
            TimeSettingRow(
                icon = Icons.Default.Bedtime,
                label = "Evening Interrupt",
                hour = eveningHour,
                minute = eveningMinute,
                onClick = { editingTimePicker = "evening" }
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Adaptive Timing toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ADAPTIVE TIMING",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Auto-adjust morning reminder based on when you train",
                        style = MaterialTheme.typography.bodySmall,
                        color = KineticOnSurfaceVariant
                    )
                }
                Switch(
                    checked = adaptiveEnabled,
                    onCheckedChange = { adaptiveEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = KineticGreen,
                        checkedThumbColor = KineticBackground
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVE CHANGES button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KineticGreen, RoundedCornerShape(12.dp))
                    .clickable {
                        onSave(
                            "",
                            morningHour, morningMinute,
                            afternoonHour, afternoonMinute,
                            eveningHour, eveningMinute,
                            adaptiveEnabled
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SAVE CHANGES",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 18.sp
                        ),
                        color = KineticBackground
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = KineticBackground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // CANCEL button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KineticSurfaceContainerHigh
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "CANCEL",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "Set Goal Level" ghost link
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SET GOAL LEVEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.sp
                    ),
                    color = KineticOnSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.clickable { showDayDialog = true }
                )
            }
        }
    }

    // Time picker dialog
    editingTimePicker?.let { which ->
        val initialH = when (which) { "morning" -> morningHour; "afternoon" -> afternoonHour; else -> eveningHour }
        val initialM = when (which) { "morning" -> morningMinute; "afternoon" -> afternoonMinute; else -> eveningMinute }
        val pickerState = rememberTimePickerState(initialHour = initialH, initialMinute = initialM)
        val title = when (which) { "morning" -> "Morning Reminder"; "afternoon" -> "Afternoon Nudge"; else -> "Evening Interrupt" }

        AlertDialog(
            onDismissRequest = { editingTimePicker = null },
            containerColor = KineticSurfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            title = {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        letterSpacing = 1.sp
                    )
                )
            },
            text = {
                TimePicker(
                    state = pickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = KineticBackground,
                        clockDialSelectedContentColor = KineticBackground,
                        clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        selectorColor = KineticGreen,
                        containerColor = KineticSurfaceContainer,
                        periodSelectorBorderColor = KineticGreen.copy(alpha = 0.3f),
                        periodSelectorSelectedContainerColor = KineticGreen,
                        periodSelectorUnselectedContainerColor = KineticBackground,
                        periodSelectorSelectedContentColor = KineticBackground,
                        periodSelectorUnselectedContentColor = KineticOnSurfaceVariant,
                        timeSelectorSelectedContainerColor = KineticGreen,
                        timeSelectorUnselectedContainerColor = KineticBackground,
                        timeSelectorSelectedContentColor = KineticBackground,
                        timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                    )
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(KineticBackground, RoundedCornerShape(8.dp))
                            .clickable { editingTimePicker = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontStyle = FontStyle.Italic,
                                fontSize = 18.sp
                            ),
                            color = KineticOnSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(KineticGreen, RoundedCornerShape(8.dp))
                            .clickable {
                                when (which) {
                                    "morning" -> { morningHour = pickerState.hour; morningMinute = pickerState.minute }
                                    "afternoon" -> { afternoonHour = pickerState.hour; afternoonMinute = pickerState.minute }
                                    "evening" -> { eveningHour = pickerState.hour; eveningMinute = pickerState.minute }
                                }
                                editingTimePicker = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SET",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 18.sp
                                ),
                                color = KineticBackground
                            )
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = KineticBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            },
            dismissButton = null
        )
    }

    // Set Goal Level dialog
    if (showDayDialog) {
        var goalLevelText by remember { mutableStateOf(state.goalLevel.toString()) }
        AlertDialog(
            onDismissRequest = { showDayDialog = false },
            title = { Text("SET GOAL LEVEL") },
            text = {
                OutlinedTextField(
                    value = goalLevelText,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            goalLevelText = value
                        }
                    },
                    label = { Text("Goal level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(
                        goalLevelText,
                        morningHour, morningMinute,
                        afternoonHour, afternoonMinute,
                        eveningHour, eveningMinute,
                        adaptiveEnabled
                    )
                    showDayDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showDayDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TimeSettingRow(
    icon: ImageVector,
    label: String,
    hour: Int,
    minute: Int,
    onClick: () -> Unit
) {
    val amPm = if (hour < 12) "AM" else "PM"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val timeText = String.format("%02d:%02d %s", displayHour, minute, amPm)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Label with icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = KineticOnSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp
                ),
                color = KineticOnSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        // Time value field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                .clickable { onClick() }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = KineticOnSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
