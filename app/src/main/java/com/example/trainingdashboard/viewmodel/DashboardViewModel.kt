package com.example.trainingdashboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trainingdashboard.TrainingApp
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

                _uiState.value = DashboardUiState(
                    dayNumber = dayNumber,
                    exercises = exercises,
                    allCompleted = allCompleted,
                    streak = streak,
                    reminderHour = hour,
                    reminderMinute = minute,
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
        return listOf(
            Triple("Push-ups", dayNumber, completionMap["Push-ups"]),
            Triple("Sit-ups", dayNumber * 2, completionMap["Sit-ups"]),
            Triple("Squats", dayNumber * 3, completionMap["Squats"])
        ).map { (name, target, completion) ->
            val count = completion?.completedCount ?: 0
            ExerciseState(
                name = name,
                targetCount = target,
                completedCount = count,
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
