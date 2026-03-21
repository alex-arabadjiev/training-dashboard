package com.example.trainingdashboard

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trainingdashboard.data.db.AppDatabase
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletionDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun upsertAndReadCompletion() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", false, 5))

        val results = dao.getCompletionsForDay(1).first()
        assertEquals(1, results.size)
        assertEquals(5, results[0].completedCount)
        assertEquals(false, results[0].completed)
    }

    @Test
    fun upsertUpdatesExistingRecord() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", false, 3))
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", true, 10))

        val results = dao.getCompletionsForDay(1).first()
        assertEquals(1, results.size)
        assertEquals(10, results[0].completedCount)
        assertEquals(true, results[0].completed)
    }

    @Test
    fun snapshotReturnsCurrentState() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(5, "Push-ups", true, 5))
        dao.upsertCompletion(DailyCompletion(5, "Sit-ups", false, 3))
        dao.upsertCompletion(DailyCompletion(5, "Squats", false, 0))

        val snapshot = dao.getCompletionsForDaySnapshot(5)
        assertEquals(3, snapshot.size)
    }

    @Test
    fun snapshotReturnsEmptyForUnknownDay() = runTest {
        val dao = db.completionDao()
        val snapshot = dao.getCompletionsForDaySnapshot(999)
        assertTrue(snapshot.isEmpty())
    }

    @Test
    fun fullyCompletedDaysRequiresAllThreeExercises() = runTest {
        val dao = db.completionDao()

        // Day 1: only 2 exercises completed
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", true, 1))
        dao.upsertCompletion(DailyCompletion(1, "Sit-ups", true, 2))

        // Day 2: all 3 completed
        dao.upsertCompletion(DailyCompletion(2, "Push-ups", true, 2))
        dao.upsertCompletion(DailyCompletion(2, "Sit-ups", true, 4))
        dao.upsertCompletion(DailyCompletion(2, "Squats", true, 6))

        val completedDays = dao.getFullyCompletedDays()
        assertEquals(listOf(2), completedDays)
    }

    @Test
    fun updateCountSetsCompletedFlag() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(3, "Squats", false, 0))

        dao.updateCount(3, "Squats", 9, true)

        val snapshot = dao.getCompletionsForDaySnapshot(3)
        assertEquals(9, snapshot[0].completedCount)
        assertEquals(true, snapshot[0].completed)
    }
}
