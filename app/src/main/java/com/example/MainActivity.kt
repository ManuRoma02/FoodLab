package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.AppDatabase
import com.example.ui.DashboardViewModel
import com.example.ui.FoodSearchViewModel
import com.example.ui.SessionManager
import com.example.ui.SettingsViewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.MealTypes
import com.example.util.PreferencesManager
import com.example.util.ReminderScheduler
import com.example.util.SoundManager
import com.example.util.ThemeSkin
import com.example.util.WidgetUpdater
import java.util.Date

object Routes {
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val Search = "search/{mealType}"
    const val FoodsDatabase = "foods_database"
    const val CreateFood = "create_food"
    const val CreateRecipe = "create_recipe"
    const val FoodDetail = "food_detail/{foodId}/{mealType}"
    const val FoodsDatabaseDetail = "foods_database_detail/{foodId}"
    const val Analytics = "analytics"
    const val Stats = "stats"
    const val Recipes = "recipes"
    const val Shopping = "shopping"
    const val Settings = "settings"
    
    fun createSearchRoute(mealType: String) = "search/$mealType"
    fun createFoodDetailRoute(id: Int, mealType: String) = "food_detail/$id/$mealType"
    fun createFoodsDatabaseDetailRoute(id: Int) = "foods_database_detail/$id"
}

private fun getRouteIndex(route: String?): Int {
    val baseRoute = route?.substringBefore("/") ?: return 0
    return when (baseRoute) {
        Routes.Dashboard -> 0
        Routes.FoodsDatabase -> 1
        Routes.Analytics, Routes.Stats -> 2
        Routes.Settings -> 3
        else -> 0
    }
}

private fun shouldShowBottomBar(route: String?): Boolean {
    val routeBase = route?.substringBefore("/{")
    return when (route) {
        null -> false
        Routes.Search,
        Routes.Onboarding,
        Routes.CreateFood,
        Routes.CreateRecipe,
        Routes.FoodDetail,
        Routes.FoodsDatabaseDetail -> false
        else -> routeBase !in setOf("search", "food_detail", "foods_database_detail")
    }
}

class MainActivity : ComponentActivity() {
    
