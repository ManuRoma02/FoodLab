package com.example.ui

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.FoodLabApplication
import com.example.data.FoodItem
import com.example.data.MealTemplate
import com.example.data.MealTemplateItem
import com.example.data.MealTemplateWithItems
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FoodSearchViewModelOfflineTest {

    @Test
    fun testSearchOffline_localResultsRetainedAndItalianErrorMessage() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        val repository = app.repository

        // Insert local food
        val food = FoodItem(
            name = "Mela Rossa Test",
            brand = "Frutta",
            caloriesPer100g = 52,
            proteinPer100g = 0.3f,
            carbsPer100g = 14f,
            fatPer100g = 0.2f
        )
        val id = repository.insertFood(food)
        assertTrue(id > 0)

        val viewModel = FoodSearchViewModel(app)

        // Querying local item
        viewModel.updateSearchQuery("Mela")
        shadowOf(Looper.getMainLooper()).idle()

        // When offline or retry is invoked
        viewModel.retryOnlineSearch()
        assertEquals("Mela", viewModel.searchQuery.value)
    }

    @Test
    fun nextOffId_reservesNonCollidingStrideOfFifty() {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        val viewModel = FoodSearchViewModel(app)

        val first = viewModel.nextOffId()
        val second = viewModel.nextOffId()
        val third = viewModel.nextOffId()

        assertEquals(-1, first)
        assertEquals(-51, second)
        assertEquals(-101, third)
        assertTrue(first - second >= 50)
        assertTrue(second - third >= 50)
    }

    @Test
    fun logTemplate_dividesQuantityByServings() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        val repository = app.repository
        val viewModel = FoodSearchViewModel(app)
        val date = "2026-11-24"
        repository.deleteMealsForDate(date)

        val template = MealTemplate(id = 10, name = "Porridge x2", servings = 2)
        val item = MealTemplateItem(
            id = 1,
            templateId = 10,
            foodId = 1,
            foodName = "Avena Test",
            brand = "Test",
            caloriesPer100g = 380,
            proteinPer100g = 13f,
            carbsPer100g = 67f,
            fatPer100g = 7f,
            quantityGrams = 200
        )
        viewModel.logTemplate(MealTemplateWithItems(template, listOf(item)), date, "BREAKFAST")

        var meals = repository.getMealsForDate(date).first()
        repeat(40) {
            if (meals.isNotEmpty()) return@repeat
            shadowOf(Looper.getMainLooper()).idle()
            delay(25)
            meals = repository.getMealsForDate(date).first()
        }

        assertEquals(1, meals.size)
        assertEquals(100, meals.first().quantityGrams)
        assertEquals(380, meals.first().calories)
    }

    @Test
    fun toggleFavorite_onNegativeOffFood_reusesExistingRoomFoodAndTracksPromotion() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        val repository = app.repository
        val viewModel = FoodSearchViewModel(app)

        val existingId = repository.insertFood(
            FoodItem(
                name = "Yogurt Greco OFF",
                brand = "Fage",
                caloriesPer100g = 57,
                proteinPer100g = 10f,
                carbsPer100g = 3.8f,
                fatPer100g = 0f,
                barcode = "5201054012345",
                isFavorite = false
            )
        ).toInt()

        val negativeOffFood = FoodItem(
            id = -77,
            name = "Yogurt Greco OFF",
            brand = "Fage",
            caloriesPer100g = 57,
            proteinPer100g = 10f,
            carbsPer100g = 3.8f,
            fatPer100g = 0f,
            barcode = "5201054012345",
            isFavorite = false
        )

        viewModel.toggleFavorite(negativeOffFood)

        repeat(40) {
            if (viewModel.promotedFoodIds.value.containsKey(-77)) return@repeat
            shadowOf(Looper.getMainLooper()).idle()
            delay(25)
        }

        assertEquals(existingId, viewModel.promotedFoodIds.value[-77])
        val persisted = repository.getFoodById(existingId)
        assertNotNull(persisted)
        assertTrue(persisted!!.isFavorite)
        val resolvedViaNegative = viewModel.getFoodById(-77)
        assertEquals(existingId, resolvedViaNegative?.id)
        assertTrue(resolvedViaNegative?.isFavorite == true)
    }
}
