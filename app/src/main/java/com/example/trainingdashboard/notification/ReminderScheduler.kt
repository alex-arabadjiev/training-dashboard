package com.example.trainingdashboard.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val MORNING_WORK = "training_daily_reminder"
    private const val AFTERNOON_WORK = "training_afternoon_nudge"

    private const val EVENING_ALARM_REQUEST_CODE = 100
    private const val SNOOZE_ALARM_REQUEST_CODE = 101

    fun schedule(context: Context, hour: Int = 8, minute: Int = 0) {
        scheduleDailyWorker<ReminderWorker>(context, MORNING_WORK, hour, minute)
    }

    fun scheduleAfternoonNudge(context: Context, hour: Int = 14, minute: Int = 0) {
        scheduleDailyWorker<AfternoonNudgeWorker>(context, AFTERNOON_WORK, hour, minute)
    }

    fun scheduleEveningInterrupt(context: Context, hour: Int = 20, minute: Int = 0) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, EveningAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, EVENING_ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = LocalDateTime.now()
        var targetTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))
        if (now >= targetTime) {
            targetTime = targetTime.plusDays(1)
        }

        val triggerMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun scheduleSnooze(context: Context, delayMinutes: Int) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            return
        }

        val intent = Intent(context, EveningAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, SNOOZE_ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMillis = System.currentTimeMillis() + delayMinutes * 60 * 1000L
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(MORNING_WORK)
    }

    fun cancelAfternoonNudge(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(AFTERNOON_WORK)
    }

    fun cancelEveningInterrupt(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = Intent(context, EveningAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, EVENING_ALARM_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private inline fun <reified W : androidx.work.ListenableWorker> scheduleDailyWorker(
        context: Context,
        workName: String,
        hour: Int,
        minute: Int
    ) {
        val now = LocalDateTime.now()
        var targetTime = LocalDateTime.of(now.toLocalDate(), LocalTime.of(hour, minute))

        if (now >= targetTime) {
            targetTime = targetTime.plusDays(1)
        }

        val initialDelay = Duration.between(now, targetTime).toMillis()

        val workRequest = PeriodicWorkRequestBuilder<W>(24, TimeUnit.HOURS, 15, TimeUnit.MINUTES)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }
}
