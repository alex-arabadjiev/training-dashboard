package com.example.trainingdashboard.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_open_events")
data class AppOpenEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val hourOfDay: Int
)
