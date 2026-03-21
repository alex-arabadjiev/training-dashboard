# CLAUDE.md — Training Dashboard

## Project Overview

Android app that tracks a progressive daily exercise routine. Daily targets scale linearly based on day number:
- Day N: N push-ups, 2N sit-ups, 3N squats

Features: exercise completion tracking, streak counter, daily reminder notifications, settings dialog.

## Tech Stack

- **Language:** Kotlin (JVM target 17)
- **UI:** Jetpack Compose with Material 3
- **Architecture:** Single-Activity, MVVM (ViewModel + StateFlow)
- **Database:** Room (KSP code generation)
- **Preferences:** DataStore
- **Background work:** WorkManager (daily reminder notifications)
- **Build:** Gradle 8.5, Kotlin DSL, AGP 8.2.2, Kotlin 1.9.22
- **SDK:** minSdk 26, targetSdk 34, compileSdk 34

## Project Structure

```
app/src/main/java/com/example/trainingdashboard/
├── MainActivity.kt              # Single activity entry point
├── TrainingApp.kt               # Application class
├── data/
│   ├── PreferencesRepository.kt # DataStore wrapper (start date, reminder time)
│   └── db/
│       ├── AppDatabase.kt       # Room database (single instance)
│       ├── CompletionDao.kt     # DAO with Flow-based queries
│       └── DailyCompletion.kt   # Entity: composite key (dayNumber, exercise)
├── notification/
│   ├── ReminderScheduler.kt     # WorkManager periodic scheduling
│   └── ReminderWorker.kt        # Notification builder
├── ui/
│   ├── DashboardScreen.kt       # Main screen composable
│   ├── components/
│   │   ├── CompletionBanner.kt  # "All done!" banner with streak
│   │   ├── DayHeader.kt         # Day number display
│   │   └── ExerciseCard.kt      # Tap-to-complete exercise card
│   └── theme/
│       ├── Color.kt             # Material 3 color definitions
│       ├── Theme.kt             # Light/dark theme config
│       └── Type.kt              # Typography definitions
└── viewmodel/
    └── DashboardViewModel.kt    # UI state, day computation, streak logic
```

## Build & Run

```bash
# Build debug APK
gradle assembleDebug

# Run tests
gradle test

# Build release APK (requires signing env vars)
gradle assembleRelease
```

The debug APK is output to `app/build/outputs/apk/debug/app-debug.apk`.

Note: This project uses the system `gradle` command, not `./gradlew` (wrapper jar is not checked in).

## Architecture Notes

- **Reactive data flow:** Room DAO returns `Flow<List<DailyCompletion>>` → ViewModel combines into `StateFlow<DashboardUiState>` → Compose collects state
- **Day calculation:** `ChronoUnit.DAYS` from stored start date to today; can be manually overridden via settings
- **Streak calculation:** Iterates backward from current day counting consecutive days where all 3 exercises are completed
- **Database design:** Composite primary key `(dayNumber, exercise)` with upsert for idempotent updates
- **Notifications:** `PeriodicWorkRequest` with 24h interval, `ExistingPeriodicWorkPolicy.UPDATE` for rescheduling

## Key Conventions

- All UI is in Jetpack Compose — no XML layouts
- Composable functions are stateless; state is managed in `DashboardViewModel`
- ProGuard keeps Room entity classes (`data.db.**`)
- Release signing uses environment variables: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

## CI/CD

- **CI** (`.github/workflows/ci.yml`): Runs on PRs to main — `assembleDebug` + `test`
- **Release** (`.github/workflows/release.yml`): Triggered by `v*` tags — builds signed APK, creates GitHub release

## Testing

No tests exist yet. Priority areas for future tests:
- ViewModel: day computation, goal formulas, streak calculation
- Room DAO: completion queries, upsert behavior
- UI: exercise card interactions, settings dialog
