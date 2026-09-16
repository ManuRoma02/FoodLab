package com.example.util

import com.example.data.DailyTarget
import com.example.data.MealEntry
import com.example.ui.SessionManager
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorsTest {

    @Test
    fun testNutritionCalculatorFromPer100g_standard() {
        val result = NutritionCalculator.fromPer100g(
            quantityGrams = 150,
            caloriesPer100g = 200,
            proteinPer100g = 20f,
            carbsPer100g = 10f,
            fatPer100g = 5f
        )
        assertEquals(300, result.calories)
        assertEquals(30f, result.protein, 0.001f)
        assertEquals(15f, result.carbs, 0.001f)
        assertEquals(7.5f, result.fat, 0.001f)
    }

    @Test
    fun testNutritionCalculatorFromPer100g_fractionalQuantity() {
        val result = NutritionCalculator.fromPer100g(
            quantityGrams = 0.5f,
            caloriesPer100g = 400,
            proteinPer100g = 10f,
            carbsPer100g = 20f,
            fatPer100g = 30f
        )
        assertEquals(2, result.calories)
        assertEquals(0.05f, result.protein, 0.001f)
        assertEquals(0.10f, result.carbs, 0.001f)
        assertEquals(0.15f, result.fat, 0.001f)
    }

    @Test
    fun testNutritionCalculatorFromPer100g_zeroAndNegative() {
        val zero = NutritionCalculator.fromPer100g(0, 200, 20f, 10f, 5f)
        assertEquals(0, zero.calories)
        assertEquals(0f, zero.protein, 0.001f)

        val negative = NutritionCalculator.fromPer100g(-50, 200, 20f, 10f, 5f)
        assertEquals(0, negative.calories)
    }

    @Test
    fun testNutritionCalculatorCaloriesBurned_withWeight() {
        val burned70kg = NutritionCalculator.calculateCaloriesBurned(10000, 70f)
        assertEquals(400, burned70kg)

        val burned105kg = NutritionCalculator.calculateCaloriesBurned(10000, 105f)
        assertEquals(600, burned105kg)
    }

    @Test
    fun testStreakCalculator_preserveOngoingStreakMidDay() {
        val today = "2026-08-22"
        val yesterday = "2026-08-21"
        val twoDaysAgo = "2026-08-20"

        val targets = listOf(DailyTarget("FIXED", targetCalories = 2000))
        val meals = listOf(
            MealEntry(dateString = twoDaysAgo, mealType = "LUNCH", foodId = 1, foodName = "Meal", quantityGrams = 100, calories = 2000, protein = 50f, carbs = 100f, fat = 20f),
            MealEntry(dateString = yesterday, mealType = "LUNCH", foodId = 1, foodName = "Meal", quantityGrams = 100, calories = 2000, protein = 50f, carbs = 100f, fat = 20f),
            MealEntry(dateString = today, mealType = "LUNCH", foodId = 1, foodName = "Meal", quantityGrams = 100, calories = 2300, protein = 50f, carbs = 100f, fat = 20f)
        )

        // Today in progress preserves ongoing 2-day streak from yesterday
        val streakInProgress = StreakCalculator.calculate(meals, targets, today, todayBurnedCalories = 0)
        assertEquals(2, streakInProgress)

        // With steps burning 350 kcal: net = 2300 - 350 = 1950 (within 1800..2200 target range) -> today completed, streak is 3
        val streakWithBurned = StreakCalculator.calculate(meals, targets, today, todayBurnedCalories = 350)
        assertEquals(3, streakWithBurned)
    }

    @Test
    fun testSessionManager_getFriendlyDateLabel() {
        val todayStr = SessionManager.formatDate(java.util.Date())
        assertEquals("TODAY", SessionManager.getFriendlyDateLabel(todayStr))

        val yesterdayStr = SessionManager.getPreviousDay(todayStr, 1)
        assertEquals("YESTERDAY", SessionManager.getFriendlyDateLabel(yesterdayStr))

        val tomorrowStr = SessionManager.getNextDay(todayStr)
        assertEquals("TOMORROW", SessionManager.getFriendlyDateLabel(tomorrowStr))
    }
}
