package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeightLogTest {

    @Test
    fun weightLogCreation_defaultValues() {
        val entry = WeightLog(
            dateString = "2026-07-23",
            weightKg = 75.5f
        )

        assertEquals("2026-07-23", entry.dateString)
        assertEquals(75.5f, entry.weightKg, 0.001f)
        assertTrue(entry.timestamp > 0)
    }

    @Test
    fun weightLogSorting_chronologicalOrder() {
        val entry1 = WeightLog(id = 1, dateString = "2026-07-20", weightKg = 76.0f, timestamp = 1000L)
        val entry2 = WeightLog(id = 2, dateString = "2026-07-21", weightKg = 75.8f, timestamp = 2000L)
        val entry3 = WeightLog(id = 3, dateString = "2026-07-22", weightKg = 75.2f, timestamp = 3000L)

        val list = listOf(entry2, entry3, entry1)
        val sorted = list.sortedWith(compareBy({ it.dateString }, { it.timestamp }))

        assertEquals(listOf(entry1, entry2, entry3), sorted)
    }
}
