package com.example.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.example.widget.DailyCaloriesWidgetProvider
import com.example.widget.PedometerWidgetProvider
import com.example.widget.PetStatusWidgetProvider

object WidgetUpdater {
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)

            // 1. Daily Calories Widget
            val dailyIds = appWidgetManager.getAppWidgetIds(ComponentName(context, DailyCaloriesWidgetProvider::class.java))
            if (dailyIds.isNotEmpty()) {
                val dailyIntent = Intent(context, DailyCaloriesWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, dailyIds)
                }
                context.sendBroadcast(dailyIntent)
            }

            // 2. Pet Status Widget
            val petIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PetStatusWidgetProvider::class.java))
            if (petIds.isNotEmpty()) {
                val petIntent = Intent(context, PetStatusWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, petIds)
                }
                context.sendBroadcast(petIntent)
            }

            // 3. Pedometer Widget
            val pedometerIds = appWidgetManager.getAppWidgetIds(ComponentName(context, PedometerWidgetProvider::class.java))
            if (pedometerIds.isNotEmpty()) {
                val pedometerIntent = Intent(context, PedometerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, pedometerIds)
                }
                context.sendBroadcast(pedometerIntent)
            }
        } catch (e: Exception) {
            android.util.Log.e("WidgetUpdater", "Failed to update widgets", e)
        }
    }
}
