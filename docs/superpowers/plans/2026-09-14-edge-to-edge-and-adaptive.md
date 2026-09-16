# Edge-to-Edge & Material 3 Adaptive Layout Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Android 15/16/17 edge-to-edge system insets handling and Material 3 adaptive dual-pane and navigation rail layouts for foldables and tablets in FoodLab.

**Architecture:** Detect screen width class dynamically using Compose `LocalConfiguration`. On compact screens (< 600dp), render the existing bottom bar and full-screen detail views; on medium/expanded screens (>= 600dp), render an ergonomic `FoodLabNavigationRail` and a List-Detail dual pane for the food database. Apply `imePadding()` and `WindowInsets.safeDrawing` across all form screens.

**Tech Stack:** Jetpack Compose (BOM 2024.09.00), Material 3, Navigation Compose, Room KTX, Android Activity Edge-to-Edge API.

## Global Constraints

- Android minSdk 26, targetSdk 35+
- No third-party navigation breaking changes; preserve all existing routes in `Routes`
- Preserve sound effects via `LocalSoundManager` on navigation clicks
- Strict rule: Do not send APK to Telegram unless explicitly requested by the user

---

### Task 1: Navigation Rail Component (`FoodLabNavigationRail`)

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/Components.kt`

**Interfaces:**
- Produces: `@Composable fun FoodLabNavigationRail(navController: NavController, currentRoute: String?)`

- [ ] **Step 1: Add `FoodLabNavigationRail` to `Components.kt`**

```kotlin
@Composable
fun FoodLabNavigationRail(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    NavigationRail(
        modifier = modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start)),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.VideogameAsset, contentDescription = "Stages") },
            label = { Text("Stages", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Dashboard,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Dashboard) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Fastfood, contentDescription = "Items") },
            label = { Text("Items", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.FoodsDatabase || currentRoute?.startsWith("foods_database_detail") == true || currentRoute == Routes.CreateFood,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.FoodsDatabase) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Highscore") },
            label = { Text("Highscore", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Stats || currentRoute == Routes.Analytics,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Analytics) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        NavigationRailItem(
            icon = { Icon(Icons.Default.Memory, contentDescription = "Config") },
            label = { Text("Config", style = MaterialTheme.typography.labelSmall) },
            selected = currentRoute == Routes.Settings,
            onClick = {
                soundManager.playSound("click")
                navController.navigate(Routes.Settings) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
```

- [ ] **Step 2: Verify `BottomNavBar` padding in `Components.kt`**
Ensure `BottomNavBar` uses `Modifier.navigationBarsPadding()` so gesture navigation bars never overlap on compact devices.

- [ ] **Step 3: Compile Kotlin**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" compileDebugKotlin --no-daemon`
Expected: `BUILD SUCCESSFUL`

---

### Task 2: Inset Audit & IME Padding for Text Fields & Forms

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/SearchScreen.kt`
- Modify: `app/src/main/java/com/example/ui/screens/CreateFoodScreen.kt`
- Modify: `app/src/main/java/com/example/ui/screens/FoodDetailScreen.kt`

- [ ] **Step 1: Add `imePadding()` to `SearchScreen.kt`**
Ensure search bar and results container have `Modifier.imePadding()` so keyboard popup adjusts results without obscuring.

- [ ] **Step 2: Add `imePadding()` to `CreateFoodScreen.kt`**
Ensure the dish creation form has `Modifier.imePadding()` on its scrollable Column.

- [ ] **Step 3: Add `imePadding()` to `FoodDetailScreen.kt`**
Ensure gram quantity edit input has `Modifier.imePadding()` on bottom action button.

- [ ] **Step 4: Compile Kotlin**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" compileDebugKotlin --no-daemon`
Expected: `BUILD SUCCESSFUL`

---

### Task 3: Dual-Pane List-Detail Experience for Foods Database

**Files:**
- Modify: `app/src/main/java/com/example/ui/screens/FoodsDatabaseScreen.kt`

- [ ] **Step 1: Add Adaptive Split-Pane support in `FoodsDatabaseScreen.kt`**
Detect `val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600`.
When `isWideScreen == true`, manage `var selectedFoodId by rememberSaveable { mutableStateOf<Int?>(null) }`.
Render a `Row(Modifier.fillMaxSize())`:
- Left pane (`Modifier.weight(0.42f)`): Foods list and search filters. Clicking a food sets `selectedFoodId = food.id`.
- Vertical divider.
- Right pane (`Modifier.weight(0.58f)`): Inline food detail panel showing full macros and actions if `selectedFoodId != null`, or mascot empty state if null.
When `isWideScreen == false`, maintain original behavior (`onFoodClick(food.id)` navigates to `Routes.createFoodsDatabaseDetailRoute(food.id)`).

- [ ] **Step 2: Compile Kotlin**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" compileDebugKotlin --no-daemon`
Expected: `BUILD SUCCESSFUL`

---

### Task 4: Adaptive Navigation Shell in `MainActivity.kt`

**Files:**
- Modify: `app/src/main/java/com/example/MainActivity.kt`

- [ ] **Step 1: Update `MainActivity.kt` layout structure**
Detect `val isWideScreen = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600`.
In `setContent`:
```kotlin
if (isWideScreen && showBottomBar) {
    Row(modifier = Modifier.fillMaxSize()) {
        FoodLabNavigationRail(navController = navController, currentRoute = currentRoute)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            NavHost(
                navController = navController,
                startDestination = Routes.Dashboard,
                modifier = Modifier.fillMaxSize(),
                // transitions ...
            ) { ... }
        }
    }
} else {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = { if (showBottomBar) BottomNavBar(navController, currentRoute) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Dashboard,
            modifier = Modifier.padding(innerPadding),
            // transitions ...
        ) { ... }
    }
}
```

- [ ] **Step 2: Enhance `NavHost` transitions for predictive back gestures**
Ensure slide & fade transitions utilize Material 3 motion specs (`FastOutSlowInEasing`, 280ms duration) with pop transitions properly mirrored.

- [ ] **Step 3: Compile Kotlin**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" compileDebugKotlin --no-daemon`
Expected: `BUILD SUCCESSFUL`

---

### Task 5: Full Verification & APK Assembly

**Files:**
- Test: All modified files

- [ ] **Step 1: Run unit tests**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" testDebugUnitTest --no-daemon`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Assemble Debug APK**
Run: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"; & "C:\Users\developer\.gradle\wrapper\dists\gradle-9.3.1-bin\23ovyewtku6u96viwx3xl3oks\gradle-9.3.1\bin\gradle.bat" assembleDebug --no-daemon`
Expected: `BUILD SUCCESSFUL`
