package com.example.trainingdashboard

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.trainingdashboard.data.db.AppDatabase
import com.example.trainingdashboard.data.db.DailyCompletion
import com.example.trainingdashboard.notification.AfternoonNudgeWorker
import com.example.trainingdashboard.notification.CompletionState
import com.example.trainingdashboard.notification.NotificationHelper
import com.example.trainingdashboard.notification.ReminderWorker
import com.example.trainingdashboard.notification.SnoozeWorker
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkerTest {

    private lateinit var db: AppDatabase
    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, db)
    }

    @After
    fun teardown() {
        db.close()
        val field = AppDatabase::class.java.getDeclaredField("INSTANCE")
        field.isAccessible = true
        field.set(null, null)
    }

    @Test
    fun morningWorker_alwaysReturnsSuccess() = runTest {
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun afternoonWorker_alwaysReturnsSuccess() = runTest {
        val worker = TestListenableWorkerBuilder<AfternoonNudgeWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun snoozeWorker_alwaysReturnsSuccess() = runTest {
        val worker = TestListenableWorkerBuilder<SnoozeWorker>(context).build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)
    }

    @Test
    fun completionState_allDone_workersWouldSkip() = runTest {
        val dayNumber = NotificationHelper.computeDayNumber(context)
        val dao = db.completionDao()

        dao.upsertCompletion(DailyCompletion(dayNumber, "Push-ups", true, dayNumber))
        dao.upsertCompletion(DailyCompletion(dayNumber, "Sit-ups", true, dayNumber * 2))
        dao.upsertCompletion(DailyCompletion(dayNumber, "Squats", true, dayNumber * 3))

        val state = NotificationHelper.getCompletionState(context, dayNumber)
        assertTrue("Workers should skip when all done", state is CompletionState.AllDone)
    }

    @Test
    fun completionState_noProgress_workersWouldNotify() = runTest {
        val dayNumber = NotificationHelper.computeDayNumber(context)

        val state = NotificationHelper.getCompletionState(context, dayNumber)
        assertTrue("Workers should notify when no progress", state is CompletionState.Progress)
        assertEquals(0, (state as CompletionState.Progress).overallPercent)
    }

    @Test
    fun completionState_partialProgress_workersWouldNotify() = runTest {
        val dayNumber = NotificationHelper.computeDayNumber(context)
        val dao = db.completionDao()

        dao.upsertCompletion(DailyCompletion(dayNumber, "Push-ups", true, dayNumber))
        // Sit-ups and Squats not done

        val state = NotificationHelper.getCompletionState(context, dayNumber)
        assertTrue("Workers should notify for partial", state is CompletionState.Progress)
        val progress = state as CompletionState.Progress
        assertTrue("Percent should be > 0", progress.overallPercent > 0)
        assertTrue("Percent should be < 100", progress.overallPercent < 100)
    }

    @Test
    fun completionState_nearlyDone_highPercentage() = runTest {
        val dayNumber = NotificationHelper.computeDayNumber(context)
        val dao = db.completionDao()

        dao.upsertCompletion(DailyCompletion(dayNumber, "Push-ups", true, dayNumber))
        dao.upsertCompletion(DailyCompletion(dayNumber, "Sit-ups", true, dayNumber * 2))
        dao.upsertCompletion(DailyCompletion(dayNumber, "Squats", false, dayNumber * 3 - 1))

        val state = NotificationHelper.getCompletionState(context, dayNumber)
        val progress = state as CompletionState.Progress
        assertTrue("Should be 50%+, was ${progress.overallPercent}%", progress.overallPercent >= 50)
    }
}
