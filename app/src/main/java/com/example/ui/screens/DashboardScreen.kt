package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MealEntry
import com.example.ui.DashboardViewModel
import com.example.ui.SessionManager
import com.example.util.LocalPreferencesManager
import com.example.util.LocalSoundManager
import com.example.util.MealTypes
import com.example.util.StepCounterStore
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

private data class NutritionSummary(
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val mealsByType: Map<String, List<MealEntry>>
)

private const val BASE_PAGE_INDEX = 5000
private const val TOTAL_PAGES = 10000

private fun daysBetween(fromDate: String, toDate: String): Int {
    return try {
        val calFrom = Calendar.getInstance().apply {
            time = SessionManager.parseDateString(fromDate)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calTo = Calendar.getInstance().apply {
            time = SessionManager.parseDateString(toDate)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffMillis = calTo.timeInMillis - calFrom.timeInMillis
        (diffMillis / (24L * 60 * 60 * 1000)).toInt()
    } catch (_: Exception) {
        0
    }
}

private fun dateForPageIndex(pageIndex: Int, referenceDate: String): String {
    val diffDays = BASE_PAGE_INDEX - pageIndex
    return SessionManager.getPreviousDay(referenceDate, diffDays)
}

private fun pageIndexForDate(date: String, referenceDate: String): Int {
    val daysDiff = daysBetween(date, referenceDate)
    return (BASE_PAGE_INDEX - daysDiff).coerceIn(0, TOTAL_PAGES - 1)
}

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    pedometerViewModel: com.example.ui.PedometerViewModel,
    currentDate: String,
    onDateChange: (String) -> Unit,
    onNavigateToSearch: (String) -> Unit
) {
    LaunchedEffect(currentDate) { viewModel.setDate(currentDate) }
    val dailyTarget by viewModel.dailyTarget.collectAsStateWithLifecycle()
    val allTargets by viewModel.allTargets.collectAsStateWithLifecycle()
    val allMeals by viewModel.allMealsHistory.collectAsStateWithLifecycle()
    val streakCount by viewModel.streakCount.collectAsStateWithLifecycle()
    val latestWeight by viewModel.latestWeight.collectAsStateWithLifecycle()
    val favoriteFoods by viewModel.favoriteFoods.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val soundManager = LocalSoundManager.current
    val preferencesManager = LocalPreferencesManager.current
    val syncStepsEnabled by preferencesManager.syncSteps.collectAsStateWithLifecycle()
    val stepGoal by preferencesManager.stepGoal.collectAsStateWithLifecycle()
    val currentLiveSteps by pedometerViewModel.steps.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current
    val today by viewModel.todayDate.collectAsStateWithLifecycle()

    var mealToEdit by remember { mutableStateOf<MealEntry?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showStreakCalendarDialog by remember { mutableStateOf(false) }
    var showStepOptionsDialog by remember { mutableStateOf(false) }

    var hasActivityPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasActivityPermission = granted
        if (granted) {
            pedometerViewModel.startTracking()
            pedometerViewModel.refresh()
        }
    }

    // Predictive back handler simulation / smooth exit transition
    BackHandler(enabled = true) {
        soundManager.playSound("click")
        (context as? android.app.Activity)?.moveTaskToBack(true)
    }

    val coroutineScope = rememberCoroutineScope()
    var lastKnownToday by remember { mutableStateOf(today) }
    val initialPage = remember { pageIndexForDate(currentDate, today) }
    val pagerState = rememberPagerState(initialPage = initialPage) { TOTAL_PAGES }

    // When the day rolls over (e.g. at midnight), keep user on today if they were on today
    LaunchedEffect(today) {
        if (today != lastKnownToday) {
            val wasOnToday = (currentDate == lastKnownToday)
            lastKnownToday = today
            if (wasOnToday) {
                onDateChange(today)
                try {
                    pagerState.scrollToPage(BASE_PAGE_INDEX)
                } catch (_: Exception) {}
            }
        }
    }

    // Synchronize pager when currentDate changes externally (e.g. from top date picker)
    LaunchedEffect(currentDate, today) {
        val targetPage = pageIndexForDate(currentDate, today)
        if (!pagerState.isScrollInProgress && pagerState.currentPage != targetPage) {
            try {
                pagerState.animateScrollToPage(targetPage)
            } catch (_: Exception) {}
        }
    }

    // Synchronize currentDate when pager settles on a new page
    LaunchedEffect(pagerState, today) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            val pageDate = dateForPageIndex(settledPage, today)
            if (pageDate != currentDate) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                soundManager.playSound("click")
                onDateChange(pageDate)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Sticky Horizontal Date Picker at the top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            HorizontalDatePicker(
                currentDate = currentDate,
                today = today,
                streakCount = streakCount,
                onOpenStreakCalendar = { showStreakCalendarDialog = true },
                onDateChange = { selectedDate ->
                    onDateChange(selectedDate)
                }
            )
        }

        // 2. Interactive Real-Time Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 0
        ) { page ->
            val pageDate = dateForPageIndex(page, today)
            val pageMeals = remember(allMeals, pageDate) {
                allMeals.filter { it.dateString == pageDate }
            }
            val pageSummary = remember(pageMeals) {
                NutritionSummary(
                    calories = pageMeals.sumOf { it.calories },
                    protein = pageMeals.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = pageMeals.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = pageMeals.sumOf { it.fat.toDouble() }.toFloat(),
                    mealsByType = pageMeals.groupBy { MealTypes.canonicalize(it.mealType) }
                )
            }
            val pageTarget = remember(allTargets, dailyTarget, pageDate) {
                viewModel.resolveTargetForDateSync(pageDate, allTargets)
            }
            val pageSteps = if (pageDate == today) {
                currentLiveSteps
            } else {
                remember(pageDate, syncStepsEnabled) {
                    if (syncStepsEnabled) StepCounterStore.savedSteps(context, pageDate) else 0
                }
            }

            val pageBurnedCalories = remember(pageSteps, latestWeight) {
                com.example.util.NutritionCalculator.calculateCaloriesBurned(pageSteps, latestWeight?.weightKg)
            }
            val pageRemainingCalories = remember(pageTarget.targetCalories, pageSummary.calories, pageBurnedCalories) {
                pageTarget.targetCalories - pageSummary.calories + pageBurnedCalories
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Unified Hero Header
                item(key = "hero_$pageDate") {
                    UnifiedHeroHeader(
                        remainingCalories = pageRemainingCalories,
                        targetCalories = pageTarget.targetCalories,
                        loggedCalories = pageSummary.calories,
                        burnedCalories = pageBurnedCalories,
                        proteinConsumed = pageSummary.protein,
                        proteinTarget = pageTarget.targetProtein.toFloat(),
                        carbsConsumed = pageSummary.carbs,
                        carbsTarget = pageTarget.targetCarbs.toFloat(),
                        fatConsumed = pageSummary.fat,
                        fatTarget = pageTarget.targetFat.toFloat(),
                        steps = pageSteps,
                        stepsTarget = stepGoal,
                        weightText = latestWeight?.let { String.format(Locale.getDefault(), "%.1f KG", it.weightKg) } ?: "+ Peso",
                        onWeightClick = { showWeightDialog = true },
                        onStepClick = {
                            if (!hasActivityPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showStepOptionsDialog = true
                            }
                        }
                    )
                }

                // Unified Meals & Diet Log
                item(key = "meals_$pageDate") {
                    UnifiedMealsCard(
                        mealsByType = pageSummary.mealsByType,
                        favoriteFoods = favoriteFoods,
                        onAddMealClick = { mealType ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNavigateToSearch(mealType)
                        },
                        onMealClick = { meal -> mealToEdit = meal },
                        onQuickLog = { food ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            val mealType = com.example.util.MealTypes.inferMealTypeFromHour(hour)
                            viewModel.quickLogFood(food, mealType)
                            android.widget.Toast.makeText(context, "${food.name} registrato in ${com.example.util.MealTypes.displayName(mealType).lowercase()}", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        onDeleteMeal = { meal ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.deleteMealEntry(meal.id)
                        }
                    )
                }
            }
        }
    }

    if (showWeightDialog) {
        LogWeightDialog(
            initialWeight = formatWeightInput(latestWeight?.weightKg),
            onDismiss = { showWeightDialog = false },
            onSave = { weight ->
                viewModel.updateWeight(weight, currentDate)
                showWeightDialog = false
            }
        )
    }

    if (showStreakCalendarDialog) {
        MonthlyStreakDialog(
            allMeals = allMeals,
            streakCount = streakCount,
            allTargets = allTargets,
            defaultTarget = dailyTarget.targetCalories,
            onDateSelected = { selectedDate ->
                showStreakCalendarDialog = false
                onDateChange(selectedDate)
            },
            onDeleteDate = { dateToDelete ->
                viewModel.deleteMealsForDate(dateToDelete)
            },
            onDismiss = { showStreakCalendarDialog = false }
        )
    }

    mealToEdit?.let { meal ->
        EditMealEntryDialog(
            meal = meal,
            onDismiss = { mealToEdit = null },
            onSave = { newGrams ->
                viewModel.updateMealQuantity(meal, newGrams)
                mealToEdit = null
            },
            onDelete = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteMealEntry(meal.id)
                mealToEdit = null
            }
        )
    }

    if (showStepOptionsDialog) {
        StepOptionsDialog(
            currentSteps = currentLiveSteps,
            stepGoal = stepGoal,
            onResetTodaySteps = {
                pedometerViewModel.resetTodaySteps()
            },
            onDismiss = { showStepOptionsDialog = false }
        )
    }
}
