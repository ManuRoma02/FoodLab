package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenFoodFactsMapperTest {
    @Test
    fun malformedNutritionIsBoundedAndNeverProducesNonFiniteValues() {
        val product = OFFProduct(
            product_name = "  Test food  ",
            brands = null,
            serving_quantity = "NaN",
            serving_size = null,
            nutriments = OFFNutriments(
                energy_kcal_100g = "Infinity",
                proteins_100g = -3,
                carbohydrates_100g = 101,
                fat_100g = "12,5"
            )
        )

        val mapped = OpenFoodFactsMapper.map(product, id = -1)!!

        assertEquals("Test food", mapped.food.name)
        assertEquals("Open Food Facts", mapped.food.brand)
        assertEquals(0, mapped.food.caloriesPer100g)
        assertEquals(0f, mapped.food.proteinPer100g)
        assertEquals(0f, mapped.food.carbsPer100g)
        assertEquals(12.5f, mapped.food.fatPer100g)
        assertTrue(mapped.food.defaultServingSize > 0)
    }

    @Test
    fun servingParserConvertsUnitsAndKeepsHumanLabel() {
        val slices = OpenFoodFactsMapper.parseServingSize("2 slices (56 g)", 100f)!!
        val ounce = OpenFoodFactsMapper.parseServingSize("1 oz", 100f)!!

        assertEquals("Slices", slices.first)
        assertEquals(56f, slices.second)
        assertEquals(28.3495f, ounce.second, 0.001f)
    }

    @Test
    fun energyKjIsConvertedToKcalWhenKcalIsMissing() {
        val product = OFFProduct(
            product_name = "EU Product in kJ",
            brands = "Barilla",
            serving_quantity = 100,
            serving_size = "100g",
            nutriments = OFFNutriments(
                energy_kcal_100g = null,
                energy_kj_100g = 1464.4, // ~350 kcal
                energy_100g = null,
                proteins_100g = 12.0,
                carbohydrates_100g = 70.0,
                fat_100g = 2.0
            )
        )

        val mapped = OpenFoodFactsMapper.map(product, id = -1)!!
        assertEquals(350, mapped.food.caloriesPer100g)
        assertEquals(12f, mapped.food.proteinPer100g)
    }

    @Test
    fun blankNameWithoutFallbackIsRejected() {
        val product = OFFProduct("  ", null, null, null, null)
        assertNull(OpenFoodFactsMapper.map(product, id = -1))
    }
}
