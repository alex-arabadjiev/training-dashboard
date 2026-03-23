package com.example.trainingdashboard.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticBackground
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainer
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant
import com.example.trainingdashboard.viewmodel.ExerciseState

private fun exerciseIcon(name: String): ImageVector = when {
    name.contains("push", ignoreCase = true) -> Icons.Default.FitnessCenter
    name.contains("sit", ignoreCase = true) -> Icons.Default.SelfImprovement
    name.contains("squat", ignoreCase = true) -> Icons.Default.DirectionsRun
    else -> Icons.Default.FitnessCenter
}

@Composable
fun ExerciseCard(
    exercise: ExerciseState,
    onToggle: () -> Unit,
    onUpdateCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCountDialog by remember { mutableStateOf(false) }

    val progress = if (exercise.targetCount > 0) {
        (exercise.completedCount.toFloat() / exercise.targetCount).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KineticSurfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCountDialog = true }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KineticBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = exerciseIcon(exercise.name),
                    contentDescription = null,
                    tint = KineticGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Exercise info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${exercise.completedCount}/${exercise.targetCount} REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KineticGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = KineticGreen,
                    trackColor = KineticBackground,
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Checkbox square
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (exercise.isCompleted) KineticGreen else Color.Transparent)
                    .then(
                        if (!exercise.isCompleted) Modifier.background(
                            color = Color.Transparent
                        ) else Modifier
                    )
                    .clickable { onToggle() }
                    .let { mod ->
                        if (!exercise.isCompleted) {
                            mod.background(
                                color = Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                        } else mod
                    },
                contentAlignment = Alignment.Center
            ) {
                if (exercise.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = KineticBackground,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KineticSurfaceContainer)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(KineticBackground)
                    )
                }
            }
        }
    }

    if (showCountDialog) {
        CountInputDialog(
            exerciseName = exercise.name,
            currentCount = exercise.completedCount,
            targetCount = exercise.targetCount,
            onConfirm = { count ->
                onUpdateCount(count)
                showCountDialog = false
            },
            onDismiss = { showCountDialog = false }
        )
    }
}

@Composable
private fun CountInputDialog(
    exerciseName: String,
    currentCount: Int,
    targetCount: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialText = currentCount.toString()
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(initialText, selection = TextRange(0, initialText.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log $exerciseName") },
        text = {
            Column {
                Text(
                    text = "Target: $targetCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = { value ->
                        if (value.text.isEmpty() || value.text.all { it.isDigit() }) {
                            textFieldValue = value
                        }
                    },
                    label = { Text("Completed") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val count = textFieldValue.text.toIntOrNull() ?: currentCount
                onConfirm(count)
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
}
