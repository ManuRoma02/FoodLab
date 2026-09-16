package com.example.ui

import com.example.ui.screens.MascotState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MascotComponentsTest {

    @Test
    fun testMascotStates_areDistinctAndInstantiable() {
        val states = listOf(
            MascotState.Idle,
            MascotState.OverTarget,
            MascotState.Typing,
            MascotState.Studying,
            MascotState.Cooking,
            MascotState.Party,
            MascotState.Wave
        )

        assertEquals(7, states.distinct().size)
        states.forEach { state ->
            assertNotNull(state)
        }
    }
}