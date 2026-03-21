package com.example.trainingdashboard.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

class AdaptiveTimingWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefsRepo = PreferencesRepository(applicationContext)

        if (!prefsRepo.adaptiveTimingEnabled.first()) return Result.success()

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.appOpenEventDao()

        // Clean up old events (older than 30 days)
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        dao.deleteOlderThan(cutoff)

        val events = dao.getRecentEvents(14)
        if (events.size < 3) return Result.success()

        // Weighted average: more recent events matter more
        val today = LocalDate.now()
        var weightedSum = 0.0
        var totalWeight = 0.0

        for (event in events) {
            val eventDate = Instant.ofEpochMilli(event.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val daysAgo = java.time.temporal.ChronoUnit.DAYS.between(eventDate, today).toInt()
            val weight = 1.0 / (daysAgo + 1)
            weightedSum += event.hourOfDay * weight
            totalWeight += weight
        }

        val avgHour = (weightedSum / totalWeight).roundToInt()
        // Remind 1 hour before typical usage, clamped to 6AM-10PM
        val suggestedHour = (avgHour - 1).coerceIn(6, 22)

        val currentHour = prefsRepo.reminderHour.first()
        val currentMinute = prefsRepo.reminderMinute.first()

        val currentTotalMinutes = currentHour * 60 + currentMinute
        val suggestedTotalMinutes = suggestedHour * 60

        if (kotlin.math.abs(currentTotalMinutes - suggestedTotalMinutes) > 30) {
            prefsRepo.setReminderTime(suggestedHour, 0)
            ReminderScheduler.schedule(applicationContext, suggestedHour, 0)
        }

        return Result.success()
    }
}
