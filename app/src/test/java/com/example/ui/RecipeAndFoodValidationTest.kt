package com.example.ui

import com.example.data.FoodItem
import com.example.data.MealTemplate
import com.example.data.MealTemplateItem
import com.example.data.MealTemplateWithItems
import org.junit.Assert.*
import org.junit.Test

class RecipeAndFoodValidationTest {

    @Test
    fun testRecipePerServingNutritionalCalculations() {
        val pasta = FoodItem(id = 1, name = "Pasta", brand = "Barilla", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f)
        val sauce = FoodItem(id = 2, name = "Sugo Pomodoro", brand = "Mutti", caloriesPer100g = 50, proteinPer100g = 1.5f, carbsPer100g = 8f, fatPer100g = 1f)

        // 200g pasta = 700 kcal, 24g pro, 140g carbs, 4g fat
        // 100g sauce = 50 kcal, 1.5g pro, 8g carbs, 1g fat
        // Total = 750 kcal, 25.5g pro, 148g carbs, 5g fat
        val item1 = MealTemplateItem(id = 1, templateId = 1, foodId = 1, foodName = pasta.name, brand = pasta.brand, caloriesPer100g = pasta.caloriesPer100g, proteinPer100g = pasta.proteinPer100g, carbsPer100g = pasta.carbsPer100g, fatPer100g = pasta.fatPer100g, quantityGrams = 200)
        val item2 = MealTemplateItem(id = 2, templateId = 1, foodId = 2, foodName = sauce.name, brand = sauce.brand, caloriesPer100g = sauce.caloriesPer100g, proteinPer100g = sauce.proteinPer100g, carbsPer100g = sauce.carbsPer100g, fatPer100g = sauce.fatPer100g, quantityGrams = 100)

        val servings = 2
        val template = MealTemplate(id = 1, name = "Pasta al Sugo", servings = servings)
        val recipeWithItems = MealTemplateWithItems(template = template, items = listOf(item1, item2))

        val totalCals = recipeWithItems.items.sumOf { (it.caloriesPer100g * it.quantityGrams / 100f).toInt() }
        val totalPro = recipeWithItems.items.fold(0f) { acc, item -> acc + (item.proteinPer100g * item.quantityGrams / 100f) }
        val totalCarbs = recipeWithItems.items.fold(0f) { acc, item -> acc + (item.carbsPer100g * item.quantityGrams / 100f) }
        val totalFat = recipeWithItems.items.fold(0f) { acc, item -> acc + (item.fatPer100g * item.quantityGrams / 100f) }

        assertEquals(750, totalCals)
        assertEquals(25.5f, totalPro, 0.01f)
        assertEquals(148.0f, totalCarbs, 0.01f)
        assertEquals(5.0f, totalFat, 0.01f)

        // Per portion calculations (servings = 2)
        val perServingCals = (totalCals.toFloat() / servings).toInt()
        val perServingPro = totalPro / servings
        val perServingCarbs = totalCarbs / servings
        val perServingFat = totalFat / servings

        assertEquals(375, perServingCals)
        assertEquals(12.75f, perServingPro, 0.01f)
        assertEquals(74.0f, perServingCarbs, 0.01f)
        assertEquals(2.5f, perServingFat, 0.01f)
    }

    @Test
    fun testRecipePortionLoggingQuantities() {
        val servings = 4
        val quantityGramsTotal = 400

        val portionGrams = (quantityGramsTotal.toFloat() / servings).toInt().coerceAtLeast(1)
        assertEquals(100, portionGrams)
    }

    @Test
    fun testFoodNameValidation() {
        val emptyName = ""
        val blankName = "   "
        val validName = "Mela Rossa"

        assertFalse(emptyName.trim().isNotBlank())
        assertFalse(blankName.trim().isNotBlank())
        assertTrue(validName.trim().isNotBlank())
    }

    @Test
    fun testFoodNumericValidation() {
        val negativeCalories = -100
        val validCalories = 250
        val negativeProtein = -5.0f
        val validProtein = 15.5f

        assertFalse(negativeCalories >= 0 && negativeCalories <= 900)
        assertTrue(validCalories >= 0 && validCalories <= 900)

        assertFalse(negativeProtein >= 0f && negativeProtein <= 100f)
        assertTrue(validProtein >= 0f && validProtein <= 100f)
    }

    @Test
    fun testTotalDishModeCalculation() {
        val totalWeight = 500f
        val totalCalories = 1000
        val totalProtein = 50f

        val ratio100g = 100f / totalWeight
        val caloriesPer100g = (totalCalories * ratio100g).toInt()
        val proteinPer100g = totalProtein * ratio100g

        assertEquals(200, caloriesPer100g)
        assertEquals(10f, proteinPer100g, 0.01f)
    }
}
