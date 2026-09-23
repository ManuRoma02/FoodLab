package com.example.util

import org.junit.Assert.assertEquals
import org.junit.Test

class MealTypesTest {

    @Test
    fun testMealTypeInferenceFromHour() {
        // Breakfast: 4..10
        assertEquals(MealTypes.BREAKFAST, MealTypes.inferMealTypeFromHour(4))
        assertEquals(MealTypes.BREAKFAST, MealTypes.inferMealTypeFromHour(7))
        assertEquals(MealTypes.BREAKFAST, MealTypes.inferMealTypeFromHour(10))

        // Lunch: 11..14
        assertEquals(MealTypes.LUNCH, MealTypes.inferMealTypeFromHour(11))
        assertEquals(MealTypes.LUNCH, MealTypes.inferMealTypeFromHour(13))
        assertEquals(MealTypes.LUNCH, MealTypes.inferMealTypeFromHour(14))

        // Afternoon Snack: 15..17
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(15))
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(16))
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(17))

        // Dinner: 18..22
        assertEquals(MealTypes.DINNER, MealTypes.inferMealTypeFromHour(18))
        assertEquals(MealTypes.DINNER, MealTypes.inferMealTypeFromHour(20))
        assertEquals(MealTypes.DINNER, MealTypes.inferMealTypeFromHour(22))

        // Late Night / Early Morning Snack: 23, 0, 1, 2, 3
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(23))
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(0))
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(2))
        assertEquals(MealTypes.SNACK, MealTypes.inferMealTypeFromHour(3))
    }

    @Test
    fun testCanonicalizeAndDisplayName() {
        assertEquals(MealTypes.BREAKFAST, MealTypes.canonicalize("colazione"))
        assertEquals(MealTypes.LUNCH, MealTypes.canonicalize("pranzo"))
        assertEquals(MealTypes.DINNER, MealTypes.canonicalize("cena"))
        assertEquals(MealTypes.SNACK, MealTypes.canonicalize("spuntino"))

        assertEquals("Colazione", MealTypes.displayName(MealTypes.BREAKFAST))
        assertEquals("Pranzo", MealTypes.displayName(MealTypes.LUNCH))
        assertEquals("Cena", MealTypes.displayName(MealTypes.DINNER))
        assertEquals("Spuntino", MealTypes.displayName(MealTypes.SNACK))
    }
}
