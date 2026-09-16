# 3D Skeletal Mascot Companion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a high-performance 3D Skeletal Mascot Companion with bone rigging, PBR physical jelly shaders, orbiting ghost companion, real-time touch/spring physics, and UI-driven animation states for the FoodLab Android Dashboard.

**Architecture:** An inlined Three.js WebGL engine running in a transparent, hardware-accelerated Android WebView, with a 5-bone skeletal rig and SkinnedMesh deformation. Kotlin Composables observe dashboard state and dispatch state transitions via JavaScript bridge.

**Tech Stack:** Android (Kotlin, Jetpack Compose, AndroidView WebView), Three.js r184 (embedded WebGL PBR physical shaders, SkinnedMesh bone hierarchy), Python 3 generation scripts, Gradle test runner.

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-28-mascot-3d-companion-design.md` (verbatim).
- Surfaces: in-app Dashboard (`DashboardComponents.kt`) and home widget (`widget_pet_status.xml`).
- 5-bone skeletal rig on the monster (`rootBone`, `spineBone`, `headBone`, `leftArmBone`, `rightArmBone`).
- Ghost companion: simple floating mesh with 3D elliptical orbit and skirt ripple physics.
- Real-time touch interaction: 3D drag rotation with spring return, pupil tracking, squash & stretch jump on tap.
- State contracts: `idle`, `typing`, `cooking`, `party`, `overTarget`, `wave`.
- Lifecycle awareness: `window.setMascotPaused(boolean)` stops render loop off-screen to preserve battery.
- 0ms zero-blank-frame guarantee via underlying placeholder image in Compose Box.

---

### Task 1: Upgraded Three.js 3D Skeletal Engine & Inlined HTML Generation

**Files:**
- Modify: `scripts/build_inlined_3d_html.py`
- Modify: `scripts/write_mascot_html.py`
- Create/Overwrite: `app/src/main/assets/mascot_3d/index.html`

**Interfaces:**
- Consumes: `app/src/main/assets/mascot_3d/three.min.js`
- Produces: `app/src/main/assets/mascot_3d/index.html` with JavaScript API:
  - `window.setMascotState(name: string)`
  - `window.setMascotPaused(paused: boolean)`
  - `window.triggerTap()`
  - `AndroidBridge.onMascotTapped()`

- [ ] **Step 1: Update `scripts/build_inlined_3d_html.py` with complete state transitions & enhanced skeletal kinematics**

Implement the full Three.js engine script including:
- SkinnedMesh vertex weights and organic jelly geometry deformation.
- PBR Physical material with transmission and inner subsurface scattering core.
- 3D orbiting ghost companion with blushing cheeks and skirt ripple motion.
- Drag rotation with spring return and pupil tracking.
- State handler supporting `idle`, `typing`, `cooking`, `party`, `overTarget`, and `wave`.
- `setMascotPaused` handler.

- [ ] **Step 2: Execute build script to generate the inlined HTML**

Run: `python scripts/build_inlined_3d_html.py`
Expected: `Updated app/src/main/assets/mascot_3d/index.html with bulletproof size detection & animation loop!`

- [ ] **Step 3: Validate generated HTML asset**

Run: `Get-Item app/src/main/assets/mascot_3d/index.html | Select-Object Length, LastWriteTime`
Expected: File exists with non-zero size (>600 KB).

---

### Task 2: Kotlin `MascotState` Model & Compose WebView Host

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/MascotComponents.kt`
- Test: `app/src/test/java/com/example/ui/MascotComponentsTest.kt`

**Interfaces:**
- Consumes: `app/src/main/assets/mascot_3d/index.html`
- Produces: 
  - `sealed interface MascotState { Idle, OverTarget, Typing, Studying, Cooking, Party, Wave }`
  - `AnimatedFoodLabMascot(state: MascotState, modifier: Modifier)`
  - `ThreeJsMascot3DView(state: MascotState, onTapped: () -> Unit, modifier: Modifier)`

- [ ] **Step 1: Write/update unit tests for MascotState**

Update `app/src/test/java/com/example/ui/MascotComponentsTest.kt` to assert all states and their string mapping keys:
```kotlin
package com.example.ui

import com.example.ui.screens.MascotState
import org.junit.Assert.assertEquals
import org.junit.Test

class MascotComponentsTest {
    @Test
    fun testMascotStates_areDistinctAndInstantiable() {
        val states = listOf(
            MascotState.Idle,
            MascotState.OverTarget,
            MascotState.Typing,
            MascotState.Studying,
            MascotState.Cooking,
            MascotState.Party,
            MascotState.Wave
        )
        assertEquals(7, states.distinct().size)
    }
}
```

- [ ] **Step 2: Update `MascotComponents.kt` with Wave state and lifecycle pause/resume**

Add `Wave` state and integrate `DisposableEffect` / lifecycle observer that invokes `window.setMascotPaused(true)` when paused and `window.setMascotPaused(false)` when resumed.

- [ ] **Step 3: Run unit tests**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.ui.MascotComponentsTest`
Expected: `BUILD SUCCESSFUL` and all tests pass.

---

### Task 3: Dashboard Screen State Event Wiring

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/DashboardComponents.kt`

**Interfaces:**
- Consumes: `MascotState` from `MascotComponents.kt`
- Produces: Live reactive mascot on dashboard card

- [ ] **Step 1: Update mascot state derivation in `DashboardComponents.kt`**

Wire the `AnimatedFoodLabMascot` invocation to account for calorie goals, typing, and cooking states:
```kotlin
AnimatedFoodLabMascot(
    state = when {
        remainingCalories < 0 -> MascotState.OverTarget
        isMascotTyping -> MascotState.Typing
        else -> MascotState.Idle
    },
    modifier = Modifier.size(125.dp)
)
```

- [ ] **Step 2: Verify compilation and tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`.

---

### Task 4: Full App Build & End-to-End Verification

**Files:**
- Check: All modified files

- [ ] **Step 1: Run comprehensive build verification**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verify HTML asset inside APK structure**

Confirm that `mascot_3d/index.html` packages cleanly without warnings.
