package com.example.trainingdashboard.data

import kotlin.math.roundToInt

object ExerciseTargets {

    val EXERCISE_NAMES = listOf("Push-ups", "Sit-ups", "Squats")

    val DEFAULT_INCREMENTS: Map<String, Float> = mapOf(
        "Push-ups" to 1.0f,
        "Sit-ups"  to 2.0f,
        "Squats"   to 3.0f
    )

    fun forDay(
        goalLevel: Int,
        increments: Map<String, Float> = DEFAULT_INCREMENTS,
        baseReps: Map<String, Int?> = emptyMap()
    ): List<Pair<String, Int>> = EXERCISE_NAMES.map { name ->
        val increment = increments[name] ?: (DEFAULT_INCREMENTS[name] ?: 1.0f)
        val target = when {
            increment > 0f -> (goalLevel * increment).roundToInt()
            else -> baseReps[name]
                ?: (goalLevel * (DEFAULT_INCREMENTS[name] ?: 1.0f)).roundToInt()
        }
        name to target
    }
}
