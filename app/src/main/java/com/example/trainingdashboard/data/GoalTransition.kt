package com.example.trainingdashboard.data

import com.example.trainingdashboard.data.db.DailyCompletion

object GoalTransition {

    val PROGRESS_HOLD_THRESHOLD = 1.0 / 3.0
    const val GOAL_LEVEL_FLOOR = 1

    /**
     * Computes progress for a given day's completions against the current goal level.
     *
     * Each exercise's weight equals its increment value. Total weight = sum of all increments.
     * Progress = sum of completed exercise weights / total weight.
     * Returns 0.0 when all increments are zero (no exercises enabled).
     */
    fun computeProgress(
        completions: List<DailyCompletion>,
        goalLevel: Int,
        increments: Map<String, Float> = ExerciseTargets.DEFAULT_INCREMENTS
    ): Double {
        if (goalLevel <= 0) return 0.0
        val totalWeight = increments.values.sumOf { it.toDouble() }
        if (totalWeight == 0.0) return 0.0
        val completedWeight = completions
            .filter { it.completed }
            .sumOf { (increments[it.exercise] ?: 0f).toDouble() }
        return completedWeight / totalWeight
    }

    /**
     * Computes the next goal level given the current level and progress for a day.
     *
     * - 100% progress (all enabled exercises completed) → N+1
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
     * A day's weight is computed using the current increments map.
     * Returns 0 when all increments are zero.
     */
    fun computeActiveDayCount(
        allCompleted: List<DailyCompletion>,
        increments: Map<String, Float> = ExerciseTargets.DEFAULT_INCREMENTS
    ): Int {
        val totalWeight = increments.values.sumOf { it.toDouble() }
        if (totalWeight == 0.0) return 0
        val activeThreshold = totalWeight * PROGRESS_HOLD_THRESHOLD
        return allCompleted
            .filter { it.completed }
            .groupBy { it.dayNumber }
            .count { (_, dayCompletions) ->
                dayCompletions.sumOf { (increments[it.exercise] ?: 0f).toDouble() } >= activeThreshold
            }
    }
}
