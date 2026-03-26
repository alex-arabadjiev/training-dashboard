package com.example.trainingdashboard.data

import com.example.trainingdashboard.data.db.DailyCompletion

object GoalTransition {

    val EXERCISE_WEIGHTS: Map<String, Int> = mapOf(
        "Push-ups" to 1,
        "Sit-ups" to 2,
        "Squats" to 3
    )

    const val ACTIVE_DAY_WEIGHT_THRESHOLD = 2
    val PROGRESS_HOLD_THRESHOLD = 1.0 / 3.0
    const val GOAL_LEVEL_FLOOR = 1

    /**
     * Computes progress for a given day's completions against the current goal level.
     *
     * Total target reps = 6 * goalLevel (1G push-ups + 2G sit-ups + 3G squats).
     * Progress = sum of completed exercise weights / 6.
     * Uses binary completion flag — if completed = true, adds that exercise's weight reps.
     */
    fun computeProgress(completions: List<DailyCompletion>, goalLevel: Int): Double {
        if (goalLevel <= 0) return 0.0
        val completedWeight = completions
            .filter { it.completed }
            .sumOf { EXERCISE_WEIGHTS[it.exercise] ?: 0 }
        val totalWeight = 6
        return completedWeight.toDouble() / totalWeight.toDouble()
    }

    /**
     * Computes the next goal level given the current level and progress for a day.
     *
     * - 100% progress (all 3 exercises completed) → N+1
     * - 33–99% progress (≥ 1/3) → hold at N
     * - <33% progress → N-1, floor at GOAL_LEVEL_FLOOR
     */
    fun nextLevel(currentLevel: Int, progress: Double): Int {
        return when {
            progress >= 1.0 -> currentLevel + 1
            progress >= PROGRESS_HOLD_THRESHOLD -> currentLevel
            else -> maxOf(currentLevel - 1, GOAL_LEVEL_FLOOR)
        }
    }

    /**
     * Counts "active days" — calendar days where the user achieved ≥33% progress.
     * A day is active if the sum of completed exercise weights is ≥ ACTIVE_DAY_WEIGHT_THRESHOLD (2).
     * Only rows with completed = true are counted.
     */
    fun computeActiveDayCount(allCompleted: List<DailyCompletion>): Int {
        return allCompleted
            .filter { it.completed }
            .groupBy { it.dayNumber }
            .count { (_, dayCompletions) ->
                dayCompletions.sumOf { EXERCISE_WEIGHTS[it.exercise] ?: 0 } >= ACTIVE_DAY_WEIGHT_THRESHOLD
            }
    }
}
