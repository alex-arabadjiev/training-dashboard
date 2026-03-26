# Kinetic — Design Guide

## Philosophy

**Industrial. Precise. Earned.**
The visual language is dark, dense, and typographically bold. Every element should feel like it belongs in an engineering interface built for athletes — not a consumer wellness app. Green is energy; it is used sparingly and only for things that matter. Everything else recedes into near-black.

---

## Color Tokens

| Token | Hex | Usage |
|-------|-----|-------|
| `KineticGreen` | `#C3F400` | Primary accent — active states, CTAs, highlights, progress |
| `KineticGreenDim` | `#ABD600` | Deprecated — do not use. Was used for gradients. |
| `KineticBackground` | `#131313` | App background, deepest surface |
| `KineticSurface` | `#131313` | Alias of Background |
| `KineticSurfaceContainer` | `#1A1A1A` | Cards, sheets, input fields |
| `KineticSurfaceContainerHigh` | `#222222` | Elevated surfaces — icon boxes, secondary buttons, pills |
| `KineticSurfaceContainerHighest` | `#2A2A2A` | Rarely used; maximum elevation |
| `KineticOnSurface` | `#E8E8E8` | Primary text on dark backgrounds |
| `KineticOnSurfaceVariant` | `#9E9E9E` | Secondary/muted text, labels, placeholders |
| `KineticOutline` | `#3A3A3A` | Dividers, borders |
| `KineticOutlineVariant` | `#2A2A2A` | Subtle borders (less contrast) |
| `KineticError` | `#FF5449` | Error states |
| `KineticErrorContainer` | `#3D1414` | Error background |

**Rules:**
- Never use hardcoded `Color.Black` or `Color.White` for surfaces — use the token stack
- `Color.White.copy(alpha = 0.05f–0.1f)` is acceptable for subtle borders only
- `KineticBlue` / `KineticBlueBright` are defined but unused — do not introduce them without a deliberate design decision

---

## Typography

The type system is built on **size, weight, and italics** — not a custom font. The app uses system default (Roboto on Android).

