# Project: FoodLab Code Review, Bug Hunting & Apple Design Compliance

## Architecture
FoodLab is a modern Android fitness & nutrition tracker built with Kotlin, Jetpack Compose, Room Database (v12), Kotlin Coroutines & StateFlow, and AppWidgets (RemoteViews).
- **Presentation**: Jetpack Compose screens, ViewModels with StateFlow, Emil Kowalski physics-based springs and Apple Design system.
- **Domain & Repository**: Single source of truth in `Repository.kt`, coordinating local Room caching and external API lookups (OpenFoodFacts).
- **Data**: Room DB (`AppDatabase`, v12), DAOs (`MealEntryDao`, `FoodDao`, `DailyTargetDao`, `WeightLogDao`, `MealTemplateDao`), schema export.
- **Widgets**: Home screen AppWidgets (`DailyCaloriesWidgetProvider`, `PetStatusWidgetProvider`, `QuickAddWidgetProvider`, `PedometerWidgetProvider`) synced via `WidgetUpdater`.

## Feature Inventory
| # | Feature / Issue | Description | Milestone | Source |
|---|-----------------|-------------|-----------|--------|
| 1 | Room DB Bypasses in UI | `RecipesScreen` & `AnalyticsScreen` bypass ViewModels and query `AppDatabase` directly | M1 | Survey 1 & 3 |
| 2 | Navigation Wiring & ViewModel Injection | Wire `RecipesViewModel` and `AnalyticsViewModel` in `MainActivity.kt` navigation; remove dead `StatsViewModel.kt` | M1 | Survey 1 |
| 3 | SoundManager Thread Safety | Concurrently mutated collections between SoundPool binder thread and UI thread cause CME | M1 | Survey 1 |
| 4 | Stale Date Closure in Dashboard | `DashboardScreen.kt:186-195` captures initial `currentDate`, breaking swipe-back date sync | M1 | Survey 1 |
| 5 | Pedometer HealthConnect Timeout | Missing timeout in `PedometerWidgetProvider.kt:126` risks 10s BroadcastReceiver ANR | M1 | Survey 1 |
| 6 | Repository Transaction Atomicity | Multi-row template inserts lack `@Transaction` in `Repository.kt` | M1 | Survey 1 & 3 |
| 7 | Dashboard Drag-and-Drop Safety | Double-bang operator on `dragDropState.draggedMeal!!` risks NPE | M1 | Survey 1 |
| 8 | Macro Color Standardization | Align `Color.kt` & `AppleCalorieMacroRings` to Protein `#22C55E`, Carb `#F59E0B`, Fat `#06B6D4` | M2 | Survey 2 |
| 9 | GPU Animation Optimization | Convert `SmoothProgressRail` from `fillMaxWidth` to GPU `graphicsLayer { scaleX = ... }` | M2 | Survey 2 |
| 10 | Duplicate Tap Event in Settings | Remove duplicate `.clickable` chained after `.applePressEffect` in `SettingsScreen.kt` | M2 | Survey 2 |
| 11 | Touch Target HIG Compliance | Enforce minimum 44dp height/bounds on chips, buttons, and steppers across all screens | M2 | Survey 2 |
| 12 | Motion Gating & Scale Entrances | Eliminate 0.85f bouncy squash on search clear; calm tab navigation springs; standard 0.92f scale entrances | M2 | Survey 2 |
| 13 | AppWidget Synchronization | Add `QuickAddWidgetProvider` to `WidgetUpdater.kt`; eliminate silent desync on recipe/weight logs | M3 | Survey 2 & 3 |
| 14 | AppWidget Macro & Theme Colors | Update `widget_pet_status.xml`, `widget_daily_calories.xml`, `widget_surface_bg.xml` to Apple Dark standards | M3 | Survey 2 |
| 15 | Canonical Meal Enum Consistency | Ensure all meal insertions use `MealTypes.canonicalize()` (specifically `RecipesScreen.kt:237`) | M3 | Survey 3 |
| 16 | Room Index & Query Optimization | Add index on `meal_entries(timestamp)` to avoid unindexed filesort in `getAllMeals()` | M3 | Survey 3 |
| 17 | Pedometer Wakeup Cascading | Prevent food log broadcasts from triggering unneeded step sensor listener registrations | M3 | Survey 3 |
| 18 | E2E Regression & Build Verification | Clean Gradle build (`assembleDebug`) and 100% unit test pass (`testDebugUnitTest`) | M4 | Project Plan |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Architecture & Concurrency Fixes | ViewModels wiring, DB bypass elimination, SoundManager thread safety, stale closure fix, ANR prevention | None | COMPLETED |
| M2 | UI/UX & Motion Standardization | Macro colors, GPU graphicsLayer rails, double click elimination, 44dp touch targets, Apple springs | None | COMPLETED |
| M3 | Data Layer, AppWidgets & Sync Integrity | WidgetUpdater coverage, widget XML styling, meal canonicalization, Room indexing & cascading optimization | M1, M2 | COMPLETED |
| M4 | Final Build & Regression Verification | Comprehensive unit tests, Gradle assembleDebug, forensic audit | M1, M2, M3 | COMPLETED |

## Interface Contracts
### UI Composables ↔ ViewModels
- `RecipesScreen(viewModel: RecipesViewModel = viewModel(), ...)`
  - State: `recipes: State<List<MealTemplateWithItems>>`
  - Actions: `onDeleteRecipe(id: Long)`, `onLogRecipe(recipe: MealTemplateWithItems, mealType: String, servings: Float)`
- `AnalyticsScreen(viewModel: AnalyticsViewModel = viewModel(), ...)`
  - State: `weights: State<List<WeightLog>>`, `meals: State<List<MealEntry>>`, `targets: State<List<DailyTarget>>`
  - Actions: `onLogWeight(weightKg: Float, date: LocalDate, note: String?)`

### Repository ↔ AppWidgets
- All mutation operations (`insertMeal`, `deleteMeal`, `insertWeight`, `logRecipeAsMeal`) must call `WidgetUpdater.updateAllWidgets(context)` on completion.
- `WidgetUpdater.updateAllWidgets(context)` broadcasts updates to `DailyCaloriesWidgetProvider`, `PetStatusWidgetProvider`, and `QuickAddWidgetProvider`.

## Code Layout
- `app/src/main/java/com/example/ui/screens/`: Composable screens (`DashboardScreen.kt`, `RecipesScreen.kt`, `AnalyticsScreen.kt`, `SettingsScreen.kt`, `SearchScreen.kt`, `Components.kt`, `DashboardComponents.kt`)
- `app/src/main/java/com/example/ui/theme/`: Theme system (`Color.kt`, `Theme.kt`, `Type.kt`)
- `app/src/main/java/com/example/ui/viewmodel/`: ViewModels (`DashboardViewModel.kt`, `RecipesViewModel.kt`, `AnalyticsViewModel.kt`, `FoodSearchViewModel.kt`)
- `app/src/main/java/com/example/data/`: Room entities, DAOs, Database (`AppDatabase.kt`, `Entities.kt`, `Repository.kt`)
- `app/src/main/java/com/example/util/`: Utilities & managers (`SoundManager.kt`, `WidgetUpdater.kt`, `StepCounterStore.kt`)
- `app/src/main/java/com/example/widgets/`: AppWidget providers (`DailyCaloriesWidgetProvider.kt`, `PetStatusWidgetProvider.kt`, `QuickAddWidgetProvider.kt`, `PedometerWidgetProvider.kt`)
- `app/src/main/res/`: Widget layouts and drawables (`widget_pet_status.xml`, `widget_daily_calories.xml`, `widget_surface_bg.xml`)
