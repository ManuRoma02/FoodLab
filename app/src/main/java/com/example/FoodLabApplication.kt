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

    val healthConnectManager: com.example.util.HealthConnectManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        com.example.util.HealthConnectManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationScheduler.createChannels(this)
            val notificationsEnabled = PreferencesManager.getInstance(this).notificationsEnabled.value
            NotificationScheduler.scheduleReminders(this, notificationsEnabled)
            com.example.util.WidgetUpdater.updateAllWidgets(this)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    repository.ensureDefaultFoodsSeeded()
                    CreaDatabaseImporter.importIfNeeded(this@FoodLabApplication, repository)
                } catch (e: Exception) {
                    android.util.Log.e("FoodLabApp", "Error during background seeding", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FoodLabApp", "Error during application onCreate initialization", e)
        }
    }
}
