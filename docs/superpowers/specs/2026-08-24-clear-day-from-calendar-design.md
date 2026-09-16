# Design: Clear Day From Calendar

## Summary
Permettere all'utente di eliminare tutti i pasti registrati per una data specifica direttamente dal popup del calendario mensile (`MonthlyStreakDialog`), riportando la giornata a stato "NON REGISTRATO" (0 kcal) in modo da non rompere o alterare lo streak per giornate registrate solo parzialmente.

## User Flow
1. L'utente apre il calendario mensile / journal (toccando l'icona del calendario o lo streak sulla dashboard).
2. Seleziona una data che ha dei pasti registrati (es. una data con 200 kcal).
3. Nella scheda di riepilogo del giorno compare un pulsante con icona cestino: **"SVUOTA GIORNATA"**.
4. Cliccando sul pulsante, viene mostrato un dialogo di conferma: *"Eliminare tutti i pasti registrati per [Data]? Questa azione non può essere annullata."*.
5. Alla conferma:
   - Viene eseguita la query Room `DELETE FROM meal_entries WHERE dateString = :date`.
   - Il database notifica tramite StateFlow/Flow.
   - La cella del giorno sul calendario diventa grigia (stato "NON REGISTRATO").
   - Il conteggio calorie del giorno scende a 0 kcal e lo streak viene ricalcolato automaticamente.

## Architecture & Code Changes
1. **`MealEntryDao.kt`**:
   - Aggiungere il metodo:
     ```kotlin
     @Query("DELETE FROM meal_entries WHERE dateString = :date")
     suspend fun deleteMealsForDate(date: String)
     ```
2. **`Repository.kt`**:
   - Aggiungere:
     ```kotlin
     suspend fun deleteMealsForDate(date: String) = mealEntryDao.deleteMealsForDate(date)
     ```
3. **`DashboardViewModel.kt`**:
   - Aggiungere:
     ```kotlin
     fun deleteMealsForDate(date: String) {
         viewModelScope.launch {
             repository.deleteMealsForDate(date)
         }
     }
     ```
4. **`DashboardComponents.kt` (`MonthlyStreakDialog`)**:
   - Aggiungere il parametro `onDeleteDate: ((String) -> Unit)? = null`.
   - Aggiungere uno stato per il dialogo di conferma (`showConfirmClearDialog`).
   - Quando `selIsLogged == true`, mostrare il pulsante *"Svuota Giornata"*.
   - Alla conferma nel dialogo, chiamare `onDeleteDate(selectedDate)` con feedback sonoro e aptico.
5. **`DashboardScreen.kt`**:
   - Passare `onDeleteDate = { viewModel.deleteMealsForDate(it) }` a `MonthlyStreakDialog`.

## Error Handling & Edge Cases
- Se la data ha 0 pasti (`selIsLogged == false`), il pulsante non viene mostrato.
- La cancellazione avviene in background con coroutine e transazione Room.
- Haptic feedback e audio click durante l'interazione.
