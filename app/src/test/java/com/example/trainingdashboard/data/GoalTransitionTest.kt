package com.example.trainingdashboard.data

import com.example.trainingdashboard.data.db.DailyCompletion
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalTransitionTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun completion(day: Int, exercise: String, completed: Boolean): DailyCompletion =
        DailyCompletion(dayNumber = day, exercise = exercise, completed = completed, completedCount = 0)

    private fun allThree(day: Int): List<DailyCompletion> = listOf(
        completion(day, "Push-ups", true),
        completion(day, "Sit-ups", true),
        completion(day, "Squats", true)
    )

    private fun none(day: Int): List<DailyCompletion> = listOf(
        completion(day, "Push-ups", false),
        completion(day, "Sit-ups", false),
        completion(day, "Squats", false)
    )

    // -------------------------------------------------------------------------
    // Transition logic — nextLevel via computeProgress
    // -------------------------------------------------------------------------

    @Test
    fun levelIncreasesWhenAllThreeExercisesCompleted() {
        val completions = allThree(day = 1)
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(1.0, progress, 0.0001)
        assertEquals(6, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelHoldsWhenTwoExercisesCompleted() {
        // Push-ups (weight 1) + Sit-ups (weight 2) = 3/6 = 50% — hold
        val completions = listOf(
            completion(1, "Push-ups", true),
            completion(1, "Sit-ups", true),
            completion(1, "Squats", false)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(0.5, progress, 0.0001)
        assertEquals(5, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelHoldsWhenOnlySitUpsCompleted() {
        // Sit-ups weight 2 → 2/6 = 33.3% — exactly at threshold, hold
        val completions = listOf(
            completion(1, "Push-ups", false),
            completion(1, "Sit-ups", true),
            completion(1, "Squats", false)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(2.0 / 6.0, progress, 0.0001)
        assertEquals(5, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelHoldsWhenOnlySquatsCompleted() {
        // Squats weight 3 → 3/6 = 50% — hold
        val completions = listOf(
            completion(1, "Push-ups", false),
            completion(1, "Sit-ups", false),
            completion(1, "Squats", true)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(0.5, progress, 0.0001)
        assertEquals(5, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelDecreasesWhenOnlyPushUpsCompleted() {
        // Push-ups weight 1 → 1/6 ≈ 16.7% — below threshold, decrease
        val completions = listOf(
            completion(1, "Push-ups", true),
            completion(1, "Sit-ups", false),
            completion(1, "Squats", false)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(1.0 / 6.0, progress, 0.0001)
        assertEquals(4, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelDecreasesWhenNoExercisesCompleted() {
        val completions = none(day = 1)
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5)
        assertEquals(0.0, progress, 0.0001)
        assertEquals(4, GoalTransition.nextLevel(currentLevel = 5, progress = progress))
    }

    @Test
    fun levelFloorAtOne() {
        // Level 1 with 0 progress should remain at 1
        val completions = none(day = 1)
        val progress = GoalTransition.computeProgress(completions, goalLevel = 1)
        assertEquals(0.0, progress, 0.0001)
        assertEquals(1, GoalTransition.nextLevel(currentLevel = 1, progress = progress))
    }

    @Test
    fun multiDayCatchUpEvaluatesSequentially() {
        // 3 missed days from level 5: 5→4→3→2
        var level = 5
        repeat(3) {
            val completions = none(day = it + 1)
            val progress = GoalTransition.computeProgress(completions, goalLevel = level)
            level = GoalTransition.nextLevel(level, progress)
        }
        assertEquals(2, level)
    }

    @Test
    fun multiDayCatchUpRespectsFloor() {
        // 5 missed days from level 3: 3→2→1→1→1
        var level = 3
        repeat(5) {
            val completions = none(day = it + 1)
            val progress = GoalTransition.computeProgress(completions, goalLevel = level)
            level = GoalTransition.nextLevel(level, progress)
        }
        assertEquals(1, level)
    }

    // -------------------------------------------------------------------------
    // Active day count
    // -------------------------------------------------------------------------

    @Test
    fun activeDayCountIsZeroWithNoCompletions() {
        assertEquals(0, GoalTransition.computeActiveDayCount(emptyList()))
    }

    @Test
    fun activeDayCountExcludesPushUpsOnlyDays() {
        // Push-ups weight = 1, below threshold of 2
        val completions = listOf(completion(1, "Push-ups", true))
        assertEquals(0, GoalTransition.computeActiveDayCount(completions))
    }

    @Test
    fun activeDayCountIncludesSitUpsOnlyDays() {
        // Sit-ups weight = 2, exactly at threshold
        val completions = listOf(completion(1, "Sit-ups", true))
        assertEquals(1, GoalTransition.computeActiveDayCount(completions))
    }

    @Test
    fun activeDayCountIncludesSquatsOnlyDays() {
        // Squats weight = 3, above threshold
        val completions = listOf(completion(1, "Squats", true))
        assertEquals(1, GoalTransition.computeActiveDayCount(completions))
    }

    @Test
    fun activeDayCountIncludesFullyCompletedDays() {
        val completions = allThree(day = 1)
        assertEquals(1, GoalTransition.computeActiveDayCount(completions))
    }

    @Test
    fun activeDayCountWithMixedDays() {
        // Day 1: all three → active
        // Day 2: push-ups only → NOT active
        // Day 3: sit-ups only → active
        val completions = listOf(
            completion(1, "Push-ups", true),
            completion(1, "Sit-ups", true),
            completion(1, "Squats", true),
            completion(2, "Push-ups", true),
            completion(3, "Sit-ups", true)
        )
        assertEquals(2, GoalTransition.computeActiveDayCount(completions))
    }

    // -------------------------------------------------------------------------
    // Progress calculation
    // -------------------------------------------------------------------------

    @Test
    fun progressIsZeroWithNoCompletions() {
        val completions = none(day = 1)
        assertEquals(0.0, GoalTransition.computeProgress(completions, goalLevel = 5), 0.0001)
    }

    @Test
    fun progressIs100WithAllCompleted() {
        val completions = allThree(day = 1)
        assertEquals(1.0, GoalTransition.computeProgress(completions, goalLevel = 5), 0.0001)
    }

    @Test
    fun progressIsCorrectForPartialCompletion() {
        // Only squats completed: weight 3 / total weight 6 = 0.5
        val completions = listOf(
            completion(1, "Push-ups", false),
            completion(1, "Sit-ups", false),
            completion(1, "Squats", true)
        )
        assertEquals(0.5, GoalTransition.computeProgress(completions, goalLevel = 5), 0.0001)
    }

    // -------------------------------------------------------------------------
    // Custom increments — disabled exercises (increment = 0)
    // -------------------------------------------------------------------------

    @Test
    fun progressIsZeroWhenAllIncrementsAreZero() {
        val increments = mapOf("Push-ups" to 0f, "Sit-ups" to 0f, "Squats" to 0f)
        val completions = allThree(day = 1)
        assertEquals(0.0, GoalTransition.computeProgress(completions, goalLevel = 5, increments = increments), 0.0001)
    }

    @Test
    fun activeDayCountIsZeroWhenAllIncrementsAreZero() {
        val increments = mapOf("Push-ups" to 0f, "Sit-ups" to 0f, "Squats" to 0f)
        val completions = allThree(day = 1)
        assertEquals(0, GoalTransition.computeActiveDayCount(completions, increments = increments))
    }

    @Test
    fun progressIgnoresDisabledExercise() {
        // Squats disabled (increment 0), only Push-ups (1) and Sit-ups (2) active → total weight 3
        // Only Push-ups completed → 1/3 ≈ 33.3% — exactly at hold threshold
        val increments = mapOf("Push-ups" to 1.0f, "Sit-ups" to 2.0f, "Squats" to 0f)
        val completions = listOf(
            completion(1, "Push-ups", true),
            completion(1, "Sit-ups", false),
            completion(1, "Squats", false)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5, increments = increments)
        assertEquals(1.0 / 3.0, progress, 0.0001)
        assertEquals(5, GoalTransition.nextLevel(5, progress))
    }

    @Test
    fun activeDayCountWithDisabledExerciseUsesRemainingWeight() {
        // Squats disabled — threshold is 1/3 of (1+2) = 1.0
        // Day 1: Push-ups only (weight 1) → exactly at threshold → active
        val increments = mapOf("Push-ups" to 1.0f, "Sit-ups" to 2.0f, "Squats" to 0f)
        val completions = listOf(completion(1, "Push-ups", true))
        assertEquals(1, GoalTransition.computeActiveDayCount(completions, increments = increments))
    }

    @Test
    fun progressWithCustomIncrementsHalfStep() {
        // All three at increment 0.5 → total weight 1.5
        // Squats (0.5) completed → 0.5/1.5 ≈ 33.3% — hold
        val increments = mapOf("Push-ups" to 0.5f, "Sit-ups" to 0.5f, "Squats" to 0.5f)
        val completions = listOf(
            completion(1, "Push-ups", false),
            completion(1, "Sit-ups", false),
            completion(1, "Squats", true)
        )
        val progress = GoalTransition.computeProgress(completions, goalLevel = 5, increments = increments)
        assertEquals(1.0 / 3.0, progress, 0.0001)
        assertEquals(5, GoalTransition.nextLevel(5, progress))
    }
}
