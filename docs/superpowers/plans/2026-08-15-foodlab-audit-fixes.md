# FoodLab Audit Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Risolvere sistematicamente tutte le anomalie riscontrate nell'audit di FoodLab (Room database entity consistency, ricerca remota senza 2.5s loop, calcoli nutrizionali sicuri, WorkManager initial delays, accelerometro filtrato, target a 3 livelli unificato e rimozione dead code).

**Architecture:** Mantenimento della Clean Architecture stratificata (UI/Compose -> Domain/Util -> Data Room/Retrofit), consolidamento dei contratti nel Repository e ViewModels, eliminazione del debito tecnico.

**Tech Stack:** Kotlin, Jetpack Compose, Android Room Database v9, Retrofit 2 + Moshi, WorkManager, Android SensorManager, AppWidgets.

---

### Task 1: Integrità Database Room & DAO Repository
**Files:**
- Modify: `app/src/main/java/com/example/data/AppDatabase.kt`
- Modify: `app/src/main/java/com/example/data/Repository.kt`
- Modify: `app/src/main/java/com/example/FoodLabApplication.kt`

- [ ] **Step 1:** Aggiungere `ShoppingItem::class` a `@Database(entities = [...])` e `abstract fun shoppingItemDao(): ShoppingItemDao` in `AppDatabase.kt`.
- [ ] **Step 2:** Aggiornare `FoodLabApplication.kt` e `Repository.kt` per passare ed esporre `ShoppingItemDao`.
- [ ] **Step 3:** Eseguire test unitari database per verificare la conformità dello schema Room.

---

### Task 2: Ottimizzazione Rete e Rimozione Retry Loop 2.5s
**Files:**
- Modify: `app/src/main/java/com/example/ui/FoodSearchViewModel.kt`
- Test: `app/src/test/java/com/example/ui/SearchOptimizerTest.kt`

- [ ] **Step 1:** Modificare `searchOpenFoodFacts` in `FoodSearchViewModel.kt`: se `response.products` è vuoto (HTTP 200), ritornare subito `emptyList()` senza eseguire i 5 cicli di delay.
- [ ] **Step 2:** Verificare con unit test che la ricerca non introduca ritardi ingiustificati.

---

### Task 3: Calcoli Nutrizionali Sicuri e Clamping
**Files:**
- Modify: `app/src/main/java/com/example/util/NutritionCalculator.kt`
- Modify: `app/src/main/java/com/example/ui/DashboardViewModel.kt`
- Modify: `app/src/main/java/com/example/ui/screens/FoodDetailScreen.kt`
- Create/Modify: `app/src/test/java/com/example/util/NutritionCalculatorTest.kt`

- [ ] **Step 1:** Scrivere unit test in `NutritionCalculatorTest.kt` per verificare la gestione di grammature $\le 0$, negative e valori orfani.
- [ ] **Step 2:** Aggiornare `NutritionCalculator.kt` con controlli difensivi su input negativi o non finiti.
- [ ] **Step 3:** Aggiornare `DashboardViewModel.updateMealQuantity` per limitare le grammature tra 1 e 5000g.
- [ ] **Step 4:** Allineare `FoodDetailScreen.kt` per utilizzare i grammi arrotondati sia nell'anteprima che nel salvataggio.

---

### Task 4: Schedulazione Notifiche WorkManager con Initial Delay
**Files:**
- Modify: `app/src/main/java/com/example/util/NotificationScheduler.kt`
- Delete: `app/src/main/java/com/example/util/DailyReminderWorker.kt`
- Modify: `app/src/main/java/com/example/util/ReminderScheduler.kt`
- Modify: `app/src/test/java/com/example/util/NotificationSchedulerTest.kt`

- [ ] **Step 1:** Implementare `calculateInitialDelay(targetHour, targetMinute)` in `NotificationScheduler.kt`.
- [ ] **Step 2:** Applicare i delay a colazione (08:30), pranzo (13:00), cena (20:00) e streak guard (21:30).
- [ ] **Step 3:** Eliminare `DailyReminderWorker.kt` e consolidare `ReminderScheduler.kt`.
- [ ] **Step 4:** Verificare i test in `NotificationSchedulerTest.kt`.

---

### Task 5: Filtro Debounce Accelerometro Contapassi
**Files:**
- Modify: `app/src/main/java/com/example/ui/PedometerViewModel.kt`

- [ ] **Step 1:** Inserire un controllo temporale di almeno 300ms tra picchi rilevati dall'accelerometro in `PedometerViewModel.kt`.

---

### Task 6: Unificazione Risoluzione Target a 3 Livelli
**Files:**
- Modify: `app/src/main/java/com/example/data/Repository.kt`
- Modify: `app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt`
- Modify: `app/src/main/java/com/example/widget/PetStatusWidgetProvider.kt`
- Modify: `app/src/main/java/com/example/widget/DailyCaloriesWidgetProvider.kt`

- [ ] **Step 1:** Aggiungere metodo helper di risoluzione target per giorno settimanale nel Repository / DAO.
- [ ] **Step 2:** Utilizzare la risoluzione coerente in AnalyticsScreen e nei widget.

---

### Task 7: Verifica Completa & Build
- [ ] **Step 1:** Eseguire `gradle testDebugUnitTest` per validare tutte le suite di test.
- [ ] **Step 2:** Eseguire `gradle assembleDebug` per validare la compilazione dell'APK.
