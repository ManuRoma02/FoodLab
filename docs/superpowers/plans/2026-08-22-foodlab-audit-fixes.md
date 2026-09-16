# FoodLab Audit Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Risolvere sistematicamente tutte le anomalie riscontrate nell'audit di FoodLab (Room database integrity, calcoli nutrizionali sicuri, streak calcolata con target a 3 livelli, ricerca remota ottimizzata senza delay su errori non transitori, allineamento calcolo calorie widget, rimozione doppi broadcast e ottimizzazione rendering GPU mascotte).

**Architecture:** Clean Architecture a 3 layer (UI/Compose -> Domain/Util -> Data Room/Retrofit), Unidirectional Data Flow, StateFlow reattivi.

**Tech Stack:** Kotlin, Jetpack Compose, Android Room Database v10, Retrofit 2 + Moshi, WorkManager, AppWidgets.

---

### Task 1: Integrità Database Room & Rimozione Fallback Distruttivo
**Files:**
- Modify: `app/src/main/java/com/example/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt`

- [ ] **Step 1:** Rimuovere `.fallbackToDestructiveMigration(true)` in `AppDatabase.kt`.
- [ ] **Step 2:** Rimuovere import non utilizzato di `WeightEntry` in `AnalyticsScreen.kt`.
- [ ] **Step 3:** Verificare che `AppDatabase` compili e i test passino.

---

### Task 2: Calcoli Nutrizionali Sicuri & Arrotondamento Rescale
**Files:**
- Modify: `app/src/main/java/com/example/data/Entities.kt`
- Modify: `app/src/main/java/com/example/util/NutritionCalculator.kt`
- Modify: `app/src/test/java/com/example/util/NutritionCalculatorTest.kt`

- [ ] **Step 1:** In `Entities.kt`, aggiornare i getter di `MealTemplateItem` (`calories`, `protein`, `carbs`, `fat`) delegando a `NutritionCalculator.fromPer100g`.
- [ ] **Step 2:** In `NutritionCalculator.kt`, aggiornare `rescaleExisting` per usare `kotlin.math.roundToInt()` sul calcolo delle calorie.
- [ ] **Step 3:** Aggiungere unit test in `NutritionCalculatorTest.kt` e verificare il passaggio dei test.

---

### Task 3: Risoluzione Streak con Target a 3 Livelli & Mezzanotte
**Files:**
- Modify: `app/src/main/java/com/example/util/StreakCalculator.kt`
- Modify: `app/src/main/java/com/example/MainActivity.kt`
- Modify: `app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt`
- Modify: `app/src/test/java/com/example/util/StreakCalculatorTest.kt`

- [ ] **Step 1:** Aggiornare `StreakCalculator.kt` per risolvere il target a 3 livelli (data specifica -> giorno settimanale -> FIXED) per ciascun giorno e verificare che le calorie siano entro il target (o ±10%).
- [ ] **Step 2:** In `MainActivity.kt`, collegare `LaunchedEffect` con `dashboardViewModel.todayDate` per aggiornare `currentDateState` al cambio di data a mezzanotte.
- [ ] **Step 3:** In `AnalyticsScreen.kt`, risolvere il target dinamicamente in `SlimeCalendarGrid`.
- [ ] **Step 4:** Aggiornare ed eseguire `StreakCalculatorTest.kt`.

---

### Task 4: Ottimizzazione Rete OFF & Deduplicazione Ricerca
**Files:**
- Modify: `app/src/main/java/com/example/ui/FoodSearchViewModel.kt`
- Modify: `app/src/main/java/com/example/ui/SearchOptimizer.kt`
- Modify: `app/src/test/java/com/example/ui/SearchOptimizerTest.kt`

- [ ] **Step 1:** In `FoodSearchViewModel.kt`, gestire gli errori HTTP 4xx e 504 senza eseguire retry con delay quando offline o non transitorio.
- [ ] **Step 2:** In `SearchOptimizer.kt`, rimuovere il bonus per `food.id < 0` e rendere `dedupeKey` più resiliente.
- [ ] **Step 3:** Eseguire `SearchOptimizerTest.kt`.

---

### Task 5: Allineamento Widget & Sensori
**Files:**
- Modify: `app/src/main/java/com/example/widget/DailyCaloriesWidgetProvider.kt`
- Modify: `app/src/main/java/com/example/widget/PetStatusWidgetProvider.kt`
- Modify: `app/src/main/java/com/example/util/WidgetUpdater.kt`
- Modify: `app/src/main/java/com/example/ui/PedometerViewModel.kt`

- [ ] **Step 1:** Nei widget `DailyCaloriesWidgetProvider` e `PetStatusWidgetProvider`, calcolare le calorie bruciate considerando il peso reale dal database tramite `NutritionCalculator.calculateCaloriesBurned`.
- [ ] **Step 2:** In `WidgetUpdater.kt`, rimuovere il doppio broadcast `"com.example.UPDATE_WIDGETS"`.
- [ ] **Step 3:** In `PedometerViewModel.kt`, raffinare la derivata dell'accelerometro per ridurre i falsi positivi.

---

### Task 6: Ottimizzazione Performance Rendering Mascotte
**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/MascotComponents.kt`

- [ ] **Step 1:** In `AnimatedFoodLabMascot`, spostare la lettura di `idlePhase` e il calcolo delle trasformazioni all'interno del blocco `graphicsLayer { ... }` per eliminare la ricomposizione continua ad ogni frame.

---

### Task 7: Verifica Completa & Build
- [ ] **Step 1:** Eseguire `gradle testDebugUnitTest` per validare tutte le suite di test.
- [ ] **Step 2:** Eseguire `gradle assembleDebug` per validare la compilazione.
