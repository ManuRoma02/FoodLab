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
    fun stepCounterStoreMultiDayTransitionsAndEdgeCases() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        StepCounterStore.clear(context)

        // Day 1: Phone starts with 20,000 raw steps at start of Day 1
        val day1 = "2026-09-20"
        val d1StepsStart = StepCounterStore.updateFromSensor(context, 20000, day1)
        assertEquals(0, d1StepsStart)

        // User walks 5,000 steps on Day 1 (raw reaches 25,000)
        val d1StepsEnd = StepCounterStore.updateFromSensor(context, 25000, day1)
        assertEquals(5000, d1StepsEnd)
        assertEquals(5000, StepCounterStore.savedSteps(context, day1))

        // Day 2 (consecutive): Morning starts, user hasn't walked yet (raw is still 25,000)
        val day2 = "2026-09-21"
        val d2StepsStart = StepCounterStore.updateFromSensor(context, 25000, day2)
        assertEquals("New day must start with 0 steps before user walks", 0, d2StepsStart)
        assertEquals(0, StepCounterStore.savedSteps(context, day2))

        // User walks 3,000 steps on Day 2 (raw reaches 28,000)
        val d2StepsMid = StepCounterStore.updateFromSensor(context, 28000, day2)
        assertEquals(3000, d2StepsMid)
        assertEquals(3000, StepCounterStore.savedSteps(context, day2))

        // Day 1 steps must remain preserved at 5,000 in history!
        assertEquals(5000, StepCounterStore.savedSteps(context, day1))
    }

    @Test
    fun stepCounterStoreHandlesSkippedDaysWithoutOverflow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        StepCounterStore.clear(context)

        // Day 1: Monday
        val day1 = "2026-09-14"
        StepCounterStore.updateFromSensor(context, 10000, day1)
        StepCounterStore.updateFromSensor(context, 15000, day1) // 5,000 steps on Monday

        // User skips Tue, Wed, Thu, Fri (no app launches).
        // User walks around all week, phone raw sensor reaches 45,000.
        // Saturday: User opens app for the first time in 5 days.
        val daySaturday = "2026-09-19"
        val saturdayStepsStart = StepCounterStore.updateFromSensor(context, 45000, daySaturday)
        
        // Old steps from skipped days must NOT be dumped into Saturday! Saturday must start at 0.
        assertEquals("Skipped days must not overflow into the current day", 0, saturdayStepsStart)
        assertEquals(0, StepCounterStore.savedSteps(context, daySaturday))

        // User takes 200 steps on Saturday:
        val saturdayStepsAfterWalk = StepCounterStore.updateFromSensor(context, 45200, daySaturday)
        assertEquals(200, saturdayStepsAfterWalk)
    }

    @Test
    fun stepCounterStoreResetForNewDayPreparesZeroSteps() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        StepCounterStore.clear(context)

        val day1 = "2026-09-20"
        StepCounterStore.updateFromSensor(context, 30000, day1)
        StepCounterStore.updateFromSensor(context, 35000, day1) // 5,000 steps

        val day2 = "2026-09-21"
        // Midnight arrives, MidnightWidgetSyncWorker calls resetForNewDay
        StepCounterStore.resetForNewDay(context, day2)
        assertEquals(0, StepCounterStore.savedSteps(context, day2))

        // First sensor event on day 2 arrives (user walked 50 steps: 35,050)
        val d2Steps = StepCounterStore.updateFromSensor(context, 35050, day2)
        assertEquals(50, d2Steps)
    }

    @Test
    fun mainActivityBuildsAndLaunchesWithoutCrash() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        assertNotNull(activity)
    }
}
