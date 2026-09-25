# Design Spec: Drag & Drop Interattivo tra Pasti nella Dashboard

## 1. Obiettivo
Permettere all'utente di riorganizzare rapidamente gli alimenti registrati nella schermata Home semplicemente effettuando una pressione prolungata (Long Press) su un cibo in una qualsiasi sezione (es. Colazione), sollevandolo e trascinandolo visivamente (Drag & Drop) sulla card di un altro pasto (es. Pranzo, Cena, Snack).

## 2. Esperienza Utente & Interazioni (Apple Design)
1. **Inizio del Trascinamento (Pickup)**:
   - Su ogni alimento (`MealItemRow`), un tocco breve continua ad aprire il dialog di modifica (`EditMealEntryDialog`), mentre uno swipe orizzontale verso sinistra continua ad attivare l'eliminazione rapida (`SwipeToDismissBox`).
   - Il gesto di **Long Press** avvia la sessione di trascinamento:
     - Emette un feedback aptico marcato (`HapticFeedbackType.LongPress`).
     - La riga originale nella lista assume un'opacità ridotta (`0.35f`) per fare da segnaposto senza far collassare l'altezza del layout.
     - Viene istanziata e mostrata una **Ghost Card** sovrapposta alla schermata, ancorata alle coordinate del dito.
     - La Ghost Card adotta uno stile Cupertino: scala aumentata a `1.04x`, elevazione e ombra pronunciata (`14.dp`), bordo luminescente con il colore d'accento del pasto d'origine.
2. **Trascinamento & Evidenziazione Bersaglio (Drag & Target Hover)**:
   - Lo stato condiviso `MealDragDropState` tiene traccia della posizione globale del puntatore.
   - Ogni contenitore di pasto (Colazione, Pranzo, Cena, Snack) registra il proprio rettangolo di coordinate a schermo (`Rect`).
   - Quando il puntatore entra nell'area di un pasto differente da quello di partenza:
     - La card del pasto bersaglio reagisce con un'animazione a molla fluida: il bordo si illumina con il colore d'accento del pasto e nell'header compare l'indicatore *"Sposta qui"*.
     - Viene emesso un leggero impulso aptico.
3. **Auto-Scroll Intelligente**:
   - Se il dito si avvicina alla parte superiore o inferiore dello schermo (entro `120.dp` dai bordi), la `LazyColumn` della dashboard scorre automaticamente con velocità proporzionale alla distanza dal bordo, consentendo di raggiungere pasti distanti.
4. **Rilascio & Risoluzione (Drop)**:
   - **Rilascio su un pasto di destinazione valido**:
     - Vibrazione aptica di successo.
     - Spostamento atomico: invocazione del metodo nel ViewModel che aggiorna `mealEntry.mealType` nel database Room (`insertMeal` con `REPLACE`).
     - Aggiornamento istantaneo dei widget di sistema (`WidgetUpdater.updateAllWidgets`).
     - Animazione di assestamento e scomparsa della Ghost Card.
   - **Rilascio all'esterno o sul medesimo pasto**:
     - Animazione a molla di ritorno/dissolvenza della Ghost Card.
     - Nessuna modifica ai dati.

## 3. Architettura Tecnica

### 3.1 Componenti
- **`MealDragDropState`**:
  - `isDragging: Boolean`
  - `draggedMeal: MealEntry?`
  - `sourceMealType: String?`
  - `dragPosition: Offset` (in coordinate del contenitore della schermata)
  - `hoveredMealType: String?`
  - `targetBoundsMap: Map<String, Rect>` (coordinate dei contenitori dei pasti)
  - Metodi: `startDrag(meal, sourceMealType, initialPosition)`, `updateDrag(dragAmount)`, `endDrag(onDrop: (MealEntry, String) -> Unit)`, `cancelDrag()`
- **`GhostMealCard`**:
  - Composable in overlay a livello di `DashboardScreen` che renderizza l'alimento trascinato con ombra, scala `1.04x` e offset dinamico.
- **`MealItemRow`**:
  - Integrata rilevazione gesture via `pointerInput(detectDragGesturesAfterLongPress)` coordinata con `SwipeToDismissBox`.
- **`UnifiedMealsCard` & Contenitori Pasto**:
  - Modificatori `onGloballyPositioned` per registrare i `Rect` di ciascuna sezione nel `MealDragDropState`.
  - Reattività visiva: bordo e badge dinamico se `hoveredMealType == currentMealType && sourceMealType != currentMealType`.

### 3.2 Flusso Dati
- In `DashboardScreen.kt`:
  - `onMoveMeal: (meal: MealEntry, targetMealType: String) -> Unit` passata a `UnifiedMealsCard`.
  - Callback che invoca:
    `viewModel.moveMealToType(meal, targetMealType)` (oppure `viewModel.updateMealEntry(meal.copy(mealType = targetMealType))`).

## 4. Test & Verifica
- Unit test per la transizione del tipo pasto e aggiornamento stato.
- Test di compilazione ed esecuzione dei test unitari (`.\gradlew.bat testDebugUnitTest`).
- Verifica visiva e build APK (`.\gradlew.bat assembleDebug`).
- Invio APK su Telegram e git commit/push su `main`.
