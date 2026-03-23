---
id: "002"
title: "Eliminate redundant DataStore reads in loadDashboard"
status: done
---

## Goal
Replace 7 sequential `prefsRepo.*.first()` calls inside the DAO `collect` block with a single `combine()` that merges all preference flows alongside the DAO flow, so state updates with one collector and no redundant file reads.

## Context
`loadDashboard()` in `DashboardViewModel` opens the DataStore file 7 times on every exercise toggle:
```kotlin
completionDao.getCompletionsForDay(dayNumber).collect { completions ->
    val hour = prefsRepo.reminderHour.first()        // reads DataStore
    val minute = prefsRepo.reminderMinute.first()    // reads DataStore
    // … 5 more .first() calls
}
```
Preferences change far less frequently than completions; they should not be re-read on every DB emission. The fix is to `combine` all 8 flows (1 DAO + 7 prefs) and react once.

## Steps

1. **`viewmodel/DashboardViewModel.kt`** — Replace the `collect` block in `loadDashboard()` with a `combine` call over all 8 flows:
   ```kotlin
   combine(
       completionDao.getCompletionsForDay(dayNumber),
       prefsRepo.reminderHour,
       prefsRepo.reminderMinute,
       prefsRepo.afternoonNudgeHour,
       prefsRepo.afternoonNudgeMinute,
       prefsRepo.eveningInterruptHour,
       prefsRepo.eveningInterruptMinute,
       prefsRepo.adaptiveTimingEnabled
   ) { values -> /* destructure and build DashboardUiState */ }
   .collect { state -> _uiState.value = state }
   ```
   Note: `combine` supports up to 5 parameters with a typed lambda; for 8 flows use the `Array`-based overload: `combine(listOf(...)) { arr -> … }`.

2. **`viewmodel/DashboardViewModel.kt`** — Remove the 7 `prefsRepo.*.first()` calls from the old `collect` block entirely.

## Acceptance Criteria

### Truths
- `loadDashboard()` contains exactly one `collect` call
- No call to `prefsRepo.*.first()` remains inside the `collect` block
- `DashboardUiState` is populated with the same fields as before (no regressions)
- Toggling an exercise still updates the UI immediately
- Changing reminder time via `updateReminderTime` still reflects in the UI state

### Artifacts
- `specs/002-datastore-flow-combine.md` (this file)

## Files to Modify
- `app/src/main/java/…/viewmodel/DashboardViewModel.kt`

## Out of Scope
- Changes to `PreferencesRepository` itself
- Adding tests (covered in spec 001)
- Changing the 7-flow structure of `PreferencesRepository`
