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

    fun exportUserCreatedFoods(context: android.content.Context) {
        viewModelScope.launch {
            try {
                val userFoods = repository.getUserCreatedFoodsWithPortions()
                if (userFoods.isEmpty()) {
                    _messages.tryEmit("Nessun alimento personalizzato o scansionato da esportare.")
                    return@launch
                }

                val jsonArray = org.json.JSONArray()
                for (item in userFoods) {
                    val obj = org.json.JSONObject().apply {
                        put("name", item.food.name)
                        put("brand", item.food.brand)
                        put("barcode", item.food.barcode)
                        put("caloriesPer100g", item.food.caloriesPer100g)
                        put("proteinPer100g", item.food.proteinPer100g.toDouble())
                        put("carbsPer100g", item.food.carbsPer100g.toDouble())
                        put("fatPer100g", item.food.fatPer100g.toDouble())
                        put("defaultServingSize", item.food.defaultServingSize)
                        if (item.portions.isNotEmpty()) {
                            val portionsArray = org.json.JSONArray()
                            item.portions.forEach { p ->
                                portionsArray.put(org.json.JSONObject().apply {
                                    put("name", p.name)
                                    put("weightGrams", p.weightGrams.toDouble())
                                })
                            }
                            put("portions", portionsArray)
                        }
                    }
                    jsonArray.put(obj)
                }

                val jsonString = jsonArray.toString(2)

                // Copy to Clipboard
                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                if (clipboard != null) {
                    val clip = android.content.ClipData.newPlainText("FoodLab Custom Foods", jsonString)
                    clipboard.setPrimaryClip(clip)
                }

                // Write file and share via Intent
                val file = withContext(Dispatchers.IO) {
                    java.io.File(context.cacheDir, "foodlab_user_foods.json").also {
                        it.writeText(jsonString, Charsets.UTF_8)
                    }
                }

                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "FoodLab Custom Foods Export")
                    putExtra(android.content.Intent.EXTRA_TEXT, jsonString)
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Esporta Alimenti Personalizzati"))
                _messages.tryEmit("${userFoods.size} alimenti esportati e copiati negli appunti!")
            } catch (e: Exception) {
                _messages.tryEmit("Errore durante l'esportazione degli alimenti.")
            }
        }
    }

    private fun csvEscape(value: String): String {
        val sanitized = if (value.isNotEmpty() && value[0] in "=+-@\t\r") "'$value" else value
        return "\"${sanitized.replace("\"", "\"\"")}\""
    }
}
