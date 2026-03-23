# Spec: Kinetic Engineering theme system

> **Spec ID**: 005 | **Created**: 2026-03-23 | **Status**: draft | **Complexity**: small | **Branch**: —

## Goal
Replace the current light/blue Material 3 theme with the "Kinetic Engineering" dark-only palette defined in `designs/kinetic_dark/DESIGN.md`.

## Context
All four design screens (dashboard start, dashboard complete, log-reps, settings) share the same dark palette. This is the foundation that specs 006, 007, and 008 depend on. The current theme uses blue tones and supports light mode — neither matches the designs.

## Steps
- [ ] Step 1: Replace `Color.kt` — add the full Kinetic palette as named constants (KineticGreen `#C3F400`, KineticGreenDim `#ABD600`, KineticBlue `#BDF4FF`, KineticBlueBright `#00E3FD`, and all surface/container tiers from the design token list)
- [ ] Step 2: Replace `Theme.kt` — define a single `KineticDarkColorScheme` (`darkColorScheme`) wired to the new constants; remove the light scheme; always apply dark; set the status bar to the background colour (`#131313`)
- [ ] Step 3: Update `Type.kt` — add a `displayLarge` style at 72sp / FontWeight.Black for the day number, a `headlineMedium` style at 28sp / Black / Italic for section headers, and a `labelSmall` style at 10sp / Bold / letter-spacing 0.2em for caps labels

## Acceptance Criteria
- [ ] `TrainingDashboardTheme` no longer accepts or uses a `darkTheme` parameter — it is always dark
- [ ] `MaterialTheme.colorScheme.primary` resolves to `#C3F400` (neon green)
- [ ] `MaterialTheme.colorScheme.background` and `.surface` both resolve to `#131313`
- [ ] Status bar background is `#131313` (not the previous blue primary)
- [ ] Build passes: `./gradlew :app:assembleDebug`

## Files to Modify
- `app/src/main/java/com/example/trainingdashboard/ui/theme/Color.kt` — replace with Kinetic palette constants
- `app/src/main/java/com/example/trainingdashboard/ui/theme/Theme.kt` — single dark scheme, remove light scheme
- `app/src/main/java/com/example/trainingdashboard/ui/theme/Type.kt` — extend typography scale

## Out of Scope
- Downloadable/custom fonts (Lexend, Inter) — system font approximation only for now
- Any UI component changes — theme tokens only
