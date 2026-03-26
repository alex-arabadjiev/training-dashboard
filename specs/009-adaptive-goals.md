# Spec: Adaptive goal level system

> **Spec ID**: 009 | **Created**: 2026-03-26 | **Status**: draft | **Complexity**: large | **Branch**: feat/adaptive-goals

## Goal
Replace the fixed calendar-day-driven exercise targets with an adaptive goal level that rises, holds, or falls based on the previous day's completion percentage.

## Context
Currently, exercise targets are derived from the calendar day number (Day N = N push-ups, 2N sit-ups, 3N squats). This means targets always increase regardless of whether the user completes them, which punishes missed days with an unreachable jump. The adaptive system decouples difficulty (goal level) from calendar time so that targets respond to actual performance.

## Feature Design

### Two independent concepts

1. **Goal level (N)** — persisted as `goal_level` in DataStore. Drives exercise targets via `ExerciseTargets.forDay(goalLevel)`. Transitions once per calendar day based on previous day's aggregate progress:
   - 100% progress (all 3 exercises completed) → N+1
   - 33–99% progress → hold at N
   - <33% progress → N-1, floor at 1

2. **Day N (displayed in UI)** — count of calendar days where the user achieved ≥33% progress. Computed from Room data at load time. Replaces the old `dayNumber` in `DashboardUiState`.

### Progress calculation
For a given calendar day with goal level G:
- Total target reps = G + 2G + 3G = 6G
- Completed reps = sum of `completedCount` for each exercise on that day
- Progress = completedReps / (6 × G)

Exercise weights (as fraction of 6G):
- Push-ups = G reps → 1/6 ≈ 16.7%
- Sit-ups = 2G reps → 2/6 ≈ 33.3%
- Squats = 3G reps → 3/6 = 50%

A day is "active" (≥33%) if completed exercise weight sum ≥ 2 (i.e., sit-ups alone, squats alone, or any two exercises). Push-ups alone (weight 1) is the only single-exercise case below the threshold.

The progress formula uses the binary `completed` flag per exercise: if `completed = true`, add that exercise's target reps to the numerator.

### What is removed
- `computeStreak()` method in DashboardViewModel
- `streak` field in `DashboardUiState`
- `getFullyCompletedDays()` query in CompletionDao
- Streak chip display in `DayHeader`
- `FakeCompletionDao.getFullyCompletedDays()` implementation
- Streak-related unit tests

### What is replaced
- `setCurrentDay(day: Int)` in ViewModel → `setGoalLevel(level: Int)` that writes directly to DataStore
- "Set Current Day" dialog in settings → "Set Goal Level" dialog
- `dayNumber` semantics: was calendar day, becomes active day count (days with ≥33% progress)

### DataStore additions
- `goal_level: Int` — current adaptive goal level (null = unset, triggers migration)
- `last_evaluated_day: Int` — last calendar day number evaluated for transitions (default 0)

### Migration (existing users)
On first launch after this feature ships, if `goal_level` is not present in DataStore:
1. Read the current calendar day number from the start date
2. Set `goal_level` to that calendar day number
3. Set `last_evaluated_day` to `calendarDayNumber - 1`

This preserves difficulty continuity — an existing user on calendar day 30 continues with level 30 targets.

### Transition evaluation (ViewModel init)
Runs as a suspend function before the reactive Flow is launched:

1. Compute `todayCalendarDay` from start date (unchanged logic)
2. Read `goalLevel` (nullable) and `lastEvaluatedDay` from DataStore via `.first()`
3. If `goalLevel` is null: run migration, then re-read
4. For each calendar day D from `lastEvaluatedDay + 1` to `todayCalendarDay - 1` (in order):
   - Fetch completions for day D via a one-shot Room query
   - Compute progress against the *running* goal level (order matters)
   - Apply transition: update running goal level
5. Persist final `goalLevel` and `lastEvaluatedDay = todayCalendarDay - 1` to DataStore
6. Compute active day count from all completed exercises in Room
7. Launch the reactive `combine` Flow using `goalLevel` for targets

### Active day count
Query all rows where `completed = true` from Room, group by `dayNumber` in Kotlin, and count days where total exercise weight ≥ 2:

```
WEIGHT_MAP = { "Push-ups" -> 1, "Sit-ups" -> 2, "Squats" -> 3 }
activeDays = completions.groupBy { it.dayNumber }
    .count { (_, dayCompletions) -> dayCompletions.sumOf { WEIGHT_MAP[it.exercise] ?: 0 } >= 2 }
```

## Edge Cases

