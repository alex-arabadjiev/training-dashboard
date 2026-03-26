package com.example.trainingdashboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trainingdashboard.ui.theme.KineticBackground
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainer
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainerHigh
import com.example.trainingdashboard.viewmodel.ExerciseState

@Composable
fun LogRepsScreen(
    exercise: ExerciseState,
    onUpdateCount: (Int) -> Unit,
    onDone: () -> Unit
) {
    var currentCount by remember { mutableIntStateOf(exercise.completedCount) }
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    var accelerometerMode by remember { mutableStateOf(false) }
    val isGoalComplete = currentCount >= exercise.targetCount

    LaunchedEffect(isEditing) {
        if (isEditing) {
            val text = currentCount.toString()
            editText = TextFieldValue(text, selection = TextRange(0, text.length))
            focusRequester.requestFocus()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KineticBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        onUpdateCount(currentCount)
                        onDone()
                    },
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KineticGreen
                    )
                }
                Text(
                    text = "LOG REPS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic
                    ),
                    color = KineticGreen
                )
            }

            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Exercise name with green bottom bar
                Column(
                    modifier = Modifier.width(IntrinsicSize.Max),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = exercise.name.uppercase(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 30.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = Color.White
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(KineticGreen, RoundedCornerShape(2.dp))
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Circular progress ring
                val progress = if (exercise.targetCount > 0) {
                    (currentCount.toFloat() / exercise.targetCount).coerceIn(0f, 1f)
                } else 0f
                val sweepAngle = progress * 360f
                val trackColor = Color.White.copy(alpha = 0.05f)

                Box(
                    modifier = Modifier
                        .size(288.dp)
                        .drawBehind {
                            val strokeWidth = 12.dp.toPx()
                            val arcSize = size.width - strokeWidth
                            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                            // Background track
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(arcSize, arcSize),
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Foreground arc
                            if (sweepAngle > 0f) {
                                drawArc(
                                    color = KineticGreen,
                                    startAngle = -90f,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(arcSize, arcSize),
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isEditing) {
                            CompositionLocalProvider(
                                LocalTextSelectionColors provides TextSelectionColors(
                                    handleColor = Color.Transparent,
                                    backgroundColor = Color.Transparent
                                )
                            ) {
                            BasicTextField(
                                value = editText,
                                onValueChange = { value ->
                                    if (value.text.isEmpty() || value.text.all { it.isDigit() }) editText = value
                                },
                                textStyle = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 96.sp,
                                    lineHeight = 96.sp,
                                    letterSpacing = (-2).sp,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    val count = editText.text.toIntOrNull() ?: currentCount
                                    currentCount = count.coerceIn(0, exercise.targetCount)
                                    isEditing = false
                                }),
                                singleLine = true,
                                modifier = Modifier
                                    .width(200.dp)
                                    .focusRequester(focusRequester),
                                decorationBox = { innerTextField -> innerTextField() }
                            )
                            } // CompositionLocalProvider
                        } else {
                            Text(
                                text = "$currentCount",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 96.sp,
                                    lineHeight = 96.sp,
                                    letterSpacing = (-2).sp
                                ),
                                color = if (isGoalComplete) KineticGreen else Color.White,
                                modifier = Modifier.clickable { isEditing = true }
                            )
                        }
                        // Green accent bar below number
                        Box(
                            modifier = Modifier
                                .width(64.dp)
                                .height(8.dp)
                                .background(KineticGreen, RoundedCornerShape(4.dp))
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "REPS COMPLETED",
                            style = MaterialTheme.typography.labelSmall,
                            color = KineticOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        // GOAL pill
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isGoalComplete) KineticGreen.copy(alpha = 0.15f) else KineticSurfaceContainerHigh,
                                    RoundedCornerShape(50)
                                )
                                .border(
                                    1.dp,
                                    if (isGoalComplete) KineticGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(50)
                                )
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isGoalComplete) "GOAL COMPLETE" else "GOAL: ${exercise.targetCount}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGoalComplete) KineticGreen else KineticOnSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Action buttons row — tall cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // +1 REP
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                            .border(1.dp, KineticGreen.copy(alpha = if (isGoalComplete) 0.03f else 0.1f), RoundedCornerShape(12.dp))
                            .then(
                                if (!isGoalComplete) Modifier.clickable {
                                    currentCount = (currentCount + 1).coerceAtMost(exercise.targetCount)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.then(if (isGoalComplete) Modifier.alpha(0.3f) else Modifier)
                        ) {
                            Text(
                                text = "+1",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp
                                ),
                                color = KineticGreen
                            )
                            Text(
                                text = "REP",
                                style = MaterialTheme.typography.labelSmall,
                                color = KineticOnSurfaceVariant
                            )
                        }
                    }

                    // +10 REPS
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                            .border(1.dp, KineticGreen.copy(alpha = if (isGoalComplete) 0.03f else 0.1f), RoundedCornerShape(12.dp))
                            .then(
                                if (!isGoalComplete) Modifier.clickable {
                                    currentCount = (currentCount + 10).coerceAtMost(exercise.targetCount)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.then(if (isGoalComplete) Modifier.alpha(0.3f) else Modifier)
                        ) {
                            Text(
                                text = "+10",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 30.sp
                                ),
                                color = KineticGreen
                            )
                            Text(
                                text = "REPS",
                                style = MaterialTheme.typography.labelSmall,
                                color = KineticOnSurfaceVariant
                            )
                        }
                    }

                    // EDIT
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .background(KineticSurfaceContainerHigh, RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable { isEditing = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit count",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "EDIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = KineticOnSurfaceVariant
                            )
                        }
                    }
                }

                // Accelerometer Mode toggle
                Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KineticSurfaceContainer, RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(KineticSurfaceContainerHigh, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = KineticGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACCELEROMETER MODE",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Auto-detect each ${exercise.name.lowercase().dropLast(1)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = KineticOnSurfaceVariant
                            )
                            Text(
                                text = "Experimental: requires extra permissions",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = KineticOnSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        Switch(
                            checked = accelerometerMode,
                            onCheckedChange = { accelerometerMode = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = KineticGreen,
                                checkedThumbColor = KineticBackground
                            )
                        )
                    }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Fixed DONE button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .height(64.dp)
                    .background(KineticGreen, RoundedCornerShape(12.dp))
                    .clickable {
                        onUpdateCount(currentCount)
                        onDone()
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DONE",
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
        }
    }

}
