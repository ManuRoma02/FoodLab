package com.example.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    internal fun calculateInitialDelay(targetHour: Int, targetMinute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }

    fun createChannels(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager ?: return
            val mealChannel = android.app.NotificationChannel(
                "meal_reminders_channel",
                "Meal Reminders",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to log breakfast, lunch, and dinner"
            }
            val streakChannel = android.app.NotificationChannel(
                "streak_guard_channel",
                "Streak Protection",
                android.app.NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts to protect your logging streak"
            }
            notificationManager.createNotificationChannels(listOf(mealChannel, streakChannel))
        }
    }

    fun scheduleReminders(context: Context, enabled: Boolean = true) {
        try {
            val workManager = WorkManager.getInstance(context)

            // Always schedule Midnight Widget Sync (00:01 AM Daily) to refresh widgets for the new date
            val midnightDelay = calculateInitialDelay(0, 1)
            val midnightWork = PeriodicWorkRequestBuilder<MidnightWidgetSyncWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(midnightDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "midnight_widget_sync",
                ExistingPeriodicWorkPolicy.UPDATE,
                midnightWork
            )

            if (!enabled) {
                workManager.cancelUniqueWork("meal_reminder_breakfast")
                workManager.cancelUniqueWork("meal_reminder_lunch")
                workManager.cancelUniqueWork("meal_reminder_dinner")
                workManager.cancelUniqueWork("streak_guard_check")
                return
            }

            // Schedule Breakfast reminder (08:30 AM Daily)
            val breakfastDelay = calculateInitialDelay(8, 30)
            val breakfastWork = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(breakfastDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("MEAL_TYPE" to "BREAKFAST"))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "meal_reminder_breakfast",
                ExistingPeriodicWorkPolicy.UPDATE,
                breakfastWork
            )

            // Schedule Lunch reminder (13:00 PM Daily)
            val lunchDelay = calculateInitialDelay(13, 0)
            val lunchWork = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(lunchDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("MEAL_TYPE" to "LUNCH"))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "meal_reminder_lunch",
                ExistingPeriodicWorkPolicy.UPDATE,
                lunchWork
            )

            // Schedule Dinner reminder (20:00 PM Daily)
            val dinnerDelay = calculateInitialDelay(20, 0)
            val dinnerWork = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(dinnerDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("MEAL_TYPE" to "DINNER"))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "meal_reminder_dinner",
                ExistingPeriodicWorkPolicy.UPDATE,
                dinnerWork
            )

            // Schedule Streak Guard check (21:30 PM Daily)
            val streakGuardDelay = calculateInitialDelay(21, 30)
            val streakGuardWork = PeriodicWorkRequestBuilder<StreakGuardWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(streakGuardDelay, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                "streak_guard_check",
                ExistingPeriodicWorkPolicy.UPDATE,
                streakGuardWork
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
