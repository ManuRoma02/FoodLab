# Hero Calorie Widget and Search Screen Apple Design Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the Dashboard Hero Calorie widget with Apple Design and transparent Total Budget breakdown (Base + Steps), and declutter SearchScreen with Spotlight-style layout, Italian error localization, and resilient offline handling.

**Architecture:** 
1. `DashboardComponents.kt` will feature an Apple-inspired `UnifiedHeroHeader` with a clear Budget breakdown equation (Base Target + Steps Burned = Total Budget, minus Consumed = Remaining), interactive breakdown card, and fluid progress bar.
2. `FoodSearchViewModel.kt` will localize error messages into Italian and preserve local/cached search results during network failures.
3. `SearchScreen.kt` will adopt an Apple Spotlight style with clean search chrome, compact segmented control, non-intrusive meal log summary, and graceful offline banner with retry.

**Tech Stack:** Jetpack Compose, Kotlin, Material 3, Apple Design tokens & springs (`AppleSprings`, `AppleHaptics`).

---

### Task 1: Calorie Hero Widget Unit Tests & Logic Formulation

**Files:**
- Create: `app/src/test/java/com/example/ui/DashboardHeroCalorieTest.kt`

- [ ] **Step 1: Write unit tests verifying the total budget and calorie breakdown formulation**
- [ ] **Step 2: Run test with `./gradlew testDebugUnitTest --tests com.example.ui.DashboardHeroCalorieTest` to verify it passes**

---

### Task 2: Redesign UnifiedHeroHeader in DashboardComponents.kt

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/DashboardComponents.kt:1070-1300`
- Modify: `app/src/main/java/com/example/ui/screens/DashboardScreen.kt:240-275`

- [ ] **Step 1: Implement the Expandable Total Budget card in UnifiedHeroHeader**
  - Large display number for remaining calories with negative tracking and status badge (`IN BUDGET` / `IN ECCESSO`).
  - Total Budget breakdown container (`surfaceContainerLow`, 20.dp rounded corners, 0.5.dp outline border) showing:
    - Base target + Burned steps = Total Budget
    - Consumed calories with smooth progress bar
    - Tap interaction with `AppleHaptics.tactileClick()` expanding details (steps, km, burned kcal)
  - Synchronize progress calculation to respect total budget: `consumed / (target + burned)`.
- [ ] **Step 2: Connect state in DashboardScreen.kt**
- [ ] **Step 3: Run unit tests to verify no regressions**

---

### Task 3: Localize and Harden FoodSearchViewModel Offline & Network Error Handling

**Files:**
- Modify: `app/src/main/java/com/example/ui/FoodSearchViewModel.kt:180-210`
- Create: `app/src/test/java/com/example/ui/FoodSearchViewModelOfflineTest.kt`

- [ ] **Step 1: Write test for FoodSearchViewModel Italian error message and offline fallback**
- [ ] **Step 2: Update FoodSearchViewModel error message to Italian and prevent erasing cached/local items**
- [ ] **Step 3: Add `retryOnlineSearch()` method in FoodSearchViewModel**
- [ ] **Step 4: Run unit tests and verify they pass**

---

### Task 4: Declutter and Restyle SearchScreen with Apple Spotlight Design

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/SearchScreen.kt:170-350`

- [ ] **Step 1: Replace cluttered 5-tag horizontal row with compact Apple Segmented Control**
- [ ] **Step 2: Add non-blocking Italian offline/network warning banner with "Riprova" button**
- [ ] **Step 3: Compact already-logged meal section to avoid crowding the initial screen**
- [ ] **Step 4: Relocate "+ Nuovo Alimento" and "+ Nuova Ricetta" to clean bottom actions**
- [ ] **Step 5: Verify build with `./gradlew assembleDebug`**

---

### Task 5: Build APK, Deliver to Telegram, and Push Commit

**Files:**
- All modified files

- [ ] **Step 1: Run full unit test suite `./gradlew testDebugUnitTest`**
- [ ] **Step 2: Build release/debug APK `./gradlew assembleDebug`**
- [ ] **Step 3: Deliver APK via Telegram `send_apk` tool**
- [ ] **Step 4: Git commit and git push to repository**
