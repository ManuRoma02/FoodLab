package com.example.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.ui.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class DailyCaloriesWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == ACTION_MIDNIGHT_REFRESH) {
            com.example.util.StepCounterStore.resetForNewDay(context)
            com.example.util.WidgetUpdater.updateAllWidgets(context)
            scheduleMidnightRefresh(context)
            return
        }
        if (action == Intent.ACTION_DATE_CHANGED || 
            action == Intent.ACTION_TIMEZONE_CHANGED || 
            action == Intent.ACTION_TIME_CHANGED || 
            action == "com.example.UPDATE_WIDGETS"
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                ?: appWidgetManager.getAppWidgetIds(android.content.ComponentName(context, javaClass))
            if (appWidgetIds.isNotEmpty()) {
                updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
            }
            scheduleMidnightRefresh(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
        scheduleMidnightRefresh(context)
    }

    private fun updateWidgetsAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(8000L) {
                    ids.forEach { updateWidget(context, manager, it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("DailyCaloriesWidget", "Error updating widgets", e)
            } finally {
                try {
                    pendingResult?.finish()
                } catch (_: Exception) {}
            }
        }
    }

    companion object {
        internal const val ACTION_MIDNIGHT_REFRESH = "com.example.MIDNIGHT_WIDGET_REFRESH"
    }
}

internal fun scheduleMidnightRefresh(context: Context) {
    try {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextMidnight = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 1)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val intent = Intent(context, DailyCaloriesWidgetProvider::class.java).apply {
            action = DailyCaloriesWidgetProvider.ACTION_MIDNIGHT_REFRESH
        }
        val pending = PendingIntent.getBroadcast(
            context,
            9901,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextMidnight.timeInMillis, pending)
    } catch (_: Exception) {}
}

private fun dailyPercentageOf(consumed: Int, target: Int): Int {
    if (target <= 0) return 0
    return ((consumed.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100)
}

internal suspend fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_daily_calories)
    
    // Clicking the widget container opens Home (Dashboard)
    val appIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val appPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_container, appPendingIntent)

    val searchIntent = Intent(context, MainActivity::class.java).apply {
        action = "WIDGET_ADD_FOOD"
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val searchPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId + 1000, 
        searchIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.btn_add_food, searchPendingIntent)

    try {
        val db = AppDatabase.getDatabase(context)
        val today = SessionManager.formatDate(Date())
        val caloriesConsumed = db.mealEntryDao().getCaloriesForDate(today)
        val app = context.applicationContext as? com.example.FoodLabApplication
        val target = app?.repository?.resolveTargetForDateOnce(today) ?: run {
            val dayString = com.example.util.TargetResolver.getWeekdayName(today)
            db.dailyTargetDao().getTargetForDateOnce(today)
                ?: db.dailyTargetDao().getTargetForDateOnce(dayString)
                ?: db.dailyTargetDao().getFixedTargetOnce()
        }
        val targetCalories = target?.targetCalories ?: com.example.data.DailyTarget.DEFAULT_TARGET_CALORIES
        val steps = com.example.util.StepCounterStore.savedSteps(context, today)
        val latestWeight = try {
            db.weightLogDao().getLatestWeightOnce()?.weightKg
        } catch (_: Exception) { null }
        val burned = com.example.util.NutritionCalculator.calculateCaloriesBurned(steps, latestWeight)
        val effectiveTarget = targetCalories + burned

        views.setTextViewText(R.id.calories_text, caloriesConsumed.toString())
        views.setTextViewText(R.id.daily_target_text, if (burned > 0) "su $targetCalories (+$burned) kcal" else "su $targetCalories kcal")
        val remaining = effectiveTarget - caloriesConsumed
        views.setContentDescription(
            R.id.calories_text,
            "Calorie consumate: $caloriesConsumed kcal. Calorie rimanenti: ${if (remaining > 0) remaining else 0} kcal."
        )
        appWidgetManager.updateAppWidget(appWidgetId, views)
    } catch (e: Exception) {
        views.setTextViewText(R.id.calories_text, "—")
        views.setTextViewText(R.id.daily_target_text, "Apri FoodLab")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
