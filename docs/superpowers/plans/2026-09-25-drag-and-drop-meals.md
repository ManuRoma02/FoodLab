# Drag & Drop Interattivo tra Pasti Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementare il drag & drop interattivo stile Apple per spostare gli alimenti tra i pasti (Colazione, Pranzo, Cena, Snack) tenendo premuto un cibo nella Home, sollevandolo e rilasciandolo sulla card di destinazione.

**Architecture:** Uno stato `MealDragDropState` gestisce le coordinate del puntatore e i rettangoli di collisione (`Rect`) di ogni pasto. `MealItemRow` rileva il Long-Press tramite `pointerInput`, sollevando una `GhostMealCard` fluttuante con molla e ombra fluida. Il pasto di destinazione evidenzia il suo bordo all'hovering, mentre la `LazyColumn` della dashboard supporta l'auto-scroll se il puntatore è prossimo ai bordi. Al rilascio su un pasto valido, il ViewModel aggiorna il `mealType` della `MealEntry` nel database e sincronizza i widget.

**Tech Stack:** Jetpack Compose (PointerInput, GraphicsLayer, Spring Animation, LazyListState auto-scroll), Kotlin Coroutines, Room DB, Haptic Feedback.

## Global Constraints
- Non rompere il tap per la modifica (`onClick` -> `EditMealEntryDialog`) né lo swipe orizzontale a sinistra per eliminare (`SwipeToDismissBox`).
- Rispettare rigorosamente i colori del tema corrente (usare `MaterialTheme.macroColors` e i colori d'accento di ciascun pasto).
- L'ordine universale dei macro (Carboidrati -> Proteine -> Grassi) deve rimanere inalterato nella card fluttuante.
- Non bloccare il thread UI durante il trascinamento o l'auto-scroll.

---

### Task 1: ViewModel & Repository Helper per lo Spostamento Pasto

**Files:**
- Modify: `app/src/main/java/com/example/ui/DashboardViewModel.kt`
- Test: `app/src/test/java/com/example/ui/MealDragDropTest.kt`

**Interfaces:**
- Produces: `DashboardViewModel.moveMealToType(meal: MealEntry, targetMealType: String)`

- [ ] **Step 1: Scrivere il test unitario per lo spostamento del pasto**
  Creare `MealDragDropTest.kt` che testa la logica di mutazione di `MealEntry` e il calcolo del tipo pasto.
- [ ] **Step 2: Implementare `moveMealToType` in `DashboardViewModel`**
  Aggiungere il metodo che invoca `repository.insertMeal(meal.copy(mealType = targetMealType))` e aggiorna i widget.
- [ ] **Step 3: Eseguire il test unitario e verificare che passi**
- [ ] **Step 4: Commit dei cambiamenti**

---

### Task 2: Modulo Stato & Componenti Drag & Drop (`MealDragDropState.kt`)

**Files:**
- Create: `app/src/main/java/com/example/ui/screens/MealDragDropState.kt`
- Test: `app/src/test/java/com/example/ui/MealDragDropTest.kt`

**Interfaces:**
- Produces:
  - `class MealDragDropState`:
    - `var isDragging: Boolean`
    - `var draggedMeal: MealEntry?`
    - `var sourceMealType: String?`
    - `var dragOffset: Offset` (posizione globale a schermo)
    - `var hoveredMealType: String?`
    - `fun registerMealBounds(mealType: String, bounds: Rect)`
    - `fun unregisterMealBounds(mealType: String)`
    - `fun onDragStart(meal: MealEntry, mealType: String, startOffset: Offset)`
    - `fun onDrag(dragAmount: Offset)`
    - `fun onDragEnd(onDrop: (MealEntry, String) -> Unit)`
    - `fun onDragCancel()`
  - `@Composable fun rememberMealDragDropState(): MealDragDropState`
  - `@Composable fun GhostMealCard(meal: MealEntry, offset: Offset, accentColor: Color, modifier: Modifier)`

- [ ] **Step 1: Creare `MealDragDropState.kt` con la logica delle coordinate e collisioni**
- [ ] **Step 2: Implementare il Composable `GhostMealCard` in stile Apple (scala 1.04x, elevazione 14dp, ombra e badge macro C-P-G)**
- [ ] **Step 3: Aggiungere test unitari in `MealDragDropTest.kt` per la collisione dei rettangoli di destinazione**
- [ ] **Step 4: Eseguire i test unitari**
- [ ] **Step 5: Commit dei cambiamenti**

---

### Task 3: Integrazione Gesture in `MealItemRow` e Registrazione Target in `UnifiedMealsCard`

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/DashboardComponents.kt`

**Interfaces:**
- Consumes: `MealDragDropState`
- Modifies: `MealItemRow`, `UnifiedMealsCard`

- [ ] **Step 1: Integrare la rilevazione del Long-Press Drag in `MealItemRow`**
  - Mantenere `SwipeToDismissBox` per lo swipe orizzontale verso sinistra.
  - Aggiungere `pointerInput` per rilevare la pressione prolungata senza bloccare il tap rapido.
  - Quando `dragDropState.isDragging && dragDropState.draggedMeal?.id == meal.id`, applicare opacità `0.35f` come segnaposto.
- [ ] **Step 2: Registrare i bounding box delle sezioni pasto in `UnifiedMealsCard`**
  - Aggiungere `Modifier.onGloballyPositioned` a ogni card pasto per aggiornare `dragDropState.registerMealBounds(mealType, boundsInRoot)`.
  - Quando `dragDropState.hoveredMealType == typeKey && dragDropState.sourceMealType != typeKey`, animare il bordo con il colore d'accento e mostrare il badge *"Sposta qui"*.
- [ ] **Step 3: Commit dei cambiamenti**

---

### Task 4: Integrazione DashboardScreen, Auto-Scroll e Overlay Ghost Card

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/DashboardScreen.kt`

**Interfaces:**
- Consumes: `MealDragDropState`, `GhostMealCard`, `DashboardViewModel.moveMealToType`

- [ ] **Step 1: Istanziare `rememberMealDragDropState()` in `DashboardScreen`**
- [ ] **Step 2: Implementare l'Auto-Scroll della `LazyListState`**
  - Con un `LaunchedEffect(dragDropState.isDragging, dragDropState.dragOffset)`, rilevare se il puntatore è a meno di 120dp dal bordo superiore o inferiore e invocare `listState.scrollBy()`.
- [ ] **Step 3: Renderizzare `GhostMealCard` in overlay nel `Box` principale della dashboard**
- [ ] **Step 4: Collegare la callback `onDrop` a `viewModel.moveMealToType(meal, targetMealType)` con feedback aptico**
- [ ] **Step 5: Commit dei cambiamenti**

---

### Task 5: Verifica Completa, Build APK, Telegram & Push

- [ ] **Step 1: Eseguire la suite completa di test unitari (`.\gradlew.bat testDebugUnitTest`)**
- [ ] **Step 2: Compilare l'APK di debug (`.\gradlew.bat assembleDebug`)**
- [ ] **Step 3: Git push del branch `main` su GitHub**
- [ ] **Step 4: Inviare l'APK via Telegram con la descrizione delle nuove funzionalità**
