package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class NutritionCalculatorTest {

    @Test
    fun fromPer100g_calculatesAccurately() {
        val result = NutritionCalculator.fromPer100g(
            quantityGrams = 150,
            caloriesPer100g = 200,
            proteinPer100g = 10f,
            carbsPer100g = 20f,
            fatPer100g = 5f
        )
        assertEquals(300, result.calories)
        assertEquals(15f, result.protein, 0.001f)
        assertEquals(30f, result.carbs, 0.001f)
        assertEquals(7.5f, result.fat, 0.001f)
    }

    @Test
    fun fromPer100g_guardsAgainstZeroAndNegativeQuantities() {
        val zeroResult = NutritionCalculator.fromPer100g(
            quantityGrams = 0,
            caloriesPer100g = 200,
            proteinPer100g = 10f,
            carbsPer100g = 20f,
            fatPer100g = 5f
        )
        assertEquals(0, zeroResult.calories)
        assertEquals(0f, zeroResult.protein, 0.001f)

        val negativeResult = NutritionCalculator.fromPer100g(
            quantityGrams = -50,
            caloriesPer100g = 200,
            proteinPer100g = 10f,
            carbsPer100g = 20f,
            fatPer100g = 5f
        )
        assertEquals(0, negativeResult.calories)
        assertEquals(0f, negativeResult.protein, 0.001f)
    }

    @Test
    fun rescaleExisting_handlesNormalAndEdgeCases() {
        val normal = NutritionCalculator.rescaleExisting(
            oldQuantityGrams = 100,
            newQuantityGrams = 200,
            calories = 150,
            protein = 10f,
            carbs = 15f,
            fat = 4f
        )
        assertEquals(300, normal.calories)
        assertEquals(20f, normal.protein, 0.001f)

        val invalidOld = NutritionCalculator.rescaleExisting(
            oldQuantityGrams = 0,
            newQuantityGrams = 100,
            calories = 150,
            protein = 10f,
            carbs = 15f,
            fat = 4f
        )
        assertEquals(0, invalidOld.calories)

        val invalidNew = NutritionCalculator.rescaleExisting(
            oldQuantityGrams = 100,
            newQuantityGrams = -50,
            calories = 150,
            protein = 10f,
            carbs = 15f,
            fat = 4f
        )
        assertEquals(0, invalidNew.calories)
    }

    @Test
    fun calculateCaloriesBurned_scalesWithWeightAndSteps() {
        assertEquals(0, NutritionCalculator.calculateCaloriesBurned(0))
        assertEquals(0, NutritionCalculator.calculateCaloriesBurned(-100))
        assertEquals(400, NutritionCalculator.calculateCaloriesBurned(10_000, 70f))
        assertEquals(486, NutritionCalculator.calculateCaloriesBurned(10_000, 85f))
        assertEquals(314, NutritionCalculator.calculateCaloriesBurned(10_000, 55f))
        assertEquals(3000, NutritionCalculator.calculateCaloriesBurned(200_000, 100f)) // Cap test
    }
}
