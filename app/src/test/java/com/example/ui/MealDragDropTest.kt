package com.example.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.data.MealEntry
import com.example.ui.screens.MealDragDropState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MealDragDropTest {

    private fun createSampleMeal(id: Int = 1, mealType: String = "breakfast"): MealEntry {
        return MealEntry(
            id = id,
            dateString = "2026-09-25",
            mealType = mealType,
            foodId = 100,
            foodName = "Avena e Miele",
            quantityGrams = 80,
            calories = 300,
            protein = 12f,
            carbs = 50f,
            fat = 5f
        )
    }

    @Test
    fun testMealEntryMealTypeMutation() {
        val original = createSampleMeal(id = 42, mealType = "breakfast")
        val moved = original.copy(mealType = "lunch")

        assertEquals(42, moved.id)
        assertEquals("lunch", moved.mealType)
        assertEquals(original.foodName, moved.foodName)
        assertEquals(original.calories, moved.calories)
        assertEquals(original.protein, moved.protein, 0.01f)
        assertEquals(original.carbs, moved.carbs, 0.01f)
        assertEquals(original.fat, moved.fat, 0.01f)
    }

    @Test
    fun testDragDropStateInitialValues() {
        val state = MealDragDropState()
        assertFalse(state.isDragging)
        assertNull(state.draggedMeal)
        assertNull(state.sourceMealType)
        assertNull(state.hoveredMealType)
        assertEquals(Offset.Zero, state.dragPosition)
    }

    @Test
    fun testDragDropHoverDetection() {
        val state = MealDragDropState()
        val meal = createSampleMeal(mealType = "breakfast")

        // Register two targets: breakfast (y: 100..300) and lunch (y: 350..550)
        state.registerMealBounds("breakfast", Rect(0f, 100f, 500f, 300f))
        state.registerMealBounds("lunch", Rect(0f, 350f, 500f, 550f))

        // Start drag on breakfast item
        state.onDragStart(
            meal = meal,
            sourceMealType = "breakfast",
            itemTopLeftInRoot = Offset(20f, 120f),
            itemWidthPx = 460f,
            itemHeightPx = 60f,
            touchPositionInRoot = Offset(50f, 150f)
        )

        assertTrue(state.isDragging)
        assertEquals(meal, state.draggedMeal)
        assertEquals("breakfast", state.sourceMealType)
        // Hovering over source meal should NOT be considered a target
        assertNull(state.hoveredMealType)

        // Drag down towards lunch (y: 400)
        state.onDrag(Offset(0f, 250f))
        assertEquals(Offset(50f, 400f), state.dragPosition)
        assertEquals("lunch", state.hoveredMealType)

        // Drop on lunch
        var droppedMeal: MealEntry? = null
        var targetType: String? = null
        state.onDragEnd { m, target ->
            droppedMeal = m
            targetType = target
        }

        assertEquals(meal, droppedMeal)
        assertEquals("lunch", targetType)
        // State should be reset
        assertFalse(state.isDragging)
        assertNull(state.draggedMeal)
        assertNull(state.hoveredMealType)
    }

    @Test
    fun testDragDropCancelOrDropOnSourceDoesNotTrigger() {
        val state = MealDragDropState()
        val meal = createSampleMeal(mealType = "breakfast")

        state.registerMealBounds("breakfast", Rect(0f, 100f, 500f, 300f))
        state.registerMealBounds("lunch", Rect(0f, 350f, 500f, 550f))

        state.onDragStart(
            meal = meal,
            sourceMealType = "breakfast",
            itemTopLeftInRoot = Offset(20f, 120f),
            itemWidthPx = 460f,
            itemHeightPx = 60f,
            touchPositionInRoot = Offset(50f, 150f)
        )

        var triggered = false
        // Drop while still in breakfast
        state.onDragEnd { _, _ -> triggered = true }
        assertFalse("Should not drop if over same meal", triggered)
        assertFalse(state.isDragging)

        // Test cancel
        state.onDragStart(
            meal = meal,
            sourceMealType = "breakfast",
            itemTopLeftInRoot = Offset(20f, 120f),
            itemWidthPx = 460f,
            itemHeightPx = 60f,
            touchPositionInRoot = Offset(50f, 150f)
        )
        state.onDrag(Offset(0f, 250f))
        assertEquals("lunch", state.hoveredMealType)
        state.onDragCancel()
        assertFalse(state.isDragging)
        assertNull(state.draggedMeal)
    }
}
