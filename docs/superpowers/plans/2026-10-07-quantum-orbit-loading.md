# Quantum Nutrient Orbit Startup Animation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the "Quantum Nutrient Orbit" startup animation in Jetpack Compose, featuring 3D hardware-accelerated Canvas orbits for macronutrients, central symbol breathing, quantum convergence lock with haptic feedback, and an optical spring exit.

**Architecture:** A lightweight, pure-GPU RenderThread Compose component using `Modifier.graphicsLayer`, Canvas `drawBehind`/`drawIntoCanvas`, and `Animatable` physics. Runs cold start transition on OLED black `#0C0E12`, fires haptic pulse upon convergence, and invokes `onLoadingComplete` to seamlessly reveal `MainActivity`'s Dashboard.

**Tech Stack:** Kotlin 2.x, Jetpack Compose, Compose Animation Core (`Animatable`, `spring`, `tween`), Robolectric, JUnit 4.

## Global Constraints
- Target background color: `#0C0E12` (strictly matching `splash_background` in `res/values/colors.xml`).
- Macro Palette: Protein `#22C55E`, Carbs `#F59E0B`, Fats `#06B6D4`.
- Performance: Zero layout re-measurements during continuous motion. All scale/alpha/translation on `graphicsLayer`.
- Haptics: Coordinated `HapticFeedbackType.LongPress` at convergence lock.
- Total animation duration: ~1.75s with a 2.2s safety fallback.

---

### Task 1: Create Unit Test for Startup Loading Screen

**Files:**
- Create: `app/src/test/java/com/example/ui/AppLoadingScreenTest.kt`

**Interfaces:**
- Consumes: `AppLoadingScreen(onLoadingComplete: () -> Unit, modifier: Modifier)`
- Produces: Robolectric test asserting screen render and completion callback.

- [ ] **Step 1: Write the test class**

```kotlin
package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import com.example.ui.screens.AppLoadingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLoadingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAppLoadingScreenRendersAndCompletes() {
        var completed = false
        composeTestRule.setContent {
            AppLoadingScreen(
                onLoadingComplete = { completed = true }
            )
        }

        // Verify root container is present
        composeTestRule.onNodeWithTag("quantum_loading_root").assertIsDisplayed()

        // Advance clock to trigger animation completion
        composeTestRule.mainClock.advanceTimeBy(2500)
        assertTrue("onLoadingComplete should be invoked after animation finishes", completed)
    }
}
```

- [ ] **Step 2: Run test to verify failure or compile failure**

Run: `.\gradlew testDebugUnitTest --tests "com.example.ui.AppLoadingScreenTest"`
Expected: FAIL or error indicating missing `quantum_loading_root` testTag.

- [ ] **Step 3: Commit test file**

```bash
git add app/src/test/java/com/example/ui/AppLoadingScreenTest.kt
git commit -m "test: add AppLoadingScreen Robolectric unit test"
```

---

### Task 2: Implement Quantum Nutrient Orbit in `AppLoadingScreen.kt`

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/AppLoadingScreen.kt`

**Interfaces:**
- Consumes: `foodlab_launcher_symbol` drawable, `appleSpring` physics, Compose Animation core.
- Produces: `@Composable fun AppLoadingScreen(onLoadingComplete: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Implement full high-precision Quantum Orbit Loading Screen**

Replace contents of `app/src/main/java/com/example/ui/screens/AppLoadingScreen.kt` with:
- OLED Background `#0C0E12` with subtle radial core glow.
- 3D elliptical orbits on `Canvas`:
  - Protein orbit (`#22C55E`, angle `+35°`)
  - Carbohydrate orbit (`#F59E0B`, angle `-40°`)
  - Fat orbit (`#06B6D4`, angle `0°`)
- Smooth progress indicator (`0%` -> `100%`) with typography "FOODLAB QUANTUM SYNC".
- Convergence lock collapsing radii into the center with soft bloom.
- Haptic feedback pulse on convergence.
- Exit scale expansion (`scale(1.0f -> 1.06f)`) and alpha fade to 0f.
- Test tag `quantum_loading_root` on root container.

- [ ] **Step 2: Run Robolectric unit test to verify it passes**

Run: `.\gradlew testDebugUnitTest --tests "com.example.ui.AppLoadingScreenTest"`
Expected: PASS.

- [ ] **Step 3: Commit implementation**

```bash
git add app/src/main/java/com/example/ui/screens/AppLoadingScreen.kt
git commit -m "feat: implement quantum nutrient orbit startup animation"
```

---

### Task 3: Assemble Debug APK & Verify Build Integrity

**Files:**
- Verify: Full project compilation.

- [ ] **Step 1: Run complete Gradle assembleDebug**

Run: `.\gradlew assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verify git status and clean working tree**

Run: `git status`
Expected: Clean working tree on committed changes.
