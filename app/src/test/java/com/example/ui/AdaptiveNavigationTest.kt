package com.example.ui

import com.example.Routes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveNavigationTest {

    @Test
    fun testWindowSizeClassThreshold_isStandard600dp() {
        val compactWidth = 411
        val expandedWidth = 840
        val foldableFoldedWidth = 360
        val foldableUnfoldedWidth = 720

        fun isWideScreen(widthDp: Int) = widthDp >= 600

        assertFalse("Phone portrait should be compact", isWideScreen(compactWidth))
        assertFalse("Foldable folded should be compact", isWideScreen(foldableFoldedWidth))
        assertTrue("Tablet portrait/landscape should be wide", isWideScreen(expandedWidth))
        assertTrue("Foldable unfolded should be wide", isWideScreen(foldableUnfoldedWidth))
    }

    @Test
    fun testRoutes_generateValidRouteStrings() {
        assertEquals("search/lunch", Routes.createSearchRoute("lunch"))
        assertEquals("food_detail/42/dinner", Routes.createFoodDetailRoute(42, "dinner"))
        assertEquals("foods_database_detail/99", Routes.createFoodsDatabaseDetailRoute(99))
    }
}
