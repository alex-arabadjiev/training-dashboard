# Spec: Dashboard screen redesign

> **Spec ID**: 006 | **Created**: 2026-03-23 | **Status**: draft | **Complexity**: medium | **Branch**: —

## Goal
Redesign the main dashboard UI to match `designs/day_15_kinetic_start/` and `designs/day_15_100_complete/`, replacing the generic Material scaffold with the "Kinetic Engineering" layout.

## Context
The current dashboard uses a standard `TopAppBar` + plain cards. The designs show a branded header, a streak chip, a massive italic day number with progress percentage, a "TODAY'S GOALS" section with icon-driven exercise cards, and a motivational quote footer. The 100%-complete state turns all card checkboxes neon-green and shows "100%" in the header.

Depends on spec 005 (theme tokens must be in place).

## Steps
- [ ] Step 1: Rewrite `DashboardScreen` — remove `Scaffold`/`TopAppBar`; use a `Box` with `#131313` background + `Column(verticalScroll)`; implement the fixed header row ("KINETIC" italic black + settings gear icon) at the top
- [ ] Step 2: Replace `DayHeader` composable — render a streak chip (`STREAK: N DAYS`, rounded-full, neon-green text on dark-green bg), then a left-aligned massive day number (`DAY N` at 72sp italic black) with a right-aligned progress percentage (neon green, 24sp)
- [ ] Step 3: Rewrite `ExerciseCard` — dark card (`#1A1A1A`) with a black icon box (exercise-specific Material icon in neon green), bold italic uppercase exercise name, `X/Y REPS` label in neon green, a thin progress bar, and a right-side checkbox square (empty when incomplete, neon green with checkmark when complete); tap the card body to open LogReps (spec 007), tap the checkbox to toggle complete
- [ ] Step 4: Update `CompletionBanner` — replace the teal card with an inline motivational quote block ("PRECISION IN EVERY REP" / "EXCELLENCE IS A HABIT") that is always visible at the bottom of the scroll; the streak/complete state is already communicated via the progress % and card checkmarks

## Acceptance Criteria
- [ ] App bar shows "KINETIC" in italic black neon green; no "Training Dashboard" text
- [ ] Streak chip displays current streak with correct count
- [ ] Day number is displayed left-aligned at large scale with progress % right-aligned
- [ ] Each exercise card shows the correct icon, rep count, progress bar, and checkbox state
- [ ] Completed exercises show a neon-green filled checkbox; incomplete show an empty square
- [ ] When all exercises are complete the progress % shows "100%"
- [ ] Motivational quote block is visible at the bottom of the scroll on both states
- [ ] Settings gear icon opens the settings sheet (wired to `showSettings` state, sheet implemented in spec 008)
- [ ] Build passes: `./gradlew :app:assembleDebug`

## Files to Modify
- `app/src/main/java/com/example/trainingdashboard/ui/DashboardScreen.kt` — full rewrite of layout; keep SettingsDialog wiring
- `app/src/main/java/com/example/trainingdashboard/ui/components/DayHeader.kt` — streak chip + large day number + progress %
- `app/src/main/java/com/example/trainingdashboard/ui/components/ExerciseCard.kt` — new card layout; keep `onToggle` and `onUpdateCount` callbacks
- `app/src/main/java/com/example/trainingdashboard/ui/components/CompletionBanner.kt` — replace with motivational quote block

## Out of Scope
- Log Reps screen — handled in spec 007
- Settings bottom sheet — handled in spec 008
- Accelerometer sensor integration — UI stub only
