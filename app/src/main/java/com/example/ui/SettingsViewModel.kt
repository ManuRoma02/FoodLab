package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.edit
import com.example.FoodLabApplication
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.util.PreferencesManager
import com.example.util.NotificationScheduler
import com.example.util.StepCounterStore

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as FoodLabApplication).repository
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val allTargets: StateFlow<List<DailyTarget>> = repository.getTargetsForDates(
        listOf("FIXED", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateDailyTarget(dateString: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            repository.insertTarget(DailyTarget(dateString, calories, protein, carbs, fat))
            com.example.util.WidgetUpdater.updateAllWidgets(getApplication())
            _messages.tryEmit("Targets saved.")
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            repository.wipeMeals()
            repository.wipeTemplates()
            repository.wipeFoods()
            repository.wipeTargets()
            repository.wipeWeights()
            val context = getApplication<Application>()
            PreferencesManager.getInstance(context).reset()
            StepCounterStore.clear(context)
            context.getSharedPreferences("rpg_prefs", android.content.Context.MODE_PRIVATE).edit { clear() }
            context.getSharedPreferences("sound_prefs", android.content.Context.MODE_PRIVATE).edit { clear() }
            NotificationScheduler.scheduleReminders(context, false)
            com.example.util.WidgetUpdater.updateAllWidgets(context)
            _messages.tryEmit("All local FoodLab data was removed.")
        }
    }

    fun exportData(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val meals = repository.getAllMeals().first()
                val weights = repository.allWeights.first()
                val file = withContext(Dispatchers.IO) {
                    val csvBuilder = StringBuilder("Type,Date,ItemName,Value1,Value2,Value3,Value4\n")
                    weights.forEach { weight ->
                        csvBuilder.append("Weight,${weight.dateString},,${weight.weightKg},,,\n")
                    }
                    meals.forEach { meal ->
                        csvBuilder.append("Meal,${meal.dateString},${csvEscape(meal.foodName)},${meal.calories},${meal.protein},${meal.carbs},${meal.fat}\n")
                    }
                    java.io.File(context.cacheDir, "foodlab_export.csv").also { it.writeText(csvBuilder.toString()) }
                }
                
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Health Data Export")
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Export Data"))
                _messages.tryEmit("Export ready to share.")
            } catch (e: Exception) {
                _messages.tryEmit("Export failed. Check available storage and try again.")
            }
        }
    }

    private fun csvEscape(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}
