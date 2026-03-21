package com.example.trainingdashboard.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AfternoonNudgeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        NotificationHelper.createNotificationChannel(applicationContext)

        val dayNumber = NotificationHelper.computeDayNumber(applicationContext)
        val state = NotificationHelper.getCompletionState(applicationContext, dayNumber)

        if (state is CompletionState.AllDone) return Result.success()

        val progress = state as CompletionState.Progress
        val (title, text) = NotificationHelper.buildMessageForStage(progress, NotificationStage.AFTERNOON)
        val notification = NotificationHelper.buildNotification(
            applicationContext, title, text, NotificationHelper.AFTERNOON_NOTIFICATION_ID
        )
        NotificationHelper.showNotification(
            applicationContext, notification, NotificationHelper.AFTERNOON_NOTIFICATION_ID
        )

        return Result.success()
    }
}
