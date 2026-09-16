# FoodLab 3D Skeletal Mascot Companion Design

**Date:** 2026-08-28  
**Status:** Approved by User  
**Surface:** FoodLab Android App (Dashboard & Home Widget)

---

## 1. Executive Summary

This document specifies the technical architecture and visual design for the upgraded **FoodLab 3D Skeletal Mascot Companion**. The upgrade elevates the original Monster + Ghost duo into a fully rigged, interactive 3D companion rendered via an inlined, hardware-accelerated Three.js WebGL engine embedded in the Android app. 

Key features include a 5-bone skeletal rig with organic jelly skinning deformation, realistic PBR physical materials with subsurface scattering, an expressive Pixar-style 3D face, an orbiting ethereal ghost companion, real-time touch physics (squash & stretch jump, 3D drag rotation with spring return, pupil tracking), and dynamic UI-driven animation states.

---

## 2. Architecture & Components

### 2.1 3D Skeletal Engine & Three.js Runtime
* **Asset Location:** `app/src/main/assets/mascot_3d/index.html` (inlined engine with embedded `three.min.js`).
* **Skeletal Rig:** 5-bone hierarchy:
  * `rootBone` (base/pelvis anchor)
  * `spineBone` (mid-body expansion, breathing, dynamic bending)
  * `headBone` (head tilt, smile & 3D eye assembly tracking)
  * `leftArmBone` / `rightArmBone` (gestures, typing, cooking, waving, party celebrations)
* **Organic Slime Deformation:** Pear-shaped droplet geometry with calculated vertex skin weights. Smooth skinning distributes skeletal movement naturally across the body.
* **Materials & Shaders:**
  * **Monster Body:** `MeshPhysicalMaterial` (Color: `0x22c55e`, Emissive: `0x052e16`, Transmission: `0.42`, Clearcoat: `1.0`, Roughness: `0.1`, IOR: `1.45`).
  * **Subsurface Core:** Internal pulsating sphere (`0x4ade80`, opacity `0.45`) simulating inner bioluminescence.
  * **Ghost Companion:** `MeshStandardMaterial` (Color: `0xffffff`, Emissive: `0x38bdf8`, EmissiveIntensity: `0.65`, Opacity: `0.94`, Blushing Cheeks: `0xf472b6`).
* **Environment & Lighting:**
  * 3-point cinematic lighting: Key Light (`0xfffaed`), Fill Light (`0x38bdf8`), Rim Backlight (`0x4ade80`).
  * Ambient magical dust particles with additive blending.
  * Ground contact shadow dynamically scaling with jump physics.

---

## 3. Real-Time Interaction & State Machine

### 3.1 Touch & Physics System
* **3D Drag & Spring Return:** Pointer drag rotates the duo in 3D (yaw $\pm 0.55\text{ rad}$, pitch $\pm 0.55\text{ rad}$). Spring physics smoothly re-centers the model upon touch release.
* **Eye Pupil Tracking:** Pupils dynamically follow user touch and rotation coordinates.
* **Tap Elasticity:** Tap triggers an elastic vertical jump with squash & stretch geometry deformation and ground shadow scaling.
* **Autonomous Idle:** Natural breathing cycles, counterbalance head sways, and randomized eye blinking.

### 3.2 Animation State Machine (`MascotState`)

| Mascot State | Character Motion & Expression | Accent / Glow Color | Trigger Condition |
| :--- | :--- | :--- | :--- |
| **`Idle`** | Gentle breathing, floating ghost orbit, soft sway | Emerald Green (`0x22c55e`) | Default resting state |
| **`Typing` / `Think`** | Rapid micro-arm typing motions, curious head tilt | Electric Cyan (`0x38bdf8`) | Food search, AI assistant prompt |
| **`Cooking`** | Active stirring arm gestures, kitchen rhythm | Warm Amber / Green | Recipe cooking, meal logging progress |
| **`Party` / `Celebrate`**| Victory arm waves, enthusiastic head bop, fast ghost orbit | Golden Yellow & Pink (`0xffd600`)| Calorie goal met, streak milestone |
| **`OverTarget` / `Error`** | Alert posture, ghost flinch / recoil | Coral Red (`0xef4444`) | Daily calorie limit exceeded |
| **`Wave`** | Welcoming hand wave | Emerald Green (`0x22c55e`) | Initial session open / dashboard load |

---

## 4. Android Host & Jetpack Compose Integration

### 4.1 Composable Structure (`MascotComponents.kt`)
* **`AnimatedFoodLabMascot`**: Top-level composable placed on the Dashboard screen (`DashboardComponents.kt`).
* **`ThreeJsMascot3DView`**: Embeds hardware-accelerated Android `WebView`:
  * `setLayerType(View.LAYER_TYPE_HARDWARE, null)`
  * Transparent background (`setBackgroundColor(0)`)
  * JavaScript bridge `AndroidBridge.onMascotTapped()` for haptics (`HapticFeedbackType.LongPress`) and audio (`LocalSoundManager`).

### 4.2 Lifecycle & Battery Optimization
* Evaluation of `setMascotPaused(true)` when home screen is backgrounded or leaves composition.
* Stops `requestAnimationFrame` loop to ensure zero CPU/GPU overhead when off-screen.

### 4.3 Fallback & Resilience
* Underlying `Image(painterResource(R.drawable.foodlab_mascot_duo))` eliminates any blank frame during WebGL context creation.
* Automatic fallback to high-resolution 2D visual if WebGL is unsupported.
* Home widget (`widget_pet_status.xml`) updated to match modern aesthetic while preserving existing data providers.

---

## 5. Verification & Testing

* **Rendering & Physics:** Verify 60/120 FPS frame rate, transparent background, and spring touch return in Android WebView.
* **State Machine Verification:** Verify correct visual transition for all states (`Idle`, `Typing`, `Cooking`, `Party`, `OverTarget`, `Wave`).
* **Lifecycle & Battery:** Verify WebGL animation loop halts when navigating away from dashboard.
* **Unit Tests:** Verify `MascotComponentsTest` and state mapping logic in test suite.
