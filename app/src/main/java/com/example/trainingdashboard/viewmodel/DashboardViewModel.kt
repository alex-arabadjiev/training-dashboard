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
import com.example.trainingdashboard.widget.WidgetUpdater
import com.example.trainingdashboard.data.GoalTransition
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.CompletionDao
import com.example.trainingdashboard.data.db.DailyCompletion
import com.example.trainingdashboard.notification.NotificationHelper
import com.example.trainingdashboard.notification.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ExerciseState(
    val name: String,
    val targetCount: Int,
    val completedCount: Int,
    val isCompleted: Boolean,
    val isDisabled: Boolean = false
)

data class DashboardUiState(
    val dayNumber: Int = 0,
    val goalLevel: Int = 1,
    val dayNumberOffset: Int = 0,
    val exercises: List<ExerciseState> = emptyList(),
    val allCompleted: Boolean = false,
    val exerciseIncrements: Map<String, Float> = ExerciseTargets.DEFAULT_INCREMENTS,
    val exerciseEnabled: Map<String, Boolean> = ExerciseTargets.EXERCISE_NAMES.associateWith { true },
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val afternoonNudgeHour: Int = 14,
    val afternoonNudgeMinute: Int = 0,
    val eveningInterruptHour: Int = 20,
    val eveningInterruptMinute: Int = 0,
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

    private val _accelThresholds = MutableStateFlow<Map<String, Float?>>(
        mapOf("Push-ups" to null, "Sit-ups" to null, "Squats" to null)
    )
    val accelThresholds: StateFlow<Map<String, Float?>> = _accelThresholds.asStateFlow()

    private var loadJob: Job? = null
    private var todayCalendarDay: Int = 1

    init {
        loadDashboard()
        collectAccelThresholds()
        cancelNotificationsOnCompletion()
    }

    private fun loadDashboard() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val startDate = ensureStartDate()
            todayCalendarDay = computeDayNumber(startDate)

            val increments = prefsRepo.exerciseIncrements.first()
            val enabled = prefsRepo.exerciseEnabled.first()
            val baseReps = prefsRepo.baseReps.first()

            val storedGoalLevel = prefsRepo.goalLevel.first()
            var lastEvaluatedDay = prefsRepo.lastEvaluatedDay.first()

            // Migration for existing users: if goal_level was never set, initialise from calendar day
            val resolvedGoalLevel: Int
            if (storedGoalLevel == null) {
                val migratedLevel = maxOf(todayCalendarDay, GoalTransition.GOAL_LEVEL_FLOOR)
                prefsRepo.setGoalLevel(migratedLevel)
                prefsRepo.setLastEvaluatedDay(todayCalendarDay - 1)
                resolvedGoalLevel = migratedLevel
                lastEvaluatedDay = todayCalendarDay - 1
            } else {
                resolvedGoalLevel = storedGoalLevel
            }

            // For goal transition: disabled exercises have weight 0,
            // flat exercises (increment=0, enabled) use their default weight
            val goalTransitionIncrements = increments.mapValues { (name, inc) ->
                when {
                    enabled[name] == false -> 0f
                    inc == 0f -> ExerciseTargets.DEFAULT_INCREMENTS[name] ?: 1.0f
                    else -> inc
                }
            }

            // Catch-up loop — evaluate each unevaluated past day in order
            var runningLevel = resolvedGoalLevel
            for (day in (lastEvaluatedDay + 1)..(todayCalendarDay - 1)) {
                val dayCompletions = completionDao.getCompletionsForDaySnapshot(day)
                if (dayCompletions.isEmpty()) {
                    completionDao.upsertCompletions(
                        ExerciseTargets.EXERCISE_NAMES.map { name ->
                            DailyCompletion(dayNumber = day, exercise = name, completed = false, completedCount = 0)
                        }
                    )
                }
                val progress = GoalTransition.computeProgress(dayCompletions, runningLevel, goalTransitionIncrements)
                runningLevel = GoalTransition.nextLevel(runningLevel, progress)
            }

            // Persist updated goal level and last evaluated day
            prefsRepo.setGoalLevel(runningLevel)
            prefsRepo.setLastEvaluatedDay(todayCalendarDay - 1)

            // Compute active day count from all completed exercises
            val allCompleted = completionDao.getAllCompletedExercises()
            val activeDayCount = GoalTransition.computeActiveDayCount(allCompleted, goalTransitionIncrements)

            val finalGoalLevel = runningLevel

            combine(
                completionDao.getCompletionsForDay(todayCalendarDay),
                prefsRepo.reminderHour,
                prefsRepo.reminderMinute,
                prefsRepo.afternoonNudgeHour,
                prefsRepo.afternoonNudgeMinute,
                prefsRepo.eveningInterruptHour,
                prefsRepo.eveningInterruptMinute,
                prefsRepo.dayNumberOffset
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val completions = values[0] as List<DailyCompletion>
                val dayOffset = values[7] as Int
                val exercises = buildExercises(finalGoalLevel, completions, increments, enabled, baseReps)
                val activeExercises = exercises.filter { !it.isDisabled }
                DashboardUiState(
                    dayNumber = activeDayCount + dayOffset,
                    goalLevel = finalGoalLevel,
                    dayNumberOffset = dayOffset,
                    exercises = exercises,
                    allCompleted = activeExercises.isNotEmpty() && activeExercises.all { it.isCompleted },
                    exerciseIncrements = increments,
                    exerciseEnabled = enabled,
                    reminderHour = values[1] as Int,
                    reminderMinute = values[2] as Int,
                    afternoonNudgeHour = values[3] as Int,
                    afternoonNudgeMinute = values[4] as Int,
                    eveningInterruptHour = values[5] as Int,
                    eveningInterruptMinute = values[6] as Int,
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
        if (exercise.isDisabled) return
        val newCompleted = !exercise.isCompleted
        val newCount = if (newCompleted) exercise.targetCount else 0

        viewModelScope.launch {
            completionDao.upsertCompletion(
                DailyCompletion(
                    dayNumber = todayCalendarDay,
                    exercise = exerciseName,
                    completed = newCompleted,
                    completedCount = newCount
                )
            )
            WidgetUpdater.update(getApplication())
        }
    }

    fun updateExerciseCount(exerciseName: String, count: Int) {
        val state = _uiState.value
        val exercise = state.exercises.find { it.name == exerciseName } ?: return
        if (exercise.isDisabled) return
        val clampedCount = count.coerceIn(0, exercise.targetCount)
        val completed = clampedCount >= exercise.targetCount

        viewModelScope.launch {
            completionDao.upsertCompletion(
                DailyCompletion(
                    dayNumber = todayCalendarDay,
                    exercise = exerciseName,
                    completed = completed,
                    completedCount = clampedCount
                )
            )
            WidgetUpdater.update(getApplication())
        }
    }

    fun setExerciseIncrements(increments: Map<String, Float>) {
        viewModelScope.launch {
            val prevIncrements = _uiState.value.exerciseIncrements
            val currentExercises = _uiState.value.exercises
            // Snapshot current target for any exercise whose increment transitions to 0
            increments.forEach { (name, newInc) ->
                val prevInc = prevIncrements[name] ?: ExerciseTargets.DEFAULT_INCREMENTS[name] ?: 1.0f
                if (newInc == 0f && prevInc != 0f) {
                    val currentTarget = currentExercises.find { it.name == name }?.targetCount
                    if (currentTarget != null && currentTarget > 0) {
                        prefsRepo.setBaseReps(name, currentTarget)
                    }
                }
            }
            prefsRepo.setExerciseIncrements(increments)
            loadDashboard()
        }
    }

    fun setExerciseEnabled(enabled: Map<String, Boolean>) {
        viewModelScope.launch {
            prefsRepo.setExerciseEnabled(enabled)
            loadDashboard()
        }
    }

    fun setGoalLevel(level: Int) {
        if (level < 1) return
        viewModelScope.launch {
            prefsRepo.setGoalLevel(level)
            loadDashboard()
        }
    }

    fun setDayNumberOffset(offset: Int) {
        viewModelScope.launch {
            prefsRepo.setDayNumberOffset(offset.coerceAtLeast(0))
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

    private fun cancelNotificationsOnCompletion() {
        viewModelScope.launch {
            _uiState
                .map { it.allCompleted }
                .distinctUntilChanged()
                .filter { it }
                .collect { NotificationHelper.cancelTrainingReminders(getApplication()) }
        }
    }

    private fun collectAccelThresholds() {
        viewModelScope.launch {
            combine(
                prefsRepo.accelThreshold("Push-ups"),
                prefsRepo.accelThreshold("Sit-ups"),
                prefsRepo.accelThreshold("Squats")
            ) { pushUps, sitUps, squats ->
                mapOf("Push-ups" to pushUps, "Sit-ups" to sitUps, "Squats" to squats)
            }.collect { _accelThresholds.value = it }
        }
    }

    fun saveAccelThreshold(exerciseName: String, threshold: Float) {
        viewModelScope.launch {
            prefsRepo.setAccelThreshold(exerciseName, threshold)
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
        goalLevel: Int,
        completions: List<DailyCompletion>,
        increments: Map<String, Float>,
        enabled: Map<String, Boolean>,
        baseReps: Map<String, Int?>
    ): List<ExerciseState> {
        val completionMap = completions.associateBy { it.exercise }
        return ExerciseTargets.forDay(goalLevel, increments, baseReps).map { (name, target) ->
            val completion = completionMap[name]
            ExerciseState(
                name = name,
                targetCount = target,
                completedCount = completion?.completedCount ?: 0,
                isCompleted = completion?.completed ?: false,
                isDisabled = enabled[name] == false
            )
        }
    }
}
