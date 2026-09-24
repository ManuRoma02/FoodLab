package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.foundation.AppleSprings
import com.example.ui.foundation.appleSpring
import com.example.ui.screens.applePressEffect
import com.example.ui.theme.AppleBlue
import com.example.ui.theme.AppleCardShape
import com.example.ui.theme.AppleCornerRadiusCard
import com.example.ui.theme.AppleCornerRadiusModal
import com.example.ui.theme.AppleCornerRadiusPill
import com.example.ui.theme.AppleFontFamily
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleModalShape
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.ApplePillShape
import com.example.ui.theme.ApplePurple
import com.example.ui.theme.AppleRed
import com.example.ui.theme.Background
import com.example.ui.theme.CupertinoDarkColorScheme
import com.example.ui.theme.SurfaceContainer
import com.example.ui.theme.Typography
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppleDesignFoundationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testAppleSpringCurves() {
        val springSpec = appleSpring<Float>()
        assertEquals(0.75f, springSpec.dampingRatio)
        assertEquals(300f, springSpec.stiffness)

        assertEquals(0.75f, AppleSprings.DefaultFloat.dampingRatio)
        assertEquals(300f, AppleSprings.DefaultFloat.stiffness)
        assertEquals(0.85f, AppleSprings.SnappyFloat.dampingRatio)
        assertEquals(500f, AppleSprings.SnappyFloat.stiffness)
        assertEquals(1.0f, AppleSprings.CriticallyDampedFloat.dampingRatio)
    }

    @Test
    fun testAppleTypographyHierarchyAndTracking() {
        assertEquals(FontFamily.Default, AppleFontFamily)

        // Display Large: negative tracking for large display text per Apple HIG Section 15
        assertTrue(Typography.displayLarge.letterSpacing < 0.sp)
        assertEquals(34.sp, Typography.displayLarge.fontSize)

        // Title Large: negative tracking
        assertTrue(Typography.titleLarge.letterSpacing < 0.sp)
        assertEquals(20.sp, Typography.titleLarge.fontSize)

        // Body Large: comfortable reading scale
        assertEquals(17.sp, Typography.bodyLarge.fontSize)
        assertEquals(0.sp, Typography.bodyLarge.letterSpacing)

        // Label Medium / Small: positive tracking for small text
        assertTrue(Typography.labelMedium.letterSpacing > 0.sp)
        assertTrue(Typography.labelSmall.letterSpacing > 0.sp)
    }

    @Test
    fun testCupertinoTokensAndColors() {
        // Pure dark OLED background
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), Background)

        // Inset Grouped container surface
        assertEquals(androidx.compose.ui.graphics.Color(0xFF1C1C1E), SurfaceContainer)

        // Apple accents
        assertEquals(androidx.compose.ui.graphics.Color(0xFF0A84FF), AppleBlue)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF30D158), AppleGreen)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF9F0A), AppleOrange)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFFF453A), AppleRed)
        assertEquals(androidx.compose.ui.graphics.Color(0xFFBF5AF2), ApplePurple)

        // CupertinoDarkColorScheme mapping
        assertNotNull(CupertinoDarkColorScheme)
        assertEquals(AppleBlue, CupertinoDarkColorScheme.primary)
        assertEquals(SurfaceContainer, CupertinoDarkColorScheme.surfaceContainer)
    }

    @Test
    fun testAppleCornerRadiiAndShapes() {
        assertEquals(24.dp, AppleCornerRadiusCard)
        assertEquals(28.dp, AppleCornerRadiusModal)
        assertEquals(32.dp, AppleCornerRadiusPill)

        assertNotNull(AppleCardShape)
        assertNotNull(AppleModalShape)
        assertNotNull(ApplePillShape)
    }

    @Test
    fun testApplePressEffect_standaloneOnClick_isInvoked() {
        var clicked = false
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .applePressEffect(
                        pressedScale = 0.96f,
                        onClick = { clicked = true }
                    )
                    .testTag("standalone_apple_btn")
            ) {
                Text("Standalone")
            }
        }

        composeTestRule.onNodeWithTag("standalone_apple_btn").performClick()
        assertTrue("Standalone applePressEffect must invoke onClick on release", clicked)
    }

    @Test
    fun testApplePressEffect_passiveMode_doesNotBlockChildClickable() {
        var childClicked = false
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .applePressEffect(pressedScale = 0.96f) // passive mode: onClick is null
                    .clickable { childClicked = true }
                    .testTag("passive_clickable_box")
            ) {
                Text("Child Clickable")
            }
        }

        composeTestRule.onNodeWithTag("passive_clickable_box").performClick()
        assertTrue("applePressEffect with onClick=null must NOT consume pointer events and allow child clickable to receive click", childClicked)
    }

    @Test
    fun testApplePressEffect_passiveMode_doesNotBlockIconButtonClick() {
        var iconButtonClicked = false
        composeTestRule.setContent {
            IconButton(
                onClick = { iconButtonClicked = true },
                modifier = Modifier
                    .applePressEffect(pressedScale = 0.85f) // passive mode on IconButton
                    .testTag("passive_icon_button")
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Favorite")
            }
        }

        composeTestRule.onNodeWithTag("passive_icon_button").performClick()
        assertTrue("applePressEffect with onClick=null must allow wrapped IconButton to handle click", iconButtonClicked)
    }
}
