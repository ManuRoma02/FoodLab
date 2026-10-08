# Gemini Food Vision Scanner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Integrate a visual food scanner powered by Google Gemini API structured output to analyze meal photos, estimate calories and macronutrients, and provide an Apple-style tactile review sheet to log foods instantly into FoodLab.

**Architecture:** A robust service layer (`GeminiFoodScannerService`) uses OkHttp and Moshi to send compressed photo bytes to Gemini 2.5 Flash using JSON Schema enforcement (`responseSchema`), returning structured nutritional data. A `GeminiFoodScanViewModel` manages the scanning lifecycle, photo compression, and state. An Apple-style bottom sheet (`GeminiFoodScanSheet`) presents the dish breakdown with tactile macro pills, allowing instant adjustment and single-tap logging into FoodLab's Room database and home widgets.

**Tech Stack:** Kotlin, Jetpack Compose, OkHttp, Moshi, Android Photo Picker / ActivityResultContracts, Room DB, Gemini REST API (`gemini-2.5-flash`).

## Global Constraints

- Android `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`.
- Strict Apple Design & Emil Kowalski animation standards: spring physics, GPU `Modifier.graphicsLayer` transformations, macro colors (protein `#22C55E`, carb `#F59E0B`, fat `#06B6D4`), near-black background `#121212`.
- Canonical meal types (`BREAKFAST`, `LUNCH`, `DINNER`, `SNACK`) via `com.example.util.MealTypes`.
- Non-destructive, backward-compatible Room database interactions.
- Graceful offline and missing-API-key handling without crashes.
- Verification via `./gradlew.bat testDebugUnitTest` before completion.

---

### Task 1: Gemini Data Models, API Request/Response and Service Interface

**Files:**
- Create: `app/src/main/java/com/example/data/GeminiFoodScannerModels.kt`
- Create: `app/src/main/java/com/example/data/GeminiFoodScannerService.kt`
- Test: `app/src/test/java/com/example/data/GeminiFoodScannerModelsTest.kt`

**Interfaces:**
- Consumes: Moshi JSON parsing, `BuildConfig.GEMINI_API_KEY`.
- Produces: `GeminiFoodScanResult`, `GeminiFoodScannerService` interface with `suspend fun analyzeFoodImage(jpegBytes: ByteArray): Result<GeminiFoodScanResult>`.

- [ ] **Step 1: Write the failing test for JSON serialization and parsing**
Write `GeminiFoodScannerModelsTest.kt` verifying that Gemini JSON schema responses parse into `GeminiFoodScanResult` with accurate calories, macros, and confidence.
- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew.bat testDebugUnitTest --tests com.example.data.GeminiFoodScannerModelsTest`.
- [ ] **Step 3: Implement data models and OkHttp Gemini service**
Implement `GeminiFoodScannerModels.kt` with `@JsonClass(generateAdapter = true)` and `GeminiFoodScannerService.kt` implementing the structured REST call with fallback for missing API key.
- [ ] **Step 4: Run tests and verify passing**
Verify `GeminiFoodScannerModelsTest` passes.
- [ ] **Step 5: Commit**
Commit task changes: `git commit -m "feat(gemini): add Gemini food scanner data models and service"`.

---

### Task 2: GeminiFoodScanViewModel and Meal Logging Integration

**Files:**
- Create: `app/src/main/java/com/example/ui/GeminiFoodScanViewModel.kt`
- Test: `app/src/test/java/com/example/ui/GeminiFoodScanViewModelTest.kt`

**Interfaces:**
- Consumes: `GeminiFoodScannerService`, `FoodLabRepository`.
- Produces: `GeminiFoodScanViewModel` exposing `StateFlow<GeminiScanUiState>`, `analyzeImage(bytes)`, and `confirmAndLogMeal(result, mealType, date)`.

- [ ] **Step 1: Write the failing test for ViewModel state flow**
Write `GeminiFoodScanViewModelTest.kt` testing transitions: `Idle` -> `Analyzing` -> `Success` and testing meal insertion via repository.
- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew.bat testDebugUnitTest --tests com.example.ui.GeminiFoodScanViewModelTest`.
- [ ] **Step 3: Implement GeminiFoodScanViewModel**
Implement `GeminiFoodScanViewModel.kt` with state management, image processing helper (resizing bitmap to max 1024px JPEG to save bandwidth), and logging to Room + `WidgetUpdater.updateAllWidgets()`.
- [ ] **Step 4: Run tests and verify passing**
Run `./gradlew.bat testDebugUnitTest --tests com.example.ui.GeminiFoodScanViewModelTest`.
- [ ] **Step 5: Commit**
Commit task changes: `git commit -m "feat(gemini): implement GeminiFoodScanViewModel with meal logging"`.

---

### Task 3: Apple-Style Gemini Food Scan Review Bottom Sheet

**Files:**
- Create: `app/src/main/java/com/example/ui/screens/GeminiFoodScanSheet.kt`
- Test: `app/src/test/java/com/example/ui/screens/GeminiFoodScanSheetRobolectricTest.kt`

**Interfaces:**
- Consumes: `GeminiScanUiState`, AppleSprings, AppleHaptics, Macro colors.
- Produces: `GeminiFoodScanSheet` composable with animated scanning orb, dish card, macro pills, editable gram counter, and confirm button.

- [ ] **Step 1: Write Robolectric test for GeminiFoodScanSheet**
Test that `GeminiFoodScanSheet` displays dish name, calorie count, and macro pills when in `Success` state.
- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew.bat testDebugUnitTest --tests com.example.ui.screens.GeminiFoodScanSheetRobolectricTest`.
- [ ] **Step 3: Implement GeminiFoodScanSheet UI**
Build Compose bottom sheet adhering to Apple Design / Emil Kowalski rules: spring scale-in from 0.95f, near-black container `#121212`, tactile haptics on quantity change, clear error retry.
- [ ] **Step 4: Run test and verify passing**
Run `./gradlew.bat testDebugUnitTest --tests com.example.ui.screens.GeminiFoodScanSheetRobolectricTest`.
- [ ] **Step 5: Commit**
Commit task changes: `git commit -m "feat(ui): add Apple-style GeminiFoodScanSheet review dialog"`.

---

### Task 4: SearchScreen Integration with Camera and Photo Picker

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/SearchScreen.kt`
- Test: `app/src/test/java/com/example/ui/screens/SearchScreenGeminiIntegrationTest.kt`

**Interfaces:**
- Consumes: `GeminiFoodScanSheet`, Android `PickVisualMedia` and `TakePicturePreview` contracts.
- Produces: AI Sparkle button in `SearchScreen` top bar and search input trailing row, launching photo capture/picker and presenting `GeminiFoodScanSheet`.

- [ ] **Step 1: Write integration test for SearchScreen AI trigger**
Verify AI action button triggers scan flow.
- [ ] **Step 2: Run test to confirm failure**
Run `./gradlew.bat testDebugUnitTest`.
- [ ] **Step 3: Wire Gemini photo actions in SearchScreen**
Add AI Sparkle IconButton in `SearchScreen.kt` alongside barcode camera button. Connect camera/gallery launcher to `GeminiFoodScanViewModel.analyzeImage`.
- [ ] **Step 4: Run all unit and Robolectric tests**
Run `./gradlew.bat testDebugUnitTest` to guarantee zero regressions.
- [ ] **Step 5: Commit**
Commit task changes: `git commit -m "feat(search): integrate Gemini visual food scanner into SearchScreen"`.
