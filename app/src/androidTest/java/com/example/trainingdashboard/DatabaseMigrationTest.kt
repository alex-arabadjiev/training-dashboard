package com.example.trainingdashboard

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trainingdashboard.data.db.AppDatabase
import com.example.trainingdashboard.data.db.DailyCompletion
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests that the database schema works correctly by exercising all tables
 * and columns through the DAO layer with an in-memory database.
 * This validates that the current schema (including all migration additions) is correct.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

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
    fun completedCountColumn_existsAndDefaultsToZero() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", true))

        val results = dao.getCompletionsForDaySnapshot(1)
        assertEquals(1, results.size)
        assertEquals(0, results[0].completedCount)
    }

    @Test
    fun completedCountColumn_storesNonZeroValues() = runTest {
        val dao = db.completionDao()
        dao.upsertCompletion(DailyCompletion(1, "Push-ups", false, 7))

        val results = dao.getCompletionsForDaySnapshot(1)
        assertEquals(7, results[0].completedCount)
    }

    @Test
    fun appOpenEventsTable_insertsAndQueries() = runTest {
        val dao = db.appOpenEventDao()
        dao.insert(com.example.trainingdashboard.data.db.AppOpenEvent(timestamp = 1000, hourOfDay = 14))
        dao.insert(com.example.trainingdashboard.data.db.AppOpenEvent(timestamp = 2000, hourOfDay = 15))

        val events = dao.getRecentEvents(10)
        assertEquals(2, events.size)
        // Most recent first
        assertEquals(15, events[0].hourOfDay)
        assertEquals(14, events[1].hourOfDay)
    }

    @Test
    fun appOpenEventsTable_deleteOlderThan() = runTest {
        val dao = db.appOpenEventDao()
        dao.insert(com.example.trainingdashboard.data.db.AppOpenEvent(timestamp = 100, hourOfDay = 8))
        dao.insert(com.example.trainingdashboard.data.db.AppOpenEvent(timestamp = 500, hourOfDay = 9))
        dao.insert(com.example.trainingdashboard.data.db.AppOpenEvent(timestamp = 1000, hourOfDay = 10))

        dao.deleteOlderThan(400)

        val events = dao.getRecentEvents(10)
        assertEquals(2, events.size)
        assertTrue(events.all { it.timestamp >= 400 })
    }
}
