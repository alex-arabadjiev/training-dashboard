package com.example.trainingdashboard.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.unit.ColorProvider
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.trainingdashboard.MainActivity
import com.example.trainingdashboard.R
import com.example.trainingdashboard.data.ExerciseTargets
import com.example.trainingdashboard.data.GoalTransition
import com.example.trainingdashboard.data.PreferencesRepository
import com.example.trainingdashboard.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Kinetic design tokens — two-panel semi-transparent widget design
private val LeftPanelBg   = Color(0xE0131313) // ~88% opacity — darker left panel
private val RightPanelBg  = Color(0x73131313) // ~45% opacity — more transparent right panel
private val DividerColor  = Color(0x1AFFFFFF) // subtle white divider between panels
private val SurfaceColor  = Color(0xFF222222) // KineticSurfaceContainerHigh (level pill)
private val GreenAccent   = Color(0xFFC3F400) // KineticGreen
private val TextPrimary   = Color(0xFFE8E8E8) // KineticOnSurface
private val TextSecondary = Color(0xFF9E9E9E) // KineticOnSurfaceVariant
private val DimColor      = Color(0xFF3A3A3A) // KineticOutline (progress bar segments)
private val SquareIdleBg  = Color(0x33FFFFFF) // semi-transparent white for idle status squares

class TrainingWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadWidgetData(context)
        provideContent { WidgetContent(data) }
    }

    private suspend fun loadWidgetData(context: Context): WidgetData? {
        val dao = AppDatabase.getInstance(context).completionDao()
        val prefs = PreferencesRepository(context)
        val startDate = prefs.startDate.first() ?: return null
        val goalLevel = prefs.goalLevel.first() ?: 1
        val dayOffset = prefs.dayNumberOffset.first()
        val todayCalendarDay = ChronoUnit.DAYS.between(startDate, LocalDate.now()).toInt() + 1
        val todayCompletions = dao.getCompletionsForDaySnapshot(todayCalendarDay)
        val allCompletions = dao.getAllCompletedExercises()
        val activeDayCount = GoalTransition.computeActiveDayCount(allCompletions)
        val completionMap = todayCompletions.associateBy { it.exercise }
        val exercises = ExerciseTargets.forDay(goalLevel).map { (name, target) ->
            val row = completionMap[name]
            WidgetExercise(
                name = name,
                target = target,
                completedCount = row?.completedCount ?: 0,
                isDone = row?.completed ?: false
            )
        }
        return WidgetData(
            dayNumber = activeDayCount + dayOffset,
            goalLevel = goalLevel,
            exercises = exercises
        )
    }
}

private data class WidgetData(
    val dayNumber: Int,
    val goalLevel: Int,
    val exercises: List<WidgetExercise>
)

private data class WidgetExercise(
    val name: String,
    val target: Int,
    val completedCount: Int,
    val isDone: Boolean
)

