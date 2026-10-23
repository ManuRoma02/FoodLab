package com.example.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.MealEntry
import com.example.ui.screens.AppleCalorieMacroRings
import com.example.ui.screens.CompactWeightChip
import com.example.ui.screens.MealItemRow
import com.example.ui.screens.UnifiedHeroHeader
import com.example.ui.screens.UnifiedMealsCard
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.ApplePurple
import com.example.ui.theme.CupertinoDarkColorScheme
import com.example.util.LocalSoundManager
import com.example.util.MealTypes
import com.example.util.SoundManager
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DashboardAppleDesignTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val soundManager: SoundManager by lazy { SoundManager(context) }

    @Test
    fun testCompactWeightChip_rendersAndHandlesClicks() {
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                CompactWeightChip(
                    weightText = "74.5 KG",
                    onClick = { clicked = true },
                    modifier = Modifier.testTag("weight_chip")
                )
            }
        }

        composeTestRule.onNodeWithText("74.5 KG").assertIsDisplayed()
        composeTestRule.onNodeWithTag("weight_chip").performClick()
        assertTrue("CompactWeightChip must invoke onClick when tapped", clicked)
    }

    @Test
    fun testAppleCalorieMacroRings_rendersProperly() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                AppleCalorieMacroRings(
                    calorieProgress = 0.75f,
                    proteinProgress = 0.60f,
                    carbsProgress = 0.80f,
                    fatProgress = 0.40f,
                    calorieColor = AppleOrange,
                    proteinColor = AppleBlue,
                    carbsColor = AppleGreen,
                    fatColor = ApplePurple,
                    modifier = Modifier.testTag("rings_box")
                )
            }
        }

        composeTestRule.onNodeWithTag("rings_box").assertIsDisplayed()
    }

    @Test
    fun testMealItemRow_displaysNutritionalBadgesAndCalories() {
        var rowClicked = false
        var deleteClicked = false

        val testMeal = MealEntry(
            id = 101,
            dateString = "2026-09-24",
            mealType = MealTypes.BREAKFAST,
            foodId = 1,
            foodName = "Porridge d'Avena",
            calories = 360,
            protein = 22f,
            carbs = 54f,
            fat = 7f,
            quantityGrams = 120,
            caloriesPer100g = 300,
            proteinPer100g = 18f,
            carbsPer100g = 45f,
            fatPer100g = 6f
        )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                MealItemRow(
                    meal = testMeal,
                    accentColor = AppleGreen,
                    onClick = { rowClicked = true },
                    onDelete = { deleteClicked = true },
                    modifier = Modifier.testTag("meal_row_101")
                )
            }
        }

        // Check food name and quantity
        composeTestRule.onNodeWithText("Porridge d'Avena").assertIsDisplayed()
        composeTestRule.onNodeWithText(" (120g)").assertIsDisplayed()

        // Check calories
        composeTestRule.onNodeWithText("360").assertIsDisplayed()
        composeTestRule.onNodeWithText("KCAL").assertIsDisplayed()

        // Check macro badge values
        composeTestRule.onNodeWithText("22g").assertIsDisplayed()
        composeTestRule.onNodeWithText("54g").assertIsDisplayed()
        composeTestRule.onNodeWithText("7g").assertIsDisplayed()

        // Verify tap action
        composeTestRule.onNodeWithTag("meal_row_101").performClick()
        assertTrue("MealItemRow must handle clicks via applePressEffect", rowClicked)
    }

    @Test
    fun testUnifiedHeroHeader_rendersRemainingCaloriesAndAllSections() {
        var weightClicked = false
        var stepClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                    UnifiedHeroHeader(
                        remainingCalories = 1450,
                        targetCalories = 2200,
                        loggedCalories = 750,
                        burnedCalories = 150,
                        proteinConsumed = 65f,
                        proteinTarget = 150f,
                        carbsConsumed = 90f,
                        carbsTarget = 240f,
                        fatConsumed = 28f,
                        fatTarget = 70f,
                        steps = 5420,
                        stepsTarget = 10000,
                        weightText = "72.4 KG",
                        onWeightClick = { weightClicked = true },
                        onStepClick = { stepClicked = true }
                    )
                }
            }
        }

        // Hero Typography
        composeTestRule.onNodeWithText("1450").assertIsDisplayed()
        composeTestRule.onNodeWithText("RIMASTE").assertIsDisplayed()

        // 3 Macro Metrics
        composeTestRule.onNodeWithText("Proteine").assertIsDisplayed()
        composeTestRule.onNodeWithText("65g / 150g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carboidrati").assertIsDisplayed()
        composeTestRule.onNodeWithText("90g / 240g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grassi").assertIsDisplayed()
        composeTestRule.onNodeWithText("28g / 70g").assertIsDisplayed()

        // Weight chip
        composeTestRule.onNodeWithText("72.4 KG").assertIsDisplayed()

        // Steps section
        composeTestRule.onNodeWithText("PASSI GIORNALIERI").assertIsDisplayed()
        composeTestRule.onNodeWithText("5420 / 10000 passi").assertIsDisplayed()
    }

    @Test
    fun testUnifiedHeroHeader_overTargetState_showsOverBadgeAndRed() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                    UnifiedHeroHeader(
                        remainingCalories = -250,
                        targetCalories = 2000,
                        loggedCalories = 2300,
                        burnedCalories = 50,
                        proteinConsumed = 120f,
                        proteinTarget = 140f,
                        carbsConsumed = 300f,
                        carbsTarget = 220f,
                        fatConsumed = 85f,
                        fatTarget = 65f,
                        steps = 3000,
                        stepsTarget = 10000,
                        weightText = "73.0 KG",
                        onWeightClick = {},
                        onStepClick = {}
                    )
                }
            }
        }

        // Should display 250 (positive display of overage) and IN ECCESSO badge
        composeTestRule.onNodeWithText("250").assertIsDisplayed()
        composeTestRule.onNodeWithText("IN ECCESSO").assertIsDisplayed()
    }

    @Test
    fun testUnifiedMealsCard_rendersInsetGroupedCategories() {
        var addedMealType: String? = null

        val sampleBreakfast = MealEntry(
            id = 1,
            dateString = "2026-09-24",
            mealType = MealTypes.BREAKFAST,
            foodId = 2,
            foodName = "Caffè e Biscotti",
            calories = 180,
            protein = 4f,
            carbs = 30f,
            fat = 5f,
            quantityGrams = 50,
            caloriesPer100g = 360,
            proteinPer100g = 8f,
            carbsPer100g = 60f,
            fatPer100g = 10f
        )

        val mealsMap = mapOf(
            MealTypes.BREAKFAST to listOf(sampleBreakfast),
            MealTypes.LUNCH to emptyList(),
            MealTypes.DINNER to emptyList(),
            MealTypes.SNACK to emptyList()
        )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                Box(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    UnifiedMealsCard(
                        mealsByType = mealsMap,
                        favoriteFoods = emptyList(),
                        onAddMealClick = { type -> addedMealType = type },
                        onMealClick = {},
                        onQuickLog = null,
                        onDeleteMeal = null
                    )
                }
            }
        }

        // Title
        composeTestRule.onNodeWithText("PASTI DI OGGI").assertIsDisplayed()

        // Categories
        composeTestRule.onNodeWithText("Colazione").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pranzo").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Cena").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Snack").performScrollTo().assertIsDisplayed()

        // Logged item in breakfast
        composeTestRule.onNodeWithText("Caffè e Biscotti").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("180 KCAL").performScrollTo().assertIsDisplayed()
    }
}
