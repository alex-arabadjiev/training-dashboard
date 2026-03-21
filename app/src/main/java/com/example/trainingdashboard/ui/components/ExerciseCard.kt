package com.example.trainingdashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.trainingdashboard.ui.theme.Green40
import com.example.trainingdashboard.ui.theme.Green90
import com.example.trainingdashboard.viewmodel.ExerciseState

@Composable
fun ExerciseCard(
    exercise: ExerciseState,
    onToggle: () -> Unit,
    onUpdateCount: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCountDialog by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = if (exercise.isCompleted) Green90
        else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )

    val progress = if (exercise.targetCount > 0) {
        (exercise.completedCount.toFloat() / exercise.targetCount).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${exercise.completedCount} / ${exercise.targetCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (exercise.isCompleted) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = Green40,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            IconButton(onClick = { showCountDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Log progress",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (exercise.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Outlined.Circle,
                    contentDescription = if (exercise.isCompleted) "Mark incomplete"
                    else "Mark complete",
                    tint = if (exercise.isCompleted) Green40
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
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
