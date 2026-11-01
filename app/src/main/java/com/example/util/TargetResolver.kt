package com.example.util

import com.example.data.DailyTarget
import com.example.ui.SessionManager
import java.util.Calendar

object TargetResolver {
    fun getWeekdayName(date: String): String {
        return try {
            val calendar = Calendar.getInstance().apply {
                time = SessionManager.parseDateString(date)
            }
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "MONDAY"
                Calendar.TUESDAY -> "TUESDAY"
                Calendar.WEDNESDAY -> "WEDNESDAY"
                Calendar.THURSDAY -> "THURSDAY"
                Calendar.FRIDAY -> "FRIDAY"
                Calendar.SATURDAY -> "SATURDAY"
                Calendar.SUNDAY -> "SUNDAY"
                else -> "FIXED"
            }
        } catch (_: Exception) {
            "FIXED"
        }
    }

    fun resolveTarget(
        date: String,
        targets: List<DailyTarget>,
        defaultTarget: DailyTarget = DailyTarget("FIXED")
    ): DailyTarget {
        val specific = targets.firstOrNull { it.dateString == date }
        if (specific != null) return specific

        val weekdayName = getWeekdayName(date)
        val weekdayTarget = targets.firstOrNull { it.dateString == weekdayName }
        if (weekdayTarget != null) return weekdayTarget

        return targets.firstOrNull { it.dateString == "FIXED" } ?: defaultTarget
    }

    fun resolveTargetCalories(
        date: String,
        targets: List<DailyTarget>,
        defaultCalories: Int = DailyTarget.DEFAULT_TARGET_CALORIES
    ): Int {
        val target = resolveTarget(date, targets)
        return if (target.targetCalories > 0) target.targetCalories else defaultCalories
    }
}
