package com.example.trainingdashboard.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.trainingdashboard.ui.theme.Green40
import com.example.trainingdashboard.ui.theme.Green90
import com.example.trainingdashboard.viewmodel.ExerciseState

@Composable
fun ExerciseCard(
    exercise: ExerciseState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (exercise.isCompleted) Green90
        else MaterialTheme.colorScheme.surface,
        label = "cardBg"
    )

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
                    text = "Target: ${exercise.targetCount}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = if (exercise.isCompleted) TextDecoration.LineThrough else null
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
}