| Scenario | Behaviour |
|----------|-----------|
| Brand new user (no start date) | `ensureStartDate()` sets today; `goal_level` null → migration sets level to 1, `last_evaluated_day` to 0 |
| Day 1, no completions yet | Level 1; catch-up loop range is empty (lastEvaluated=0, today=1, range=1..0); day count = 0 |
| Goal level at floor (N=1), <33% progress | Stays at 1 |
| Multiple missed days (e.g., 5 days offline) | Catch-up loop evaluates each missed day sequentially; each with 0 completions drops level by 1 (floor 1) |
| 5 missed days from level 3 | 3→2→1→1→1 |
| `last_evaluated_day` ≥ `todayCalendarDay - 1` | Loop range is empty; no-op. Handles re-launches on same day |
| `todayCalendarDay` = 1 | Loop range = `1..0` = empty. No transitions. Correct |
| User sets goal level via settings | Writes directly to DataStore; next day's transition evaluates from that level |

## Data Model Changes

### DataStore (`PreferencesRepository`)
Add to `Keys`:
```kotlin
val GOAL_LEVEL = intPreferencesKey("goal_level")
val LAST_EVALUATED_DAY = intPreferencesKey("last_evaluated_day")
```

Add properties and setters:
```kotlin
val goalLevel: Flow<Int?>  // null means unset
val lastEvaluatedDay: Flow<Int>  // default 0
suspend fun setGoalLevel(level: Int)
suspend fun setLastEvaluatedDay(day: Int)
```

### Room (`CompletionDao`)
Add:
```kotlin
@Query("SELECT * FROM daily_completions WHERE completed = 1")
suspend fun getAllCompletedExercises(): List<DailyCompletion>
```

Also add a one-shot snapshot query for the catch-up loop:
```kotlin
@Query("SELECT * FROM daily_completions WHERE dayNumber = :day")
suspend fun getCompletionsForDaySnapshot(day: Int): List<DailyCompletion>
```

Remove: `getFullyCompletedDays()`

### DashboardUiState
```kotlin
data class DashboardUiState(
    val dayNumber: Int = 0,      // active day count (≥33% progress days)
    val goalLevel: Int = 1,      // NEW: drives exercise targets
    val exercises: List<ExerciseState> = emptyList(),
    val allCompleted: Boolean = false,
    // streak: Int REMOVED
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val afternoonNudgeHour: Int = 14,
    val afternoonNudgeMinute: Int = 0,
    val eveningInterruptHour: Int = 20,
    val eveningInterruptMinute: Int = 0,
    val adaptiveTimingEnabled: Boolean = false,
    val isLoading: Boolean = true
)
```

### ExerciseTargets
No changes needed. `ExerciseTargets.forDay(N)` already accepts an Int; pass `goalLevel` instead of `dayNumber`.

## Files to Create / Modify / Delete

### Create
| File | Purpose |
|------|---------|
| `app/src/main/java/.../data/GoalTransition.kt` | Pure functions: `computeProgress`, `nextLevel`, `computeActiveDayCount`, constants |
| `app/src/test/.../data/GoalTransitionTest.kt` | Unit tests for all GoalTransition logic |

### Modify
| File | Changes |
|------|---------|
| `data/PreferencesRepository.kt` | Add `GOAL_LEVEL`, `LAST_EVALUATED_DAY` keys, flows, and setters |
| `data/db/CompletionDao.kt` | Add `getAllCompletedExercises()` and `getCompletionsForDaySnapshot()`; remove `getFullyCompletedDays()` |
| `viewmodel/DashboardViewModel.kt` | Rewrite `loadDashboard()` with catch-up evaluation; replace `setCurrentDay()` with `setGoalLevel()`; remove `computeStreak()`; update `buildExercises()` to use `goalLevel` |
| `viewmodel/DashboardUiState` (in ViewModel file) | Add `goalLevel`, remove `streak` |
| `ui/DashboardScreen.kt` | Pass `goalLevel` to header; update settings dialog to "Set Goal Level" calling `setGoalLevel()` |
| `ui/components/DayHeader.kt` | Remove `streak` param and chip; add `goalLevel` param |
| `app/src/test/.../viewmodel/DashboardViewModelTest.kt` | Remove streak tests; add migration tests |
| `app/src/test/.../viewmodel/FakeCompletionDao.kt` | Add `getAllCompletedExercises()` and `getCompletionsForDaySnapshot()`; remove `getFullyCompletedDays()` |
| `app/src/test/.../viewmodel/FakePreferencesRepository.kt` | Add `goalLevel`, `lastEvaluatedDay` flows and setters |

