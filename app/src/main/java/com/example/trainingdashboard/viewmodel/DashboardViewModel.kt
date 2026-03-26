package com.example.trainingdashboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.example.trainingdashboard.TrainingApp
import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.CompletionDao
import com.example.trainingdashboard.data.db.DailyCompletion
import com.example.trainingdashboard.notification.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ExerciseState(
    val name: String,
    val targetCount: Int,
    val completedCount: Int,
    val isCompleted: Boolean
)

data class DashboardUiState(
    val dayNumber: Int = 1,
    val exercises: List<ExerciseState> = emptyList(),
    val allCompleted: Boolean = false,
    val streak: Int = 0,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val afternoonNudgeHour: Int = 14,
    val afternoonNudgeMinute: Int = 0,
    val eveningInterruptHour: Int = 20,
    val eveningInterruptMinute: Int = 0,
    val adaptiveTimingEnabled: Boolean = false,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    application: Application,
    private val completionDao: CompletionDao,
    private val prefsRepo: PreferencesRepository
) : AndroidViewModel(application) {

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val app = extras[APPLICATION_KEY] as TrainingApp
                return DashboardViewModel(
                    app,
                    app.database.completionDao(),
                    PreferencesRepository(app)
                ) as T
            }
        }
    }

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadDashboard()
    }

    private fun loadDashboard() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val startDate = ensureStartDate()
            val dayNumber = computeDayNumber(startDate)

            combine(
                completionDao.getCompletionsForDay(dayNumber),
                prefsRepo.reminderHour,
                prefsRepo.reminderMinute,
                prefsRepo.afternoonNudgeHour,
                prefsRepo.afternoonNudgeMinute,
                prefsRepo.eveningInterruptHour,
                prefsRepo.eveningInterruptMinute,
                prefsRepo.adaptiveTimingEnabled
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val completions = values[0] as List<DailyCompletion>
                val exercises = buildExercises(dayNumber, completions)
                DashboardUiState(
                    dayNumber = dayNumber,
                    exercises = exercises,
                    allCompleted = exercises.all { it.isCompleted },
                    streak = computeStreak(dayNumber),
                    reminderHour = values[1] as Int,
                    reminderMinute = values[2] as Int,
                    afternoonNudgeHour = values[3] as Int,
                    afternoonNudgeMinute = values[4] as Int,
                    eveningInterruptHour = values[5] as Int,
                    eveningInterruptMinute = values[6] as Int,
                    adaptiveTimingEnabled = values[7] as Boolean,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleExercise(exerciseName: String) {
        val state = _uiState.value
        val exercise = state.exercises.find { it.name == exerciseName } ?: return
        val newCompleted = !exercise.isCompleted
        val newCount = if (newCompleted) exercise.targetCount else 0

        viewModelScope.launch {
            completionDao.upsertCompletion(
                DailyCompletion(
                    dayNumber = state.dayNumber,
                    exercise = exerciseName,
                    completed = newCompleted,
                    completedCount = newCount
                )
            )
        }
    }

    fun updateExerciseCount(exerciseName: String, count: Int) {
        val state = _uiState.value
        val exercise = state.exercises.find { it.name == exerciseName } ?: return
        val clampedCount = count.coerceIn(0, exercise.targetCount)
        val completed = clampedCount >= exercise.targetCount

        viewModelScope.launch {
            completionDao.upsertCompletion(
                DailyCompletion(
                    dayNumber = state.dayNumber,
                    exercise = exerciseName,
                    completed = completed,
                    completedCount = clampedCount
                )
            )
        }
    }

    fun setCurrentDay(day: Int) {
        if (day < 1) return
        viewModelScope.launch {
            val newStartDate = LocalDate.now().minusDays((day - 1).toLong())
            prefsRepo.setStartDate(newStartDate)
            loadDashboard()
        }
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setReminderTime(hour, minute)
            _uiState.value = _uiState.value.copy(reminderHour = hour, reminderMinute = minute)
            ReminderScheduler.schedule(getApplication(), hour, minute)
        }
    }

    fun updateAfternoonNudgeTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setAfternoonNudgeTime(hour, minute)
            _uiState.value = _uiState.value.copy(
                afternoonNudgeHour = hour, afternoonNudgeMinute = minute
            )
            ReminderScheduler.scheduleAfternoonNudge(getApplication(), hour, minute)
        }
    }

    fun updateEveningInterruptTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setEveningInterruptTime(hour, minute)
            _uiState.value = _uiState.value.copy(
                eveningInterruptHour = hour, eveningInterruptMinute = minute
            )
            ReminderScheduler.scheduleEveningInterrupt(getApplication(), hour, minute)
        }
    }

    fun setAdaptiveTimingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAdaptiveTimingEnabled(enabled)
            _uiState.value = _uiState.value.copy(adaptiveTimingEnabled = enabled)
            if (enabled) {
                ReminderScheduler.scheduleAdaptiveTiming(getApplication())
            } else {
                ReminderScheduler.cancelAdaptiveTiming(getApplication())
            }
        }
    }

    private suspend fun ensureStartDate(): LocalDate {
        val existing = prefsRepo.startDate.first()
        if (existing != null) return existing

        val today = LocalDate.now()
        prefsRepo.setStartDate(today)
        return today
    }

    private fun computeDayNumber(startDate: LocalDate): Int {
        return ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1
    }

    private fun buildExercises(
        dayNumber: Int,
        completions: List<DailyCompletion>
    ): List<ExerciseState> {
        val completionMap = completions.associateBy { it.exercise }
        return ExerciseTargets.forDay(dayNumber).map { (name, target) ->
            val completion = completionMap[name]
            ExerciseState(
                name = name,
                targetCount = target,
                completedCount = completion?.completedCount ?: 0,
                isCompleted = completion?.completed ?: false
            )
        }
    }

    private suspend fun computeStreak(currentDay: Int): Int {
        val completedDays = completionDao.getFullyCompletedDays().toSet()
        var streak = 0
        var day = currentDay
        while (day > 0 && completedDays.contains(day)) {
            streak++
            day--
        }
        return streak
    }
}