@Composable
private fun WidgetContent(data: WidgetData?) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(LeftPanelBg)
            .cornerRadius(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (data == null) {
            Text(
                text = "OPEN APP TO START",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.padding(12.dp)
            )
            return@Row
        }

        val allDone = data.exercises.all { it.isDone }
        val anyStarted = data.exercises.any { it.completedCount > 0 }
        val totalReps = data.exercises.sumOf { it.target }
        val completedReps = data.exercises.sumOf { minOf(it.completedCount, it.target) }
        val progressPct = if (totalReps > 0) (completedReps * 100) / totalReps else 0
        val filledSegments = progressPct / 10

        // 35/65 split: left panel is narrower (just TRAINING + DAY N),
        // right panel is wider (squares + level + progress)
        val accentBarWidth = 5.dp
        val totalWidth = LocalSize.current.width
        val isCompact = totalWidth < 200.dp   // 2×1 breakpoint
        val leftPanelWidth = totalWidth * (if (isCompact) 0.50f else 0.35f) - accentBarWidth
        val rightPanelWidth = totalWidth * (if (isCompact) 0.50f else 0.65f)

        // Left green accent bar — flush to edge, clipped by root cornerRadius
        Box(
            modifier = GlanceModifier
                .width(accentBarWidth)
                .fillMaxHeight()
                .background(GreenAccent)
        ) {}

        // Left panel: TRAINING + DAY N (inherits LeftPanelBg from root)
        Column(
            modifier = GlanceModifier
                .width(leftPanelWidth)
                .fillMaxHeight()
                .padding(start = 10.dp, end = 8.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRAINING",
                style = TextStyle(
                    color = ColorProvider(TextSecondary),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "DAY ${data.dayNumber}",
                style = TextStyle(
                    color = ColorProvider(TextPrimary),
                    fontSize = if (isCompact) 18.sp else 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )
            )
        }

        // Divider between the two panels
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DividerColor)
        ) {}

        // Right panel: layout differs between compact (2×1) and normal (3×1 / 4×1)
        if (isCompact) {
            // Compact layout: single column — level pill → squares → progress bar + %
            Column(
                modifier = GlanceModifier
                    .width(rightPanelWidth)
                    .fillMaxHeight()
                    .background(RightPanelBg)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level pill — top
                Row(
                    modifier = GlanceModifier
                        .background(SurfaceColor)
                        .cornerRadius(50.dp)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_flame),
                        contentDescription = null,
                        modifier = GlanceModifier.width(10.dp).height(10.dp),
                        colorFilter = ColorFilter.tint(ColorProvider(GreenAccent))
                    )
                    Spacer(GlanceModifier.width(3.dp))
                    Text(
                        text = "LEVEL ${data.goalLevel}",
                        style = TextStyle(
                            color = ColorProvider(TextPrimary),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(GlanceModifier.height(5.dp))
                // Status squares — middle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    data.exercises.forEachIndexed { index, exercise ->
                        if (index > 0) Spacer(GlanceModifier.width(5.dp))
                        Box(
                            modifier = GlanceModifier
                                .width(14.dp)
                                .height(14.dp)
                                .background(if (exercise.isDone) GreenAccent else SquareIdleBg)
                                .cornerRadius(3.dp)
                        ) {}
                    }
                }
                Spacer(GlanceModifier.height(5.dp))
                // Progress bar + % — bottom
                // Width pinned to squares row width (3×14dp + 2×5dp = 52dp) so it doesn't overrun
                val squaresRowWidth = (data.exercises.size * 14 + (data.exercises.size - 1) * 5).dp
                Row(
                    modifier = GlanceModifier.width(squaresRowWidth).height(2.dp)
                ) {
                    repeat(10) { i ->
                        Box(
                            modifier = GlanceModifier
                                .defaultWeight()
                                .fillMaxHeight()
                                .background(if (i < filledSegments) GreenAccent else DimColor)
                        ) {}
                    }
                }
                Spacer(GlanceModifier.height(3.dp))
                Text(
                    text = "$progressPct% COMPLETE",
                    style = TextStyle(
                        color = ColorProvider(if (allDone) GreenAccent else TextSecondary),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        } else {
            // Normal layout (3×1 / 4×1): centre + right columns side by side
            Row(
                modifier = GlanceModifier
                    .width(rightPanelWidth)
                    .fillMaxHeight()
                    .background(RightPanelBg)
                    .cornerRadius(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Centre section: exercise status squares + state label
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        data.exercises.forEachIndexed { index, exercise ->
                            if (index > 0) Spacer(GlanceModifier.width(5.dp))
                            Box(
                                modifier = GlanceModifier
                                    .width(14.dp)
                                    .height(14.dp)
                                    .background(if (exercise.isDone) GreenAccent else SquareIdleBg)
                                    .cornerRadius(3.dp)
                            ) {}
                        }
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        text = when {
                            allDone -> "ALL DONE"
                            anyStarted -> "IN PROGRESS"
                            else -> "AWAITING INPUT"
                        },
                        style = TextStyle(
                            color = ColorProvider(if (allDone) GreenAccent else TextSecondary),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }

                // Right section: level pill + progress bar + % label
                Column(
                    modifier = GlanceModifier
                        .defaultWeight()
                        .fillMaxHeight()
                        .padding(start = 4.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = GlanceModifier
                            .background(SurfaceColor)
                            .cornerRadius(50.dp)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_flame),
                            contentDescription = null,
                            modifier = GlanceModifier.width(10.dp).height(10.dp),
                            colorFilter = ColorFilter.tint(ColorProvider(GreenAccent))
                        )
                        Spacer(GlanceModifier.width(3.dp))
                        Text(
                            text = "LEVEL ${data.goalLevel}",
                            style = TextStyle(
                                color = ColorProvider(TextPrimary),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(GlanceModifier.height(4.dp))
                    // Segmented progress bar — 10 equal segments
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().height(2.dp)
                    ) {
                        repeat(10) { i ->
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .fillMaxHeight()
                                    .background(if (i < filledSegments) GreenAccent else DimColor)
                            ) {}
                        }
                    }
                    Spacer(GlanceModifier.height(3.dp))
                    Text(
                        text = "$progressPct% COMPLETE",
                        style = TextStyle(
                            color = ColorProvider(if (allDone) GreenAccent else TextSecondary),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
