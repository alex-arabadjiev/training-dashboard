package com.example.trainingdashboard.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.trainingdashboard.MainActivity
import com.example.trainingdashboard.R
import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.AppDatabase
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class NotificationStage { MORNING, AFTERNOON, EVENING }

sealed class CompletionState {
    object AllDone : CompletionState()
    data class Progress(
        val dayNumber: Int,
        val overallPercent: Int,
        val exerciseSummaries: List<ExerciseSummary>
    ) : CompletionState()
}

data class ExerciseSummary(
    val name: String,
    val completedCount: Int,
    val targetCount: Int
) {
    val remaining: Int get() = (targetCount - completedCount).coerceAtLeast(0)
    val isDone: Boolean get() = completedCount >= targetCount
}

object NotificationHelper {

    const val CHANNEL_ID = "training_reminders"
    const val CHANNEL_ID_URGENT = "training_interrupt"
    const val MORNING_NOTIFICATION_ID = 1
    const val AFTERNOON_NOTIFICATION_ID = 2
    const val EVENING_NOTIFICATION_ID = 3

    fun createNotificationChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            CHANNEL_ID,
            "Training Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily training reminder notifications"
        }
        manager.createNotificationChannel(reminderChannel)

        val interruptChannel = NotificationChannel(
            CHANNEL_ID_URGENT,
            "Training Interrupt",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Evening interrupt to complete exercises"
            setBypassDnd(true)
        }
        manager.createNotificationChannel(interruptChannel)
    }

    suspend fun computeDayNumber(context: Context): Int {
        val prefsRepo = PreferencesRepository(context)
        val startDate = prefsRepo.startDate.first() ?: LocalDate.now()
        return ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1
    }

    suspend fun getCompletionState(context: Context, dayNumber: Int): CompletionState {
        val db = AppDatabase.getInstance(context)
        val prefsRepo = PreferencesRepository(context)
        val goalLevel = prefsRepo.goalLevel.first() ?: 1
        val completions = db.completionDao().getCompletionsForDaySnapshot(dayNumber)
        return computeCompletionState(dayNumber, goalLevel, completions)
    }

    /**
     * Pure computation extracted for testability.
     * Uses [goalLevel] (the adaptive goal level) — not [dayNumber] — to determine targets.
     */
    internal fun computeCompletionState(
        dayNumber: Int,
        goalLevel: Int,
        completions: List<DailyCompletion>
    ): CompletionState {
        val completionMap = completions.associateBy { it.exercise }
        val targets = ExerciseTargets.forDay(goalLevel)

        val summaries = targets.map { (name, target) ->
            val completion = completionMap[name]
            ExerciseSummary(
                name = name,
                completedCount = completion?.completedCount ?: 0,
                targetCount = target
            )
        }

        val totalCompleted = summaries.sumOf { it.completedCount }
        val totalTarget = summaries.sumOf { it.targetCount }

        if (summaries.all { it.isDone }) {
            return CompletionState.AllDone
        }

        val percent = if (totalTarget > 0) (totalCompleted * 100) / totalTarget else 0
        return CompletionState.Progress(dayNumber, percent, summaries)
    }

    fun buildMessageForStage(state: CompletionState.Progress, stage: NotificationStage): Pair<String, String> {
        val day = state.dayNumber
        val pct = state.overallPercent
        val remaining = state.exerciseSummaries.filter { !it.isDone }
        val remainingText = remaining.joinToString(", ") { "${it.name}: ${it.remaining} left" }
        val targetText = state.exerciseSummaries.joinToString(", ") { "${it.targetCount} ${it.name.lowercase()}" }

        return when (stage) {
            NotificationStage.MORNING -> when {
                pct == 0 -> "Time to train!" to "Day $day: $targetText"
                pct < 50 -> "Good start! You're $pct% there" to "Keep it up — $remainingText"
                pct < 90 -> "Halfway there! $pct% of Day $day complete" to remainingText
                else -> "Nearly done!" to "Just $remainingText"
            }
            NotificationStage.AFTERNOON -> when {
                pct == 0 -> "You haven't started today's exercises yet!" to "Day $day: $targetText"
                pct < 50 -> "You're $pct% done — don't lose momentum!" to remainingText
                pct < 90 -> "Almost there! $pct% done — finish strong!" to remainingText
                else -> "Almost there! Just a little more!" to remainingText
            }
            NotificationStage.EVENING -> when {
                pct == 0 -> "Your Day $day exercises are still waiting!" to targetText
                pct < 50 -> "You're $pct% in — just a bit more to finish Day $day!" to remainingText
                pct < 90 -> "So close! $pct% done — let's wrap up Day $day!" to remainingText
                else -> "Just a few more reps to finish Day $day!" to remainingText
            }
        }
    }

    fun buildNotification(
        context: Context,
        title: String,
        text: String,
        notificationId: Int
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun showNotification(context: Context, notification: Notification, notificationId: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
