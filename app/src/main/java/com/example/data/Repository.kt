package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf

class Repository(
    private val foodDao: FoodDao,
    private val mealDao: MealEntryDao,
    private val targetDao: DailyTargetDao,
    private val weightDao: WeightLogDao,
    private val templateDao: MealTemplateDao,
    private val portionDao: FoodPortionDao,
    private val shoppingDao: ShoppingItemDao? = null,
    val openFoodFactsApi: OpenFoodFactsApi? = null
) {
    val allFoods: Flow<List<FoodItem>> = foodDao.getAllFoods()
    val allWeights: Flow<List<WeightLog>> = weightDao.getAllWeights()
    val allWeightsDesc: Flow<List<WeightLog>> = weightDao.getAllWeightsDesc()
    val allTargets: Flow<List<DailyTarget>> = targetDao.getAllTargets()
    val allTemplates: Flow<List<MealTemplateWithItems>> = templateDao.getAllTemplatesWithItems()
    val allShoppingItems: Flow<List<ShoppingItem>> = shoppingDao?.getAllShoppingItems() ?: flowOf(emptyList())
    
    suspend fun getPortionsForFood(foodId: Int) = portionDao.getPortionsForFood(foodId)
    suspend fun insertPortions(portions: List<FoodPortion>) = portionDao.insertPortions(portions)
    suspend fun deletePortionsForFood(foodId: Int) = portionDao.deletePortionsForFood(foodId)

    
    fun searchFoods(query: String) = foodDao.searchFoods(query)
    suspend fun getFoodById(id: Int) = foodDao.getFoodById(id)
    suspend fun getFoodByBarcode(barcode: String): FoodItem? = foodDao.getFoodByBarcode(barcode)
    suspend fun getFoodsByIds(ids: List<Int>) = if (ids.isEmpty()) emptyList() else foodDao.getFoodsByIds(ids)
    suspend fun incrementUseCount(id: Int) = foodDao.incrementUseCount(id)
    suspend fun incrementUseCounts(ids: List<Int>) {
        if (ids.isNotEmpty()) foodDao.incrementUseCounts(ids.distinct())
    }
    suspend fun setFavorite(id: Int, isFavorite: Boolean) = foodDao.setFavorite(id, isFavorite)
    suspend fun insertFood(food: FoodItem): Long = foodDao.insertFood(food)
    suspend fun insertInitialFoods(foods: List<FoodItem>): List<Long> = foodDao.insertFoods(foods)

    suspend fun ensureDefaultFoodsSeeded() {
        if (foodDao.getFoodCount() == 0) {
            DefaultFoodCatalog.items.forEach { seed ->
                val foodId = foodDao.insertFood(seed.food).toInt()
                if (seed.portions.isNotEmpty()) {
                    val portions = seed.portions.map { (name, weight) ->
                        FoodPortion(foodId = foodId, name = name, weightGrams = weight)
                    }
                    portionDao.insertPortions(portions)
                }
            }
        }
    }

    fun getMealsForDate(date: String) = mealDao.getMealsForDate(date)
    fun getAllMeals() = mealDao.getAllMeals()
    suspend fun insertMeal(meal: MealEntry) = mealDao.insertMeal(meal)
    suspend fun insertMeals(meals: List<MealEntry>) {
        if (meals.isNotEmpty()) mealDao.insertMeals(meals)
    }
    suspend fun deleteMeal(id: Int) = mealDao.deleteMeal(id)
    suspend fun deleteMealsForDate(date: String) = mealDao.deleteMealsForDate(date)

    suspend fun updateMealQuantity(meal: MealEntry, newQuantityGrams: Int): MealEntry {
        val safeQuantity = newQuantityGrams.coerceIn(1, 5_000)
        val hasSnapshot = meal.caloriesPer100g > 0 || meal.proteinPer100g > 0f || meal.carbsPer100g > 0f || meal.fatPer100g > 0f || (meal.calories == 0 && meal.protein == 0f && meal.carbs == 0f && meal.fat == 0f)
        val calPer100 = if (hasSnapshot) meal.caloriesPer100g else {
            val food = if (meal.foodId > 0) foodDao.getFoodById(meal.foodId) else null
            food?.caloriesPer100g ?: if (meal.quantityGrams > 0) kotlin.math.round((meal.calories * 100f) / meal.quantityGrams).toInt() else 0
        }
        val proPer100 = if (hasSnapshot) meal.proteinPer100g else {
            val food = if (meal.foodId > 0) foodDao.getFoodById(meal.foodId) else null
            food?.proteinPer100g ?: if (meal.quantityGrams > 0) (meal.protein * 100f) / meal.quantityGrams else 0f
        }
        val carbPer100 = if (hasSnapshot) meal.carbsPer100g else {
            val food = if (meal.foodId > 0) foodDao.getFoodById(meal.foodId) else null
            food?.carbsPer100g ?: if (meal.quantityGrams > 0) (meal.carbs * 100f) / meal.quantityGrams else 0f
        }
        val fatPer100 = if (hasSnapshot) meal.fatPer100g else {
            val food = if (meal.foodId > 0) foodDao.getFoodById(meal.foodId) else null
            food?.fatPer100g ?: if (meal.quantityGrams > 0) (meal.fat * 100f) / meal.quantityGrams else 0f
        }

        val nutrition = com.example.util.NutritionCalculator.fromPer100g(
            quantityGrams = safeQuantity,
            caloriesPer100g = calPer100,
            proteinPer100g = proPer100,
            carbsPer100g = carbPer100,
            fatPer100g = fatPer100
        )
        val updated = meal.copy(
            quantityGrams = safeQuantity,
            calories = nutrition.calories,
            protein = nutrition.protein,
            carbs = nutrition.carbs,
            fat = nutrition.fat,
            caloriesPer100g = calPer100,
            proteinPer100g = proPer100,
            carbsPer100g = carbPer100,
            fatPer100g = fatPer100
        )
        mealDao.insertMeal(updated)
        return updated
    }

    suspend fun insertTemplate(template: MealTemplate): Long = templateDao.insertTemplate(template)
    suspend fun insertTemplateItems(items: List<MealTemplateItem>) = templateDao.insertTemplateItems(items)
    suspend fun insertTemplateWithItems(template: MealTemplate, items: List<MealTemplateItem>): Long {
        val templateId = templateDao.insertTemplate(template)
        val remappedItems = items.map { it.copy(templateId = templateId.toInt()) }
        templateDao.insertTemplateItems(remappedItems)
        return templateId
    }
    suspend fun deleteTemplate(id: Int) = templateDao.deleteTemplate(id)

    suspend fun wipeMeals() = mealDao.wipeMeals()
    suspend fun wipeFoods() = foodDao.wipeFoods()
    suspend fun wipeTargets() = targetDao.wipeTargets()
    suspend fun wipeWeights() = weightDao.wipeWeights()
    suspend fun wipeTemplates() = templateDao.wipeTemplates()

    fun getTargetForDate(date: String) = targetDao.getTargetForDate(date)
    fun getFixedTarget() = targetDao.getFixedTarget()
    fun getTargetsForDates(dates: List<String>) = targetDao.getTargetsForDates(dates)
    suspend fun insertTarget(target: DailyTarget) = targetDao.insertTarget(target)

    fun resolveTargetForDate(date: String): Flow<DailyTarget> {
        val calendar = java.util.Calendar.getInstance().apply {
            time = com.example.ui.SessionManager.parseDateString(date)
        }
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayString = mapOf(
            java.util.Calendar.MONDAY to "MONDAY",
            java.util.Calendar.TUESDAY to "TUESDAY",
            java.util.Calendar.WEDNESDAY to "WEDNESDAY",
            java.util.Calendar.THURSDAY to "THURSDAY",
            java.util.Calendar.FRIDAY to "FRIDAY",
            java.util.Calendar.SATURDAY to "SATURDAY",
            java.util.Calendar.SUNDAY to "SUNDAY"
        )[dayOfWeek] ?: "FIXED"

        return targetDao.getTargetForDate(date).combine(targetDao.getTargetForDate(dayString)) { specificTarget, dayTarget ->
            Pair(specificTarget, dayTarget)
        }.combine(targetDao.getFixedTarget()) { (specific, day), fixed ->
            specific ?: day ?: fixed ?: DailyTarget("FIXED")
        }
    }

    suspend fun resolveTargetForDateOnce(date: String): DailyTarget {
        val specific = targetDao.getTargetForDateOnce(date)
        if (specific != null) return specific

        val calendar = java.util.Calendar.getInstance().apply {
            time = com.example.ui.SessionManager.parseDateString(date)
        }
        val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val dayString = mapOf(
            java.util.Calendar.MONDAY to "MONDAY",
            java.util.Calendar.TUESDAY to "TUESDAY",
            java.util.Calendar.WEDNESDAY to "WEDNESDAY",
            java.util.Calendar.THURSDAY to "THURSDAY",
            java.util.Calendar.FRIDAY to "FRIDAY",
            java.util.Calendar.SATURDAY to "SATURDAY",
            java.util.Calendar.SUNDAY to "SUNDAY"
        )[dayOfWeek] ?: "FIXED"

        val dayTarget = targetDao.getTargetForDateOnce(dayString)
        if (dayTarget != null) return dayTarget

        return targetDao.getFixedTargetOnce() ?: DailyTarget("FIXED")
    }

    suspend fun insertShoppingItem(item: ShoppingItem): Long = shoppingDao?.insertItem(item) ?: 0L
    suspend fun setShoppingItemChecked(id: Long, isChecked: Boolean) { shoppingDao?.setChecked(id, isChecked) }
    suspend fun deleteShoppingItem(id: Long) { shoppingDao?.deleteItem(id) }
    suspend fun clearCheckedShoppingItems() { shoppingDao?.clearCheckedItems() }

    suspend fun insertWeight(weight: WeightLog) = weightDao.insertWeight(weight)
    suspend fun getLatestWeightOnce(): WeightLog? = weightDao.getLatestWeightOnce()
    suspend fun deleteWeightById(id: Int) = weightDao.deleteWeightById(id)
    suspend fun deleteLatestWeight() = weightDao.deleteLatestWeight()

    suspend fun searchRemoteFoods(query: String, startId: Int = -1): List<MappedOpenFoodFactsProduct> {
        val api = openFoodFactsApi ?: return emptyList()
        var attempts = 0
        var idCounter = startId
        while (attempts < 2) {
            try {
                attempts++
                val response = api.searchFoods(query)
                return response.products.orEmpty()
                    .mapNotNull { product ->
                        val mappedId = idCounter--
                        OpenFoodFactsMapper.map(product, mappedId)
                    }
                    .distinctBy { it.food.name.lowercase() to it.food.brand.lowercase() }
                    .take(25)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 400..499 || e.code() == 504) return emptyList()
                if (attempts >= 2) return emptyList()
                kotlinx.coroutines.delay(200L)
            } catch (e: java.io.IOException) {
                if (attempts >= 2) return emptyList()
                kotlinx.coroutines.delay(200L)
            } catch (_: Exception) {
                return emptyList()
            }
        }
        return emptyList()
    }

    suspend fun fetchProductByBarcode(barcode: String, tempId: Int = -1): BarcodeLookupResult {
        val cleanBarcode = barcode.filter(Char::isDigit).take(32)
        if (cleanBarcode.length !in 8..32) return BarcodeLookupResult.NotFound

        // 1. Check local database first
        val localFood = foodDao.getFoodByBarcode(cleanBarcode)
        if (localFood != null) {
            val portions = portionDao.getPortionsForFood(localFood.id)
            return BarcodeLookupResult.Success(localFood, portions.firstOrNull())
        }

        // 2. Query remote Open Food Facts API
        val api = openFoodFactsApi ?: return BarcodeLookupResult.NetworkError("Remote API unavailable")
        return try {
            val response = api.getProductByBarcode(cleanBarcode)
            val product = response.product
            if (response.status == 1 && product != null) {
                val mapped = OpenFoodFactsMapper.map(product, tempId, fallbackName = "Barcode product")
                    ?: return BarcodeLookupResult.NotFound
                BarcodeLookupResult.Success(mapped.food.copy(barcode = cleanBarcode), mapped.portion)
            } else {
                BarcodeLookupResult.NotFound
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            BarcodeLookupResult.NetworkError("Could not reach Open Food Facts. Check your connection and try again.")
        }
    }
}

sealed interface BarcodeLookupResult {
    data class Success(val food: FoodItem, val portion: FoodPortion?) : BarcodeLookupResult
    data object NotFound : BarcodeLookupResult
    data class NetworkError(val message: String) : BarcodeLookupResult
}
