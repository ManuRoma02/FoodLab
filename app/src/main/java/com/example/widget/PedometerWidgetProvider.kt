package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.util.PreferencesManager
import com.example.util.StepCounterStore
import kotlinx.coroutines.launch

class PedometerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updatePedometerWidget(context, appWidgetManager, appWidgetId, StepCounterStore.savedSteps(context))
        }
        triggerSensorUpdate(context)
        triggerHealthConnectSync(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "ACTION_REFRESH_STEPS" || intent.action == Intent.ACTION_DATE_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == "com.example.UPDATE_WIDGETS") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, javaClass))
            val saved = StepCounterStore.savedSteps(context)
            for (id in appWidgetIds) {
                updatePedometerWidget(context, appWidgetManager, id, saved)
            }
            triggerSensorUpdate(context)
            triggerHealthConnectSync(context)
        }
    }

    private fun triggerSensorUpdate(context: Context) {
        if (!PreferencesManager.getInstance(context).syncSteps.value) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) return
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return

        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        var isFinished = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (isFinished) return
                event?.let {
                    if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                        val currentSteps = it.values[0].toInt()
                        val dailySteps = StepCounterStore.updateFromSensor(context, currentSteps)
                        
                        val appWidgetManager = AppWidgetManager.getInstance(context)
                        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PedometerWidgetProvider::class.java))
                        
                        for (id in appWidgetIds) {
                            updatePedometerWidget(context, appWidgetManager, id, dailySteps)
                        }
                    }
                }
                cleanup()
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            fun cleanup() {
                if (!isFinished) {
                    isFinished = true
                    try {
                        sensorManager.unregisterListener(this)
                        pendingResult?.finish()
                    } catch (_: Exception) {}
                }
            }
        }

        handler.postDelayed({ listener.cleanup() }, 4000L)
        try {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (_: Exception) {
            listener.cleanup()
        }
    }

    private fun triggerHealthConnectSync(context: Context) {
        val app = context.applicationContext as? com.example.FoodLabApplication ?: return
        val hcManager = app.healthConnectManager
        if (hcManager.checkAvailability() != com.example.util.HealthConnectAvailability.AVAILABLE) return

        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                if (hcManager.checkPermissionsGranted()) {
                    val result = hcManager.syncDailyData()
                    result.onSuccess { data ->
                        val hcSteps = data.steps.toInt()
                        val currentSteps = StepCounterStore.savedSteps(context)
                        if (hcSteps > currentSteps) {
                            StepCounterStore.setSteps(context, hcSteps)
                            val appWidgetManager = AppWidgetManager.getInstance(context)
                            val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PedometerWidgetProvider::class.java))
                            for (id in appWidgetIds) {
                                updatePedometerWidget(context, appWidgetManager, id, hcSteps)
                            }
                            com.example.util.WidgetUpdater.updateAllWidgets(context)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("PedometerWidgetProvider", "Failed to sync HC from widget", e)
            } finally {
                try { pendingResult?.finish() } catch (_: Exception) {}
            }
        }
    }
}

internal fun updatePedometerWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, steps: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_pedometer)

    val launchIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingLaunch = PendingIntent.getActivity(
        context,
        appWidgetId,
        launchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_pedometer_container, pendingLaunch)

    val refreshIntent = Intent(context, PedometerWidgetProvider::class.java).apply {
        action = "ACTION_REFRESH_STEPS"
    }
    val pendingRefresh = PendingIntent.getBroadcast(
        context,
        appWidgetId, // use appWidgetId as requestCode
        refreshIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_refresh, pendingRefresh)
    
    if (steps >= 0) {
        val stepGoal = try {
            PreferencesManager.getInstance(context).stepGoal.value.coerceAtLeast(1000)
        } catch (_: Exception) {
            10000
        }
        views.setTextViewText(R.id.steps_text, steps.toString())
        views.setProgressBar(R.id.steps_progress, stepGoal, steps, false)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
