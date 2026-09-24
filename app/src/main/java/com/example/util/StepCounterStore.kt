package com.example.util

import android.content.Context
import androidx.core.content.edit
import com.example.ui.SessionManager
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
        val yesterday = SessionManager.getPreviousDay(dateString, 1)

        var baseline = savedBaseline
        var offset = savedOffset

        if (baseline == -1) {
            // New day initialization
            if (globalLastDate == yesterday && globalLastRaw in 0..rawSensorSteps) {
                // Smooth transition from yesterday to today with no reboot
                baseline = globalLastRaw
                offset = 0
            } else {
                // First initialization, device rebooted, or skipped days: start today's counter cleanly at 0
                baseline = rawSensorSteps
                offset = 0
            }
        } else if (rawSensorSteps < baseline || (lastRaw >= 0 && rawSensorSteps < lastRaw)) {
            // Device rebooted on the current day while app was tracking: retain steps counted before reboot
            offset = maxOf(savedTotal, savedOffset)
            baseline = rawSensorSteps
        }

        val totalSteps = (offset + (rawSensorSteps - baseline).coerceAtLeast(0)).coerceAtLeast(0)

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

    fun resetForNewDay(context: Context, dateString: String = today()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = baselineKey(dateString)
        val totalKey = totalKey(dateString)
        val offsetKey = offsetKey(dateString)
        val globalLastRaw = prefs.getInt(KEY_GLOBAL_LAST_RAW, -1)
        val globalLastDate = prefs.getString(KEY_GLOBAL_LAST_DATE, null)
        val yesterday = SessionManager.getPreviousDay(dateString, 1)

        val baseline = if (globalLastDate == yesterday && globalLastRaw >= 0) {
            globalLastRaw
        } else {
            -1 // Will be set to current rawSensorSteps on next sensor event
        }

        prefs.edit {
            if (baseline >= 0) {
                putInt(baselineKey, baseline)
            } else {
                remove(baselineKey)
            }
            putInt(totalKey, 0)
            putInt(offsetKey, 0)
        }
        return 0
    }

    fun resetStepsForDate(context: Context, dateString: String = today()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = baselineKey(dateString)
        val totalKey = totalKey(dateString)
        val offsetKey = offsetKey(dateString)
        val lastRawKey = lastRawKey(dateString)
        val lastRaw = prefs.getInt(lastRawKey, -1)
        val globalLastRaw = prefs.getInt(KEY_GLOBAL_LAST_RAW, -1)
        val currentRaw = if (lastRaw >= 0) lastRaw else globalLastRaw

        prefs.edit {
            if (currentRaw >= 0) {
                putInt(baselineKey, currentRaw)
            } else {
                remove(baselineKey)
            }
            putInt(totalKey, 0)
            putInt(offsetKey, 0)
        }
        return 0
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
