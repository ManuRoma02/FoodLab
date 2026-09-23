package com.example.ui

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.example.util.PreferencesManager
import com.example.util.StepCounterStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class PedometerViewModel(application: Application) : AndroidViewModel(application), SensorEventListener, DefaultLifecycleObserver {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepCounterSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private val stepDetectorSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val appContext = application.applicationContext
    private val preferencesManager = PreferencesManager.getInstance(application)
    
    private val _steps = MutableStateFlow(StepCounterStore.savedSteps(appContext))
    val steps: StateFlow<Int> = _steps.asStateFlow()
    private var isTracking = false
    private var lastAccelMagnitude = 0f
    private var lastStepTimestamp = 0L
    private var lastWidgetUpdateTime = 0L
    private var currentTrackingDate = StepCounterStore.today()

    private val dateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            onDateChanged()
        }
    }
    
    fun getStepsForDateFlow(dateString: String): Flow<Int> {
        val today = StepCounterStore.today()
        return if (dateString == today) {
            steps
        } else {
            flow { emit(if (preferencesManager.syncSteps.value) StepCounterStore.savedSteps(appContext, dateString) else 0) }
        }
    }

    init {
        refresh()
        viewModelScope.launch {
            preferencesManager.syncSteps.collect { enabled ->
                if (enabled) {
                    refresh()
                    startTracking()
                } else {
                    stopTracking()
                    _steps.value = 0
                }
            }
        }

        // Ticker to automatically catch midnight transitions while app is running
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(10_000L)
                val today = StepCounterStore.today()
                if (today != currentTrackingDate) {
                    onDateChanged(today)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        try {
            appContext.registerReceiver(dateChangeReceiver, filter)
        } catch (_: Exception) {}
    }

    fun onDateChanged(newDate: String = StepCounterStore.today()) {
        currentTrackingDate = newDate
        StepCounterStore.resetForNewDay(appContext, newDate)
        _steps.value = if (preferencesManager.syncSteps.value) {
            StepCounterStore.savedSteps(appContext, newDate)
        } else {
            0
        }
        com.example.util.WidgetUpdater.updateAllWidgets(appContext)
    }

    fun refresh() {
        val today = StepCounterStore.today()
        currentTrackingDate = today
        _steps.value = if (preferencesManager.syncSteps.value) {
            StepCounterStore.savedSteps(appContext, today)
        } else {
            0
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        refresh()
        startTracking()
    }

    override fun onPause(owner: LifecycleOwner) {
        stopTracking()
    }

    fun startTracking() {
        if (!preferencesManager.syncSteps.value) return
        if (isTracking) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            _steps.value = StepCounterStore.savedSteps(appContext)
            return
        }

        isTracking = if (stepCounterSensor != null) {
            try {
                sensorManager?.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL) ?: false
            } catch (_: SecurityException) {
                false
            }
        } else if (stepDetectorSensor != null) {
            try {
                sensorManager?.registerListener(this, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL) ?: false
            } catch (_: SecurityException) {
                false
            }
        } else if (accelSensor != null) {
            try {
                sensorManager?.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_UI) ?: false
            } catch (_: SecurityException) {
                false
            }
        } else {
            false
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        sensorManager?.unregisterListener(this)
        isTracking = false
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (!preferencesManager.syncSteps.value) return
        event?.let {
            var updated = false
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = it.values[0].toInt()
                val newSteps = StepCounterStore.updateFromSensor(appContext, currentSteps)
                if (newSteps != _steps.value) {
                    _steps.value = newSteps
                    updated = true
                }
            } else if (it.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                val newSteps = StepCounterStore.incrementSteps(appContext, 1)
                _steps.value = newSteps
                updated = true
            } else if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                val rawMagnitude = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                
                // Exponential moving average to isolate gravity baseline
                val alpha = 0.8f
                if (lastAccelMagnitude == 0f) lastAccelMagnitude = 9.8f
                lastAccelMagnitude = alpha * lastAccelMagnitude + (1 - alpha) * rawMagnitude
                val userAccel = kotlin.math.abs(rawMagnitude - lastAccelMagnitude)

                val now = android.os.SystemClock.elapsedRealtime()
                val timeDelta = now - lastStepTimestamp
                if (userAccel > 2.6f && (timeDelta in 260L..2000L || lastStepTimestamp == 0L)) {
                    lastStepTimestamp = now
                    val newSteps = StepCounterStore.incrementSteps(appContext, 1)
                    _steps.value = newSteps
                    updated = true
                }
            }

            if (updated) {
                val now = android.os.SystemClock.elapsedRealtime()
                if (now - lastWidgetUpdateTime >= 10_000L) {
                    lastWidgetUpdateTime = now
                    com.example.util.WidgetUpdater.updateAllWidgets(appContext)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    override fun onCleared() {
        super.onCleared()
        stopTracking()
        try {
            appContext.unregisterReceiver(dateChangeReceiver)
        } catch (_: Exception) {}
    }
}
