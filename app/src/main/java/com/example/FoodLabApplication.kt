package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.OpenFoodFactsApi
import com.example.data.Repository

import com.example.util.CreaDatabaseImporter
import com.example.util.NotificationScheduler
import com.example.util.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FoodLabApplication : Application() {
    val database: AppDatabase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AppDatabase.getDatabase(this)
    }

    val repository: Repository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Repository(
            database.foodDao(),
            database.mealEntryDao(),
            database.dailyTargetDao(),
            database.weightLogDao(),
            database.mealTemplateDao(),
            database.foodPortionDao(),
            database.shoppingItemDao(),
            openFoodFactsApi
        )
    }

    val openFoodFactsApi: OpenFoodFactsApi by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OpenFoodFactsApi.create(cacheDir)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            app.rive.runtime.kotlin.core.Rive.init(this)
        } catch (t: Throwable) {
            android.util.Log.w("FoodLabApp", "Rive initialization skipped or failed: ${t.message}")
        }
        try {
            val notificationsEnabled = PreferencesManager.getInstance(this).notificationsEnabled.value
            NotificationScheduler.scheduleReminders(this, notificationsEnabled)
            com.example.util.WidgetUpdater.updateAllWidgets(this)
            CoroutineScope(Dispatchers.IO).launch {
                CreaDatabaseImporter.importIfNeeded(this@FoodLabApplication, repository)
            }
        } catch (e: Exception) {
            android.util.Log.e("FoodLabApp", "Error during application onCreate initialization", e)
        }
    }
}
