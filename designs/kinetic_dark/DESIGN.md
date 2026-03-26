# Design System Specification: Engineered Momentum

## 1. Overview & Creative North Star
This design system is built for the "High-Performance Athlete"—an individual who demands precision, discipline, and clarity. Moving away from the cluttered, "gamified" tropes of fitness apps, we are embracing a Creative North Star of **"Kinetic Engineering."**

The aesthetic is inspired by high-end automotive cockpits and precision medical gear. It breaks the "standard template" look through intentional asymmetry, massive typographic scales, and deep tonal layering. We do not use borders to define space; we use light and depth. The goal is a UI that feels like it’s vibrating with energy but remains rock-solid under the pressure of a high-intensity workout.

## 2. Colors & Surface Logic
The palette is rooted in the "void"—a deep charcoal foundation that allows our neon accents to serve as functional beacons of motivation.

*   **Foundation:** Use `surface` (#131313) for the primary canvas. 
*   **The "No-Line" Rule:** Explicitly prohibit 1px solid borders for sectioning. Boundaries must be defined solely through background color shifts. For example, a workout history section should sit on `surface_container_low`, while the individual workout cards within it sit on `surface_container_high`.
*   **Surface Hierarchy & Nesting:** Treat the UI as physical layers. 
    *   *Level 0:* `surface_container_lowest` (deepest background).
    *   *Level 1:* `surface` (base workspace).
    *   *Level 2:* `surface_container_high` (active cards/modules).
*   **Signature Textures:** For primary CTAs (like "Start Workout"), use a subtle linear gradient (45-degree) transitioning from `primary_fixed_dim` (#abd600) to `primary_container` (#c3f400). This adds "soul" and a sense of three-dimensional energy that flat colors lack.
*   **Glassmorphism:** For floating navigation or modal overlays, use `surface_variant` at 60% opacity with a `backdrop-filter: blur(12px)`. This keeps the athlete grounded in their context while bringing focus to the foreground.

## 3. Typography
Our typography is the "Voice of the Coach": authoritative, loud, and unwavering.

*   **Display & Headlines (Lexend):** We use Lexend for all data-heavy and motivational text. Its geometric precision mimics technical engineering. 
    *   Use `display-lg` (3.5rem) for primary metrics like heart rate or rep counts. 
    *   Use `headline-sm` (1.5rem) for section headers to maintain a bold, editorial feel.
*   **Body & Labels (Inter):** We use Inter for high-density information. Its neutral tone provides a necessary "quiet" contrast to the aggressive headlines.
*   **Intentional Asymmetry:** Don't center-align everything. Use left-heavy layouts for headlines and right-aligned data points to create a visual "pull" that suggests forward motion.

## 4. Elevation & Depth
In a dark UI, traditional shadows often turn into "mud." We achieve depth through **Tonal Layering** and light.

*   **The Layering Principle:** Stack `surface-container` tiers. A `surface_container_highest` (#353534) card placed on a `surface` (#131313) background creates a natural lift.
*   **Ambient Shadows:** If a floating element (like a FAB) requires a shadow, use a large blur (30px-40px) at 10% opacity, using the `on_surface` color as the shadow tint. This mimics a soft glow rather than a harsh drop-shadow.
*   **The Ghost Border:** If a boundary is strictly required for accessibility, use the `outline_variant` token at 15% opacity. It should be felt, not seen.
*   **Active States:** When a card is "Completed," it should fill entirely with the `primary_container` (#c3f400). The text transitions to `on_primary_container` (#556d00) for maximum contrast and "reward" psychology.

## 5. Components

### Buttons & Touch Targets
*   **Primary Action:** Minimum height of 56dp (spacing `12` or `14`). Roundedness: `md` (0.375rem). Use the signature neon gradient.
*   **Secondary Action:** Use `secondary_container` (#00e3fd) with `on_secondary_container` text. This provides a clear visual hierarchy between "Critical" and "Supporting" actions.

### Cards & Metrics
*   **No Dividers:** Never use a line to separate "Sets" or "Reps." Use vertical whitespace (`spacing-4` or `spacing-6`) and subtle `surface_container` shifts.
*   **Progress Indicators:** Use the `secondary` (#bdf4ff) token for ongoing progress and `primary` (#ffffff) for completed milestones.

### Input Fields
*   **Styling:** Use `surface_container_lowest` for the field body.
*   **Focus State:** Instead of a thick border, use a 2px bottom-bar of `primary_fixed` (#c3f400) and a subtle increase in the container's brightness.

### Performance Chips
*   Used for tags like "Strength," "High Intensity," or "Recovery."
*   Roundedness: `full` (9999px).
*   Background: `surface_variant` (#353534); Text: `on_surface_variant`.

## 6. Do’s and Don’ts

### Do
*   **Do** use extreme typographic scale. Make the "Big Numbers" (like weight lifted) feel massive.
*   **Do** use `spacing-20` (5rem) for top-level section margins to give the content room to breathe.
*   **Do** use the `secondary` electric blue for technical "data" and the `primary` neon green for "action/success."

### Don’t
*   **Don’t** use pure black (#000000). It kills the depth of the "Kinetic Engineering" look. Stick to #131313.
*   **Don’t** use `rounded-xl` for everything. Keep it disciplined with `md` (0.375rem) to maintain a technical, sharp edge.
*   **Don’t** use animations that are "bouncy" or "cute." Use linear or "ease-in" transitions that feel fast and efficient.

## 7. Spacing Reference
Always snap to the defined scale. 
*   **Inner Card Padding:** `6` (1.5rem).
*   **Gutter between Cards:** `4` (1rem).
*   **Section Vertical Margin:** `12` (3rem) or `16` (4rem).

By following these rules, you will create a dashboard that doesn't just track a workout—it fuels the mindset required to complete it.