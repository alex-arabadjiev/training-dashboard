---
id: "003"
title: "Move ReminderScheduler calls out of composable into ViewModel"
status: done
---

## Goal
Remove the 4 `ReminderScheduler.*` calls from `DashboardScreen.onSave` and invoke them from the ViewModel's update methods instead, restoring the MVVM contract that composables must not trigger side effects directly.

## Context
`DashboardScreen` currently calls `ReminderScheduler` directly from the `onSave` lambda (lines 131–138 of `DashboardScreen.kt`):
```kotlin
ReminderScheduler.schedule(context, morningH, morningM)
ReminderScheduler.scheduleAfternoonNudge(context, afternoonH, afternoonM)
ReminderScheduler.scheduleEveningInterrupt(context, eveningH, eveningM)
if (adaptiveEnabled) ReminderScheduler.scheduleAdaptiveTiming(context)
else ReminderScheduler.cancelAdaptiveTiming(context)
```
This violates MVVM: side effects must originate in the ViewModel. `DashboardViewModel` is an `AndroidViewModel` so it already holds an `Application` context, which is sufficient for WorkManager and AlarmManager scheduling.

## Steps

1. **`viewmodel/DashboardViewModel.kt`** — Add `ReminderScheduler` calls inside each update method:
   - `updateReminderTime(hour, minute)` → call `ReminderScheduler.schedule(getApplication(), hour, minute)` after the DataStore write
   - `updateAfternoonNudgeTime(hour, minute)` → call `ReminderScheduler.scheduleAfternoonNudge(getApplication(), hour, minute)`
   - `updateEveningInterruptTime(hour, minute)` → call `ReminderScheduler.scheduleEveningInterrupt(getApplication(), hour, minute)`
   - `setAdaptiveTimingEnabled(enabled)` → call `scheduleAdaptiveTiming` or `cancelAdaptiveTiming` based on `enabled`

2. **`ui/DashboardScreen.kt`** — Remove the 4 `ReminderScheduler.*` calls from the `onSave` lambda. Remove the `import` of `ReminderScheduler`. Remove the `val context = LocalContext.current` line if it is no longer used elsewhere in the file.

## Acceptance Criteria

### Truths
- `DashboardScreen.kt` contains no `import` of `ReminderScheduler`
- `DashboardScreen.kt` contains no direct call to `ReminderScheduler.*`
- Each of the four ViewModel update methods triggers the corresponding `ReminderScheduler` call
- `getApplication<Application>()` (not `LocalContext`) is used to obtain the context in the ViewModel
- Saving settings from the UI still schedules/cancels workers (functional equivalence)

### Artifacts
- `specs/003-reminder-scheduler-mvvm.md` (this file)

## Files to Modify
- `app/src/main/java/…/viewmodel/DashboardViewModel.kt`
- `app/src/main/java/…/ui/DashboardScreen.kt`

## Out of Scope
- Changing `ReminderScheduler` itself
- Dependency injection refactoring
- Adding tests for scheduling (covered by spec 001 scope expansion if desired)
