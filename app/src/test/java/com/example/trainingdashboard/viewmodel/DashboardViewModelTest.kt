package com.example.trainingdashboard.viewmodel

import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.GoalTransition
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Unit tests for pure logic extracted from DashboardViewModel.
 *
 * Full ViewModel integration tests (catch-up evaluation via FakeCompletionDao,
 * updateExerciseCount via the ViewModel itself) require Robolectric because
 * DashboardViewModel extends AndroidViewModel and PreferencesRepository takes a
 * Context. Add testImplementation("org.robolectric:robolectric:4.11.1") to
 * app/build.gradle.kts to enable those tests.
 */
class DashboardViewModelTest {

    // -------------------------------------------------------------------------
    // ExerciseTargets.forDay()
    // -------------------------------------------------------------------------

    @Test
    fun `forDay(1) returns correct targets`() {
        val targets = ExerciseTargets.forDay(1)
        assertEquals(3, targets.size)
        assertEquals("Push-ups" to 1, targets[0])
        assertEquals("Sit-ups" to 2, targets[1])
        assertEquals("Squats" to 3, targets[2])
    }

    @Test
    fun `forDay(5) scales targets linearly`() {
        val targets = ExerciseTargets.forDay(5)
        assertEquals("Push-ups" to 5, targets[0])
        assertEquals("Sit-ups" to 10, targets[1])
        assertEquals("Squats" to 15, targets[2])
    }

    @Test
    fun `forDay(0) returns zero targets`() {
        val targets = ExerciseTargets.forDay(0)
        assertEquals("Push-ups" to 0, targets[0])
        assertEquals("Sit-ups" to 0, targets[1])
        assertEquals("Squats" to 0, targets[2])
    }

    @Test
    fun `forDay large N scales linearly`() {
        val n = 100
        val targets = ExerciseTargets.forDay(n)
        assertEquals("Push-ups" to 100, targets[0])
        assertEquals("Sit-ups" to 200, targets[1])
        assertEquals("Squats" to 300, targets[2])
    }

    @Test
    fun `forDay with zero increment returns zero target`() {
        val increments = mapOf("Push-ups" to 0f, "Sit-ups" to 2.0f, "Squats" to 3.0f)
        val targets = ExerciseTargets.forDay(5, increments)
        assertEquals("Push-ups" to 0, targets[0])
        assertEquals("Sit-ups" to 10, targets[1])
        assertEquals("Squats" to 15, targets[2])
    }

    @Test
    fun `forDay with 0_5 increment rounds at odd levels`() {
        // Level 3 × 0.5 = 1.5 → rounds to 2
        val increments = mapOf("Push-ups" to 0.5f, "Sit-ups" to 0.5f, "Squats" to 0.5f)
        val targets = ExerciseTargets.forDay(3, increments)
        assertEquals("Push-ups" to 2, targets[0])
        assertEquals("Sit-ups" to 2, targets[1])
        assertEquals("Squats" to 2, targets[2])
    }

    @Test
    fun `forDay with 0_5 increment rounds at even levels`() {
        // Level 4 × 0.5 = 2.0 → exactly 2
        val increments = mapOf("Push-ups" to 0.5f, "Sit-ups" to 0.5f, "Squats" to 0.5f)
        val targets = ExerciseTargets.forDay(4, increments)
        assertEquals("Push-ups" to 2, targets[0])
        assertEquals("Sit-ups" to 2, targets[1])
        assertEquals("Squats" to 2, targets[2])
    }

    // -------------------------------------------------------------------------
    // Day number computation (mirrors DashboardViewModel.computeDayNumber)
    // -------------------------------------------------------------------------

    private fun computeDayNumber(startDate: LocalDate, today: LocalDate = LocalDate.now()): Int =
        ChronoUnit.DAYS.between(startDate, today).toInt() + 1

    @Test
    fun `computeDayNumber returns 1 when start date is today`() {
        val today = LocalDate.now()
        assertEquals(1, computeDayNumber(startDate = today, today = today))
    }

    @Test
    fun `computeDayNumber returns 2 when start date is yesterday`() {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        assertEquals(2, computeDayNumber(startDate = yesterday, today = today))
    }

    @Test
    fun `computeDayNumber returns correct value for arbitrary offset`() {
        val today = LocalDate.now()
        val startDate = today.minusDays(29)
        assertEquals(30, computeDayNumber(startDate = startDate, today = today))
    }

    // -------------------------------------------------------------------------
    // Clamp logic (mirrors DashboardViewModel.updateExerciseCount clamping)
    // -------------------------------------------------------------------------

