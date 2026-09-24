package com.example.widget

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
import com.example.util.MealTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class PetStatusWidgetProvider : AppWidgetProvider() {
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
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
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgetsAsync(context, appWidgetManager, appWidgetIds)
    }

    private fun updateWidgetsAsync(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = try { goAsync() } catch (_: Exception) { null }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(8000L) {
                    ids.forEach { updatePetWidget(context, manager, it) }
                }
            } catch (e: Exception) {
                android.util.Log.e("PetStatusWidget", "Error updating pet widgets", e)
            } finally {
                try {
                    pendingResult?.finish()
                } catch (_: Exception) {}
            }
        }
    }
}

private fun getMealTypeForCurrentTime(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..10 -> MealTypes.BREAKFAST
        in 11..14 -> MealTypes.LUNCH
        in 15..17 -> MealTypes.SNACK
        else -> MealTypes.DINNER
    }
}

private fun percentageOf(consumed: Int, target: Int): Int {
    if (target <= 0) return 0
    return ((consumed.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100)
}

internal suspend fun updatePetWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_pet_status)
    
    // Clicking the widget opens Home (Dashboard)
    val appIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val appPendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        appIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    views.setOnClickPendingIntent(R.id.widget_pet_container, appPendingIntent)

    try {
        val db = AppDatabase.getDatabase(context)
        val today = SessionManager.formatDate(Date())
        
        val meals = db.mealEntryDao().getMealsForDateOnce(today)
        val caloriesConsumed = meals.sumOf { it.calories }
        val proConsumed = meals.sumOf { it.protein.toDouble() }.toInt()
        val carbConsumed = meals.sumOf { it.carbs.toDouble() }.toInt()
        val fatConsumed = meals.sumOf { it.fat.toDouble() }.toInt()

        val app = context.applicationContext as? com.example.FoodLabApplication
        val target = app?.repository?.resolveTargetForDateOnce(today) ?: run {
            val calendar = java.util.Calendar.getInstance().apply {
                time = SessionManager.parseDateString(today)
            }
            val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            val dayString = mapOf(
                java.util.Calendar.MONDAY to "MONDAY",
                java.util.Calendar.TUESDAY to "TUESDAY",
                java.util.Calendar.WEDNESDAY to "WEDNESDAY",
                java.util.Calendar.THURSDAY to "THURSDAY",
                java.util.Calendar.FRIDAY to "FRIDAY",
                java.util.Calendar.SATURDAY to "SATURDAY",
                java.util.Calendar.SUNDAY to "SUNDAY"
            )[dayOfWeek] ?: "FIXED"
            db.dailyTargetDao().getTargetForDateOnce(today)
                ?: db.dailyTargetDao().getTargetForDateOnce(dayString)
                ?: db.dailyTargetDao().getFixedTargetOnce()
        }

        val targetCals = target?.targetCalories ?: com.example.data.DailyTarget.DEFAULT_TARGET_CALORIES
        val steps = com.example.util.StepCounterStore.savedSteps(context, today)
        val latestWeight = try {
            db.weightLogDao().getLatestWeightOnce()?.weightKg
        } catch (_: Exception) { null }
        val burned = com.example.util.NutritionCalculator.calculateCaloriesBurned(steps, latestWeight)
        val effectiveTarget = targetCals + burned
        val isOverTarget = caloriesConsumed > effectiveTarget

        views.setTextViewText(R.id.pet_hp_text, caloriesConsumed.toString())
        views.setTextViewText(R.id.pet_target_text, if (burned > 0) "su $targetCals (+$burned) kcal" else "su $targetCals kcal")
        views.setTextViewText(R.id.protein_text, "${proConsumed}g")
        views.setTextViewText(R.id.carbs_text, "${carbConsumed}g")
        views.setTextViewText(R.id.fat_text, "${fatConsumed}g")
        views.setContentDescription(R.id.protein_text, "Proteine: $proConsumed grammi")
        views.setContentDescription(R.id.carbs_text, "Carboidrati: $carbConsumed grammi")
        views.setContentDescription(R.id.fat_text, "Grassi: $fatConsumed grammi")
        if (isOverTarget) {
            views.setTextViewText(R.id.pet_status_text, "ECCESSO")
            views.setTextColor(R.id.pet_status_text, 0xFFFF453A.toInt())
        } else {
            views.setTextViewText(R.id.pet_status_text, "")
        }
        views.setImageViewResource(R.id.pet_image, R.drawable.foodlab_mascot_duo)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    } catch (e: Exception) {
        views.setTextViewText(R.id.pet_status_text, "APRI APP")
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