### Delete (if exists as standalone file)
None — all removals are within modified files.

## Test Requirements

### GoalTransitionTest.kt (new, pure JVM — no Robolectric needed)

**Transition logic:**
- `levelIncreasesWhenAllThreeExercisesCompleted` — 100% → N+1
- `levelHoldsWhenTwoExercisesCompleted` — push-ups + sit-ups = 50% → hold
- `levelHoldsWhenOnlySitUpsCompleted` — 33.3% → hold
- `levelHoldsWhenOnlySquatsCompleted` — 50% → hold
- `levelDecreasesWhenOnlyPushUpsCompleted` — 16.7% < 33% → N-1
- `levelDecreasesWhenNoExercisesCompleted` — 0% → N-1
- `levelFloorAtOne` — N=1 with 0% stays at 1
- `multiDayCatchUpEvaluatesSequentially` — 3 missed days from level 5: 5→4→3→2
- `multiDayCatchUpRespectsFloor` — 5 missed days from level 3: 3→2→1→1→1

**Active day count:**
- `activeDayCountIsZeroWithNoCompletions`
- `activeDayCountExcludesPushUpsOnlyDays`
- `activeDayCountIncludesSitUpsOnlyDays`
- `activeDayCountIncludesSquatsOnlyDays`
- `activeDayCountIncludesFullyCompletedDays`
- `activeDayCountWithMixedDays`

**Progress calculation:**
- `progressIsZeroWithNoCompletions`
- `progressIs100WithAllCompleted`
- `progressIsCorrectForPartialCompletion`

### Migration logic (in DashboardViewModelTest.kt)
- `migrationSetsGoalLevelToCalendarDay` — existing user on day 15 gets goalLevel=15
- `migrationSetsLastEvaluatedDayToYesterday`
- `newUserMigrationSetsGoalLevelToOne`

### Modified tests
- Remove all `computeStreak` tests
- Remove `getFullyCompletedDays` tests
- Update `FakeCompletionDao` and `FakePreferencesRepository` stubs

## Acceptance Criteria

- [ ] Exercise targets are driven by `goalLevel` from DataStore, not calendar day
- [ ] Goal level transitions correctly on each new calendar day
- [ ] Multiple missed days are caught up sequentially on next launch
- [ ] Goal level never drops below 1
- [ ] "Day N" in the UI shows active day count (≥33% days), not calendar day
- [ ] Streak counter fully removed (UI state, ViewModel, DAO, components)
- [ ] Existing users migrated: `goalLevel` initialised to their current calendar day number
- [ ] New users start at `goalLevel = 1`
- [ ] "Set Goal Level" replaces "Set Current Day" in settings
- [ ] All new logic has unit tests covering happy path and edge cases
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:assembleDebug` builds successfully

## Out of Scope
- Storing historical goal level per day
- Partial rep tracking (binary completion retained)
- UI redesign beyond DayHeader streak removal and goal level display
- Notification system changes

---

## Implementation Plan

### Phase 1 — Pure logic layer (no Android dependencies)

**1.1** Create `app/src/main/java/com/example/trainingdashboard/data/GoalTransition.kt`:
- `EXERCISE_WEIGHTS: Map<String, Int>` = `{"Push-ups" -> 1, "Sit-ups" -> 2, "Squats" -> 3}`
- `ACTIVE_DAY_WEIGHT_THRESHOLD = 2`
- `PROGRESS_HOLD_THRESHOLD = 1.0 / 3.0`
- `GOAL_LEVEL_FLOOR = 1`
- `fun computeProgress(completions: List<DailyCompletion>, goalLevel: Int): Double`
- `fun nextLevel(currentLevel: Int, progress: Double): Int`
- `fun computeActiveDayCount(allCompleted: List<DailyCompletion>): Int`

**1.2** Write `GoalTransitionTest.kt` — all tests listed in Test Requirements above.

**1.3** Run `./gradlew :app:testDebugUnitTest` — all new tests pass.

---

### Phase 2 — Data layer

**2.1** `PreferencesRepository.kt`:
- Add `GOAL_LEVEL` and `LAST_EVALUATED_DAY` to `Keys` object
- Add `val goalLevel: Flow<Int?>` (null when key absent)
- Add `val lastEvaluatedDay: Flow<Int>` (default 0)
- Add `suspend fun setGoalLevel(level: Int)`
- Add `suspend fun setLastEvaluatedDay(day: Int)`

**2.2** `CompletionDao.kt`:
- Add `suspend fun getAllCompletedExercises(): List<DailyCompletion>`
- Add `suspend fun getCompletionsForDaySnapshot(day: Int): List<DailyCompletion>`
- Remove `getFullyCompletedDays()`

**2.3** `FakePreferencesRepository.kt`:
- Add `MutableStateFlow<Int?>` for `goalLevel` (initially null)
- Add `MutableStateFlow<Int>` for `lastEvaluatedDay` (initially 0)
- Add `setGoalLevel()` and `setLastEvaluatedDay()` setters
- Add `seedGoalLevel(level: Int)` and `seedLastEvaluatedDay(day: Int)` test helpers

**2.4** `FakeCompletionDao.kt`:
- Add `getAllCompletedExercises()` — filter in-memory list by `completed = true`
- Add `getCompletionsForDaySnapshot(day: Int)` — filter by `dayNumber`
- Remove `getFullyCompletedDays()`

**2.5** Build check: `./gradlew :app:assembleDebug`

---

### Phase 3 — ViewModel

**3.1** Update `DashboardUiState`:
- Add `goalLevel: Int = 1`
- Remove `streak: Int = 0`

**3.2** Rewrite `loadDashboard()`:
```
val startDate = ensureStartDate()
val todayCalendarDay = computeDayNumber(startDate)

