package com.example.util

import android.content.Context
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StepCounterStore {
    private const val PREFS_NAME = "pedometer_prefs"
    private const val KEY_GLOBAL_LAST_RAW = "global_last_raw_steps"
    private const val KEY_GLOBAL_LAST_DATE = "global_last_raw_date"

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun savedSteps(context: Context, dateString: String = today()): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(totalKey(dateString), 0)
    }

    fun updateFromSensor(context: Context, rawSensorSteps: Int, dateString: String = today()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = baselineKey(dateString)
        val offsetKey = offsetKey(dateString)
        val totalKey = totalKey(dateString)
        val lastRawKey = lastRawKey(dateString)

        val savedTotal = prefs.getInt(totalKey, 0).coerceAtLeast(0)
        val savedBaseline = prefs.getInt(baselineKey, -1)
        val savedOffset = prefs.getInt(offsetKey, 0).coerceAtLeast(0)
        val lastRaw = prefs.getInt(lastRawKey, -1)
        val globalLastRaw = prefs.getInt(KEY_GLOBAL_LAST_RAW, -1)
        val globalLastDate = prefs.getString(KEY_GLOBAL_LAST_DATE, null)

        var baseline = savedBaseline
        var offset = savedOffset

        if (baseline == -1) {
            if (globalLastDate != null && globalLastDate != dateString && globalLastRaw >= 0 && rawSensorSteps >= globalLastRaw) {
                // Moving to a new day with no device reboot: use yesterday's last raw as baseline
                baseline = globalLastRaw
                offset = savedTotal
            } else if (globalLastRaw >= 0 && rawSensorSteps < globalLastRaw) {
                // Device rebooted between days
                baseline = 0
                offset = savedTotal
            } else {
                // First initialization
                baseline = rawSensorSteps
                offset = savedTotal
            }
        } else if (rawSensorSteps < baseline || (lastRaw >= 0 && rawSensorSteps < lastRaw)) {
            // Device rebooted on the current day
            offset = maxOf(savedTotal, savedOffset)
            baseline = rawSensorSteps
        }

        val totalSteps = (offset + (rawSensorSteps - baseline).coerceAtLeast(0)).coerceAtLeast(savedTotal)

        prefs.edit {
            putInt(baselineKey, baseline)
            putInt(offsetKey, offset)
            putInt(totalKey, totalSteps)
            putInt(lastRawKey, rawSensorSteps)
            putInt(KEY_GLOBAL_LAST_RAW, rawSensorSteps)
            putString(KEY_GLOBAL_LAST_DATE, dateString)
        }

        return totalSteps
    }

    fun incrementSteps(context: Context, delta: Int = 1, dateString: String = today()): Int {
        if (delta <= 0) return savedSteps(context, dateString)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalKey = totalKey(dateString)
        val offsetKey = offsetKey(dateString)
        val currentTotal = prefs.getInt(totalKey, 0).coerceAtLeast(0)
        val currentOffset = prefs.getInt(offsetKey, 0).coerceAtLeast(0)
        val newTotal = currentTotal + delta
        val newOffset = currentOffset + delta
        prefs.edit {
            putInt(totalKey, newTotal)
            putInt(offsetKey, newOffset)
        }
        return newTotal
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit { clear() }
    }

    private fun baselineKey(dateString: String) = "baseline_steps_$dateString"
    private fun offsetKey(dateString: String) = "offset_steps_$dateString"
    private fun totalKey(dateString: String) = "total_steps_$dateString"
    private fun lastRawKey(dateString: String) = "last_raw_steps_$dateString"
}
