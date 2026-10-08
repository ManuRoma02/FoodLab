# Project Guidelines & Preferences

- **Telegram Delivery**: Send the APK via Telegram only upon explicit user request (e.g. "mandami l'apk su telegram", "invia l'apk", etc.). Do not send automatically on every background build.

---

## 1. UI, Motion & Animation Standards (Apple Design + Emil Kowalski)

All UI development in Jetpack Compose and AppWidgets must strictly follow the combined **Apple Design System** and **Emil Kowalski Animation Philosophy**:

* **Gating Frequency**:
  * 100+ times/day interactions (keyboard/numpad, fast toggles) have **zero** animations (instant state change).
  * Tens of times/day interactions (meal item click, tab navigation) use near-imperceptible, subtle transitions.
  * Delight budget (bouncy springs, celebrations) is strictly reserved for rare milestone events (e.g. daily target reached, streak progress).
* **GPU & Render Performance**:
  * Always animate via `Modifier.graphicsLayer { ... }` (`translation`, `scale`, `alpha`, `rotation`) to run on the GPU RenderThread and skip Recomposition/Layout passes.
  * Never animate layout dimensions (`width`, `height`, `padding`) during continuous motion.
  * Never expand from zero: all scale entrances start from `scale(0.90f - 0.95f)` with alpha fade.
* **Physics & Tactility**:
  * Use physical springs (`Spring.DampingRatioMediumBouncy` or `Spring.DampingRatioNoBouncy`) instead of robotic linear tweens.
  * Exits must always be 30-40% faster than entrances.
  * Pair spring peaks and key actions with subtle haptic feedback (`HapticFeedbackType.LongPress` or `TextHandleMove`).
* **Spatial Consistency & Materials**:
  * Retain translucent materials, near-black surfaces (`#121212`), slate text, and macro colors (protein green `#22C55E`, carb amber `#F59E0B`, fat cyan `#06B6D4`).

---

## 2. Engineering & Refactoring Discipline (Karpathy Guidelines)

All agents working on FoodLab must strictly uphold Andrej Karpathy's coding guidelines:

* **1. Think Before Coding**:
  * Explicitly state assumptions before touching code.
  * If requirements or design details are ambiguous, ask instead of guessing.
  * Present trade-offs clearly; if a simpler architecture or existing utility exists, advocate for it.
* **2. Simplicity First**:
  * Implement the minimum code that solves the problem. Zero speculative features or unused configurability.
  * No fake abstractions or wrapper classes for single-use code.
  * Favor concise, readable, idiomatic Kotlin.
* **3. Surgical Changes**:
  * Touch only what is necessary to satisfy the prompt.
  * Keep diffs minimal, clean, and directly reviewable.
  * Do not reformat unrelated files or touch adjacent features without explicit instruction.
* **4. Goal-Driven Execution**:
  * Define unambiguous, verifiable success criteria before editing.
  * Verify changes through unit tests or runtime builds before claiming completion.

---

## 3. Codebase Memory & Architectural Rigor

* Maintain structural knowledge graph continuity across sessions (`codebase-memory-mcp` / tree-sitter AST).
* Respect established canonical conventions:
  * Room DB migrations (currently v6) and Room entities.
  * Proportional 2×2 layout rules for AppWidgets (`widget_pet_status.xml`, `widget_daily_calories.xml`).
  * Canonical meal enum names (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`).
  * JDK 22 and non-destructive Gradle builds.
