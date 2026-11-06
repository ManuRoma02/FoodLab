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
import com.example.ui.screens.UnifiedMealsCard
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
    fun testUnifiedHeroHeader_displaysRemainingCaloriesAndMacroMetrics() {
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
        composeTestRule.onNodeWithText("73.0 KG").assertIsDisplayed()

        // 3 Macro Metrics
        composeTestRule.onNodeWithText("Proteine").assertIsDisplayed()
        composeTestRule.onNodeWithText("50g / 140g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carboidrati").assertIsDisplayed()
        composeTestRule.onNodeWithText("70g / 200g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grassi").assertIsDisplayed()
        composeTestRule.onNodeWithText("20g / 65g").assertIsDisplayed()

        // Steps
        composeTestRule.onNodeWithText("PASSI GIORNALIERI").assertIsDisplayed()
        composeTestRule.onNodeWithText("6200 / 10000 passi").assertIsDisplayed()
    }

    @Test
    fun testUnifiedHeroHeader_bentoCraftTheme() {
        var weightClicked = false
        var stepClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                com.example.ui.theme.MyApplicationTheme(themeSkin = com.example.util.ThemeSkin.BENTO_CRAFT) {
                    UnifiedHeroHeader(
                        remainingCalories = 1450,
                        targetCalories = 2200,
                        loggedCalories = 750,
                        burnedCalories = 300,
                        proteinConsumed = 60f,
                        proteinTarget = 150f,
                        carbsConsumed = 90f,
                        carbsTarget = 220f,
                        fatConsumed = 25f,
                        fatTarget = 70f,
                        steps = 8500,
                        stepsTarget = 10000,
                        weightText = "71.5 KG",
                        onWeightClick = { weightClicked = true },
                        onStepClick = { stepClicked = true }
                    )
                }
            }
        }

        // Primary Display
        composeTestRule.onNodeWithText("1450").assertIsDisplayed()
        composeTestRule.onNodeWithText("RIMASTE").assertIsDisplayed()
        composeTestRule.onNodeWithText("71.5 KG").assertIsDisplayed()

        // 3 Macro Metrics in Bento Cells
        composeTestRule.onNodeWithText("Proteine").assertIsDisplayed()
        composeTestRule.onNodeWithText("60g / 150g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carboidrati").assertIsDisplayed()
        composeTestRule.onNodeWithText("90g / 220g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grassi").assertIsDisplayed()
        composeTestRule.onNodeWithText("25g / 70g").assertIsDisplayed()

        // Steps Compact Chip in Bento Craft
        composeTestRule.onNodeWithText("8500").assertIsDisplayed().performClick()
        org.junit.Assert.assertTrue(stepClicked)
    }

    @Test
    fun testUnifiedMealsCard_bentoCraftAlternatingCards() {
        val sampleMeals = mapOf(
            com.example.util.MealTypes.BREAKFAST to listOf(
                com.example.data.MealEntry(
                    id = 1,
                    foodId = 1,
                    foodName = "Avena & Frutti di Bosco",
                    quantityGrams = 80,
                    calories = 290,
                    protein = 10f,
                    carbs = 48f,
                    fat = 5f,
                    mealType = com.example.util.MealTypes.BREAKFAST,
                    dateString = "2026-11-04"
                )
            ),
            com.example.util.MealTypes.LUNCH to listOf(
                com.example.data.MealEntry(
                    id = 2,
                    foodId = 2,
                    foodName = "Pasta Integrale al Tonno",
                    quantityGrams = 120,
                    calories = 450,
                    protein = 32f,
                    carbs = 62f,
                    fat = 7f,
                    mealType = com.example.util.MealTypes.LUNCH,
                    dateString = "2026-11-04"
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                com.example.ui.theme.MyApplicationTheme(themeSkin = com.example.util.ThemeSkin.BENTO_CRAFT) {
                    UnifiedMealsCard(
                        mealsByType = sampleMeals,
                        favoriteFoods = emptyList(),
                        onAddMealClick = {},
                        onMealClick = {}
                    )
                }
            }
        }

        // Section Titles and Bento Badges
        composeTestRule.onNodeWithText("PASTI DI OGGI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Colazione").assertIsDisplayed()
        composeTestRule.onNodeWithText("SMALL BATCH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pranzo").assertIsDisplayed()
        composeTestRule.onNodeWithText("100% REAL").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cena").assertIsDisplayed()
        composeTestRule.onNodeWithText("HEART-MADE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snack").assertExists()
        composeTestRule.onNodeWithText("HANDCRAFTED").assertExists()

        // Meal Item Names and Grams inside cards
        composeTestRule.onNodeWithText("Avena & Frutti di Bosco").assertExists()
        composeTestRule.onNodeWithText("Pasta Integrale al Tonno").assertExists()
    }

    @Test
    fun testUnifiedHeroHeader_nothingOsTheme() {
        var weightClicked = false
        var stepClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                com.example.ui.theme.MyApplicationTheme(themeSkin = com.example.util.ThemeSkin.NOTHING_OS) {
                    UnifiedHeroHeader(
                        remainingCalories = 1750,
                        targetCalories = 2400,
                        loggedCalories = 650,
                        burnedCalories = 200,
                        proteinConsumed = 75f,
                        proteinTarget = 160f,
                        carbsConsumed = 110f,
                        carbsTarget = 240f,
                        fatConsumed = 30f,
                        fatTarget = 70f,
                        steps = 9200,
                        stepsTarget = 10000,
                        weightText = "72.0 KG",
                        onWeightClick = { weightClicked = true },
                        onStepClick = { stepClicked = true }
                    )
                }
            }
        }

        // Primary Display
        composeTestRule.onNodeWithText("1750").assertIsDisplayed()
        composeTestRule.onNodeWithText("RIMASTE").assertIsDisplayed()
        composeTestRule.onNodeWithText("72.0 KG").assertIsDisplayed()

        // 3 Macro Metrics in Nothing Pillars
        composeTestRule.onNodeWithText("Proteine").assertIsDisplayed()
        composeTestRule.onNodeWithText("75g / 160g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Carboidrati").assertIsDisplayed()
        composeTestRule.onNodeWithText("110g / 240g").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grassi").assertIsDisplayed()
        composeTestRule.onNodeWithText("30g / 70g").assertIsDisplayed()

        // Telemetry
        composeTestRule.onNodeWithText("SYS // CALORIE.MATRIX").assertIsDisplayed()
        composeTestRule.onNodeWithText("9200").assertIsDisplayed().performClick()
        org.junit.Assert.assertTrue(stepClicked)
    }

    @Test
    fun testUnifiedMealsCard_nothingOsTheme() {
        val sampleMeals = mapOf(
            com.example.util.MealTypes.BREAKFAST to listOf(
                com.example.data.MealEntry(
                    id = 1,
                    foodId = 1,
                    foodName = "Avena & Frutti di Bosco",
                    quantityGrams = 80,
                    calories = 290,
                    protein = 10f,
                    carbs = 48f,
                    fat = 5f,
                    mealType = com.example.util.MealTypes.BREAKFAST,
                    dateString = "2026-11-04"
                )
            )
        )

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                com.example.ui.theme.MyApplicationTheme(themeSkin = com.example.util.ThemeSkin.NOTHING_OS) {
                    UnifiedMealsCard(
                        mealsByType = sampleMeals,
                        favoriteFoods = emptyList(),
                        onAddMealClick = {},
                        onMealClick = {}
                    )
                }
            }
        }

        // Section Title & Categories
        composeTestRule.onNodeWithText("PASTI DI OGGI").assertIsDisplayed()
        composeTestRule.onNodeWithText("Colazione").assertIsDisplayed()
        composeTestRule.onNodeWithText("01 // ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pranzo").assertIsDisplayed()
        composeTestRule.onNodeWithText("02 // ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cena").assertIsDisplayed()
        composeTestRule.onNodeWithText("03 // ").assertIsDisplayed()
        composeTestRule.onNodeWithText("Snack").assertExists()
        composeTestRule.onNodeWithText("04 // ").assertExists()

        // Meal Item Names and Grams inside cards
        composeTestRule.onNodeWithText("Avena & Frutti di Bosco").assertExists()
    }

    @Test
    fun testUnifiedHeroHeader_neoBrutalistTheme() {
        var weightClicked = false
        var stepClicked = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                com.example.ui.theme.MyApplicationTheme(themeSkin = com.example.util.ThemeSkin.NEO_BRUTALISM) {
                    UnifiedHeroHeader(
                        remainingCalories = 1850,
                        targetCalories = 2300,
                        loggedCalories = 450,
                        burnedCalories = 200,
                        proteinConsumed = 75f,
                        proteinTarget = 160f,
                        carbsConsumed = 110f,
                        carbsTarget = 250f,
                        fatConsumed = 30f,
                        fatTarget = 75f,
                        steps = 7400,
                        stepsTarget = 10000,
                        weightText = "72.0 KG",
                        onWeightClick = { weightClicked = true },
                        onStepClick = { stepClicked = true }
                    )
                }
            }
        }

        // Neo-Brutalist elements
        composeTestRule.onNodeWithText("⚡ TARGET DEL GIORNO").assertIsDisplayed()
        composeTestRule.onNodeWithText("1850").assertIsDisplayed()
        composeTestRule.onNodeWithText("KCAL RIMASTE").assertIsDisplayed()
        composeTestRule.onNodeWithText("PROTEINE").assertIsDisplayed()
        composeTestRule.onNodeWithText("75g").assertIsDisplayed()
        composeTestRule.onNodeWithText("CARBOIDRATI").assertIsDisplayed()
        composeTestRule.onNodeWithText("110g").assertIsDisplayed()
        composeTestRule.onNodeWithText("GRASSI").assertIsDisplayed()
        composeTestRule.onNodeWithText("30g").assertIsDisplayed()
    }
}
