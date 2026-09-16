package com.example.ui

import com.example.data.FoodItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchOptimizerTest {
    @Test
    fun likeWildcardsAreEscapedBeforeRoomQuery() {
        assertEquals("50\\% \\_mix\\\\test", SearchOptimizer.escapeForLike("  50% _mix\\test  "))
    }

    @Test
    fun exactLocalMatchRanksBeforePopularPartialMatch() {
        val exact = food(id = 1, name = "Apple")
        val popularPartial = food(id = 2, name = "Apple pie", useCount = 20)

        val ranked = SearchOptimizer.rank(listOf(popularPartial, exact), emptyList(), "apple")

        assertEquals(exact.id, ranked.first().id)
    }

    @Test
    fun duplicateApiAndLocalFoodIsShownOnce() {
        val local = food(id = 1, name = "Yogurt")
        val remote = local.copy(id = -1)

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "yogurt")

        assertEquals(1, ranked.size)
        assertTrue(ranked.single().id > 0)
    }

    @Test
    fun accentedItalianFoodMatchesNonAccentedQueryAndDeduplicates() {
        val local = food(id = 1, name = "Caffè espresso", caloriesPer100g = 2)
        val remote = food(id = -1, name = "Caffe espresso", caloriesPer100g = 2)

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "caffe")

        assertEquals(1, ranked.size)
        assertEquals(1, ranked.single().id)
    }

    @Test
    fun boundaryCalorieDifferenceIsDeduplicatedWithinTolerance() {
        val local = food(id = 1, name = "Latte intero", caloriesPer100g = 64)
        val remote = food(id = -1, name = "Latte intero", caloriesPer100g = 68) // 4 kcal difference

        val ranked = SearchOptimizer.rank(listOf(local), listOf(remote), "latte")

        assertEquals(1, ranked.size)
        assertEquals(1, ranked.single().id)
    }

    private fun food(id: Int, name: String, caloriesPer100g: Int = 100, useCount: Int = 0) = FoodItem(
        id = id,
        name = name,
        brand = "Test",
        caloriesPer100g = caloriesPer100g,
        proteinPer100g = 1f,
        carbsPer100g = 2f,
        fatPer100g = 3f,
        useCount = useCount
    )
}
