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

## Commands

- `gradle assembleDebug` — build debug APK (output: `app/build/outputs/apk/debug/app-debug.apk`)
- `gradle assembleRelease` — build signed release APK (requires `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` env vars)
- `gradle test` — run unit tests

Use the system `gradle` command, not `./gradlew` (wrapper jar is not checked in).

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

## Critical Rules

### Code Style
- Use Kotlin idioms: prefer `val` over `var`, data classes for state, named arguments for clarity
- Composable function names are PascalCase nouns; preview functions are suffixed `Preview`
- No XML layouts — all UI is Jetpack Compose; never add View-based components

### Architecture
- All state lives in `DashboardViewModel` as `StateFlow`; composables are stateless and receive state as parameters
- Side effects (DB writes, notifications) are triggered from the ViewModel, never from composables
- Use `combine`/`map` on Flows in the ViewModel; collect with `collectAsStateWithLifecycle` in UI

### Database
- All DAO operations must be `suspend` or return `Flow` — never call them on the main thread
- Use `@Upsert` for idempotent writes; never duplicate insert + delete logic
- Keep entity classes in `data.db.**` so ProGuard rules continue to protect them

### Notifications / Background Work
- Schedule via `ReminderScheduler` using `ExistingPeriodicWorkPolicy.UPDATE` to avoid duplicate workers
- Never post notifications directly from a composable or ViewModel — use `ReminderWorker`
- Notification channels must be created before posting; channel creation is idempotent

### Testing
- Tests go in `app/src/test/` (unit) or `app/src/androidTest/` (instrumented)
- ViewModel tests must inject a fake `AppDatabase` or DAO — never rely on production Room instances
- Test day computation, streak logic, and goal formulas with boundary values (day 0, day 1, large N)

## Documentation Lookup
Always use Context7 MCP when you need library/API documentation, code generation,
setup or configuration steps. Add "use context7" to prompts or it will be auto-invoked.
