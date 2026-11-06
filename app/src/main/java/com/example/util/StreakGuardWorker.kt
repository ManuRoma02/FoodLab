package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.ui.SessionManager
import java.util.Date

class StreakGuardWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        try {
            val db = AppDatabase.getDatabase(context)
            val today = SessionManager.formatDate(Date())
            val meals = db.mealEntryDao().getMealsForDateOnce(today)
            val caloriesConsumed = meals.sumOf { it.calories }
            val app = context.applicationContext as? com.example.FoodLabApplication
            val target = app?.repository?.resolveTargetForDateOnce(today) ?: run {
                val dayString = TargetResolver.getWeekdayName(today)
                db.dailyTargetDao().getTargetForDateOnce(today)
                    ?: db.dailyTargetDao().getTargetForDateOnce(dayString)
                    ?: db.dailyTargetDao().getFixedTargetOnce()
            }
            val targetCals = target?.targetCalories ?: com.example.data.DailyTarget.DEFAULT_TARGET_CALORIES

            if (meals.isEmpty() || caloriesConsumed < (targetCals * 0.5f)) {
                sendStreakWarningNotification(context, caloriesConsumed, targetCals)
            }
            WidgetUpdater.updateAllWidgets(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Result.success()
    }

    private fun sendStreakWarningNotification(context: Context, consumed: Int, target: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MealReminderWorker.CHANNEL_ID,
                "Promemoria pasti",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MealReminderWorker.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Aggiorna il diario di oggi")
            .setContentText("Hai registrato $consumed / $target kcal. Completa i pasti prima di mezzanotte.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(9999, notification)
    }
}
