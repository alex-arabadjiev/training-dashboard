# Training Dashboard

An Android app that tracks a simple progressive daily exercise routine. You start with 1 push-up, 2 sit-ups, and 3 squats on Day 1 — each day the targets increase by +1, +2, and +3 respectively.

## Features

- **Daily goal dashboard** — Shows your current day number and exercise targets
- **Tap to complete** — Mark each exercise done; cards turn green with animated feedback
- **Streak tracking** — Completion banner with consecutive-day streak count when all exercises are finished
- **Morning reminder** — Configurable daily notification via WorkManager (default: 8:00 AM)
- **Persistent progress** — Completion state and preferences survive app restarts

## Goal Progression

| Day | Push-ups | Sit-ups | Squats |
|-----|----------|---------|--------|
| 1   | 1        | 2       | 3      |
| 2   | 2        | 4       | 6      |
| 10  | 10       | 20      | 30     |
| N   | N        | 2N      | 3N     |

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