| Role | Size | Weight | Italic | Usage |
|------|------|--------|--------|-------|
| Display / Hero | 96sp | Black | No | Ring counter on Log Reps |
| Display Large | 72sp | Black | Yes | DAY number on dashboard |
| Headline Large | 36sp | Black | Yes | CompletionBanner tagline |
| Headline Medium | 28–30sp | Black | Yes | Exercise name on Log Reps, KINETIC wordmark |
| Title Medium | 18sp | Black | Yes | CTA buttons (DONE, SAVE CHANGES, SET) |
| Title Small | 14–16sp | Black | No | Section headers (ACCELEROMETER MODE, ADAPTIVE TIMING) |
| Body Small | 12sp | Normal | No | Descriptive subtitles under section headers |
| Label Small | 10sp | Bold | No | Metadata labels, uppercase tracking labels (TODAY'S GOALS, STREAK, REPS) |
| Micro | 9sp | Bold | No | Fine-print hints (Experimental notices) |

**Rules:**
- **All UI labels are UPPERCASE.** No sentence case in UI controls.
- Hero text (DAY N, counter) is always Black weight
- CTA button text is always Black weight + Italic at 18sp
- Section labels use `letterSpacing = 2.sp` minimum
- `lineHeight` should be set tightly (≤ fontSize) for display text

---

## Spacing & Layout

| Token | Value | Usage |
|-------|-------|-------|
| Screen horizontal padding | `20.dp` | All screens |
| Screen vertical padding top | `16.dp` | Dashboard |
| Card internal padding | `24.dp` | ExerciseCard |
| Section spacing | `16.dp` | Between major dashboard sections |
| Element spacing | `8–12.dp` | Within a section |

**Rules:**
- Use `Arrangement.spacedBy()` instead of manual Spacer chains where possible
- All scroll content should have `padding(bottom = 32.dp)` to clear the bottom nav/gesture bar

---

## Corner Radius

| Context | Radius |
|---------|--------|
| Primary buttons (DONE, SAVE CHANGES) | `12.dp` |
| Secondary buttons (CANCEL) | `12.dp` |
| Cards / containers | `12.dp` |
| Icon boxes | `12.dp` |
| Pill / chip shapes | `50` (fully round) |
| Progress bar / accent lines | `2–4.dp` |
| Dialog confirm buttons | `8.dp` |

**Rule: The minimum corner radius for any interactive or card surface is 12dp.** Anything smaller breaks the visual coherence.

---

## Interactive Elements

### Primary CTA Button
Full-width, 64dp tall, `KineticGreen` background, `12.dp` radius.
Text: 18sp Black Italic `KineticBackground`. Always paired with `CheckCircle` icon at 22dp.
Implementation: `Box + .clickable {}` (not Material `Button`).

```
Box(
    modifier = Modifier
        .fillMaxWidth()
        .height(64.dp)
        .background(KineticGreen, RoundedCornerShape(12.dp))
        .clickable { ... },
    contentAlignment = Alignment.Center
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), ...) {
        Text("ACTION", style = titleMedium.copy(Black, Italic, 18sp), color = KineticBackground)
        Icon(CheckCircle, tint = KineticBackground, modifier = Modifier.size(22.dp))
    }
}
```

### Secondary / Cancel Button
Full-width, 64dp tall, `KineticSurfaceContainerHigh` background, `12.dp` radius.
Text: 18sp Black, `KineticOnSurface`. No icon.
Implementation: Material `Button` with `ButtonDefaults.buttonColors(containerColor = KineticSurfaceContainerHigh)`.

### Dialog Buttons (paired)
Split 50/50 in a `Row`, 48dp tall, `8.dp` radius.
- Dismiss: `KineticBackground` bg, muted text, no icon
- Confirm: `KineticGreen` bg, dark text, CheckCircle icon at 18dp

### Icon Boxes
56dp square, `KineticSurfaceContainerHigh` background, `12.dp` radius.
Icon: 24–28dp, `KineticGreen` tint.

### Toggle Switch
`checkedTrackColor = KineticGreen`, `checkedThumbColor = KineticBackground`.

### Progress Bar (inline)
4dp tall, `128.dp` wide, `KineticGreen` fill, `KineticBackground` track, `2.dp` clip radius.

---

## Borders

| Context | Value |
|---------|-------|
| Active card border | `KineticGreen.copy(alpha = 0.1f)` |
| Inactive card border | `Color.White.copy(alpha = 0.05f–0.08f)` |
| Input field border | `Color.White.copy(alpha = 0.08f)` |
| Completion / goal achieved | `KineticGreen.copy(alpha = 0.5f)` |

---

## Screen Patterns

### Screen Header (Log Reps style)
Back arrow in `KineticGreen` (64dp `IconButton`), screen title beside it in `KineticGreen` bold italic `titleMedium`.

### Section Label
`labelSmall` UPPERCASE, `KineticOnSurfaceVariant`, `letterSpacing = 2.sp`. Optional small icon at 16dp same color.

### Green Accent Underbar
4dp tall, 64dp wide (or `fillMaxWidth`), `KineticGreen`, `RoundedCornerShape(2.dp)`.
Used under section titles and exercise names to ground headings.

### Watermark Text
`displayLarge`, `FontWeight.Black`, `FontStyle.Italic`, `KineticGreen.copy(alpha = 0.05f)`, aligned `BottomEnd` of container.

---

## State: Completion / Goal Achieved

When a metric reaches its target:
- Primary number / value: color changes from `KineticOnSurface` → `KineticGreen`
- Badge: background `KineticGreen.copy(alpha = 0.15f)`, border `KineticGreen.copy(alpha = 0.5f)`, text `KineticGreen`
- Inactive additive controls (e.g. +1, +10): `alpha = 0.3f`, non-clickable

---

## Known Issues / Anti-Patterns to Avoid

1. **Do not use `Color.Black` for surfaces.** Use `KineticBackground` or `KineticSurfaceContainerHigh`.
2. **Do not use Material `Button` for primary CTAs.** Use `Box + .clickable`. The Material ripple and padding fight with our layout.
3. **Do not use gradients.** `KineticGreenDim` exists as a historical artifact from an old gradient button — do not reintroduce.
4. **Do not use raw `MaterialTheme.colorScheme.primary`** for any styled text. Always resolve to a named Kinetic token.
5. **`CompletionBanner` is a tagline footer, not a conditional completion state.** Despite its name, it is always visible as a motivational footer at the bottom of the dashboard.
