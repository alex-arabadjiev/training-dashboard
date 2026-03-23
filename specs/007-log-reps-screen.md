# Spec: Log Reps full-screen overlay

> **Spec ID**: 007 | **Created**: 2026-03-23 | **Status**: draft | **Complexity**: medium | **Branch**: —

## Goal
Implement the `LogRepsScreen` composable that matches `designs/harmonized_log_reps_squats/`, replacing the current `CountInputDialog` with a full-screen rep-logging experience.

## Context
The current count editing UX is a plain `AlertDialog` with a text field. The design shows a full-screen overlay with: a back arrow, the exercise name underlined in neon green, a circular progress ring drawn with Canvas, a large rep count, +1/+10/Edit quick-entry buttons, an "Accelerometer Mode" toggle (squats only), and a fixed "DONE" button. This screen is reached by tapping the body of an exercise card (spec 006).

Depends on spec 005 (theme tokens) and spec 006 (card tap wiring).

## Steps
- [ ] Step 1: Create `LogRepsScreen.kt` in `ui/` — a full-screen `Box` (dark background) containing a fixed top bar (back arrow + "LOG REPS" neon label), a scrollable body, and a fixed bottom CTA
- [ ] Step 2: Implement the circular progress ring — use `Canvas` with `drawArc` to render a background track (`white/5%`) and a foreground arc (`#C3F400`) sweeping from -90° clockwise proportional to `completedCount / targetCount`; centre the large white rep count and "REPS COMPLETED" / "GOAL: N" labels inside
- [ ] Step 3: Implement the three action buttons row — "+1 REP", "+10 REPS" (both neon-green tinted), and an "EDIT" pencil button that opens a minimal numeric input dialog; each +N button calls `onUpdateCount(current + N)` clamped to `targetCount`
- [ ] Step 4: Implement the Accelerometer Mode toggle row — visible only when `exercise.name == "Squats"`; renders icon + label + description + a styled Switch; wire the checked state to a local `var` (UI-only for now, no sensor integration)
- [ ] Step 5: Implement the fixed "DONE" button — neon green gradient full-width; calls `onDone()` which resolves back to the dashboard
- [ ] Step 6: Wire into `DashboardScreen` — replace the `showCountDialog` local state in `ExerciseCard` with a `selectedExercise: ExerciseState?` state hoisted to `DashboardScreen`; conditionally render `LogRepsScreen` instead of the main dashboard content when non-null

## Acceptance Criteria
- [ ] Tapping an exercise card body opens `LogRepsScreen` for that exercise
- [ ] The circular progress ring reflects the current `completedCount` relative to `targetCount`
- [ ] +1 increments the count by 1 (clamped to target); +10 increments by 10
- [ ] EDIT button opens a numeric dialog; confirming it updates the count
- [ ] Accelerometer Mode toggle is visible only for Squats
- [ ] DONE button and back arrow both return to the dashboard
- [ ] Count changes are persisted via `viewModel.updateExerciseCount` before navigating back
- [ ] Build passes: `./gradlew :app:assembleDebug`

## Files to Create
- `app/src/main/java/com/example/trainingdashboard/ui/LogRepsScreen.kt` — new composable

## Files to Modify
- `app/src/main/java/com/example/trainingdashboard/ui/DashboardScreen.kt` — hoist selected-exercise state; render LogRepsScreen when non-null
- `app/src/main/java/com/example/trainingdashboard/ui/components/ExerciseCard.kt` — remove internal `showCountDialog`; expose `onLogReps: () -> Unit` callback for card-body tap

## Out of Scope
- Actual accelerometer sensor reads — toggle state is local/ephemeral
- Haptic feedback on +1/+10 taps
