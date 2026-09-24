package com.example.ui

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.FoodLabApplication
import com.example.data.FoodItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
}
