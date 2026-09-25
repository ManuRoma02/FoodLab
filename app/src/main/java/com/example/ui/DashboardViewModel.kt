package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.FoodLabApplication
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.NutritionCalculator
import com.example.util.StreakCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FoodLabApplication).repository

    init {
        // Init fake database data for empty states
        viewModelScope.launch {
            val foods = repository.allFoods.first()
            if (foods.isEmpty()) {
                repository.insertInitialFoods(
                    listOf(
                        FoodItem(name = "Oatmeal", brand = "Quaker", caloriesPer100g = 379, proteinPer100g = 13.1f, carbsPer100g = 67.7f, fatPer100g = 6.5f, isFavorite = true),
                        FoodItem(name = "Chicken Breast", brand = "Generic", caloriesPer100g = 165, proteinPer100g = 31f, carbsPer100g = 0f, fatPer100g = 3.6f, isFavorite = true),
                        FoodItem(name = "Almonds", brand = "Generic", caloriesPer100g = 579, proteinPer100g = 21.1f, carbsPer100g = 21.6f, fatPer100g = 49.9f),
                        FoodItem(name = "Greek Yogurt", brand = "Chobani", caloriesPer100g = 59, proteinPer100g = 10f, carbsPer100g = 3.6f, fatPer100g = 0.4f, isFavorite = true),
                        FoodItem(name = "Banana", brand = "Generic", caloriesPer100g = 89, proteinPer100g = 1.1f, carbsPer100g = 22.8f, fatPer100g = 0.3f, isFavorite = true),
                        FoodItem(name = "Salmon", brand = "Generic", caloriesPer100g = 208, proteinPer100g = 20f, carbsPer100g = 0f, fatPer100g = 13f)
                    )
                )
            }
            val target = repository.getFixedTarget().first()
            if (target == null) {
                repository.insertTarget(DailyTarget(dateString = "FIXED"))
            }
        }
    }

    private val _currentDate = MutableStateFlow(SessionManager.formatDate(java.util.Date()))
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    fun setDate(date: String) {
        _currentDate.value = date
    }

    val allTargets: StateFlow<List<DailyTarget>> = repository.allTargets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyTarget: StateFlow<DailyTarget> = currentDate
        .flatMapLatest { date -> repository.resolveTargetForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyTarget("FIXED"))

    fun resolveTargetForDateSync(date: String, targets: List<DailyTarget>): DailyTarget {
        val specific = targets.firstOrNull { it.dateString == date }
        if (specific != null) return specific

        val calendar = Calendar.getInstance().apply {
            time = SessionManager.parseDateString(date)
        }
        val weekdayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            else -> ""
        }
        val weekdayTarget = targets.firstOrNull { it.dateString == weekdayName }
        if (weekdayTarget != null) return weekdayTarget

        return targets.firstOrNull { it.dateString == "FIXED" } ?: DailyTarget("FIXED")
    }

    val latestWeight: StateFlow<WeightLog?> = repository.allWeights
        .map { weights -> weights.maxByOrNull { it.timestamp } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateWeight(weightKg: Float, dateString: String = SessionManager.formatDate(java.util.Date())) {
        if (!weightKg.isFinite() || weightKg !in 20f..400f) return
        viewModelScope.launch {
            repository.insertWeight(
                WeightLog(
                    dateString = dateString, 
                    weightKg = weightKg,
                    timestamp = System.currentTimeMillis()
                )
            )
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    val dailyMeals: StateFlow<List<MealEntry>> = currentDate
        .flatMapLatest { date -> repository.getMealsForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMealsHistory: StateFlow<List<MealEntry>> = repository.getAllMeals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteFoods: StateFlow<List<FoodItem>> = repository.allFoods
        .map { foods -> foods.filter { it.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayKey = flow {
        while (true) {
            emit(SessionManager.formatDate(java.util.Date()))
            delay(60_000)
        }
    }.distinctUntilChanged()

    val todayDate: StateFlow<String> = todayKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionManager.formatDate(java.util.Date()))

    val streakCount: StateFlow<Int> = combine(
        repository.getAllMeals(),
        repository.allTargets,
        repository.allWeights,
        todayKey
    ) { meals, targets, weights, today ->
        val context = getApplication<Application>().applicationContext
        val steps = com.example.util.StepCounterStore.savedSteps(context, today)
        val weight = weights.maxByOrNull { it.timestamp }?.weightKg
        val burned = NutritionCalculator.calculateCaloriesBurned(steps, weight)
        val distinctDates = meals.asSequence().map { it.dateString }.filter { it < today }.distinct()
        val historicalBurned = distinctDates.associateWith { date ->
            val pastSteps = com.example.util.StepCounterStore.savedSteps(context, date)
            NutritionCalculator.calculateCaloriesBurned(pastSteps, weight)
        }
        StreakCalculator.calculate(
            meals = meals,
            targets = targets,
            today = today,
            todayBurnedCalories = burned,
            historicalBurnedCalories = historicalBurned
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun deleteMealEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteMeal(id)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteMealsForDate(date: String) {
        viewModelScope.launch {
            repository.deleteMealsForDate(date)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    private var lastQuickLogTime = 0L
    private var lastQuickLogFoodId = -1

    fun quickLogFood(food: FoodItem, mealType: String = com.example.util.MealTypes.SNACK) {
        val now = System.currentTimeMillis()
        if (food.id == lastQuickLogFoodId && now - lastQuickLogTime < 400L) {
            return
        }
        lastQuickLogTime = now
        lastQuickLogFoodId = food.id
        viewModelScope.launch {
            val servingGrams = if (food.defaultServingSize > 0) food.defaultServingSize else 100
            val nutrition = NutritionCalculator.fromPer100g(
                quantityGrams = servingGrams,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g
            )
            val newMeal = MealEntry(
                dateString = currentDate.value,
                mealType = mealType,
                foodId = food.id,
                foodName = food.name,
                quantityGrams = servingGrams,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fat = nutrition.fat,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g
            )
            repository.insertMeal(newMeal)
            repository.incrementUseCount(food.id)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun updateMealQuantity(meal: MealEntry, newQuantityGrams: Int) {
        viewModelScope.launch {
            repository.updateMealQuantity(meal, newQuantityGrams)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun updateMealEntry(meal: MealEntry) {
        viewModelScope.launch {
            repository.insertMeal(meal)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun moveMealToType(meal: MealEntry, targetMealType: String) {
        if (meal.mealType == targetMealType) return
        val updated = meal.copy(mealType = targetMealType)
        viewModelScope.launch {
            repository.insertMeal(updated)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }
}
