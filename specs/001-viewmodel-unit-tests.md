---
id: "001"
title: "ViewModel unit tests"
status: done
---

## Goal
Add unit tests for `DashboardViewModel` covering streak calculation, day computation, goal formulas, and `updateExerciseCount` clamping. No production Room instances allowed — inject fakes.

## Context
Zero tests exist despite JUnit 4 and `coroutines-test` being present (in `androidTestImplementation`). `DashboardViewModel` extends `AndroidViewModel` and hard-wires its dependencies via `TrainingApp`, making it untestable as-is. The ViewModel must be refactored to accept injected `CompletionDao` and `PreferencesRepository` before tests can be written.

All test deps (`junit`, `coroutines-test`) are currently in `androidTestImplementation` only. Unit tests live in `app/src/test/` and need `testImplementation` equivalents.

## Steps

1. **`app/build.gradle.kts`** — Move `junit` and `coroutines-test` to `testImplementation`; add `kotlinx-coroutines-test` to `testImplementation` if not already resolvable from `androidTestImplementation`.

2. **`viewmodel/DashboardViewModel.kt`** — Extract `CompletionDao` and `PreferencesRepository` as constructor parameters with defaults that mirror the current `AndroidViewModel` wiring:
   ```kotlin
   class DashboardViewModel(
       application: Application,
       private val completionDao: CompletionDao = (application as TrainingApp).database.completionDao(),
       private val prefsRepo: PreferencesRepository = PreferencesRepository(application)
   ) : AndroidViewModel(application)
   ```

3. **`app/src/test/java/com/example/trainingdashboard/viewmodel/FakeCompletionDao.kt`** — Create an in-memory fake DAO implementing `CompletionDao` backed by a `MutableStateFlow<List<DailyCompletion>>`.

4. **`app/src/test/java/com/example/trainingdashboard/viewmodel/FakePreferencesRepository.kt`** — Create a fake `PreferencesRepository`-like interface (or subclass) returning `MutableStateFlow` defaults for all preferences.

5. **`app/src/test/java/com/example/trainingdashboard/viewmodel/DashboardViewModelTest.kt`** — Write tests:
   - `computeDayNumber`: start date = today → day 1; start date = yesterday → day 2
   - `computeStreak`: no completions → 0; consecutive days → count; gap breaks streak
   - `ExerciseTargets.forDay`: day 1 → (1, 2, 3); day 5 → (5, 10, 15); day 0 → (0, 0, 0)
   - `updateExerciseCount`: count above target clamps to target; count below 0 clamps to 0; exact target marks `isCompleted = true`

## Acceptance Criteria

### Truths
- `app/src/test/` contains `DashboardViewModelTest.kt`, `FakeCompletionDao.kt`, `FakePreferencesRepository.kt`
- `gradle test` passes with no failures
- No test uses a real `AppDatabase` or `Room.databaseBuilder`
- `DashboardViewModel` still compiles and works in production with no behaviour change
- Boundary cases covered: day 0, streak with a gap at day N-1, count clamped at 0 and at target

### Artifacts
- `specs/001-viewmodel-unit-tests.md` (this file)
- `app/src/test/…/DashboardViewModelTest.kt`
- `app/src/test/…/FakeCompletionDao.kt`
- `app/src/test/…/FakePreferencesRepository.kt`

## Files to Modify
- `app/build.gradle.kts`
- `app/src/main/java/…/viewmodel/DashboardViewModel.kt`

## Files to Create
- `app/src/test/java/…/viewmodel/FakeCompletionDao.kt`
- `app/src/test/java/…/viewmodel/FakePreferencesRepository.kt`
- `app/src/test/java/…/viewmodel/DashboardViewModelTest.kt`

## Out of Scope
- Instrumented (androidTest) tests
- UI tests / Espresso
- Room DAO query tests
