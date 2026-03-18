package com.example.trainingdashboard.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trainingdashboard.TrainingApp
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.DailyCompletion
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

    init {
        viewModelScope.launch {
            val startDate = ensureStartDate()
            val dayNumber = computeDayNumber(startDate)

            completionDao.getCompletionsForDay(dayNumber).collect { completions ->
                val completionMap = completions.associate { it.exercise to it.completed }
                val exercises = buildExercises(dayNumber, completionMap)
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

        viewModelScope.launch {
            completionDao.upsertCompletion(
                DailyCompletion(
                    dayNumber = state.dayNumber,
                    exercise = exerciseName,
                    completed = !exercise.isCompleted
                )
            )
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
        completionMap: Map<String, Boolean>
    ): List<ExerciseState> {
        return listOf(
            ExerciseState(
                name = "Push-ups",
                targetCount = dayNumber,
                isCompleted = completionMap["Push-ups"] ?: false
            ),
            ExerciseState(
                name = "Sit-ups",
                targetCount = dayNumber * 2,
                isCompleted = completionMap["Sit-ups"] ?: false
            ),
            ExerciseState(
                name = "Squats",
                targetCount = dayNumber * 3,
                isCompleted = completionMap["Squats"] ?: false
            )
        )
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