var goalLevel = prefsRepo.goalLevel.first()
var lastEvaluatedDay = prefsRepo.lastEvaluatedDay.first()

// Migration for existing users
if (goalLevel == null) {
    goalLevel = todayCalendarDay
    lastEvaluatedDay = todayCalendarDay - 1
    prefsRepo.setGoalLevel(goalLevel)
    prefsRepo.setLastEvaluatedDay(lastEvaluatedDay)
}

// Catch-up loop — evaluate unevaluated past days in order
for (day in (lastEvaluatedDay + 1)..(todayCalendarDay - 1)) {
    val completions = completionDao.getCompletionsForDaySnapshot(day)
    val progress = GoalTransition.computeProgress(completions, goalLevel)
    goalLevel = GoalTransition.nextLevel(goalLevel, progress)
}

// Persist
prefsRepo.setGoalLevel(goalLevel)
prefsRepo.setLastEvaluatedDay(todayCalendarDay - 1)

// Active day count
val allCompleted = completionDao.getAllCompletedExercises()
val activeDayCount = GoalTransition.computeActiveDayCount(allCompleted)

// Reactive flow
val finalGoalLevel = goalLevel
combine(
    completionDao.getCompletionsForDay(todayCalendarDay),
    prefsRepo.reminderHour,
    ...
) { values ->
    DashboardUiState(
        dayNumber = activeDayCount,
        goalLevel = finalGoalLevel,
        exercises = buildExercises(finalGoalLevel, completions),
        ...
    )
}.collect { _uiState.value = it }
```

**3.3** Replace `setCurrentDay(day: Int)` with:
```kotlin
fun setGoalLevel(level: Int) {
    if (level < 1) return
    viewModelScope.launch {
        prefsRepo.setGoalLevel(level)
        loadDashboard()
    }
}
```

**3.4** Remove `computeStreak()`.

**3.5** Update `buildExercises()` call: pass `goalLevel` instead of `dayNumber`.

**3.6** Update `DashboardViewModelTest.kt`:
- Remove all streak-related tests
- Add migration tests using seeded `FakePreferencesRepository`

---

### Phase 4 — UI

**4.1** `DayHeader.kt`:
- Remove `streak: Int` parameter and streak chip block
- Add `goalLevel: Int` parameter
- Display goal level alongside "Day N" (e.g. secondary label "LEVEL N")

**4.2** `DashboardScreen.kt`:
- Pass `state.goalLevel` to `DayHeader()`; remove `state.streak`
- Update settings dialog: label "SET GOAL LEVEL", input label "Goal level", call `viewModel.setGoalLevel(value.toInt())`
- Remove all references to `setCurrentDay()`

**4.3** Build + install: `./gradlew :app:installDebug`

---

### Phase 5 — Final verification

**5.1** `./gradlew :app:testDebugUnitTest` — full suite passes.

**5.2** Manual emulator verification:
- Fresh install → level 1, day count 0
- Complete all exercises → next launch: level 2, day count 1
- Complete sit-ups only → next launch: level holds, day count increments
- Complete push-ups only → next launch: level drops, day count does NOT increment
- Miss a day entirely → next launch: level drops, day count unchanged
- "Set Goal Level" in settings updates targets immediately

**5.3** Dead code check — grep for `streak`, `getFullyCompletedDays`, `setCurrentDay` — no remaining references.
