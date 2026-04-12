package com.example.trainingdashboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.AppOpenEvent
import com.example.trainingdashboard.notification.NotificationHelper
import com.example.trainingdashboard.notification.ReminderScheduler
import com.example.trainingdashboard.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import com.example.trainingdashboard.ui.DashboardScreen
import com.example.trainingdashboard.ui.theme.TrainingDashboardTheme
import kotlinx.coroutines.launch
import java.time.LocalTime

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scheduleAllReminders()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermissionAndSchedule()

        setContent {
            TrainingDashboardTheme {
                DashboardScreen()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recordAppOpen()
        WidgetUpdater.update(this)
    }

    private fun recordAppOpen() {
        val db = (application as TrainingApp).database
        val dao = db.appOpenEventDao()
        val now = System.currentTimeMillis()

        lifecycleScope.launch {
            val recent = dao.getRecentEvents(1)
            val lastTimestamp = recent.firstOrNull()?.timestamp ?: 0L
            val thirtyMinutes = 30 * 60 * 1000L

            if (now - lastTimestamp > thirtyMinutes) {
                dao.insert(AppOpenEvent(timestamp = now, hourOfDay = LocalTime.now().hour))
            }
        }
    }

    private fun scheduleAllReminders() {
        val context = this
        lifecycleScope.launch {
            val prefs = PreferencesRepository(context)
            val morningH = prefs.reminderHour.first()
            val morningM = prefs.reminderMinute.first()
            val afternoonH = prefs.afternoonNudgeHour.first()
            val afternoonM = prefs.afternoonNudgeMinute.first()
            val eveningH = prefs.eveningInterruptHour.first()
            val eveningM = prefs.eveningInterruptMinute.first()

            ReminderScheduler.schedule(context, morningH, morningM)
            ReminderScheduler.scheduleAfternoonNudge(context, afternoonH, afternoonM)
            ReminderScheduler.scheduleEveningInterrupt(context, eveningH, eveningM)
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleAllReminders()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleAllReminders()
        }
    }
}
