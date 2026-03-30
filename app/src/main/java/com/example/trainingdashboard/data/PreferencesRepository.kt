package com.example.trainingdashboard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "training_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val START_DATE = stringPreferencesKey("start_date")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val AFTERNOON_NUDGE_HOUR = intPreferencesKey("afternoon_nudge_hour")
        val AFTERNOON_NUDGE_MINUTE = intPreferencesKey("afternoon_nudge_minute")
        val EVENING_INTERRUPT_HOUR = intPreferencesKey("evening_interrupt_hour")
        val EVENING_INTERRUPT_MINUTE = intPreferencesKey("evening_interrupt_minute")
        val ADAPTIVE_TIMING_ENABLED = booleanPreferencesKey("adaptive_timing_enabled")
        val GOAL_LEVEL = intPreferencesKey("goal_level")
        val LAST_EVALUATED_DAY = intPreferencesKey("last_evaluated_day")
        val ACCEL_THRESHOLD_PUSH_UPS = floatPreferencesKey("accel_threshold_push_ups")
        val ACCEL_THRESHOLD_SIT_UPS = floatPreferencesKey("accel_threshold_sit_ups")
        val ACCEL_THRESHOLD_SQUATS = floatPreferencesKey("accel_threshold_squats")
        val DAY_NUMBER_OFFSET = intPreferencesKey("day_number_offset")
    }

    val startDate: Flow<LocalDate?> = context.dataStore.data.map { prefs ->
        prefs[Keys.START_DATE]?.let { LocalDate.parse(it) }
    }

    val reminderHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_HOUR] ?: 8
    }

    val reminderMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_MINUTE] ?: 0
    }

    suspend fun setStartDate(date: LocalDate) {
        context.dataStore.edit { prefs ->
            prefs[Keys.START_DATE] = date.toString()
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.REMINDER_HOUR] = hour
            prefs[Keys.REMINDER_MINUTE] = minute
        }
    }

    val afternoonNudgeHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.AFTERNOON_NUDGE_HOUR] ?: 14
    }

    val afternoonNudgeMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.AFTERNOON_NUDGE_MINUTE] ?: 0
    }

    suspend fun setAfternoonNudgeTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AFTERNOON_NUDGE_HOUR] = hour
            prefs[Keys.AFTERNOON_NUDGE_MINUTE] = minute
        }
    }

    val eveningInterruptHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.EVENING_INTERRUPT_HOUR] ?: 20
    }

    val eveningInterruptMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.EVENING_INTERRUPT_MINUTE] ?: 0
    }

    suspend fun setEveningInterruptTime(hour: Int, minute: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.EVENING_INTERRUPT_HOUR] = hour
            prefs[Keys.EVENING_INTERRUPT_MINUTE] = minute
        }
    }

    val adaptiveTimingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ADAPTIVE_TIMING_ENABLED] ?: false
    }

    suspend fun setAdaptiveTimingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ADAPTIVE_TIMING_ENABLED] = enabled
        }
    }

    val goalLevel: Flow<Int?> = context.dataStore.data.map { prefs ->
        prefs[Keys.GOAL_LEVEL]
    }

    val lastEvaluatedDay: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_EVALUATED_DAY] ?: 0
    }

    suspend fun setGoalLevel(level: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOAL_LEVEL] = level
        }
    }

    suspend fun setLastEvaluatedDay(day: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_EVALUATED_DAY] = day
        }
    }

    val dayNumberOffset: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.DAY_NUMBER_OFFSET] ?: 0
    }

    suspend fun setDayNumberOffset(offset: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DAY_NUMBER_OFFSET] = offset
        }
    }

    fun accelThreshold(exerciseName: String): Flow<Float?> =
        context.dataStore.data.map { prefs -> prefs[accelThresholdKey(exerciseName)] }

    suspend fun setAccelThreshold(exerciseName: String, threshold: Float) {
        context.dataStore.edit { prefs -> prefs[accelThresholdKey(exerciseName)] = threshold }
    }

    suspend fun clearAccelThreshold(exerciseName: String) {
        context.dataStore.edit { prefs -> prefs.remove(accelThresholdKey(exerciseName)) }
    }

    private fun accelThresholdKey(exerciseName: String) = when (exerciseName) {
        "Push-ups" -> Keys.ACCEL_THRESHOLD_PUSH_UPS
        "Sit-ups"  -> Keys.ACCEL_THRESHOLD_SIT_UPS
        "Squats"   -> Keys.ACCEL_THRESHOLD_SQUATS
        else       -> throw IllegalArgumentException("Unknown exercise: $exerciseName")
    }
}
