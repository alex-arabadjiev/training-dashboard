package com.example.trainingdashboard.notification

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.trainingdashboard.MainActivity
import com.example.trainingdashboard.ui.theme.TrainingDashboardTheme

class InterruptActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val overallPercent = intent.getIntExtra(EXTRA_PERCENT, 0)
        val dayNumber = intent.getIntExtra(EXTRA_DAY_NUMBER, 1)
        val summariesRaw = intent.getStringExtra(EXTRA_SUMMARIES) ?: ""

        setContent {
            TrainingDashboardTheme {
                InterruptScreen(
                    dayNumber = dayNumber,
                    overallPercent = overallPercent,
                    summariesText = summariesRaw,
                    onTrain = {
                        startActivity(Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    },
                    onSnooze = { minutes ->
                        ReminderScheduler.scheduleSnooze(this, minutes)
                        finish()
                    }
                )
            }
        }
    }

    @Deprecated("Use onBackPressedDispatcher")
    override fun onBackPressed() {
        // Block back press — user must choose train or snooze
    }

    companion object {
        const val EXTRA_PERCENT = "extra_percent"
        const val EXTRA_DAY_NUMBER = "extra_day_number"
        const val EXTRA_SUMMARIES = "extra_summaries"
    }
}

@Composable
private fun InterruptScreen(
    dayNumber: Int,
    overallPercent: Int,
    summariesText: String,
    onTrain: () -> Unit,
    onSnooze: (Int) -> Unit
) {
    var showSnoozeOptions by remember { mutableStateOf(false) }

    val title = when {
        overallPercent == 0 -> "You haven't trained today!"
        overallPercent >= 90 -> "Almost there! Just a few more reps!"
        else -> "You're $overallPercent% done — finish your workout!"
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Day $dayNumber",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (summariesText.isNotBlank()) {
                        summariesText.split(";").forEach { line ->
                            if (line.isNotBlank()) {
                                Text(
                                    text = line.trim(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onTrain,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Let's go", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!showSnoozeOptions) {
                OutlinedButton(
                    onClick = { showSnoozeOptions = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Snooze")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 60, 120).forEach { minutes ->
                        val label = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
                        OutlinedButton(
                            onClick = { onSnooze(minutes) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(label)
                        }
                    }
                }
            }
        }
    }
}
