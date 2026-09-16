package com.example.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.FoodLabApplication
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.DailyTarget
import com.example.data.FoodItem
import com.example.data.MealEntry
import com.example.data.Repository
import com.example.util.NutritionCalculator
import com.example.util.PreferencesManager
import com.example.util.StepCounterStore
import com.example.util.StreakCalculator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppStabilityTest {

    @Test
    fun applicationInitializesSuccessfully() {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        assertNotNull(app)
        assertNotNull(app.database)
        assertNotNull(app.repository)
    }

    @Test
    fun databaseOperationsAndMigrationsWorkCleanly() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<FoodLabApplication>()
        val repository = app.repository

        // Insert initial test food
        val food = FoodItem(
            name = "Test Apple",
            brand = "Bio",
            caloriesPer100g = 52,
            proteinPer100g = 0.3f,
            carbsPer100g = 13.8f,
            fatPer100g = 0.2f
        )
        val id = repository.insertFood(food)
        assertTrue(id > 0)

        val retrieved = repository.getFoodById(id.toInt())
        assertNotNull(retrieved)
        assertEquals("Test Apple", retrieved?.name)

        // Insert a meal entry
        val today = SessionManager.formatDate(java.util.Date())
        val meal = MealEntry(
            dateString = today,
            mealType = "snack",
            foodId = id.toInt(),
            foodName = "Test Apple",
            quantityGrams = 100,
            calories = 52,
            protein = 0.3f,
            carbs = 13.8f,
            fat = 0.2f
        )
        repository.insertMeal(meal)

        val target = DailyTarget(dateString = "FIXED", targetCalories = 2000)
        repository.insertTarget(target)

        val resolved = repository.resolveTargetForDateOnce(today)
        assertNotNull(resolved)
        assertEquals(2000, resolved.targetCalories)
    }

    @Test
    fun streakCalculatorHandlesComplexDataSafely() {
        val today = "2026-08-21"
        val targets = listOf(DailyTarget(dateString = "FIXED", targetCalories = 2000))
        val meals = listOf(
            MealEntry(dateString = "2026-08-21", mealType = "lunch", foodId = 1, foodName = "Meal 1", quantityGrams = 100, calories = 2000, protein = 30f, carbs = 60f, fat = 20f),
            MealEntry(dateString = "2026-08-20", mealType = "dinner", foodId = 1, foodName = "Meal 2", quantityGrams = 100, calories = 1950, protein = 40f, carbs = 70f, fat = 25f),
            MealEntry(dateString = "2026-08-19", mealType = "breakfast", foodId = 1, foodName = "Meal 3", quantityGrams = 100, calories = 2050, protein = 25f, carbs = 50f, fat = 15f)
        )
        val streak = StreakCalculator.calculate(meals = meals, targets = targets, today = today)
        assertEquals(3, streak)
    }

    @Test
    fun stepCounterStoreHandlesMultiDaySensorEventsSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        StepCounterStore.clear(context)

        val today = StepCounterStore.today()
        val steps1 = StepCounterStore.updateFromSensor(context, 1000, today)
        assertEquals(0, steps1)

        val steps2 = StepCounterStore.updateFromSensor(context, 1500, today)
        assertEquals(500, steps2)

        val steps3 = StepCounterStore.incrementSteps(context, 100, today)
        assertEquals(600, steps3)
    }

    @Test
    fun mainActivityBuildsAndLaunchesWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
    }
}
