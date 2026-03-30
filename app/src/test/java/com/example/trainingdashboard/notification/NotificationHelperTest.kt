package com.example.trainingdashboard.notification

import com.example.trainingdashboard.data.db.DailyCompletion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationHelperTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun completion(exercise: String, count: Int, completed: Boolean = count > 0) =
        DailyCompletion(dayNumber = 1, exercise = exercise, completed = completed, completedCount = count)

    private fun allCompleted(goalLevel: Int) = listOf(
        completion("Push-ups", goalLevel),
        completion("Sit-ups", goalLevel * 2),
        completion("Squats", goalLevel * 3),
    )

    private fun noCompletions() = emptyList<DailyCompletion>()

    // -------------------------------------------------------------------------
    // AllDone detection uses goalLevel, not dayNumber
    // -------------------------------------------------------------------------

    @Test
    fun allDoneWhenCompletedExactlyAtGoalLevel() {
        // Bug scenario: dayNumber=6, goalLevel=5, user has completed 5/10/15 reps.
        // Old code would use dayNumber=6 as target → 6/12/18 → NOT AllDone (fires notification).
        // Fixed code uses goalLevel=5 → 5/10/15 → AllDone (no notification).
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 6,
            goalLevel = 5,
            completions = allCompleted(5)
        )
        assertTrue("Expected AllDone when completions match goalLevel", state is CompletionState.AllDone)
    }

    @Test
    fun notAllDoneWhenCompletedAtGoalLevelMinusOne() {
        // User completed yesterday's targets (goalLevel-1) but not today's → should notify.
        val goalLevel = 5
        val completions = allCompleted(goalLevel - 1)  // 4/8/12 vs targets 5/10/15
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 5,
            goalLevel = goalLevel,
            completions = completions
        )
        assertTrue(state is CompletionState.Progress)
    }

    // -------------------------------------------------------------------------
    // Off-by-one regression: dayNumber = goalLevel + 1
    // -------------------------------------------------------------------------

    @Test
    fun noFalseNotificationWhenDayNumberOneAheadOfGoalLevel() {
        // The exact off-by-one scenario from the bug report:
        // dayNumber=N+1, goalLevel=N, user has completed N/2N/3N reps.
        // Old code: targets=N+1/2(N+1)/3(N+1) → remaining=1/2/3 → fires.
        // Fixed code: targets=N/2N/3N → AllDone → silent.
        val goalLevel = 4
        val state = NotificationHelper.computeCompletionState(
            dayNumber = goalLevel + 1,
            goalLevel = goalLevel,
            completions = allCompleted(goalLevel)
        )
        assertTrue(
            "Should be AllDone — dayNumber ahead of goalLevel must not cause false notification",
            state is CompletionState.AllDone
        )
    }

    @Test
    fun remainingCountsReflectGoalLevelNotDayNumber() {
        // When not all done, remaining reps must be based on goalLevel targets.
        // goalLevel=3 → targets 3/6/9.  User has done 0 reps.
        // Old code with dayNumber=5 would report targets 5/10/15.
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 5,
            goalLevel = 3,
            completions = noCompletions()
        ) as CompletionState.Progress

        val pushUps = state.exerciseSummaries.first { it.name == "Push-ups" }
        val sitUps = state.exerciseSummaries.first { it.name == "Sit-ups" }
        val squats = state.exerciseSummaries.first { it.name == "Squats" }

        assertEquals(3, pushUps.targetCount)
        assertEquals(6, sitUps.targetCount)
        assertEquals(9, squats.targetCount)
    }

    // -------------------------------------------------------------------------
    // Progress percentage
    // -------------------------------------------------------------------------

    @Test
    fun progressPercentageCalculatedAgainstGoalLevelTargets() {
        // goalLevel=4 → total target = 4+8+12 = 24.  User done 4+8+0 = 12 → 50%.
        val completions = listOf(
            completion("Push-ups", 4),
            completion("Sit-ups", 8),
            completion("Squats", 0, completed = false),
        )
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 7,
            goalLevel = 4,
            completions = completions
        ) as CompletionState.Progress

        assertEquals(50, state.overallPercent)
    }

    @Test
    fun zeroProgressWhenNoCompletions() {
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 3,
            goalLevel = 3,
            completions = noCompletions()
        ) as CompletionState.Progress

        assertEquals(0, state.overallPercent)
    }

    // -------------------------------------------------------------------------
    // dayNumber is preserved in the Progress payload (used for display)
    // -------------------------------------------------------------------------

    @Test
    fun dayNumberPreservedInProgressState() {
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 10,
            goalLevel = 7,
            completions = noCompletions()
        ) as CompletionState.Progress

        assertEquals(10, state.dayNumber)
    }

    // -------------------------------------------------------------------------
    // Partial completion
    // -------------------------------------------------------------------------

    @Test
    fun partiallyCompletedExercisesNotAllDone() {
        // Push-ups done, sit-ups and squats not started
        val completions = listOf(completion("Push-ups", 3))
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 3,
            goalLevel = 3,
            completions = completions
        )
        assertTrue(state is CompletionState.Progress)
    }

    @Test
    fun remainingIsZeroForCompletedExercise() {
        val goalLevel = 3
        val completions = listOf(
            completion("Push-ups", goalLevel),
            completion("Sit-ups", 0, completed = false),
            completion("Squats", 0, completed = false),
        )
        val state = NotificationHelper.computeCompletionState(
            dayNumber = 3,
            goalLevel = goalLevel,
            completions = completions
        ) as CompletionState.Progress

        val pushUps = state.exerciseSummaries.first { it.name == "Push-ups" }
        assertEquals(0, pushUps.remaining)
        assertTrue(pushUps.isDone)
    }
}
