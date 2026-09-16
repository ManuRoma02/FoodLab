package com.example.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SessionManager {
    // CRITICAL: Use Locale.US for database keys so they don't break if the user changes device language
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun parseDateString(dateStr: String): Date {
        return synchronized(dateFormat) {
            try {
                dateFormat.parse(dateStr) ?: Date()
            } catch (_: Exception) {
                Date()
            }
        }
    }

    fun formatDate(date: Date): String {
        return synchronized(dateFormat) {
            dateFormat.format(date)
        }
    }

    fun isToday(dateStr: String): Boolean {
        return dateStr == formatDate(Date())
    }

    fun getPreviousDay(dateStr: String, daysAgo: Int = 1): String {
        val date = parseDateString(dateStr)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return formatDate(calendar.time)
    }

    fun getNextDay(dateStr: String): String {
        val date = parseDateString(dateStr)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        return formatDate(calendar.time)
    }

    /**
     * Converts calendar keys into dynamic friendly names (e.g., TODAY, YESTERDAY, MONDAY, TUESDAY)
     * fallback to nicely formatted string for older dates.
     */
    fun getFriendlyDateLabel(dateStr: String): String {
        try {
            val targetDate = parseDateString(dateStr)
            val calTarget = Calendar.getInstance().apply { time = targetDate }
            val calToday = Calendar.getInstance().apply { time = Date() }
            
            val clearTime = { cal: Calendar ->
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }
            clearTime(calTarget)
            clearTime(calToday)
            
            val diffMs = calTarget.timeInMillis - calToday.timeInMillis
            val diffDays = kotlin.math.round(diffMs.toDouble() / (1000.0 * 60.0 * 60.0 * 24.0)).toLong()
            
            return when {
                diffDays == 0L -> "TODAY"
                diffDays == -1L -> "YESTERDAY"
                diffDays == 1L -> "TOMORROW"
                diffDays in -7..-2 -> {
                    SimpleDateFormat("EEEE", Locale.US).format(targetDate).uppercase()
                }
                else -> {
                    SimpleDateFormat("EEEE, MMM d", Locale.US).format(targetDate).uppercase()
                }
            }
        } catch (e: Exception) {
            return dateStr
        }
    }
}
