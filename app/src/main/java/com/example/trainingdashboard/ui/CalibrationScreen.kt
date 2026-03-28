package com.example.trainingdashboard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.trainingdashboard.sensor.CalibrationResult
import com.example.trainingdashboard.sensor.RepCalibrator
import com.example.trainingdashboard.sensor.TimestampedSample
import com.example.trainingdashboard.ui.theme.KineticBackground
import com.example.trainingdashboard.ui.theme.KineticGreen
import com.example.trainingdashboard.ui.theme.KineticOnSurfaceVariant
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainer
import com.example.trainingdashboard.ui.theme.KineticSurfaceContainerHigh
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private sealed class CalibrationUiState {
    object Instructions : CalibrationUiState()
    data class Countdown(val secondsLeft: Int) : CalibrationUiState()
    object Recording : CalibrationUiState()
    object Analyzing : CalibrationUiState()
    data class Success(val repCount: Int, val threshold: Float) : CalibrationUiState()
    object Failed : CalibrationUiState()
}

/**
 * Full-screen calibration overlay. Shown inside LogRepsScreen's root Box.
 *
 * @param exerciseName Used for display copy only.
 * @param onSuccess Called with the computed threshold when calibration succeeds.
 * @param onCancel Called when the user cancels at any step. Does NOT clear any existing threshold.
 */
@Composable
fun CalibrationScreen(
    exerciseName: String,
    onSuccess: (threshold: Float) -> Unit,
    onCancel: () -> Unit
) {
    var uiState by remember { mutableStateOf<CalibrationUiState>(CalibrationUiState.Instructions) }
    val samples = remember { mutableListOf<TimestampedSample>() }
    val context = LocalContext.current
    val view = LocalView.current

    // --- Permission state ---
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val activity = context as? ComponentActivity
        val canAskAgain = activity?.shouldShowRequestPermissionRationale(Manifest.permission.BODY_SENSORS) ?: true
        val status = resolveSensorPermissionStatus(isGranted = granted, canAskAgain = canAskAgain)
        permissionGranted = status == SensorPermissionStatus.Granted
        permanentlyDenied = status == SensorPermissionStatus.PermanentlyDenied
    }

    // Request permission immediately when arriving at instructions
    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(Manifest.permission.BODY_SENSORS)
        }
    }

    // Keep screen on during countdown and recording so the sensor isn't suspended
    val shouldKeepScreenOn = uiState is CalibrationUiState.Countdown || uiState is CalibrationUiState.Recording
    DisposableEffect(shouldKeepScreenOn) {
        view.keepScreenOn = shouldKeepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    // Countdown ticker
    LaunchedEffect(uiState) {
        if (uiState is CalibrationUiState.Countdown) {
            val state = uiState as CalibrationUiState.Countdown
            if (state.secondsLeft > 0) {
                delay(1000L)
                uiState = CalibrationUiState.Countdown(state.secondsLeft - 1)
            } else {
                samples.clear()
                uiState = CalibrationUiState.Recording
            }
        }
    }

    // Sensor registration during Recording
    DisposableEffect(uiState) {
        if (uiState !is CalibrationUiState.Recording) return@DisposableEffect onDispose {}

        val sensorManager = context.getSystemService(SensorManager::class.java)
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: return@DisposableEffect onDispose {}

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
                val magnitude = sqrt(x * x + y * y + z * z)
                samples.add(TimestampedSample(System.currentTimeMillis(), magnitude))
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    // Run analysis when state transitions to Analyzing
    LaunchedEffect(uiState) {
        if (uiState !is CalibrationUiState.Analyzing) return@LaunchedEffect
        val result = RepCalibrator.analyze(samples.toList())
        uiState = when (result) {
            is CalibrationResult.Success -> CalibrationUiState.Success(result.repCount, result.threshold)
            is CalibrationResult.Failed  -> CalibrationUiState.Failed
        }
    }

    // Auto-close on success after brief display
    LaunchedEffect(uiState) {
        if (uiState is CalibrationUiState.Success) {
            delay(1500L)
            onSuccess((uiState as CalibrationUiState.Success).threshold)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KineticBackground)
            .padding(horizontal = 20.dp)
    ) {
        when (val state = uiState) {
            is CalibrationUiState.Instructions -> InstructionsPage(
                exerciseName = exerciseName,
                permissionGranted = permissionGranted,
                permanentlyDenied = permanentlyDenied,
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.BODY_SENSORS) },
                onStart = { uiState = CalibrationUiState.Countdown(3) },
                onCancel = onCancel
            )
            is CalibrationUiState.Countdown -> CountdownPage(
                secondsLeft = state.secondsLeft,
                onCancel = onCancel
            )
            is CalibrationUiState.Recording -> RecordingPage(
                onStop = { uiState = CalibrationUiState.Analyzing },
                onCancel = onCancel
            )
            is CalibrationUiState.Analyzing -> AnalyzingPage()
            is CalibrationUiState.Success -> SuccessPage(repCount = state.repCount)
            is CalibrationUiState.Failed -> FailedPage(
                onRetry = { uiState = CalibrationUiState.Instructions },
                onCancel = onCancel
            )
        }
    }
}

