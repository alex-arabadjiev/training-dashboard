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

    @Query("SELECT * FROM daily_completions WHERE dayNumber = :day")
    suspend fun getCompletionsForDaySnapshot(day: Int): List<DailyCompletion>

    @Query("SELECT * FROM daily_completions WHERE completed = 1")
    suspend fun getAllCompletedExercises(): List<DailyCompletion>

    @Upsert
    suspend fun upsertCompletions(completions: List<DailyCompletion>)

    @Query("SELECT * FROM daily_completions ORDER BY dayNumber ASC")
    fun getExerciseHistory(): Flow<List<DailyCompletion>>
}
