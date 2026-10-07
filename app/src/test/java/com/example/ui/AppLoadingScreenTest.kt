package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsDisplayed
import com.example.ui.screens.AppLoadingScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppLoadingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAppLoadingScreenRendersAndCompletes() {
        var completed = false
        composeTestRule.setContent {
            AppLoadingScreen(
                onLoadingComplete = { completed = true }
            )
        }

        // Verify root container is present
        composeTestRule.onNodeWithTag("quantum_loading_root").assertIsDisplayed()

        // Advance clock to trigger animation completion
        composeTestRule.mainClock.advanceTimeBy(2500)
        assertTrue("onLoadingComplete should be invoked after animation finishes", completed)
    }
}
