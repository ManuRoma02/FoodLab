package com.example.util

object NutritionCalculator {
    data class NutritionResult(
        val calories: Int,
        val protein: Float,
        val carbs: Float,
        val fat: Float
    )

    fun fromPer100g(
        quantityGrams: Float,
        caloriesPer100g: Int,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float
    ): NutritionResult {
        if (!quantityGrams.isFinite() || quantityGrams <= 0f) {
            return NutritionResult(0, 0f, 0f, 0f)
        }
        val safeCal = caloriesPer100g.coerceAtLeast(0)
        val safePro = if (proteinPer100g.isFinite()) proteinPer100g.coerceAtLeast(0f) else 0f
        val safeCarbs = if (carbsPer100g.isFinite()) carbsPer100g.coerceAtLeast(0f) else 0f
        val safeFat = if (fatPer100g.isFinite()) fatPer100g.coerceAtLeast(0f) else 0f

        val ratio = quantityGrams / 100f
        return NutritionResult(
            calories = kotlin.math.round(safeCal * ratio).toInt(),
            protein = safePro * ratio,
            carbs = safeCarbs * ratio,
            fat = safeFat * ratio
        )
    }

    fun fromPer100g(
        quantityGrams: Int,
        caloriesPer100g: Int,
        proteinPer100g: Float,
        carbsPer100g: Float,
        fatPer100g: Float
    ): NutritionResult = fromPer100g(
        quantityGrams = quantityGrams.toFloat(),
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = proteinPer100g,
        carbsPer100g = carbsPer100g,
        fatPer100g = fatPer100g
    )

    fun rescaleExisting(
        oldQuantityGrams: Int,
        newQuantityGrams: Int,
        calories: Int,
        protein: Float,
        carbs: Float,
        fat: Float
    ): NutritionResult {
        if (oldQuantityGrams <= 0 || newQuantityGrams <= 0) {
            return NutritionResult(0, 0f, 0f, 0f)
        }
        val safeCal = calories.coerceAtLeast(0)
        val safePro = if (protein.isFinite()) protein.coerceAtLeast(0f) else 0f
        val safeCarbs = if (carbs.isFinite()) carbs.coerceAtLeast(0f) else 0f
        val safeFat = if (fat.isFinite()) fat.coerceAtLeast(0f) else 0f

        val ratio = newQuantityGrams.toFloat() / oldQuantityGrams.toFloat()
        return NutritionResult(
            calories = kotlin.math.round(safeCal * ratio).toInt(),
            protein = safePro * ratio,
            carbs = safeCarbs * ratio,
            fat = safeFat * ratio
        )
    }

    fun calculateCaloriesBurned(steps: Int, weightKg: Float? = null): Int {
        if (steps <= 0) return 0
        val safeWeight = if (weightKg != null && weightKg.isFinite() && weightKg in 20f..300f) weightKg else 70f
        val factor = 0.04f * (safeWeight / 70f)
        return kotlin.math.round(steps * factor).toInt().coerceIn(0, 3000)
    }
}
