# Specifica di Progetto: Edge-to-Edge & Material 3 Adaptive per FoodLab

**Data:** 2026-09-14  
**Stato:** Approvato in brainstorming  
**Obiettivo:** Modernizzazione di FoodLab per Android 15, Android 16 e Android 17, implementando il supporto Edge-to-Edge immersivo e Layout Adattivo per tablet e foldable.

---

## 1. Obiettivi e Contesto

Con l'evoluzione di Android (Android 15, 16 e anteprime 17):
1. **Edge-to-Edge Obbligatorio**: Le app devono estendersi sotto la status bar e la barra dei gesti di sistema, gestendo esplicitamente i `WindowInsets` per prevenire sovrapposizioni visive o bande nere artificiali.
2. **Predictive Back Gesture**: Il framework di navigazione deve reagire in modo continuo al gesto di back dell'utente, offrendo animazioni fluide di anteprima della schermata precedente prima del completamento del gesto.
3. **Dispositivi Ampi e Foldable**: L'uso di FoodLab su schermi ampi (tablet, telefoni pieghevoli aperti, modalitÃ  landscape) richiede un layout a due colonne (List-Detail) e una Navigation Rail laterale ergonomica anzichÃ© una Bottom Bar allungata.

---

## 2. Architettura Tecnica

### 2.1 Edge-to-Edge & Window Insets
- **Configurazione Sistema**: In `MainActivity.kt`, abilitazione di `enableEdgeToEdge()` con barre di sistema trasparenti e icone adattive sia in modalitÃ  chiara che scura.
- **Gestione Insets nei Contenitori**:
  - `Scaffold` gestisce i safe insets primari (`WindowInsets.safeDrawing`).
  - La barra di navigazione fluttuante o ancorata include `WindowInsets.navigationBars` per non coprire mai la gesture pill di sistema.
  - I form di inserimento (`SearchScreen`, `CreateFoodScreen`, `FoodDetailScreen`, `Dialogs`) applicano `Modifier.imePadding()` combinato a scroll automatico, garantendo che tastiera software e pulsanti di azione rimangano sempre perfettamente visibili.
- **Predictive Back Navigation**:
  - Il `NavHost` Compose adotta animazioni di transizione orizzontali e sfumate calibrate per supportare il back gesture nativo di Android 14+.

### 2.2 Layout Adattivo (Adaptive Window Size)
- **Window Size Classes**:
  - `Compact` (larghezza < 600dp, smartphone tipici in verticale):
    - Layout a colonna singola per Dashboard e Database Cibi.
    - Barra di navigazione inferiore `BottomNavBar`.
  - `Medium` & `Expanded` (larghezza >= 600dp, foldable aperti, tablet, landscape):
    - Navigazione laterale ergonomica (`FoodLabNavigationRail`) ancorata al lato sinistro.
    - `FoodsDatabaseScreen` adotta un'interfaccia **List-Detail Pane**:
      - **Pannello Sinistro (40-45%)**: barra di ricerca, filtri categoria, elenco cibi scrollabile.
      - **Pannello Destro (55-60%)**: scheda dettaglio del cibo selezionato (macro, calorie, porzioni, aggiunta rapida al pasto o preferiti). Se nessun cibo Ã¨ selezionato, mostra un'anteprima elegante con la mascotte di FoodLab.
    - `DashboardScreen`: disposizione a griglia/doppia colonna (Hero Header e Macro sulla colonna sinistra, sezioni pasti sulla colonna destra).

---

## 3. Dettaglio Componenti e File

### 3.1 [`MainActivity.kt`](app/src/main/java/com/example/MainActivity.kt)
- Riconoscimento della larghezza della finestra (`calculateWindowSizeClass` o rilevamento larghezza schermo Compose con `LocalConfiguration.current.screenWidthDp >= 600`).
- Struttura Scaffold adattiva:
  - Se compatto: `Scaffold(bottomBar = { BottomNavBar(...) })`
  - Se ampio: `Row { NavigationRail(...) ; Box(modifier = Modifier.weight(1f)) { NavHost(...) } }`
- Supporto insets e animazioni predittive di transizione tra rotte.

### 3.2 [`Components.kt`](app/src/main/java/com/example/ui/screens/Components.kt)
- Creazione del nuovo componente Composable `FoodLabNavigationRail` coordinato nello stile estetico cyberpunk/retro-moderno di FoodLab (icone RPG, color palette neon/slate coerente con `BottomNavBar`).
- Aggiornamento di `BottomNavBar` con rispetto rigoroso di `WindowInsets.navigationBars.asPaddingValues()`.

### 3.3 [`FoodsDatabaseScreen.kt`](app/src/main/java/com/example/ui/screens/FoodsDatabaseScreen.kt)
- Supporto dual-pane quando la larghezza Ã¨ ampia:
  - Gestione dello stato `selectedFoodId: Int?`.
  - In modalitÃ  compatta: click sul cibo naviga verso `Routes.createFoodsDatabaseDetailRoute(id)`.
  - In modalitÃ  ampia: click sul cibo aggiorna `selectedFoodId` mostrando il pannello di dettaglio inline a destra, senza transizione di pagina completa.

### 3.4 Inset Audit & IME Padding
- Audit dei campi di testo e dialog in `SearchScreen.kt`, `CreateFoodScreen.kt`, `Dialogs.kt`:
  - Aggiunta di `imePadding()` per la digitazione fluida dei grammi e della ricerca cibi.

---

## 4. Gestione Casi Limite ed Errori
- **Rotazione dello Schermo / Piegatura del Foldable**:
  - Lo stato `selectedFoodId` Ã¨ preservato con `rememberSaveable` per evitare reset quando il foldable viene aperto o chiuso durante l'uso.
- **Tastiera e Finestre Ristrette**:
  - Con la tastiera aperta, i bottoni "Salva" o "Aggiungi" restano ancorati sopra la tastiera tramite `imePadding()`.
- **Nessun cibo selezionato nel pannello destro**:
  - Placeholder visivo dedicato che invita a selezionare un alimento dall'elenco.

---

## 5. Piano di Verifica
1. **Compilazione & Build**:
   - `compileDebugKotlin`: verifica zero errori di tipo, import o sintassi.
   - `assembleDebug`: generazione con successo dell'APK debug senza warning R8 o manifest issues.
2. **Test Unitari**:
   - Esecuzione della suite di test unitari con `./gradlew testDebugUnitTest`.
3. **Verifica Layout e Inset**:
   - Riconoscimento della configurazione compatto vs ampio.
   - Corretto dimensionamento dei padding con e senza barre di sistema.
