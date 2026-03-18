package com.example.trainingdashboard.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
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
}