    private val _intentFlow = kotlinx.coroutines.flow.MutableStateFlow<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        _intentFlow.value = intent
    }

    override fun onResume() {
        super.onResume()
        WidgetUpdater.updateAllWidgets(this)
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _intentFlow.value = intent
        try {
            val appPreferences = PreferencesManager.getInstance(this)
            ReminderScheduler.sync(this, appPreferences.notificationsEnabled.value)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error syncing reminders", e)
        }
        
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
            window.isStatusBarContrastEnforced = false
        }
        window.decorView.isVerticalScrollBarEnabled = false
        setContent {
            val context = LocalContext.current
            val soundManager = remember { SoundManager(context) }
            val preferencesManager = remember { PreferencesManager.getInstance(context) }
            val currentThemeSkin by preferencesManager.themeSkin.collectAsStateWithLifecycle()
            
            DisposableEffect(Unit) {
                onDispose {
                    soundManager.release()
                }
            }

            CompositionLocalProvider(
                LocalSoundManager provides soundManager,
                LocalPreferencesManager provides preferencesManager
            ) {
                MyApplicationTheme(themeSkin = currentThemeSkin) {
                    val navController = rememberNavController()
                    val dashboardViewModel: DashboardViewModel = viewModel()
                    val foodSearchViewModel: FoodSearchViewModel = viewModel()
                    val pedometerViewModel: com.example.ui.PedometerViewModel = viewModel()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route
                    val showBottomBar = shouldShowBottomBar(currentRoute)
                    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
                    
                    val todayDate by dashboardViewModel.todayDate.collectAsStateWithLifecycle()
                    val currentDateState = rememberSaveable { mutableStateOf(todayDate) }
                    val currentDate = currentDateState.value

                    val previousToday = remember { mutableStateOf(todayDate) }
                    LaunchedEffect(todayDate) {
                        if (currentDateState.value == previousToday.value) {
                            currentDateState.value = todayDate
                        }
                        previousToday.value = todayDate
                    }

                    DisposableEffect(context) {
                        val receiver = object : android.content.BroadcastReceiver() {
                            override fun onReceive(c: android.content.Context?, intent: Intent?) {
                                val newToday = SessionManager.formatDate(Date())
                                if (currentDateState.value == previousToday.value) {
                                    currentDateState.value = newToday
                                }
                                previousToday.value = newToday
                            }
                        }
                        val filter = android.content.IntentFilter().apply {
                            addAction(Intent.ACTION_DATE_CHANGED)
                            addAction(Intent.ACTION_TIME_CHANGED)
                            addAction(Intent.ACTION_TIMEZONE_CHANGED)
                        }
                        context.registerReceiver(receiver, filter)
                        onDispose {
                            try { context.unregisterReceiver(receiver) } catch (_: Exception) {}
                        }
                    }
                    
                    val currentIntent by _intentFlow.collectAsStateWithLifecycle()

                    DisposableEffect(pedometerViewModel) {
                        lifecycle.addObserver(pedometerViewModel)
                        onDispose { lifecycle.removeObserver(pedometerViewModel) }
                    }

                    val activityPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        if (granted) {
                            pedometerViewModel.startTracking()
                        }
                    }

                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { granted ->
                        preferencesManager.setNotificationsEnabled(granted)
                        ReminderScheduler.sync(context, granted)
                    }

                    LaunchedEffect(Unit) {
                        if (preferencesManager.syncSteps.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                                activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            }
                        }
                        if (preferencesManager.notificationsEnabled.value && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                    
                    LaunchedEffect(currentIntent) {
                        currentIntent?.let { intentObj ->
                            if (intentObj.action == "WIDGET_ADD_FOOD") {
                                currentDateState.value = SessionManager.formatDate(Date())
                                val mealType = MealTypes.canonicalize(intentObj.getStringExtra("MEAL_TYPE"))
                                navController.navigate(Routes.createSearchRoute(mealType)) {
                                    popUpTo(Routes.Dashboard) { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                            _intentFlow.value = null
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isWideScreen && showBottomBar) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            ) {
                                FoodLabNavigationRail(navController = navController, currentRoute = currentRoute)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End))
                                ) {
                                    FoodLabNavHost(
                                        navController = navController,
                                        dashboardViewModel = dashboardViewModel,
                                        foodSearchViewModel = foodSearchViewModel,
                                        pedometerViewModel = pedometerViewModel,
                                        currentDate = currentDate,
                                        onDateChange = { currentDateState.value = it }
                                    )
                                }
                            }
                        } else {
                            Scaffold(
                                modifier = Modifier.fillMaxSize(),
                                bottomBar = { if (showBottomBar && !isWideScreen) BottomNavBar(navController, currentRoute) }
                            ) { innerPadding ->
                                FoodLabNavHost(
                                    navController = navController,
                                    dashboardViewModel = dashboardViewModel,
                                    foodSearchViewModel = foodSearchViewModel,
                                    pedometerViewModel = pedometerViewModel,
                                    currentDate = currentDate,
                                    onDateChange = { currentDateState.value = it },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodLabNavHost(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    foodSearchViewModel: FoodSearchViewModel,
    pedometerViewModel: com.example.ui.PedometerViewModel,
    currentDate: String,
    onDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Dashboard,
        modifier = modifier,
        enterTransition = {
            val initialIdx = getRouteIndex(initialState.destination.route)
            val targetIdx = getRouteIndex(targetState.destination.route)
            val direction = if (targetIdx >= initialIdx) 1 else -1
            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * fullWidth },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        exitTransition = {
            val initialIdx = getRouteIndex(initialState.destination.route)
            val targetIdx = getRouteIndex(targetState.destination.route)
            val direction = if (targetIdx >= initialIdx) -1 else 1
            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { fullWidth -> direction * fullWidth },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            val initialIdx = getRouteIndex(initialState.destination.route)
            val targetIdx = getRouteIndex(targetState.destination.route)
            val direction = if (targetIdx >= initialIdx) 1 else -1
            androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
            androidx.compose.animation.slideInHorizontally(
                initialOffsetX = { fullWidth -> direction * fullWidth },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            val initialIdx = getRouteIndex(initialState.destination.route)
            val targetIdx = getRouteIndex(targetState.destination.route)
            val direction = if (targetIdx >= initialIdx) -1 else 1
            androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(240, easing = androidx.compose.animation.core.FastOutSlowInEasing)) +
            androidx.compose.animation.slideOutHorizontally(
                targetOffsetX = { fullWidth -> direction * fullWidth },
                animationSpec = androidx.compose.animation.core.tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        }
    ) {
        composable(Routes.Dashboard) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                pedometerViewModel = pedometerViewModel,
                currentDate = currentDate,
                onDateChange = onDateChange,
                onNavigateToSearch = { mealType -> navController.navigate(Routes.createSearchRoute(mealType)) }
            )
        }
        composable(
            route = Routes.Search,
            arguments = listOf(navArgument("mealType") { type = NavType.StringType })
        ) { backStackEntry ->
            val mealType = MealTypes.canonicalize(backStackEntry.arguments?.getString("mealType"))
            SearchScreen(
                viewModel = foodSearchViewModel,
                dashboardViewModel = dashboardViewModel,
                pedometerViewModel = pedometerViewModel,
                mealType = mealType,
                currentDate = currentDate,
                onNavigateBack = { navController.popBackStack() },
                onFoodSelected = { foodId ->
                    navController.navigate(Routes.createFoodDetailRoute(foodId, mealType))
                },
                onNavigateToCreateFood = {
                    navController.navigate(Routes.CreateFood)
                },
                onNavigateToCreateRecipe = {
                    navController.navigate(Routes.CreateRecipe)
                }
            )
        }
        composable(Routes.CreateFood) {
            CreateFoodScreen(foodSearchViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.CreateRecipe) {
            CreateRecipeScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.FoodDetail,
            arguments = listOf(
                navArgument("foodId") { type = NavType.IntType },
                navArgument("mealType") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getInt("foodId") ?: 0
            val mealType = MealTypes.canonicalize(backStackEntry.arguments?.getString("mealType"))
            FoodDetailScreen(
                viewModel = foodSearchViewModel,
                dashboardViewModel = dashboardViewModel,
                pedometerViewModel = pedometerViewModel,
                foodId = foodId,
                currentDate = currentDate,
                mealType = mealType,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FoodsDatabase) {
            FoodsDatabaseScreen(
                viewModel = foodSearchViewModel,
                onNavigateToCreateFood = { navController.navigate(Routes.CreateFood) },
                onNavigateToCreateRecipe = { navController.navigate(Routes.CreateRecipe) },
                onFoodSelected = { foodId ->
                    navController.navigate(Routes.createFoodsDatabaseDetailRoute(foodId))
                }
            )
        }
        composable(
            route = Routes.FoodsDatabaseDetail,
            arguments = listOf(navArgument("foodId") { type = NavType.IntType })
        ) { backStackEntry ->
            val foodId = backStackEntry.arguments?.getInt("foodId") ?: 0
            FoodDatabaseDetailScreen(
                viewModel = foodSearchViewModel,
                foodId = foodId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Analytics) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Stats) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Recipes) {
            RecipesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateRecipe = { navController.navigate(Routes.CreateRecipe) }
            )
        }
        composable(Routes.Settings) {
            val settingsViewModel: SettingsViewModel = viewModel()
            SettingsScreen(settingsViewModel)
        }
    }
}
