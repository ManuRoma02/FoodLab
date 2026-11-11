package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.FoodItem
import com.example.data.FoodPortion
import com.example.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.InputStreamReader
import java.util.Locale

object CreaDatabaseImporter {
    private const val TAG = "CreaDatabaseImporter"
    private const val PREF_KEY_IMPORTED = "crea_imported_v3_enhanced"

    suspend fun importIfNeeded(context: Context, repository: Repository) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("foodlab_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean(PREF_KEY_IMPORTED, false)) {
                Log.d(TAG, "Enhanced CREA/Chains/Scientific dataset already imported.")
                return@withContext
            }

            try {
                Log.d(TAG, "Starting food dataset import from minified assets...")
                val assetManager = context.assets
                val stream = assetManager.open("data/crea_foods.json")
                val jsonString = InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
                val jsonArray = JSONArray(jsonString)

                // Query existing foods to prevent duplicate entries across version upgrades
                val existingNames = try {
                    repository.getAllFoodNames().map { it.trim().lowercase(Locale.ROOT) }.toSet()
                } catch (e: Exception) {
                    emptySet()
                }

                val foodList = mutableListOf<FoodItem>()
                val portionGramsMap = mutableMapOf<Int, Pair<Int, String?>>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = (if (obj.has("n")) obj.optString("n") else obj.optString("name", "")).trim()
                    val category = (if (obj.has("c")) obj.optString("c") else obj.optString("category", "CREA Italia")).trim()
                    val kcal = if (obj.has("k")) obj.optInt("k", 0) else obj.optInt("kcal", 0)
                    val protein = (if (obj.has("p")) obj.optDouble("p", 0.0) else obj.optDouble("protein", 0.0)).toFloat()
                    val carbs = (if (obj.has("cb")) obj.optDouble("cb", 0.0) else obj.optDouble("carbs", 0.0)).toFloat()
                    val fat = (if (obj.has("f")) obj.optDouble("f", 0.0) else obj.optDouble("fat", 0.0)).toFloat()
                    val portion = if (obj.has("pt")) obj.optInt("pt", 100) else obj.optInt("portion", 100)
                    val portionName = if (obj.has("pn")) obj.optString("pn").trim().ifEmpty { null } else null
                    val barcode = (if (obj.has("b")) obj.optString("b") else obj.optString("barcode", "")).trim()

                    if (name.isNotEmpty() && kcal > 0) {
                        val normName = name.lowercase(Locale.ROOT)
                        if (normName in existingNames) {
                            continue
                        }

                        val food = FoodItem(
                            name = name,
                            brand = category.ifEmpty { "CREA Italia" },
                            caloriesPer100g = kcal,
                            proteinPer100g = protein.coerceIn(0f, 100f),
                            carbsPer100g = carbs.coerceIn(0f, 100f),
                            fatPer100g = fat.coerceIn(0f, 100f),
                            defaultServingSize = if (portion > 0) portion else 100,
                            barcode = barcode
                        )
                        val index = foodList.size
                        foodList.add(food)
                        if (portion > 0 && (portion != 100 || portionName != null)) {
                            portionGramsMap[index] = Pair(portion, portionName)
                        }
                    }
                }

                if (foodList.isNotEmpty()) {
                    val insertedIds = repository.insertInitialFoods(foodList)
                    val portions = mutableListOf<FoodPortion>()
                    portionGramsMap.forEach { (index, portionData) ->
                        val foodId = insertedIds.getOrNull(index)?.toInt() ?: 0
                        if (foodId > 0) {
                            val (grams, customName) = portionData
                            val pName = customName ?: "Porzione (${grams}g)"
                            portions.add(
                                FoodPortion(
                                    foodId = foodId,
                                    name = pName,
                                    weightGrams = grams.toFloat()
                                )
                            )
                        }
                    }
                    if (portions.isNotEmpty()) {
                        repository.insertPortions(portions)
                    }
                }

                prefs.edit().putBoolean(PREF_KEY_IMPORTED, true).apply()
                Log.i(TAG, "Successfully imported ${foodList.size} new foods into Room database!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import food dataset", e)
            }
        }
    }
}
