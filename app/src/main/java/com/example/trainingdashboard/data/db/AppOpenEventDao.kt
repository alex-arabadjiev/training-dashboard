package com.example.trainingdashboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AppOpenEventDao {

    @Insert
    suspend fun insert(event: AppOpenEvent)

    @Query("SELECT * FROM app_open_events ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int = 14): List<AppOpenEvent>

    @Query("DELETE FROM app_open_events WHERE timestamp < :cutoffMillis")
    suspend fun deleteOlderThan(cutoffMillis: Long)
}
