package com.example.util

import com.example.data.DailyTarget
import com.example.data.MealEntry
import com.example.ui.SessionManager
import java.util.Calendar

object StreakCalculator {
    private const val MAX_REASONABLE_STREAK_DAYS = 3650

    fun calculate(
        meals: List<MealEntry>,
        targets: List<DailyTarget> = emptyList(),
        today: String,
        todayBurnedCalories: Int = 0,
        historicalBurnedCalories: Map<String, Int> = emptyMap()
    ): Int {
        if (meals.isEmpty()) return 0

        val mealsByDate = meals
            .asSequence()
            .filter { it.dateString <= today }
            .groupBy(MealEntry::dateString)
            .mapValues { (_, entries) -> entries.sumOf(MealEntry::calories) }

        val grossTodayCalories = mealsByDate[today] ?: 0
        val netTodayCalories = (grossTodayCalories - todayBurnedCalories).coerceAtLeast(0)
        val todayTarget = resolveTargetCalories(today, targets)
        val todayMin = (todayTarget * 0.5f).toInt()
        val todayMax = (todayTarget * 1.1f).toInt()

        val yesterday = previousDay(today)
        val isTodayCompleted = grossTodayCalories >= todayMin && netTodayCalories <= todayMax

        // If today is completed within target (including burned calories), count streak starting from today.
        // If today is still in progress (or 0 logged), preserve ongoing streak starting from yesterday.
        var cursor = if (isTodayCompleted) today else yesterday
        var streak = 0

        while (streak < MAX_REASONABLE_STREAK_DAYS) {
            val grossCalories = mealsByDate[cursor] ?: 0
            if (grossCalories <= 0) break

            val burned = if (cursor == today) todayBurnedCalories else (historicalBurnedCalories[cursor] ?: 0)
            val netCalories = (grossCalories - burned).coerceAtLeast(0)

            val targetCals = resolveTargetCalories(cursor, targets)
            val minCals = (targetCals * 0.5f).toInt()
            val maxCals = (targetCals * 1.1f).toInt()

            if (grossCalories >= minCals && netCalories <= maxCals) {
                streak++
                cursor = previousDay(cursor)
            } else {
                break
            }
        }
        return streak
    }

    internal fun resolveTargetCalories(date: String, targets: List<DailyTarget>): Int {
        val specific = targets.firstOrNull { it.dateString == date }
        if (specific != null) return specific.targetCalories

        val weekdayName = getWeekdayName(date)
        val weekdayTarget = targets.firstOrNull { it.dateString == weekdayName }
        if (weekdayTarget != null) return weekdayTarget.targetCalories

        return targets.firstOrNull { it.dateString == "FIXED" }?.targetCalories ?: DailyTarget.DEFAULT_TARGET_CALORIES
    }

    private fun getWeekdayName(date: String): String {
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
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun previousDay(date: String): String {
        val calendar = Calendar.getInstance().apply {
            time = SessionManager.parseDateString(date)
            add(Calendar.DAY_OF_YEAR, -1)
        }
        return SessionManager.formatDate(calendar.time)
    }
}
