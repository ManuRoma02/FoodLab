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

class MealReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val mealType = inputData.getString("MEAL_TYPE") ?: "MEAL"
        sendNotification(mealType)
        try {
            WidgetUpdater.updateAllWidgets(applicationContext)
        } catch (_: Exception) {}
        return Result.success()
    }

    private fun sendNotification(mealType: String) {
        val context = applicationContext
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "FoodLab Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily meal logging reminders and streak preservation alerts"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (mealType.uppercase()) {
            "BREAKFAST" -> "Breakfast Time! 🍳"
            "LUNCH" -> "Lunch Reminder 🥗"
            "DINNER" -> "Dinner Time! 🍽️"
            else -> "FoodLab Reminder 🍏"
        }

        val text = "Don't forget to track your meal in FoodLab to keep your macros on target!"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(mealType.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "foodlab_reminders"
    }
}
