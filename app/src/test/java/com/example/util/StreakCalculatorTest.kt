package com.example.util

import com.example.data.DailyTarget
import com.example.data.MealEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {
    private val fixedTarget = DailyTarget(dateString = "FIXED", targetCalories = 2_000)

    @Test
    fun countsConsecutiveLoggingDays() {
        val meals = listOf(
            meal("2026-07-23", 2_000),
            meal("2026-07-22", 1_800),
            meal("2026-07-21", 2_200),
            meal("2026-07-20", 1_900),
            meal("2026-07-19", 2_000)
        )

        assertEquals(5, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23"))
    }

    @Test
    fun incompleteTodayPreservesTheStreakThroughYesterday() {
        val meals = listOf(
            meal("2026-07-22", 2_000),
            meal("2026-07-21", 2_000),
            meal("2026-07-20", 2_000)
        )

        assertEquals(3, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23"))
    }

    @Test
    fun overTargetTodayWithoutBurnedCaloriesPreservesOngoingStreakThroughYesterday() {
        val meals = listOf(
            meal("2026-07-23", 2_500), // > 2200 max without steps
            meal("2026-07-22", 2_000),
            meal("2026-07-21", 2_000)
        )

        // Mid-day over target does not wipe the historical streak (preserves 2 days through yesterday)
        assertEquals(2, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23", todayBurnedCalories = 0))

        // If 500 kcal were burned from steps, net calories = 2000 (within target) -> streak becomes 3
        assertEquals(3, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23", todayBurnedCalories = 500))
    }

    @Test
    fun pastDayOutsideTargetBreaksTheStreak() {
        val meals = listOf(
            meal("2026-07-23", 2_000),
            meal("2026-07-22", 500), // Far below 1800 min
            meal("2026-07-21", 2_000)
        )

        assertEquals(1, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23"))
    }

    @Test
    fun aMissingCalendarDayBreaksTheStreak() {
        val meals = listOf(
            meal("2026-07-23", 2_000),
            meal("2026-07-21", 2_000)
        )

        assertEquals(1, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23"))
    }

    @Test
    fun resolvesSpecificThenWeekdayThenFixedTargets() {
        val targets = listOf(
            fixedTarget,
            DailyTarget(dateString = "THURSDAY", targetCalories = 2_200),
            DailyTarget(dateString = "2026-07-22", targetCalories = 1_800)
        )
        val meals = listOf(
            meal("2026-07-23", 1_980),
            meal("2026-07-22", 1_800),
            meal("2026-07-21", 2_000)
        )

        assertEquals(3, StreakCalculator.calculate(meals, targets, "2026-07-23"))
    }

    @Test
    fun pastDayCompensatedByBurnedCaloriesPreservesStreak() {
        val meals = listOf(
            meal("2026-07-23", 2_000),
            meal("2026-07-22", 2_500), // gross 2500 > 2200 max, but burned 500 -> net 2000
            meal("2026-07-21", 2_000)
        )
        val historicalBurned = mapOf("2026-07-22" to 500)

        assertEquals(3, StreakCalculator.calculate(
            meals = meals,
            targets = listOf(fixedTarget),
            today = "2026-07-23",
            todayBurnedCalories = 0,
            historicalBurnedCalories = historicalBurned
        ))
    }

    @Test
    fun futureMealsDoNotAffectTheCurrentStreak() {
        val meals = listOf(
            meal("2026-07-24", 2_500),
            meal("2026-07-23", 2_000),
            meal("2026-07-22", 2_000)
        )

        assertEquals(2, StreakCalculator.calculate(meals, listOf(fixedTarget), "2026-07-23"))
    }

    @Test
    fun netZeroCaloriesFromStepsPreservesStreakWhenMealsLogged() {
        val meals = listOf(
            meal("2026-07-23", 2_000),
            meal("2026-07-22", 2_000),
            meal("2026-07-21", 2_000)
        )
        // 2000 gross calories on 2026-07-22 and 2000 burned calories -> net 0 kcal
        val historicalBurned = mapOf("2026-07-22" to 2_000)

        assertEquals(3, StreakCalculator.calculate(
            meals = meals,
            targets = listOf(fixedTarget),
            today = "2026-07-23",
            todayBurnedCalories = 2_000,
            historicalBurnedCalories = historicalBurned
        ))
    }

    private fun meal(date: String, calories: Int) = MealEntry(
        dateString = date,
        mealType = MealTypes.LUNCH,
        foodId = 1,
        foodName = "Test meal",
        quantityGrams = 100,
        calories = calories,
        protein = 0f,
        carbs = 0f,
        fat = 0f
    )
}
