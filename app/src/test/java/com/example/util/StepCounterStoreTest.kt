package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StepCounterStoreTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        StepCounterStore.clear(context)
    }

    @Test
    fun updateFromSensor_whenBaselineMissing_preservesPreSensorSteps() {
        val day = "2026-10-01"
        StepCounterStore.setSteps(context, 1200, day)
        StepCounterStore.incrementSteps(context, 50, day)

        val afterFirstTick = StepCounterStore.updateFromSensor(context, 8000, day)
        assertEquals(1250, afterFirstTick)

        val afterWalk = StepCounterStore.updateFromSensor(context, 8100, day)
        assertEquals(1350, afterWalk)
    }

    @Test
    fun setSteps_syncsBaselineWithLastRaw_preventsDoubleCounting() {
        val day = "2026-10-02"
        StepCounterStore.updateFromSensor(context, 10000, day)
        assertEquals(500, StepCounterStore.updateFromSensor(context, 10500, day))

        // Health Connect sync sets authoritative steps for today
        StepCounterStore.setSteps(context, 3000, day)

        // Next sensor tick adds only delta since lastRaw (10500 -> 10540 = +40)
        val afterSyncTick = StepCounterStore.updateFromSensor(context, 10540, day)
        assertEquals(3040, afterSyncTick)
    }

    @Test
    fun setSteps_withoutPriorSensorReading_clearsBaselineAndRetainsStepsOnFirstTick() {
        val yesterday = "2026-10-02"
        val today = "2026-10-03"
        StepCounterStore.updateFromSensor(context, 10000, yesterday)
        StepCounterStore.updateFromSensor(context, 12000, yesterday)

        StepCounterStore.setSteps(context, 2500, today)
        val afterTick = StepCounterStore.updateFromSensor(context, 12080, today)
        assertEquals(2580, afterTick)
    }

    @Test
    fun resetForNewDay_returnsEarlyWhenTodayAlreadyTrackedBySensor() {
        val day = "2026-10-04"
        StepCounterStore.updateFromSensor(context, 5000, day)
        assertEquals(300, StepCounterStore.updateFromSensor(context, 5300, day))

        // Delayed midnight sync runs after user already walked on `day`
        val result = StepCounterStore.resetForNewDay(context, day)
        assertEquals(300, result)
        assertEquals(300, StepCounterStore.savedSteps(context, day))
    }

    @Test
    fun resetForNewDay_returnsEarlyWhenDateAlreadyHasPositiveStepsWithoutSensor() {
        val day = "2026-10-05"
        StepCounterStore.setSteps(context, 420, day)

        val result = StepCounterStore.resetForNewDay(context, day)
        assertEquals(420, result)
        assertEquals(420, StepCounterStore.savedSteps(context, day))
    }

    @Test
    fun resetForNewDay_preparesZeroWhenNewDayHasNoActivity() {
        val day1 = "2026-10-06"
        val day2 = "2026-10-07"
        StepCounterStore.updateFromSensor(context, 9000, day1)
        StepCounterStore.updateFromSensor(context, 9500, day1)

        val resetResult = StepCounterStore.resetForNewDay(context, day2)
        assertEquals(0, resetResult)
        assertEquals(25, StepCounterStore.updateFromSensor(context, 9525, day2))
    }
}
