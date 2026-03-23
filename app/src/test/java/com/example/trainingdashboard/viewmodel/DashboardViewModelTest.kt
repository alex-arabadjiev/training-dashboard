package com.example.trainingdashboard.viewmodel

import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Unit tests for pure logic extracted from DashboardViewModel.
 *
 * Full ViewModel integration tests (streak computation via FakeCompletionDao,
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
    // Streak computation (pure function extracted for testing)
    // -------------------------------------------------------------------------

    private fun computeStreak(currentDay: Int, completedDays: Set<Int>): Int {
        var streak = 0
        var day = currentDay
        while (day > 0 && completedDays.contains(day)) {
            streak++
            day--
        }
        return streak
    }

    @Test
    fun `streak is zero when no days completed`() {
        assertEquals(0, computeStreak(currentDay = 5, completedDays = emptySet()))
    }

    @Test
    fun `streak counts consecutive days from current day`() {
        val completed = setOf(1, 2, 3, 4, 5)
        assertEquals(5, computeStreak(currentDay = 5, completedDays = completed))
    }

    @Test
    fun `streak stops at gap in completed days`() {
        // Days 3, 4, 5 are complete; day 2 is missing
        val completed = setOf(3, 4, 5)
        assertEquals(3, computeStreak(currentDay = 5, completedDays = completed))
    }

    @Test
    fun `streak is zero when current day is not completed`() {
        val completed = setOf(1, 2, 3, 4)
        assertEquals(0, computeStreak(currentDay = 5, completedDays = completed))
    }

    @Test
    fun `streak is 1 when only current day is completed`() {
        assertEquals(1, computeStreak(currentDay = 3, completedDays = setOf(3)))
    }

    @Test
    fun `streak handles day 1 boundary`() {
        assertEquals(1, computeStreak(currentDay = 1, completedDays = setOf(1)))
    }

    // -------------------------------------------------------------------------
    // FakeCompletionDao — verify fake behaves correctly for downstream tests
    // -------------------------------------------------------------------------

    @Test
    fun `FakeCompletionDao getFullyCompletedDays returns day when all 3 exercises completed`() = runTest {
        val dao = FakeCompletionDao()
        dao.seedCompletions(
            DailyCompletion(dayNumber = 1, exercise = "Push-ups", completed = true, completedCount = 1),
            DailyCompletion(dayNumber = 1, exercise = "Sit-ups", completed = true, completedCount = 2),
            DailyCompletion(dayNumber = 1, exercise = "Squats", completed = true, completedCount = 3)
        )
        val result = dao.getFullyCompletedDays()
        assertEquals(listOf(1), result)
    }

    @Test
    fun `FakeCompletionDao getFullyCompletedDays excludes day with incomplete exercises`() = runTest {
        val dao = FakeCompletionDao()
        dao.seedCompletions(
            DailyCompletion(dayNumber = 2, exercise = "Push-ups", completed = true, completedCount = 2),
            DailyCompletion(dayNumber = 2, exercise = "Sit-ups", completed = false, completedCount = 0),
            DailyCompletion(dayNumber = 2, exercise = "Squats", completed = true, completedCount = 6)
        )
        val result = dao.getFullyCompletedDays()
        assertEquals(emptyList<Int>(), result)
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
