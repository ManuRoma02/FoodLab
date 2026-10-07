# FoodLab: Quantum Nutrient Orbit Startup Animation Spec

## 1. Overview & Goals
Replace the current startup screen with a high-precision, cinematic startup animation ("Quantum Nutrient Orbit") following the combined Apple Design System and Emil Kowalski animation guidelines.

### Primary Goals
- **Zero-Flicker Cold Start Continuity:** Match window background `#0C0E12` with Android 12+ system splash screen.
- **Cinematic Delight:** Render three 3D elliptical orbits representing macronutrients (Protein `#22C55E`, Carbs `#F59E0B`, Fats `#06B6D4`) circling the central FoodLab core, accelerating into a unified convergence lock.
- **Physical Tactility:** Coordinated haptic pulse (`HapticFeedbackType.LongPress`) at the exact millisecond of quantum orbital convergence.
- **Strict GPU Performance:** Zero layout passes or recomposition re-measures during continuous motion; all motion runs on the RenderThread via `Modifier.graphicsLayer` and hardware-accelerated Canvas drawing.
- **Snappy Exit:** Exit scale expansion (`scale(1.0f -> 1.06f)`) and alpha fade completing in ~200ms to reveal the pre-composed Dashboard seamlessly.

---

## 2. Geometric & Visual Design

### 2.1 The Canvas & Palette
- Background: Solid deep OLED slate `#0C0E12`.
- Ambient Core Glow: Dynamic radial gradient centered at the core (`#22C55E` / `#06B6D4` with alpha 0.10f - 0.25f pulsating at 0.5Hz).
- Central Symbol: FoodLab launcher symbol (`foodlab_launcher_symbol.png`), sized at `72.dp`, floating with gentle harmonic breathing (`sin(t) * 4.dp`).

### 2.2 The 3 Quantum Nutrient Orbits
Drawn via hardware-accelerated `Canvas`:
1. **Protein Orbit (`#22C55E` - Emerald Green):**
   - Incline angle: `+35°`
   - Radius X: `110.dp`, Radius Y: `42.dp`
   - Particle: Lead glowing comet with trailing fading path.
2. **Carbohydrate Orbit (`#F59E0B` - Energetic Amber):**
   - Incline angle: `-40°`
   - Radius X: `100.dp`, Radius Y: `38.dp`
   - Particle: Harmonic counter-rotation.
3. **Healthy Fat Orbit (`#06B6D4` - Cyan Hydration):**
   - Incline angle: `0°` (equatorial plane)
   - Radius X: `120.dp`, Radius Y: `32.dp`
   - Particle: Variable orbital speed.

---

## 3. Cinematic Timeline & Phase Breakdown

| Time Window | Phase | Description | Physics / Animation Spec |
|---|---|---|---|
| `0ms - 280ms` | **Phase 1: Entrance** | Core and orbits enter screen from `scale(0.93f)` and `alpha(0f)` to `1.0f`. | `appleSpring(dampingRatio = 0.72f, stiffness = 280f)` |
| `280ms - 1300ms` | **Phase 2: Charge & Spin** | Orbits accelerate smoothly around the core. Progress counter runs `0% -> 100%`. Minimalist status label displays "FOODLAB QUANTUM SYNC". | Continuous linear rotation phase + `FastOutSlowInEasing` progress curve. |
| `1300ms - 1550ms` | **Phase 3: Quantum Lock & Convergence** | Orbits rapidly collapse inward from their 3D radii to radius `0.dp`, fusing into the central symbol. Soft bloom flare triggers. | Sharp inward spring (`stiffness = Spring.StiffnessMediumHigh`). **Haptic LongPress fired**. |
| `1550ms - 1750ms` | **Phase 4: Optical Exit** | Core scales up to `1.06f` as alpha dissolves to `0f`. Dashboard underneath takes immediate foreground focus. | `tween(200, easing = FastOutLinearInEasing)`. |

---

## 4. Architecture & Component Boundary

### File Modification
- **Target File:** `app/src/main/java/com/example/ui/screens/AppLoadingScreen.kt`
- **Caller:** `MainActivity.kt` lines 337-343:
  ```kotlin
  if (!isAppReady) {
      AppLoadingScreen(
          onLoadingComplete = {
              isAppReady = true
          }
      )
  }
  ```
- **Fallback Guard:** `LaunchedEffect(Unit)` includes a safety delay/timeout of 2200ms ensuring `onLoadingComplete()` is strictly invoked even if any animation listener is cancelled.

---

## 5. Verification & Quality Gates
1. **Compilation Check:** Run `./gradlew assembleDebug` to guarantee zero Kotlin/Compose compilation issues.
2. **Performance Audit:** Motion logic must use `Animatable` and `Canvas` `drawBehind`/`drawIntoCanvas`, without allocating objects inside the draw loop.
3. **Contrast & Theme:** All colors must meet WCAG AAA contrast against `#0C0E12`.
