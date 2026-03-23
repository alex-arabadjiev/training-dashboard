package com.example.trainingdashboard.viewmodel

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate

/**
 * Test double for PreferencesRepository. Exposes the same property names and
 * suspend setters used by DashboardViewModel, backed by MutableStateFlows so
 * tests can pre-seed values and observe changes without a real Context.
 *
 * NOTE: DashboardViewModel takes a concrete PreferencesRepository, so full
 * ViewModel integration tests require Robolectric. This fake is used for
 * testing pure logic extracted from the ViewModel directly.
 */
class FakePreferencesRepository {

    private val _startDate = MutableStateFlow<LocalDate?>(null)
    val startDate: Flow<LocalDate?> = _startDate

    private val _reminderHour = MutableStateFlow(8)
    val reminderHour: Flow<Int> = _reminderHour

    private val _reminderMinute = MutableStateFlow(0)
    val reminderMinute: Flow<Int> = _reminderMinute

    private val _afternoonNudgeHour = MutableStateFlow(14)
    val afternoonNudgeHour: Flow<Int> = _afternoonNudgeHour

    private val _afternoonNudgeMinute = MutableStateFlow(0)
    val afternoonNudgeMinute: Flow<Int> = _afternoonNudgeMinute

    private val _eveningInterruptHour = MutableStateFlow(20)
    val eveningInterruptHour: Flow<Int> = _eveningInterruptHour

    private val _eveningInterruptMinute = MutableStateFlow(0)
    val eveningInterruptMinute: Flow<Int> = _eveningInterruptMinute

    private val _adaptiveTimingEnabled = MutableStateFlow(false)
    val adaptiveTimingEnabled: Flow<Boolean> = _adaptiveTimingEnabled

    suspend fun setStartDate(date: LocalDate) {
        _startDate.value = date
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        _reminderHour.value = hour
        _reminderMinute.value = minute
    }

    suspend fun setAfternoonNudgeTime(hour: Int, minute: Int) {
        _afternoonNudgeHour.value = hour
        _afternoonNudgeMinute.value = minute
    }

    suspend fun setEveningInterruptTime(hour: Int, minute: Int) {
        _eveningInterruptHour.value = hour
        _eveningInterruptMinute.value = minute
    }

    suspend fun setAdaptiveTimingEnabled(enabled: Boolean) {
        _adaptiveTimingEnabled.value = enabled
    }

    /** Seed a start date so tests can bypass the null-check in ensureStartDate(). */
    fun seedStartDate(date: LocalDate) {
        _startDate.value = date
    }
}
