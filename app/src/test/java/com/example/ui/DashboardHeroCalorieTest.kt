package com.example.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.UnifiedHeroHeader
import com.example.ui.theme.CupertinoDarkColorScheme
import com.example.util.LocalSoundManager
import com.example.util.SoundManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardHeroCalorieTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val soundManager: SoundManager by lazy { SoundManager(context) }

    @Test
    fun testUnifiedHeroHeader_displaysTotalBudgetWithStepsBurnedBreakdown() {
        var weightClicked = false
        var stepClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                    UnifiedHeroHeader(
                        remainingCalories = 1600,
                        targetCalories = 2000,
                        loggedCalories = 650,
                        burnedCalories = 250,
                        proteinConsumed = 50f,
                        proteinTarget = 140f,
                        carbsConsumed = 70f,
                        carbsTarget = 200f,
                        fatConsumed = 20f,
                        fatTarget = 65f,
                        steps = 6200,
                        stepsTarget = 10000,
                        weightText = "73.0 KG",
                        onWeightClick = { weightClicked = true },
                        onStepClick = { stepClicked = true }
                    )
                }
            }
        }

        // Primary Display
        composeTestRule.onNodeWithText("1600").assertIsDisplayed()
        composeTestRule.onNodeWithText("RIMASTE").assertIsDisplayed()

        // Total Budget Breakdown Card (2000 base + 250 passi = 2250 kcal budget)
        composeTestRule.onNodeWithText("Budget: 2250 kcal (2000 base + 250 passi)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Assunte: 650 kcal").assertIsDisplayed()
    }
}
