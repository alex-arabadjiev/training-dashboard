---
id: "004"
title: "Replace collectAsState with collectAsStateWithLifecycle"
status: done
---

## Goal
Replace `collectAsState()` with `collectAsStateWithLifecycle()` in `DashboardScreen` so the app stops collecting the ViewModel's StateFlow when it moves to the background, matching the project's stated convention.

## Context
`DashboardScreen.kt` line 58:
```kotlin
val state by viewModel.uiState.collectAsState()
```
Project conventions (STACK.md, CLAUDE.md) explicitly require `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose`. The plain `collectAsState()` keeps the collector active even when the Activity is stopped, wasting CPU and preventing lifecycle-aware cancellation.

Checking current dependencies — `lifecycle-runtime-compose` may not yet be declared in `app/build.gradle.kts`; the spec includes adding it if absent.

## Steps

1. **`app/build.gradle.kts`** — Check `libs.versions.toml` for a `lifecycle-runtime-compose` alias. If present, add `implementation(libs.lifecycle.runtime.compose)`. If absent, add the Maven coordinate directly: `implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")` (align version with existing lifecycle deps).

2. **`ui/DashboardScreen.kt`** — Replace the import:
   ```kotlin
   // remove:
   import androidx.compose.runtime.collectAsState
   // add:
   import androidx.lifecycle.compose.collectAsStateWithLifecycle
   ```

3. **`ui/DashboardScreen.kt`** — Replace the call on line 58:
   ```kotlin
   // before:
   val state by viewModel.uiState.collectAsState()
   // after:
   val state by viewModel.uiState.collectAsStateWithLifecycle()
   ```

## Acceptance Criteria

### Truths
- `DashboardScreen.kt` contains no call to `collectAsState()`
- `DashboardScreen.kt` imports `collectAsStateWithLifecycle` from `androidx.lifecycle.compose`
- `gradle assembleDebug` succeeds with no compilation errors
- `lifecycle-runtime-compose` appears in `app/build.gradle.kts` dependencies

### Artifacts
- `specs/004-collect-as-state-lifecycle.md` (this file)

## Files to Modify
- `app/build.gradle.kts`
- `app/src/main/java/…/ui/DashboardScreen.kt`

## Out of Scope
- Replacing `collectAsState()` in any component files other than `DashboardScreen.kt` (none currently use it)
- Changing the ViewModel's `StateFlow` type
