package com.example.trainingdashboard.data

object ExerciseTargets {
    fun forDay(dayNumber: Int): List<Pair<String, Int>> = listOf(
        "Push-ups" to dayNumber,
        "Sit-ups" to dayNumber * 2,
        "Squats" to dayNumber * 3
    )
}
