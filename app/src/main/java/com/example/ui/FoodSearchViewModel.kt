package com.example.ui

import android.app.Application
import android.util.Log
import com.example.FoodLabApplication
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.util.NutritionCalculator
import com.example.util.MealTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FoodSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as FoodLabApplication
    private val repository = app.repository

    val allTemplates: StateFlow<List<MealTemplateWithItems>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveMealGroupAsTemplate(name: String, entries: List<MealEntry>) {
        viewModelScope.launch {
            val cleanName = name.trim().take(80)
            if (cleanName.isEmpty() || entries.isEmpty()) return@launch
            val templateId = repository.insertTemplate(MealTemplate(name = cleanName))
            val foodsById = repository.getFoodsByIds(entries.map { it.foodId }.filter { it > 0 }.distinct())
                .associateBy { it.id }
            val items = entries.map { entry ->
                val food = foodsById[entry.foodId]
                val caloriesPer100 = food?.caloriesPer100g ?: if (entry.caloriesPer100g > 0) entry.caloriesPer100g else if (entry.quantityGrams > 0) ((entry.calories * 100f) / entry.quantityGrams).toInt() else 0
                val proteinPer100 = food?.proteinPer100g ?: if (entry.proteinPer100g > 0f) entry.proteinPer100g else if (entry.quantityGrams > 0) ((entry.protein * 100f) / entry.quantityGrams) else 0f
                val carbsPer100 = food?.carbsPer100g ?: if (entry.carbsPer100g > 0f) entry.carbsPer100g else if (entry.quantityGrams > 0) ((entry.carbs * 100f) / entry.quantityGrams) else 0f
                val fatPer100 = food?.fatPer100g ?: if (entry.fatPer100g > 0f) entry.fatPer100g else if (entry.quantityGrams > 0) ((entry.fat * 100f) / entry.quantityGrams) else 0f
                val brand = food?.brand ?: ""
                val defaultServing = food?.defaultServingSize ?: 100

                MealTemplateItem(
                    templateId = templateId.toInt(),
                    foodId = entry.foodId,
                    foodName = entry.foodName,
                    brand = brand,
                    caloriesPer100g = caloriesPer100,
                    proteinPer100g = proteinPer100,
                    carbsPer100g = carbsPer100,
                    fatPer100g = fatPer100,
                    defaultServingSize = defaultServing,
                    quantityGrams = entry.quantityGrams
                )
            }
            repository.insertTemplateItems(items)
        }
    }

    fun logTemplate(templateWithItems: MealTemplateWithItems, date: String, mealType: String) {
        viewModelScope.launch {
            val entries = templateWithItems.items.map { item ->
                val quantity = item.quantityGrams.coerceIn(1, 5_000)
                val nutrition = NutritionCalculator.fromPer100g(
                    quantityGrams = quantity,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g
                )

                MealEntry(
                    dateString = date,
                    mealType = MealTypes.canonicalize(mealType),
                    foodId = item.foodId,
                    foodName = item.foodName,
                    quantityGrams = quantity,
                    calories = nutrition.calories,
                    protein = nutrition.protein,
                    carbs = nutrition.carbs,
                    fat = nutrition.fat,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g
                )
            }
            repository.insertMeals(entries)
            repository.incrementUseCounts(templateWithItems.items.map { it.foodId }.filter { it > 0 })
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteTemplate(templateId: Int) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSearchLoading = MutableStateFlow(false)
    val isSearchLoading = _isSearchLoading.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError = _searchError.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline = _isOffline.asStateFlow()

    private val _offResults = MutableStateFlow<List<FoodItem>>(emptyList())
    private val offSearchCache: MutableMap<String, List<FoodItem>> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<String, List<FoodItem>>(24, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FoodItem>>?): Boolean = size > 40
        }
    )
    private var offIdCounter = -1
    private val tempPortions: MutableMap<Int, List<FoodPortion>> = java.util.Collections.synchronizedMap(
        object : LinkedHashMap<Int, List<FoodPortion>>(32, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, List<FoodPortion>>?): Boolean = size > 100
        }
    )

    private fun nextOffId(): Int {
        if (offIdCounter >= 0) offIdCounter = -1
        return offIdCounter--
    }

    private val localSearchResults = _searchQuery
        .map(SearchOptimizer::normalizeQuery)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) repository.allFoods else repository.searchFoods(SearchOptimizer.escapeForLike(query))
        }

    val searchResults: StateFlow<List<FoodItem>> = combine(
        localSearchResults,
        _offResults,
        _searchQuery
    ) { localFoods, offFoods, query ->
        SearchOptimizer.rank(localFoods, offFoods, query)
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var searchJob: kotlinx.coroutines.Job? = null

    fun updateSearchQuery(query: String) {
        val normalizedQuery = SearchOptimizer.normalizeQuery(query)
        val queryKey = SearchOptimizer.cacheKey(query)
        _searchQuery.value = query
        _searchError.value = null
        searchJob?.cancel()

        if (SearchOptimizer.shouldSearch(query)) {
            val cachedResults = offSearchCache[queryKey]
            if (cachedResults != null) {
                _offResults.value = cachedResults
                _isSearchLoading.value = false
                _isOffline.value = false
                return
            } else {
                _offResults.value = emptyList()
            }

            _isSearchLoading.value = true
            searchJob = viewModelScope.launch {
                val activeQuery = normalizedQuery
                val activeKey = queryKey
                try {
                    delay(250)
                    val mappedProducts = repository.searchRemoteFoods(activeQuery, nextOffId())
                    val foods = mappedProducts.map { mapped ->
                        mapped.portion?.let { tempPortions[mapped.food.id] = listOf(it) }
                        mapped.food
                    }
                    if (SearchOptimizer.isResponseForActiveQuery(activeQuery, _searchQuery.value)) {
                        if (SearchOptimizer.shouldCacheApiResults(foods)) {
                            offSearchCache[activeKey] = foods
                        }
                        _offResults.value = foods
                        _isOffline.value = false
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Open Food Facts search failed", e)
                    if (SearchOptimizer.isResponseForActiveQuery(activeQuery, _searchQuery.value)) {
                        _isOffline.value = true
                        _searchError.value = "Ricerca online non disponibile. Verifica la connessione e riprova."
                    }
                } finally {
                    if (SearchOptimizer.isResponseForActiveQuery(activeQuery, _searchQuery.value)) {
                        _isSearchLoading.value = false
                    }
                }
            }
        } else {
            _offResults.value = emptyList()
            _isSearchLoading.value = false
            _searchError.value = null
            _isOffline.value = false
        }
    }

    fun retryOnlineSearch() {
        val current = _searchQuery.value
        if (current.isNotBlank()) {
            _searchError.value = null
            _isOffline.value = false
            updateSearchQuery(current)
        }
    }

    suspend fun fetchProductByBarcode(barcode: String): BarcodeLookupResult {
        val cleanBarcode = barcode.filter(Char::isDigit).take(32)
        if (cleanBarcode.isBlank()) return BarcodeLookupResult.NotFound
        val result = repository.fetchProductByBarcode(cleanBarcode, nextOffId())
        if (result is BarcodeLookupResult.Success) {
            result.portion?.let { tempPortions[result.food.id] = listOf(it) }
            val currentList = _offResults.value.toMutableList()
            currentList.add(0, result.food)
            _offResults.value = currentList.distinctBy { it.id }.take(30)
            _isOffline.value = false
        } else if (result is BarcodeLookupResult.NetworkError) {
            _isOffline.value = true
        }
        return result
    }

    fun addMealEntry(food: FoodItem, quantityGrams: Int, mealType: String, dateString: String) {
        viewModelScope.launch {
            val safeQuantity = quantityGrams.coerceIn(1, 5_000)
            var localFoodId = food.id
            if (food.id < 0) {
                val newFood = food.copy(id = 0, useCount = 1)
                localFoodId = repository.insertFood(newFood).toInt()
                // Save cached portion size if available
                val cached = tempPortions[food.id]
                if (!cached.isNullOrEmpty()) {
                    val dbPortions = cached.map { it.copy(id = 0, foodId = localFoodId) }
                    repository.insertPortions(dbPortions)
                }
            } else {
                repository.incrementUseCount(food.id)
            }
            
            val nutrition = NutritionCalculator.fromPer100g(
                quantityGrams = safeQuantity,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g
            )

            val entry = MealEntry(
                dateString = dateString,
                mealType = MealTypes.canonicalize(mealType),
                foodId = localFoodId,
                foodName = food.name,
                quantityGrams = safeQuantity,
                calories = nutrition.calories,
                protein = nutrition.protein,
                carbs = nutrition.carbs,
                fat = nutrition.fat,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g
            )
            repository.insertMeal(entry)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun quickLogFood(food: FoodItem, dateString: String, mealType: String, quantityGrams: Int = if (food.defaultServingSize > 0) food.defaultServingSize else 100) {
        addMealEntry(food, quantityGrams, mealType, dateString)
    }

    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch {
            if (food.id < 0) {
                val newFood = food.copy(id = 0, isFavorite = true)
                val localFoodId = repository.insertFood(newFood).toInt()
                val cached = tempPortions[food.id]
                if (!cached.isNullOrEmpty()) {
                    val dbPortions = cached.map { it.copy(id = 0, foodId = localFoodId) }
                    repository.insertPortions(dbPortions)
                }
            } else {
                repository.setFavorite(food.id, !food.isFavorite)
            }
        }
    }

    suspend fun getFoodById(id: Int): FoodItem? {
        if (id < 0) {
            return _offResults.value.find { it.id == id }
        }
        return repository.getFoodById(id)
    }

    suspend fun getPortionsForFood(foodId: Int): List<FoodPortion> {
        if (foodId < 0) {
            return tempPortions[foodId] ?: emptyList()
        }
        return repository.getPortionsForFood(foodId)
    }

    fun getLoggedMealsForDate(date: String): Flow<List<MealEntry>> {
        return repository.getMealsForDate(date)
    }

    fun deleteMeal(id: Int) {
        viewModelScope.launch {
            repository.deleteMeal(id)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun updateMealQuantity(meal: MealEntry, newQuantityGrams: Int) {
        viewModelScope.launch {
            repository.updateMealQuantity(meal, newQuantityGrams)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun updateMeal(meal: MealEntry) {
        viewModelScope.launch {
            repository.insertMeal(meal)
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun createFood(
        name: String, 
        brand: String, 
        calories: Int, 
        protein: Float, 
        carbs: Float, 
        fat: Float, 
        portions: List<Pair<String, Float>> = emptyList(),
        barcode: String = ""
    ) {
        val cleanName = name.trim().take(160)
        if (cleanName.isEmpty() || calories !in 0..900 || !protein.isFinite() || !carbs.isFinite() || !fat.isFinite()) return
        viewModelScope.launch {
            val foodId = repository.insertFood(
                FoodItem(
                    name = cleanName,
                    brand = brand.trim().take(160).ifEmpty { "Custom" },
                    caloriesPer100g = calories,
                    proteinPer100g = protein.coerceIn(0f, 100f),
                    carbsPer100g = carbs.coerceIn(0f, 100f),
                    fatPer100g = fat.coerceIn(0f, 100f),
                    barcode = barcode.filter(Char::isDigit).take(32)
                )
            )
            if (portions.isNotEmpty()) {
                val dbPortions = portions.map {
                    FoodPortion(foodId = foodId.toInt(), name = it.first.trim().take(60), weightGrams = it.second.coerceIn(0.1f, 5_000f))
                }
                repository.insertPortions(dbPortions)
            }
        }
    }

    private companion object {
        const val TAG = "FoodSearchViewModel"
    }
}
