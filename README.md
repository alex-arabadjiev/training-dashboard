# Training Dashboard

An Android app that tracks a progressive daily exercise routine with adaptive goal setting. You start at goal level 1 — targets increase as you hit your goals, hold when you make a solid effort, and ease back slightly on rough days.

## Features

- **Daily goal dashboard** — Shows your training day count and today's exercise targets
- **Tap to complete** — Mark each exercise done; cards turn green with animated feedback
- **Adaptive goals** — Goal level adjusts automatically based on yesterday's performance
- **Morning reminder** — Configurable daily notification via WorkManager (default: 8:00 AM)
- **Persistent progress** — Completion state and preferences survive app restarts

## Goal Progression

Targets are based on a goal level N that adapts each day:

| Goal level | Push-ups | Sit-ups | Squats |
|------------|----------|---------|--------|
| 1          | 1        | 2       | 3      |
| 2          | 2        | 4       | 6      |
| 10         | 10       | 20      | 30     |
| N          | N        | 2N      | 3N     |

Goal level transitions daily based on the previous day's weighted rep completion:
- **100%** → level up
- **33–99%** → hold
- **<33%** → level down (minimum: 1)

**Day N** in the UI counts days where you achieved at least 33% — not calendar days.

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room — daily completion persistence
- DataStore — preferences (start date, reminder time)
- WorkManager — scheduled reminder notifications
- Single-activity, single-screen architecture
- Min SDK 26 (Android 8.0)

## Building

Open the project in Android Studio and sync Gradle, or from the command line:

```bash
./gradlew assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Project Structure

```
app/src/main/java/com/example/trainingdashboard/
├── MainActivity.kt              # Entry point, notification permission request
├── TrainingApp.kt               # Application class, Room DB init
├── data/
│   ├── PreferencesRepository.kt # DataStore wrapper (start date, reminder time)
│   └── db/
│       ├── AppDatabase.kt       # Room database singleton
│       ├── CompletionDao.kt     # Queries for daily completions and streaks
│       └── DailyCompletion.kt   # Room entity
├── notification/
│   ├── ReminderScheduler.kt     # Schedules daily WorkManager reminder
│   └── ReminderWorker.kt       # Builds and posts the notification
├── ui/
│   ├── DashboardScreen.kt      # Main screen with time picker dialog
│   ├── components/
│   │   ├── CompletionBanner.kt  # Animated "All done!" + streak banner
│   │   ├── DayHeader.kt        # "Day X" header
│   │   └── ExerciseCard.kt     # Exercise card with completion toggle
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    └── DashboardViewModel.kt   # Day/goal computation, streak logic, state
```
