package com.example.ui.screens

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.GeminiFoodScanResult
import com.example.ui.GeminiScanUiState
import com.example.util.MealTypes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiFoodScanSheetRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleResult = GeminiFoodScanResult(
        foodName = "Pasta al Pesto",
        estimatedGrams = 200,
        calories = 400,
        protein = 12.0f,
        carbs = 56.0f,
        fat = 14.0f,
        confidence = "HIGH",
        notes = "Pasta con pesto fresco e pinoli",
        detectedItems = listOf("Pasta", "Pesto", "Pinoli")
    )

    @Test
    fun testAnalyzingStateRendersProgressHint() {
        composeTestRule.setContent {
            GeminiFoodScanSheetContent(
                uiState = GeminiScanUiState.Analyzing("Analisi del piatto con Gemini AI..."),
                currentMealType = MealTypes.LUNCH,
                currentDate = "2026-10-08",
                onAdjustGrams = {},
                onConfirmMeal = { _, _, _, _ -> },
                onRetry = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Gemini Food Scanner").assertIsDisplayed()
        composeTestRule.onNodeWithText("Analisi del piatto con Gemini AI...").assertIsDisplayed()
    }

    @Test
    fun testSuccessStateRendersDishDetailsAndMacros() {
        var confirmed = false
        var confirmedGrams = 0

        composeTestRule.setContent {
            GeminiFoodScanSheetContent(
                uiState = GeminiScanUiState.Success(sampleResult, adjustedGrams = 200),
                currentMealType = MealTypes.LUNCH,
                currentDate = "2026-10-08",
                onAdjustGrams = {},
                onConfirmMeal = { _, grams, _, _ ->
                    confirmed = true
                    confirmedGrams = grams
                },
                onRetry = {},
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Pasta al Pesto").assertIsDisplayed()
        composeTestRule.onNodeWithText("400").assertIsDisplayed()
        composeTestRule.onNodeWithText("200 g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Proteine").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carboidrati").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grassi").assertIsDisplayed()

        // Confirm button
        composeTestRule.onNodeWithText("Aggiungi al pasto").performScrollTo().performClick()
        assertTrue("onConfirmMeal should be called", confirmed)
        assertEquals(200, confirmedGrams)
    }

    @Test
    fun testPortionAdjustmentCallsCallback() {
        var adjustedDelta = 0

        composeTestRule.setContent {
            GeminiFoodScanSheetContent(
                uiState = GeminiScanUiState.Success(sampleResult, adjustedGrams = 200),
                currentMealType = MealTypes.LUNCH,
                currentDate = "2026-10-08",
                onAdjustGrams = { newGrams ->
                    adjustedDelta = newGrams - 200
                },
                onConfirmMeal = { _, _, _, _ -> },
                onRetry = {},
                onDismiss = {}
            )
        }

        // Tap +50g
        composeTestRule.onNodeWithText("+50g").performScrollTo().performClick()
        assertEquals(50, adjustedDelta)
    }

    @Test
    fun testErrorStateRendersMessageAndRetry() {
        var retried = false
        var dismissed = false

        composeTestRule.setContent {
            GeminiFoodScanSheetContent(
                uiState = GeminiScanUiState.Error("Connessione assente", canRetry = true),
                currentMealType = MealTypes.DINNER,
                currentDate = "2026-10-08",
                onAdjustGrams = {},
                onConfirmMeal = { _, _, _, _ -> },
                onRetry = { retried = true },
                onDismiss = { dismissed = true }
            )
        }

        composeTestRule.onNodeWithText("Connessione assente").assertIsDisplayed()
        composeTestRule.onNodeWithText("Riprova").performClick()
        assertTrue(retried)

        composeTestRule.onNodeWithText("Annulla").performClick()
        assertTrue(dismissed)
    }
}
