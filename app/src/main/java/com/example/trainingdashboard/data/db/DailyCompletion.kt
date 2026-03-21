package com.example.trainingdashboard.data.db

import androidx.room.Entity

@Entity(tableName = "daily_completions", primaryKeys = ["dayNumber", "exercise"])
data class DailyCompletion(
    val dayNumber: Int,
    val exercise: String,
    val completed: Boolean,
    val completedCount: Int = 0
)