    private fun clampCount(count: Int, target: Int): Int = count.coerceIn(0, target)

    @Test
    fun `clamp keeps value at target when count equals target`() {
        assertEquals(10, clampCount(10, 10))
    }

    @Test
    fun `clamp clamps count above target down to target`() {
        assertEquals(10, clampCount(15, 10))
    }

    @Test
    fun `clamp clamps negative count up to zero`() {
        assertEquals(0, clampCount(-5, 10))
    }

    @Test
    fun `clamp preserves value within valid range`() {
        assertEquals(7, clampCount(7, 10))
    }

    @Test
    fun `clamp with zero target always yields zero`() {
        assertEquals(0, clampCount(5, 0))
        assertEquals(0, clampCount(0, 0))
    }

    // -------------------------------------------------------------------------
    // Migration logic (using FakePreferencesRepository and FakeCompletionDao)
    // -------------------------------------------------------------------------

    /**
     * Simulates the migration logic from loadDashboard() to test it without
     * requiring AndroidViewModel / Robolectric.
     */
    private suspend fun runMigration(
        todayCalendarDay: Int,
        prefs: FakePreferencesRepository
    ): Int {
        var goalLevel = prefs.goalLevel.first()
        if (goalLevel == null) {
            val migratedLevel = maxOf(todayCalendarDay, GoalTransition.GOAL_LEVEL_FLOOR)
            prefs.setGoalLevel(migratedLevel)
            prefs.setLastEvaluatedDay(todayCalendarDay - 1)
            goalLevel = migratedLevel
        }
        return goalLevel
    }

    @Test
    fun migrationSetsGoalLevelToCalendarDay() = runTest {
        val prefs = FakePreferencesRepository()
        // goalLevel is null (unset) — simulates an existing user on day 15
        val todayCalendarDay = 15

        val resultLevel = runMigration(todayCalendarDay, prefs)

        assertEquals(15, resultLevel)
        assertEquals(15, prefs.goalLevel.first())
    }

    @Test
    fun migrationSetsLastEvaluatedDayToYesterday() = runTest {
        val prefs = FakePreferencesRepository()
        val todayCalendarDay = 15

        runMigration(todayCalendarDay, prefs)

        assertEquals(14, prefs.lastEvaluatedDay.first())
    }

    @Test
    fun newUserMigrationSetsGoalLevelToOne() = runTest {
        val prefs = FakePreferencesRepository()
        // Brand new user — today is day 1
        val todayCalendarDay = 1

        val resultLevel = runMigration(todayCalendarDay, prefs)

        assertEquals(1, resultLevel)
        assertEquals(1, prefs.goalLevel.first())
    }

    // -------------------------------------------------------------------------
    // FakeCompletionDao — verify fake behaves correctly for downstream tests
    // -------------------------------------------------------------------------

    @Test
    fun `FakeCompletionDao getAllCompletedExercises returns only completed entries`() = runTest {
        val dao = FakeCompletionDao()
        dao.seedCompletions(
            DailyCompletion(dayNumber = 1, exercise = "Push-ups", completed = true, completedCount = 1),
            DailyCompletion(dayNumber = 1, exercise = "Sit-ups", completed = false, completedCount = 0),
            DailyCompletion(dayNumber = 1, exercise = "Squats", completed = true, completedCount = 3)
        )
        val result = dao.getAllCompletedExercises()
        assertEquals(2, result.size)
        assertEquals(true, result.all { it.completed })
    }

    @Test
    fun `FakeCompletionDao getAllCompletedExercises returns empty when none completed`() = runTest {
        val dao = FakeCompletionDao()
        dao.seedCompletions(
            DailyCompletion(dayNumber = 1, exercise = "Push-ups", completed = false, completedCount = 0)
        )
        val result = dao.getAllCompletedExercises()
        assertEquals(emptyList<DailyCompletion>(), result)
    }

    @Test
    fun `FakeCompletionDao upsert overwrites existing entry`() = runTest {
        val dao = FakeCompletionDao()
        dao.upsertCompletion(DailyCompletion(dayNumber = 1, exercise = "Push-ups", completed = false, completedCount = 0))
        dao.upsertCompletion(DailyCompletion(dayNumber = 1, exercise = "Push-ups", completed = true, completedCount = 1))

        val snapshot = dao.getCompletionsForDaySnapshot(1)
        assertEquals(1, snapshot.size)
        assertEquals(true, snapshot[0].completed)
        assertEquals(1, snapshot[0].completedCount)
    }
}
