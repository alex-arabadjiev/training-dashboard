# Spec: Settings bottom sheet redesign

> **Spec ID**: 008 | **Created**: 2026-03-23 | **Status**: draft | **Complexity**: small | **Branch**: —

## Goal
Replace the existing `AlertDialog`-based settings UI with a `ModalBottomSheet` styled to match `designs/harmonized_settings/`.

## Context
The current settings are in a plain `AlertDialog` with `OutlinedTextField` rows. The design shows a bottom sheet with: a drag handle, "SETTINGS" heading with a neon underline bar, three time-picker rows (Morning Reminder ☀, Afternoon Nudge ☀, Evening Interrupt 🌙), an Adaptive Timing toggle, a "SAVE CHANGES" gradient CTA, a "CANCEL" secondary button, and a "Set Current Day" ghost link.

Depends on spec 005 (theme tokens).

## Steps
- [ ] Step 1: Replace `AlertDialog` in `DashboardScreen` with `ModalBottomSheet` — use `rememberModalBottomSheetState(skipPartiallyExpanded = true)` so it opens fully; keep the same `onSave` callback signature so ViewModel wiring is unchanged
- [ ] Step 2: Implement the sheet header — "SETTINGS" at 36sp italic black with a 4dp-high × 64dp-wide neon green bar underneath it
- [ ] Step 3: Style the three time rows — each row is a dark (`surfaceContainerLow`) container with a Material icon label (wb_sunny / light_mode / bedtime), a label string, and the time value; tapping opens the existing `TimePicker` dialog unchanged
- [ ] Step 4: Implement the Adaptive Timing row — bold label + description text + a `Switch` with neon green track colour when checked; matches existing ViewModel wiring
- [ ] Step 5: Implement the action button group — full-width neon gradient "SAVE CHANGES" button (56dp height, small radius) + full-width `surfaceContainerHigh` "CANCEL" button + ghost text "Set Current Day" link that shows the day-number input dialog

## Acceptance Criteria
- [ ] Settings opens as a bottom sheet (not a dialog)
- [ ] Sheet contains all three time pickers and they open the same `TimePicker` overlay as before
- [ ] Adaptive Timing toggle state matches `state.adaptiveTimingEnabled` on open
- [ ] SAVE CHANGES calls the existing `onSave` with all current values and dismisses the sheet
- [ ] CANCEL dismisses without saving
- [ ] "Set Current Day" link opens a numeric input dialog and updates the day via `viewModel.setCurrentDay`
- [ ] Build passes: `./gradlew :app:assembleDebug`

## Files to Modify
- `app/src/main/java/com/example/trainingdashboard/ui/DashboardScreen.kt` — replace `AlertDialog` / `SettingsDialog` with `ModalBottomSheet`; keep `TimeSettingRow` and `TimePicker` overlay logic

## Out of Scope
- Notification permission requests inside the sheet
- Start date picker (only numeric day override is in scope)
