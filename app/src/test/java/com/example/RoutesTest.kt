package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {

    @Test
    fun testCreateSearchRoute() {
        assertEquals("search/BREAKFAST", Routes.createSearchRoute("BREAKFAST"))
        assertEquals("search/LUNCH", Routes.createSearchRoute("LUNCH"))
    }

    @Test
    fun testCreateFoodDetailRoute() {
        assertEquals("food_detail/42/DINNER", Routes.createFoodDetailRoute(42, "DINNER"))
    }

    @Test
    fun testCreateFoodsDatabaseDetailRoute() {
        assertEquals("foods_database_detail/99", Routes.createFoodsDatabaseDetailRoute(99))
    }

    @Test
    fun testCreateCreateFoodRoute() {
        assertEquals("create_food", Routes.createCreateFoodRoute("", ""))
        assertEquals("create_food?barcode=8001234567890", Routes.createCreateFoodRoute("8001234567890", ""))
        assertEquals("create_food?initialName=Pollo+Arrosto", Routes.createCreateFoodRoute("", "Pollo Arrosto"))
        assertEquals("create_food?barcode=8001234567890&initialName=Pollo+Arrosto", Routes.createCreateFoodRoute("8001234567890", "Pollo Arrosto"))
    }
}
