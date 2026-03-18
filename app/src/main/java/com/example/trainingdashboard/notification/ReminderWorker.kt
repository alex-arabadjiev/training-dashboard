package com.example.trainingdashboard.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trainingdashboard.MainActivity
import com.example.trainingdashboard.R
import com.example.trainingdashboard.data.PreferencesRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefsRepo = PreferencesRepository(applicationContext)
        val startDate = prefsRepo.startDate.first() ?: LocalDate.now()
        val dayNumber = ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1

        createNotificationChannel()
        showNotification(dayNumber)

        return Result.success()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Training Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily training reminder notifications"
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun showNotification(dayNumber: Int) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Time to train!")
            .setContentText("Day $dayNumber: $dayNumber push-ups, ${dayNumber * 2} sit-ups, ${dayNumber * 3} squats")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "training_reminders"
        const val NOTIFICATION_ID = 1
    }
}
