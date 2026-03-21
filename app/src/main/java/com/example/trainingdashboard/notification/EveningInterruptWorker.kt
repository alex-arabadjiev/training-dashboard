package com.example.trainingdashboard.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trainingdashboard.R

class EveningInterruptWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannel(applicationContext)

        val dayNumber = NotificationHelper.computeDayNumber(applicationContext)
        val state = NotificationHelper.getCompletionState(applicationContext, dayNumber)

        if (state is CompletionState.AllDone) return Result.success()

        val progress = state as CompletionState.Progress
        val (title, text) = NotificationHelper.buildMessageForStage(progress, NotificationStage.EVENING)
        val summariesText = progress.exerciseSummaries.joinToString("; ") {
            "${it.name}: ${it.completedCount}/${it.targetCount}"
        }

        val interruptIntent = Intent(applicationContext, InterruptActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(InterruptActivity.EXTRA_PERCENT, progress.overallPercent)
            putExtra(InterruptActivity.EXTRA_DAY_NUMBER, progress.dayNumber)
            putExtra(InterruptActivity.EXTRA_SUMMARIES, summariesText)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NotificationHelper.EVENING_NOTIFICATION_ID,
            interruptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID_URGENT)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.notify(NotificationHelper.EVENING_NOTIFICATION_ID, notification)

        return Result.success()
    }
}
