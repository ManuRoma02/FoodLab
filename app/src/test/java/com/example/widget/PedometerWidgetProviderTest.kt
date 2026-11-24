package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.example.R
import com.example.util.PreferencesManager
import com.example.util.StepCounterStore
import com.example.util.WidgetUpdater
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PedometerWidgetProviderTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun widgetPedometerLayout_inflatesExpectedViews() {
        val root = LayoutInflater.from(context).inflate(R.layout.widget_pedometer, null)
        assertNotNull(root.findViewById(R.id.widget_pedometer_container))
        assertNotNull(root.findViewById(R.id.widget_refresh))
        assertNotNull(root.findViewById<TextView>(R.id.steps_text))
        assertNotNull(root.findViewById<TextView>(R.id.steps_goal_text))
        assertNotNull(root.findViewById<ProgressBar>(R.id.steps_progress))
    }

    @Test
    fun updatePedometerWidget_bindsSuccessfully() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val shadow = shadowOf(appWidgetManager)
        val widgetId = shadow.createWidget(PedometerWidgetProvider::class.java, R.layout.widget_pedometer)
        StepCounterStore.setSteps(context, 5420)
        PreferencesManager.getInstance(context).setStepGoal(8000)

        // Verify updatePedometerWidget does not throw and binds values
        updatePedometerWidget(context, appWidgetManager, widgetId, 5420)
        assertEquals(5420, StepCounterStore.savedSteps(context))
        val boundView = shadow.getViewFor(widgetId)
        assertNotNull(boundView)
        assertEquals("5420", boundView.findViewById<TextView>(R.id.steps_text).text.toString())
        assertEquals("Obiettivo: 8.000", boundView.findViewById<TextView>(R.id.steps_goal_text).text.toString())
    }

    @Test
    fun sharedPendingResult_finishesOnceOnlyAfterAllRegisteredTasksSettle() {
        var finishCalls = 0
        val coordinator = SharedPendingResult(pendingResult = null) { finishCalls++ }

        val setupHold = coordinator.register()
        val sensorDone = coordinator.register()
        val hcDone = coordinator.register()
        setupHold()

        assertEquals(0, finishCalls)
        sensorDone()
        assertEquals(0, finishCalls)
        // Duplicate call must be a no-op
        sensorDone()
        assertEquals(0, finishCalls)

        hcDone()
        assertEquals(1, finishCalls)
        // Extra call after completion must not double-finish
        hcDone()
        assertEquals(1, finishCalls)
    }

    @Test
    fun sharedPendingResult_finishesImmediatelyWhenNoAsyncTasksRegister() {
        var finished = false
        val coordinator = SharedPendingResult(pendingResult = null) { finished = true }
        val setupHold = coordinator.register()
        assertFalse(finished)
        setupHold()
        assertTrue(finished)
    }

    @Test
    fun widgetUpdater_executesWithoutCrashing() {
        // Safe to call even with 0 widgets active
        WidgetUpdater.updateAllWidgets(context)
    }

    @Test
    fun onReceive_handlesActionRefreshSteps() {
        val provider = PedometerWidgetProvider()
        val intent = Intent("ACTION_REFRESH_STEPS")
        provider.onReceive(context, intent)
    }
}
