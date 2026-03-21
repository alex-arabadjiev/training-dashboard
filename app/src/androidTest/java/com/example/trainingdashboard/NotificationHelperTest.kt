package com.example.trainingdashboard

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.trainingdashboard.data.db.AppDatabase
import com.example.trainingdashboard.data.db.DailyCompletion
import com.example.trainingdashboard.notification.CompletionState
import com.example.trainingdashboard.notification.NotificationHelper
import com.example.trainingdashboard.notification.NotificationStage
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationHelperTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        // Replace the singleton so NotificationHelper uses our in-memory DB
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
    fun completionState_allDone_returnsAllDone() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = db.completionDao()

        // Day 5: targets are 5, 10, 15
        dao.upsertCompletion(DailyCompletion(5, "Push-ups", true, 5))
        dao.upsertCompletion(DailyCompletion(5, "Sit-ups", true, 10))
        dao.upsertCompletion(DailyCompletion(5, "Squats", true, 15))

        val state = NotificationHelper.getCompletionState(context, 5)
        assertTrue("Expected AllDone", state is CompletionState.AllDone)
    }

    @Test
    fun completionState_noProgress_returnsZeroPercent() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // No completions inserted for day 3

        val state = NotificationHelper.getCompletionState(context, 3)
        assertTrue("Expected Progress", state is CompletionState.Progress)
        val progress = state as CompletionState.Progress
        assertEquals(0, progress.overallPercent)
        assertEquals(3, progress.dayNumber)
    }

    @Test
    fun completionState_partialProgress_calculatesCorrectPercent() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = db.completionDao()

        // Day 10: targets are 10, 20, 30 = total 60
        dao.upsertCompletion(DailyCompletion(10, "Push-ups", true, 10))  // 10/10
        dao.upsertCompletion(DailyCompletion(10, "Sit-ups", false, 10)) // 10/20
        dao.upsertCompletion(DailyCompletion(10, "Squats", false, 0))   // 0/30
        // Total: 20/60 = 33%

        val state = NotificationHelper.getCompletionState(context, 10)
        val progress = state as CompletionState.Progress
        assertEquals(33, progress.overallPercent)
        assertEquals(3, progress.exerciseSummaries.size)
    }

    @Test
    fun completionState_nearlyDone_returns90PlusPercent() = runTest {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dao = db.completionDao()

        // Day 2: targets are 2, 4, 6 = total 12
        dao.upsertCompletion(DailyCompletion(2, "Push-ups", true, 2))   // done
        dao.upsertCompletion(DailyCompletion(2, "Sit-ups", true, 4))    // done
        dao.upsertCompletion(DailyCompletion(2, "Squats", false, 5))    // 5/6
        // Total: 11/12 = 91%

        val state = NotificationHelper.getCompletionState(context, 2)
        val progress = state as CompletionState.Progress
        assertEquals(91, progress.overallPercent)
    }

    @Test
    fun messageForStage_morningZeroPercent_containsTimeToTrain() {
        val state = CompletionState.Progress(
            dayNumber = 5,
            overallPercent = 0,
            exerciseSummaries = listOf(
                com.example.trainingdashboard.notification.ExerciseSummary("Push-ups", 0, 5),
                com.example.trainingdashboard.notification.ExerciseSummary("Sit-ups", 0, 10),
                com.example.trainingdashboard.notification.ExerciseSummary("Squats", 0, 15),
            )
        )
        val (title, _) = NotificationHelper.buildMessageForStage(state, NotificationStage.MORNING)
        assertTrue("Title should say 'Time to train!', got: $title", title.contains("Time to train"))
    }

    @Test
    fun messageForStage_afternoonPartial_containsPercentage() {
        val state = CompletionState.Progress(
            dayNumber = 5,
            overallPercent = 40,
            exerciseSummaries = listOf(
                com.example.trainingdashboard.notification.ExerciseSummary("Push-ups", 5, 5),
                com.example.trainingdashboard.notification.ExerciseSummary("Sit-ups", 4, 10),
                com.example.trainingdashboard.notification.ExerciseSummary("Squats", 3, 15),
            )
        )
        val (title, _) = NotificationHelper.buildMessageForStage(state, NotificationStage.AFTERNOON)
        assertTrue("Title should contain '40%', got: $title", title.contains("40%"))
    }

    @Test
    fun messageForStage_eveningNearlyDone_mentionsFinishing() {
        val state = CompletionState.Progress(
            dayNumber = 3,
            overallPercent = 95,
            exerciseSummaries = listOf(
                com.example.trainingdashboard.notification.ExerciseSummary("Push-ups", 3, 3),
                com.example.trainingdashboard.notification.ExerciseSummary("Sit-ups", 6, 6),
                com.example.trainingdashboard.notification.ExerciseSummary("Squats", 8, 9),
            )
        )
        val (title, _) = NotificationHelper.buildMessageForStage(state, NotificationStage.EVENING)
        assertTrue("Title should mention finishing, got: $title",
            title.contains("few more") || title.contains("reps"))
    }

    @Test
    fun exerciseSummary_remainingCalculation() {
        val summary = com.example.trainingdashboard.notification.ExerciseSummary("Squats", 7, 15)
        assertEquals(8, summary.remaining)
        assertEquals(false, summary.isDone)

        val doneSummary = com.example.trainingdashboard.notification.ExerciseSummary("Push-ups", 5, 5)
        assertEquals(0, doneSummary.remaining)
        assertEquals(true, doneSummary.isDone)
    }
}