@Composable
private fun InstructionsPage(
    exerciseName: String,
    permissionGranted: Boolean,
    permanentlyDenied: Boolean,
    onRequestPermission: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Header
        Text(
            text = "CALIBRATE",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            ),
            color = KineticGreen
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .background(KineticGreen, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (permanentlyDenied) {
            // Permission permanently denied — explain and offer settings link
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Sensor access denied.",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    Text(
                        text = "Calibration and accelerometer mode require sensor access. Enable the Body Sensors permission in Settings to use this feature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KineticOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KineticGreen, RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null)
                        )
                        context.startActivity(intent)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "OPEN SETTINGS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontStyle = FontStyle.Italic,
                        fontSize = 18.sp
                    ),
                    color = KineticBackground
                )
            }
        } else {
            // Normal instructions
            CalibrationStep(number = "1", text = "GET INTO YOUR TRAINING POSITION")
            Spacer(modifier = Modifier.height(12.dp))
            CalibrationStep(number = "2", text = "DO A FEW ${exerciseName.uppercase()} REPS")
            Spacer(modifier = Modifier.height(12.dp))
            CalibrationStep(number = "3", text = "FINISH YOUR REPS AND TAP STOP")

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "We'll measure your movement to set detection sensitivity.",
                style = MaterialTheme.typography.bodySmall,
                color = KineticOnSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (!permissionGranted) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Sensor access is required to detect movement. Tap below to grant it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = KineticOnSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // START / GRANT ACCESS button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(KineticGreen, RoundedCornerShape(12.dp))
                    .clickable { if (permissionGranted) onStart() else onRequestPermission() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (permissionGranted) "START" else "GRANT SENSOR ACCESS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontStyle = FontStyle.Italic,
                            fontSize = 18.sp
                        ),
                        color = KineticBackground
                    )
                    if (permissionGranted) {
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

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KineticSurfaceContainerHigh),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "CANCEL",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                ),
                color = KineticOnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CalibrationStep(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(KineticSurfaceContainerHigh, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                color = KineticGreen
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
            ),
            color = Color.White
        )
    }
}

@Composable
private fun CountdownPage(secondsLeft: Int, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GET READY",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            ),
            color = KineticOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (secondsLeft > 0) "$secondsLeft" else "GO",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                fontSize = 96.sp,
                lineHeight = 96.sp
            ),
            color = KineticGreen
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Get into position now",
            style = MaterialTheme.typography.bodySmall,
            color = KineticOnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(80.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KineticSurfaceContainerHigh),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "CANCEL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                color = KineticOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordingPage(onStop: () -> Unit, onCancel: () -> Unit) {
    var elapsedSeconds by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing recording indicator
        Box(
            modifier = Modifier
                .size(80.dp)
                .scale(pulseScale)
                .background(KineticGreen.copy(alpha = 0.15f), CircleShape)
                .border(2.dp, KineticGreen.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(KineticGreen, CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "RECORDING",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            ),
            color = KineticGreen
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${elapsedSeconds}s",
            style = MaterialTheme.typography.bodySmall,
            color = KineticOnSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Do your reps, then take out your phone",
            style = MaterialTheme.typography.bodySmall,
            color = KineticOnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(80.dp))

        // STOP button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(KineticGreen, RoundedCornerShape(12.dp))
                .clickable { onStop() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "STOP",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 18.sp
                ),
                color = KineticBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KineticSurfaceContainerHigh),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "CANCEL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                color = KineticOnSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnalyzingPage() {
    val pulse = rememberInfiniteTransition(label = "analyzePulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
        label = "analyzeAlpha"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "ANALYZING",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            ),
            color = KineticGreen,
            modifier = Modifier.alpha(alpha)
        )
    }
}

@Composable
private fun SuccessPage(repCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = KineticGreen,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "$repCount REPS DETECTED",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            ),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "CALIBRATION COMPLETE",
            style = MaterialTheme.typography.bodySmall,
            color = KineticGreen
        )
    }
}

@Composable
private fun FailedPage(onRetry: () -> Unit, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "CALIBRATION FAILED",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(4.dp)
                .background(KineticOnSurfaceVariant, RoundedCornerShape(2.dp))
        )

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KineticSurfaceContainer, RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Not enough movement detected.",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                    color = Color.White
                )
                Text(
                    text = "Try doing the reps more deliberately — a clear, full range of motion works best.",
                    style = MaterialTheme.typography.bodySmall,
                    color = KineticOnSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This feature is experimental — it's okay if detection doesn't work for you.",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = KineticOnSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(KineticGreen, RoundedCornerShape(12.dp))
                .clickable { onRetry() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TRY AGAIN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontStyle = FontStyle.Italic,
                    fontSize = 18.sp
                ),
                color = KineticBackground
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KineticSurfaceContainerHigh),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "CANCEL",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 18.sp),
                color = KineticOnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
