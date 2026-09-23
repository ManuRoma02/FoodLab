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
            if (globalLastDate == yesterday && globalLastRaw >= 0) {
                if (rawSensorSteps >= globalLastRaw) {
                    // Normal transition from yesterday to today with no reboot
                    baseline = globalLastRaw
                    offset = savedTotal
                } else {
                    // Device rebooted between yesterday's last event and today's first event
                    val startOfDayMillis = try {
                        val cal = java.util.Calendar.getInstance().apply {
                            time = SessionManager.parseDateString(dateString)
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }
                        cal.timeInMillis
                    } catch (_: Exception) { 0L }
                    val bootTimeMillis = System.currentTimeMillis() - android.os.SystemClock.elapsedRealtime()

                    if (startOfDayMillis > 0L && bootTimeMillis >= startOfDayMillis) {
                        // Reboot occurred today: sensor steps since reboot belong to today
                        baseline = 0
                        offset = savedTotal
                    } else {
                        // Reboot occurred before today: old steps belong to prior days, so start today from 0
                        baseline = rawSensorSteps
                        offset = savedTotal
                    }
                }
            } else {
                // First initialization, or skipped 1+ days: start today's counter cleanly from current sensor reading
                baseline = rawSensorSteps
                offset = savedTotal
            }
        } else if (rawSensorSteps < baseline || (lastRaw >= 0 && rawSensorSteps < lastRaw)) {
            // Device rebooted on the current day while app was tracking
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

    fun resetForNewDay(context: Context, dateString: String = today()): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = baselineKey(dateString)
        val totalKey = totalKey(dateString)
        val offsetKey = offsetKey(dateString)
        val globalLastRaw = prefs.getInt(KEY_GLOBAL_LAST_RAW, -1)
        val globalLastDate = prefs.getString(KEY_GLOBAL_LAST_DATE, null)
        val yesterday = SessionManager.getPreviousDay(dateString, 1)

        val savedBaseline = prefs.getInt(baselineKey, -1)
        if (savedBaseline == -1) {
            val baseline = if (globalLastDate == yesterday && globalLastRaw >= 0) {
                globalLastRaw
            } else {
                -1
            }
            prefs.edit {
                if (baseline >= 0) {
                    putInt(baselineKey, baseline)
                }
                putInt(totalKey, 0)
                putInt(offsetKey, 0)
            }
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
