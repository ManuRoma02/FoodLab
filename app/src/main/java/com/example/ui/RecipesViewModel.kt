package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FoodLabApplication
import com.example.data.*
import com.example.util.MealTypes
import com.example.util.NutritionCalculator
import com.example.util.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FoodLabApplication).repository

    val allTemplates: StateFlow<List<MealTemplateWithItems>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFoods: StateFlow<List<FoodItem>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveRecipe(
        name: String,
        servings: Int,
        ingredients: List<Pair<FoodItem, Int>>
    ) {
        val cleanName = name.trim().take(160)
        if (cleanName.isEmpty() || ingredients.isEmpty() || servings < 1) return

        viewModelScope.launch {
            val template = MealTemplate(name = cleanName, servings = servings)
            val items = ingredients.map { (food, grams) ->
                MealTemplateItem(
                    templateId = 0,
                    foodId = food.id,
                    foodName = food.name,
                    brand = food.brand,
                    caloriesPer100g = food.caloriesPer100g,
                    proteinPer100g = food.proteinPer100g,
                    carbsPer100g = food.carbsPer100g,
                    fatPer100g = food.fatPer100g,
                    defaultServingSize = food.defaultServingSize,
                    quantityGrams = grams
                )
            }
            repository.insertTemplateWithItems(template, items)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun deleteRecipe(templateId: Int) {
        viewModelScope.launch {
            repository.deleteTemplate(templateId)
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun logRecipeMeal(
        recipeWithItems: MealTemplateWithItems,
        mealType: String,
        dateString: String
    ) {
        viewModelScope.launch {
            val servings = recipeWithItems.template.servings.coerceAtLeast(1)
            val canonicalMealType = MealTypes.canonicalize(mealType)
            val entries = recipeWithItems.items.map { item ->
                val portionGrams = (item.quantityGrams.toFloat() / servings).toInt().coerceAtLeast(1)
                val nutrition = NutritionCalculator.fromPer100g(
                    quantityGrams = portionGrams,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g
                )
                MealEntry(
                    dateString = dateString,
                    mealType = canonicalMealType,
                    foodId = item.foodId,
                    foodName = item.foodName,
                    quantityGrams = portionGrams,
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
            repository.incrementUseCounts(recipeWithItems.items.map { it.foodId }.filter { it > 0 })
            WidgetUpdater.updateAllWidgets(getApplication())
        }
    }
}
