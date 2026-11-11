package com.example.ui

import com.example.data.FoodItem
import com.example.data.FoodItemWithPortions
import com.example.data.FoodPortion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserCreatedFoodsExportTest {

    @Test
    fun defaultFoodItemIsNotUserCreated() {
        val defaultFood = FoodItem(
            name = "Pasta di semola",
            brand = "Barilla",
            caloriesPer100g = 350,
            proteinPer100g = 12f,
            carbsPer100g = 72f,
            fatPer100g = 1.5f
        )
        assertFalse(defaultFood.isUserCreated)
    }

    @Test
    fun customCreatedFoodIsFlaggedAsUserCreated() {
        val customFood = FoodItem(
            name = "Pancake Avena e Cioccolato",
            brand = "Ricetta Mia",
            caloriesPer100g = 220,
            proteinPer100g = 14f,
            carbsPer100g = 30f,
            fatPer100g = 4f,
            barcode = "8012345678901",
            isUserCreated = true
        )
        assertTrue(customFood.isUserCreated)
    }

    @Test
    fun foodWithPortionsPreservesAttributesForExport() {
        val food = FoodItem(
            id = 1,
            name = "Barretta Proteica",
            brand = "Custom",
            caloriesPer100g = 380,
            proteinPer100g = 30f,
            carbsPer100g = 40f,
            fatPer100g = 8f,
            barcode = "8099999999999",
            isUserCreated = true
        )
        val portions = listOf(
            FoodPortion(id = 1, foodId = 1, name = "1 barretta (55g)", weightGrams = 55f)
        )
        val itemWithPortions = FoodItemWithPortions(food = food, portions = portions)

        assertEquals("Barretta Proteica", itemWithPortions.food.name)
        assertEquals("8099999999999", itemWithPortions.food.barcode)
        assertEquals(380, itemWithPortions.food.caloriesPer100g)
        assertEquals(30f, itemWithPortions.food.proteinPer100g)
        assertEquals(1, itemWithPortions.portions.size)
        assertEquals("1 barretta (55g)", itemWithPortions.portions.first().name)
        assertEquals(55f, itemWithPortions.portions.first().weightGrams)
        assertTrue(itemWithPortions.food.isUserCreated)
    }
}
