package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.FoodItem
import com.example.data.FoodPortion
import com.example.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.GZIPInputStream

object CreaDatabaseImporter {
    private const val TAG = "CreaDatabaseImporter"
    private const val PREF_KEY_MASTER_IMPORTED = "foodlab_master_dataset_v4"
    private const val PREF_KEY_CREA_IMPORTED = "crea_imported_v3_enhanced"

    suspend fun importIfNeeded(context: Context, repository: Repository) {
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences("foodlab_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean(PREF_KEY_MASTER_IMPORTED, false)) {
                Log.d(TAG, "Master Italian food dataset already imported.")
                return@withContext
            }

            // Check if foodlab_master.db.gz exists in assets
            val hasMasterAsset = try {
                context.assets.list("data")?.contains("foodlab_master.db.gz") == true
            } catch (e: Exception) {
                false
            }

            if (hasMasterAsset) {
                try {
                    Log.i(TAG, "Importing master Italian dataset from assets/data/foodlab_master.db.gz...")
                    val tempDbFile = File(context.cacheDir, "foodlab_master_temp.db")
                    if (tempDbFile.exists()) tempDbFile.delete()

                    val buffer = ByteArray(64 * 1024)
                    context.assets.open("data/foodlab_master.db.gz").use { assetIn ->
                        GZIPInputStream(assetIn).use { gzIn ->
                            tempDbFile.outputStream().use { fileOut ->
                                var bytesRead: Int
                                while (gzIn.read(buffer).also { bytesRead = it } != -1) {
                                    fileOut.write(buffer, 0, bytesRead)
                                }
                            }
                        }
                    }

                    val appDb = AppDatabase.getDatabase(context)
                    val db = appDb.openHelper.writableDatabase

                    val escapedPath = tempDbFile.absolutePath.replace("'", "''")
                    db.execSQL("ATTACH DATABASE '$escapedPath' AS master_import")

                    db.beginTransaction()
                    try {
                        db.execSQL("""
                            INSERT OR IGNORE INTO foods (
                                id, name, brand, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g,
                                defaultServingSize, isFavorite, useCount, barcode
                            )
                            SELECT 
                                id, name, brand, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g,
                                defaultServingSize, 0, 0, barcode
                            FROM master_import.foods
                        """)
                        db.execSQL("""
                            INSERT OR IGNORE INTO food_portions (id, foodId, name, weightGrams)
                            SELECT id, foodId, name, weightGrams
                            FROM master_import.food_portions
                        """)
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                        try {
                            db.execSQL("DETACH DATABASE master_import")
                        } catch (e: Exception) {
                            Log.w(TAG, "Error detaching master_import", e)
                        }
                        tempDbFile.delete()
                    }

                    prefs.edit()
                        .putBoolean(PREF_KEY_MASTER_IMPORTED, true)
                        .putBoolean(PREF_KEY_CREA_IMPORTED, true)
                        .apply()
                    Log.i(TAG, "Master Italian food dataset (218k foods) successfully imported into Room!")
                    return@withContext
                } catch (e: Exception) {
                    Log.e(TAG, "Failed importing foodlab_master.db.gz, falling back to JSON...", e)
                }
            }

            // Fallback for minimal test environments or when master asset is omitted
            if (prefs.getBoolean(PREF_KEY_CREA_IMPORTED, false)) {
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

                prefs.edit().putBoolean(PREF_KEY_CREA_IMPORTED, true).apply()
                Log.i(TAG, "Successfully imported ${foodList.size} new foods into Room database!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import food dataset", e)
            }
        }
    }
}
