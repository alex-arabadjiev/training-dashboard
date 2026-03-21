package com.example.trainingdashboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trainingdashboard.TrainingApp
import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = (application as TrainingApp).database
    private val completionDao = db.completionDao()
    private val prefsRepo = PreferencesRepository(application)

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

            completionDao.getCompletionsForDay(dayNumber).collect { completions ->
                val exercises = buildExercises(dayNumber, completions)
                val allCompleted = exercises.all { it.isCompleted }
                val streak = computeStreak(dayNumber)

                val hour = prefsRepo.reminderHour.first()
                val minute = prefsRepo.reminderMinute.first()
                val afternoonHour = prefsRepo.afternoonNudgeHour.first()
                val afternoonMinute = prefsRepo.afternoonNudgeMinute.first()
                val eveningHour = prefsRepo.eveningInterruptHour.first()
                val eveningMinute = prefsRepo.eveningInterruptMinute.first()
                val adaptiveEnabled = prefsRepo.adaptiveTimingEnabled.first()

                _uiState.value = DashboardUiState(
                    dayNumber = dayNumber,
                    exercises = exercises,
                    allCompleted = allCompleted,
                    streak = streak,
                    reminderHour = hour,
                    reminderMinute = minute,
                    afternoonNudgeHour = afternoonHour,
                    afternoonNudgeMinute = afternoonMinute,
                    eveningInterruptHour = eveningHour,
                    eveningInterruptMinute = eveningMinute,
                    adaptiveTimingEnabled = adaptiveEnabled,
                    isLoading = false
                )
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
        }
    }

    fun updateAfternoonNudgeTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setAfternoonNudgeTime(hour, minute)
            _uiState.value = _uiState.value.copy(
                afternoonNudgeHour = hour, afternoonNudgeMinute = minute
            )
        }
    }

    fun updateEveningInterruptTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            prefsRepo.setEveningInterruptTime(hour, minute)
            _uiState.value = _uiState.value.copy(
                eveningInterruptHour = hour, eveningInterruptMinute = minute
            )
        }
    }

    fun setAdaptiveTimingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setAdaptiveTimingEnabled(enabled)
            _uiState.value = _uiState.value.copy(adaptiveTimingEnabled = enabled)
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
