package com.example.ui

import com.example.ui.screens.buildAlignedStreakCalendarCells
import com.example.ui.screens.daysBetween
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class DashboardLifecycleFixesTest {

    @Test
    fun daysBetween_handlesDstSpringForwardAndFallBackAccurately() {
        val originalTz = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Rome"))

            // 2026-03-29 is DST spring-forward (23-hour day) in Europe/Rome
            assertEquals(1, daysBetween("2026-03-29", "2026-03-30"))
            assertEquals(2, daysBetween("2026-03-28", "2026-03-30"))
            assertEquals(-2, daysBetween("2026-03-30", "2026-03-28"))

            // 2026-10-25 is DST fall-back (25-hour day) in Europe/Rome
            assertEquals(1, daysBetween("2026-10-25", "2026-10-26"))
            assertEquals(2, daysBetween("2026-10-24", "2026-10-26"))

            // Invalid date falls back to 0 safely
            assertEquals(0, daysBetween("not-a-date", "2026-10-26"))
        } finally {
            TimeZone.setDefault(originalTz)
        }
    }

    @Test
    fun buildAlignedStreakCalendarCells_alignsEveryDateToMondayThroughSundayColumns() {
        // Tuesday 2026-11-24 -> first day (27 days ago) is Wednesday 2026-10-28 (col index 2)
        val tuesdayCells = buildAlignedStreakCalendarCells("2026-11-24")
        assertEquals(0, tuesdayCells.size % 7)
        assertEquals(28, tuesdayCells.count { it != null })
        assertNull(tuesdayCells[0])
        assertNull(tuesdayCells[1])
        assertEquals("2026-10-28", tuesdayCells[2])
        tuesdayCells.forEachIndexed { index, date ->
            if (date != null) {
                val expectedCol = LocalDate.parse(date).dayOfWeek.value - 1
                assertEquals("Date $date should align with weekday column", expectedCol, index % 7)
            }
        }

        // Sunday 2026-11-22 -> first day (27 days ago) is Monday 2026-10-26 (col index 0), exact 4x7 grid
        val sundayCells = buildAlignedStreakCalendarCells("2026-11-22")
        assertEquals(28, sundayCells.size)
        assertTrue(sundayCells.none { it == null })
        assertEquals("2026-10-26", sundayCells.first())
        assertEquals("2026-11-22", sundayCells.last())
    }
}
