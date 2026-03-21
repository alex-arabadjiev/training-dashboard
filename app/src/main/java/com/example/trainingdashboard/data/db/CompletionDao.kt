package com.example.trainingdashboard.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CompletionDao {

    @Query("SELECT * FROM daily_completions WHERE dayNumber = :day")
    fun getCompletionsForDay(day: Int): Flow<List<DailyCompletion>>

    @Upsert
    suspend fun upsertCompletion(completion: DailyCompletion)

    @Query("UPDATE daily_completions SET completedCount = :count, completed = :completed WHERE dayNumber = :day AND exercise = :exercise")
    suspend fun updateCount(day: Int, exercise: String, count: Int, completed: Boolean)

    @Query(
        """
        SELECT DISTINCT dayNumber FROM daily_completions
        WHERE completed = 1
        GROUP BY dayNumber
        HAVING COUNT(*) = 3
        ORDER BY dayNumber DESC
        """
    )
    suspend fun getFullyCompletedDays(): List<Int>
}
