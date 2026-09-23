package com.example.data

import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultFoodCatalogTest {

    @Test
    fun catalogContainsEssentialStapleFoods() {
        val names = DefaultFoodCatalog.items.map { it.food.name }
        assertTrue("Deve contenere pasta", names.any { it.contains("Pasta", ignoreCase = true) })
        assertTrue("Deve contenere riso", names.any { it.contains("Riso", ignoreCase = true) })
        assertTrue("Deve contenere pollo", names.any { it.contains("Pollo", ignoreCase = true) })
        assertTrue("Deve contenere uova", names.any { it.contains("Uovo", ignoreCase = true) })
        assertTrue("Deve contenere olio", names.any { it.contains("Olio", ignoreCase = true) })
        assertTrue("Deve contenere mela", names.any { it.contains("Mela", ignoreCase = true) })
        assertTrue("Il catalogo deve contenere almeno 15 alimenti", DefaultFoodCatalog.items.size >= 15)
    }

    @Test
    fun allItemsHaveValidNutritionalValuesAndPortions() {
        DefaultFoodCatalog.items.forEach { seed ->
            val food = seed.food
            assertTrue("Il cibo ${food.name} deve avere calorie positive", food.caloriesPer100g > 0)
            assertTrue("Il cibo ${food.name} deve avere proteine non negative", food.proteinPer100g >= 0f)
            assertTrue("Il cibo ${food.name} deve avere carbo non negativi", food.carbsPer100g >= 0f)
            assertTrue("Il cibo ${food.name} deve avere grassi non negativi", food.fatPer100g >= 0f)
            assertTrue("Il cibo ${food.name} deve avere porzione standard > 0", food.defaultServingSize > 0)

            seed.portions.forEach { (portionName, grams) ->
                assertTrue("Nome porzione non vuoto per ${food.name}", portionName.isNotBlank())
                assertTrue("Grammatura porzione positiva per ${food.name}", grams > 0f)
            }
        }
    }

    @Test
    fun allItemsHaveUniqueNames() {
        val names = DefaultFoodCatalog.items.map { it.food.name.lowercase().trim() }
        val uniqueNames = names.distinct()
        assertEquals("I nomi nel catalogo devono essere univoci", names.size, uniqueNames.size)
    }
}
