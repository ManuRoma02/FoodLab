package com.example.ui

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.Routes
import com.example.ui.screens.BottomNavBar
import com.example.ui.theme.CupertinoDarkColorScheme
import com.example.util.LocalSoundManager
import com.example.util.SoundManager
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BottomNavBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val soundManager: SoundManager by lazy { SoundManager(context) }

    @Test
    fun testBottomNavBar_rendersAllTabsAndHandlesNavigation() {
        var currentNavRoute by mutableStateOf(Routes.Dashboard)
        var navigatedRoute: String? = null

        composeTestRule.setContent {
            CompositionLocalProvider(LocalSoundManager provides soundManager) {
                MaterialTheme(colorScheme = CupertinoDarkColorScheme) {
                    BottomNavBar(
                        currentRoute = currentNavRoute,
                        onNavigateToRoute = { target ->
                            navigatedRoute = target
                            currentNavRoute = target
                        }
                    )
                }
            }
        }

        // Verify all four tabs are displayed
        composeTestRule.onNodeWithText("Diario").assertIsDisplayed()
        composeTestRule.onNodeWithText("Alimenti").assertIsDisplayed()
        composeTestRule.onNodeWithText("Progressi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Impostazioni").assertIsDisplayed()

        // Click Alimenti
        composeTestRule.onNodeWithText("Alimenti").performClick()
        assertEquals(Routes.FoodsDatabase, navigatedRoute)

        // Click Progressi
        composeTestRule.onNodeWithText("Progressi").performClick()
        assertEquals(Routes.Stats, navigatedRoute)

        // Click Impostazioni
        composeTestRule.onNodeWithText("Impostazioni").performClick()
        assertEquals(Routes.Settings, navigatedRoute)

        // Click Diario
        composeTestRule.onNodeWithText("Diario").performClick()
        assertEquals(Routes.Dashboard, navigatedRoute)
    }
}
