package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods ORDER BY name ASC")
    fun getAllFoods(): Flow<List<FoodItem>>

    @Query("""
        SELECT * FROM foods
        WHERE name LIKE '%' || :query || '%' ESCAPE '\'
           OR brand LIKE '%' || :query || '%' ESCAPE '\'
        ORDER BY isFavorite DESC, useCount DESC, name ASC
        LIMIT 200
    """)
    fun searchFoods(query: String): Flow<List<FoodItem>>

    @Query("SELECT COUNT(*) FROM foods")
    suspend fun getFoodCount(): Int

    @Query("SELECT * FROM foods WHERE id = :id LIMIT 1")
    suspend fun getFoodById(id: Int): FoodItem?

    @Query("SELECT * FROM foods WHERE barcode = :barcode AND barcode != '' LIMIT 1")
    suspend fun getFoodByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM foods WHERE id IN (:ids)")
    suspend fun getFoodsByIds(ids: List<Int>): List<FoodItem>

    @Query("UPDATE foods SET useCount = useCount + 1 WHERE id = :id")
    suspend fun incrementUseCount(id: Int)

    @Query("UPDATE foods SET useCount = useCount + 1 WHERE id IN (:ids)")
    suspend fun incrementUseCounts(ids: List<Int>)

    @Query("UPDATE foods SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(food: FoodItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoods(foods: List<FoodItem>): List<Long>

    @Query("DELETE FROM foods")
    suspend fun wipeFoods()
}

@Dao
interface MealEntryDao {
    @Query("SELECT * FROM meal_entries WHERE dateString = :date ORDER BY timestamp ASC")
    fun getMealsForDate(date: String): Flow<List<MealEntry>>

    @Query("SELECT * FROM meal_entries WHERE dateString = :date ORDER BY timestamp ASC")
    suspend fun getMealsForDateOnce(date: String): List<MealEntry>
    
    @Query("SELECT * FROM meal_entries ORDER BY timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntry>)

    @Query("SELECT COALESCE(SUM(calories), 0) FROM meal_entries WHERE dateString = :date")
    suspend fun getCaloriesForDate(date: String): Int

    @Query("DELETE FROM meal_entries WHERE id = :id")
    suspend fun deleteMeal(id: Int)

    @Query("DELETE FROM meal_entries WHERE dateString = :date")
    suspend fun deleteMealsForDate(date: String)

    @Query("DELETE FROM meal_entries")
    suspend fun wipeMeals()
}

@Dao
interface DailyTargetDao {
    @Query("SELECT * FROM daily_targets")
    fun getAllTargets(): Flow<List<DailyTarget>>

    @Query("SELECT * FROM daily_targets WHERE dateString = :date LIMIT 1")
    fun getTargetForDate(date: String): Flow<DailyTarget?>

    @Query("SELECT * FROM daily_targets WHERE dateString = :date LIMIT 1")
    suspend fun getTargetForDateOnce(date: String): DailyTarget?

    @Query("SELECT * FROM daily_targets WHERE dateString = 'FIXED' LIMIT 1")
    fun getFixedTarget(): Flow<DailyTarget?>

    @Query("SELECT * FROM daily_targets WHERE dateString = 'FIXED' LIMIT 1")
    suspend fun getFixedTargetOnce(): DailyTarget?

    @Query("SELECT * FROM daily_targets WHERE dateString IN (:dates)")
    fun getTargetsForDates(dates: List<String>): Flow<List<DailyTarget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: DailyTarget)

    @Query("DELETE FROM daily_targets")
    suspend fun wipeTargets()
}

@Dao
interface WeightLogDao {
    @Query("SELECT * FROM weight_logs ORDER BY timestamp ASC")
    fun getAllWeights(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC")
    fun getAllWeightsDesc(): Flow<List<WeightLog>>

    @Query("SELECT * FROM weight_logs ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestWeightOnce(): WeightLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightLog): Long

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightById(id: Int)

    @Query("DELETE FROM weight_logs")
    suspend fun wipeWeights()

    @Query("DELETE FROM weight_logs WHERE id = (SELECT id FROM weight_logs ORDER BY timestamp DESC LIMIT 1)")
    suspend fun deleteLatestWeight()
}

@Dao
interface MealTemplateDao {
    @Transaction
    @Query("SELECT * FROM meal_templates ORDER BY name ASC")
    fun getAllTemplatesWithItems(): Flow<List<MealTemplateWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MealTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateItems(items: List<MealTemplateItem>)

    @Query("DELETE FROM meal_templates WHERE id = :templateId")
    suspend fun deleteTemplate(templateId: Int)

    @Query("DELETE FROM meal_templates")
    suspend fun wipeTemplates()
}

@Dao
interface FoodPortionDao {
    @Query("SELECT * FROM food_portions WHERE foodId = :foodId ORDER BY id ASC")
    suspend fun getPortionsForFood(foodId: Int): List<FoodPortion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortions(portions: List<FoodPortion>)

    @Query("DELETE FROM food_portions WHERE foodId = :foodId")
    suspend fun deletePortionsForFood(foodId: Int)
}
