package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositorySearchAndChunkTest {

    @Test
    fun getFoodsByIds_chunksOver900Ids() = runBlocking {
        val fakeFoodDao = FakeFoodDao()
        val repo = createFakeRepository(foodDao = fakeFoodDao)

        val ids = (1..1850).toList() + listOf(1, 2, 900)
        val result = repo.getFoodsByIds(ids)

        assertEquals(listOf(900, 900, 50), fakeFoodDao.getFoodsByIdsBatches.map { it.size })
        assertEquals(1850, result.size)
    }

    @Test
    fun incrementUseCounts_chunksOver900Ids() = runBlocking {
        val fakeFoodDao = FakeFoodDao()
        val repo = createFakeRepository(foodDao = fakeFoodDao)

        val ids = (1..1900).toList() + listOf(5, 10)
        repo.incrementUseCounts(ids)

        assertEquals(listOf(900, 900, 100), fakeFoodDao.incrementUseCountsBatches.map { it.size })
    }

    @Test
    fun getTargetsForDates_chunksOver900DatesAndCombinesResults() = runBlocking {
        val fakeTargetDao = FakeDailyTargetDao()
        val repo = createFakeRepository(targetDao = fakeTargetDao)

        val dates = (1..1850).map { "2026-01-%04d".format(it) } + listOf("2026-01-0001")
        val combined = repo.getTargetsForDates(dates).first()

        assertEquals(listOf(900, 900, 50), fakeTargetDao.getTargetsForDatesBatches.map { it.size })
        assertEquals(1850, combined.size)
    }

    @Test
    fun searchFoods_addsOrderByBeforeLimitInAllTokenQueries() = runBlocking {
        val fakeFoodDao = FakeFoodDao()
        val repo = createFakeRepository(foodDao = fakeFoodDao)

        // Triggers multi-token AND -> prefix AND -> UNION fallback (3 queries)
        repo.searchFoods("barila penne").first()
        // Triggers multi-token with stopword fallback
        repo.searchFoods("pasta di semola").first()
        // Triggers single-token AND -> typo prefix fallback
        repo.searchFoods("nutela").first()

        assertTrue(fakeFoodDao.rawQueries.isNotEmpty())
        val expectedOrder = "ORDER BY isFavorite DESC, useCount DESC, isUserCreated DESC, name ASC LIMIT 1000"
        fakeFoodDao.rawQueries.forEach { sql ->
            assertTrue("Expected '$expectedOrder' in SQL: $sql", sql.contains(expectedOrder))
        }
    }

    @Test
    fun searchFoods_inMemoryRoom_ordersByFavoriteUseCountUserCreatedAndName() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = createRoomRepository(db)
            repo.insertInitialFoods(
                listOf(
                    FoodItem(name = "Pasta Zeta", brand = "Brand", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f),
                    FoodItem(name = "Pasta Alpha", brand = "Brand", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f),
                    FoodItem(name = "Pasta Mia", brand = "User", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f, isUserCreated = true),
                    FoodItem(name = "Pasta Usata", brand = "Brand", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f, useCount = 5),
                    FoodItem(name = "Pasta Preferita", brand = "Brand", caloriesPer100g = 350, proteinPer100g = 12f, carbsPer100g = 70f, fatPer100g = 2f, isFavorite = true)
                )
            )

            val results = repo.searchFoods("Pasta").first().map { it.name }
            assertEquals(
                listOf("Pasta Preferita", "Pasta Usata", "Pasta Mia", "Pasta Alpha", "Pasta Zeta"),
                results
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun ensureDefaultFoodsSeeded_mutexPreventsDuplicateSeedingUnderConcurrency() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = createRoomRepository(db)
            val jobs = (1..8).map {
                async { repo.ensureDefaultFoodsSeeded() }
            }
            jobs.awaitAll()

            assertEquals(DefaultFoodCatalog.items.size, db.foodDao().getFoodCount())
        } finally {
            db.close()
        }
    }

    private fun createRoomRepository(db: AppDatabase) = Repository(
        foodDao = db.foodDao(),
        mealDao = db.mealEntryDao(),
        targetDao = db.dailyTargetDao(),
        weightDao = db.weightLogDao(),
        templateDao = db.mealTemplateDao(),
        portionDao = db.foodPortionDao(),
        shoppingDao = db.shoppingItemDao(),
        openFoodFactsApi = null,
        database = db
    )

    private fun createFakeRepository(
        foodDao: FoodDao = FakeFoodDao(),
        targetDao: DailyTargetDao = FakeDailyTargetDao()
    ) = Repository(
        foodDao = foodDao,
        mealDao = FakeMealEntryDao(),
        targetDao = targetDao,
        weightDao = FakeWeightLogDao(),
        templateDao = FakeMealTemplateDao(),
        portionDao = FakeFoodPortionDao(),
        shoppingDao = null,
        openFoodFactsApi = null,
        database = null
    )

    private class FakeFoodDao : FoodDao {
        val getFoodsByIdsBatches = mutableListOf<List<Int>>()
        val incrementUseCountsBatches = mutableListOf<List<Int>>()
        val rawQueries = mutableListOf<String>()

        override fun getAllFoods(): Flow<List<FoodItem>> = flowOf(emptyList())
        override fun searchFoods(query: String): Flow<List<FoodItem>> = flowOf(emptyList())
        override fun searchFoodsRaw(query: SupportSQLiteQuery): Flow<List<FoodItem>> {
            rawQueries += query.sql
            return flowOf(emptyList())
        }
        override suspend fun getAllFoodNames(): List<String> = emptyList()
        override suspend fun getFoodCount(): Int = 0
        override suspend fun getFoodById(id: Int): FoodItem? = null
        override suspend fun getFoodByBarcode(barcode: String): FoodItem? = null
        override suspend fun getFoodByNameAndBrand(name: String, brand: String): FoodItem? = null
        override suspend fun getUserCreatedFoodsWithPortions(): List<FoodItemWithPortions> = emptyList()
        override suspend fun getUserCreatedFoods(): List<FoodItem> = emptyList()
        override suspend fun getFoodsByIds(ids: List<Int>): List<FoodItem> {
            getFoodsByIdsBatches += ids
            return ids.map { id ->
                FoodItem(id = id, name = "Food $id", brand = "", caloriesPer100g = 100, proteinPer100g = 1f, carbsPer100g = 1f, fatPer100g = 1f)
            }
        }
        override suspend fun incrementUseCount(id: Int) = Unit
        override suspend fun incrementUseCounts(ids: List<Int>) {
            incrementUseCountsBatches += ids
        }
        override suspend fun setFavorite(id: Int, isFavorite: Boolean) = Unit
        override suspend fun insertFood(food: FoodItem): Long = 1L
        override suspend fun insertFoods(foods: List<FoodItem>): List<Long> = foods.indices.map { (it + 1).toLong() }
        override suspend fun wipeFoods() = Unit
    }

    private class FakeDailyTargetDao : DailyTargetDao {
        val getTargetsForDatesBatches = mutableListOf<List<String>>()
        override fun getAllTargets(): Flow<List<DailyTarget>> = flowOf(emptyList())
        override fun getTargetForDate(date: String): Flow<DailyTarget?> = flowOf(null)
        override suspend fun getTargetForDateOnce(date: String): DailyTarget? = null
        override fun getFixedTarget(): Flow<DailyTarget?> = flowOf(null)
        override suspend fun getFixedTargetOnce(): DailyTarget? = null
        override fun getTargetsForDates(dates: List<String>): Flow<List<DailyTarget>> {
            getTargetsForDatesBatches += dates
            return MutableStateFlow(dates.map { DailyTarget(dateString = it) })
        }
        override suspend fun insertTarget(target: DailyTarget) = Unit
        override suspend fun wipeTargets() = Unit
    }

    private class FakeMealEntryDao : MealEntryDao {
        override fun getMealsForDate(date: String): Flow<List<MealEntry>> = flowOf(emptyList())
        override suspend fun getMealsForDateOnce(date: String): List<MealEntry> = emptyList()
        override fun getAllMeals(): Flow<List<MealEntry>> = flowOf(emptyList())
        override suspend fun insertMeal(meal: MealEntry) = Unit
        override suspend fun insertMeals(meals: List<MealEntry>) = Unit
        override suspend fun getCaloriesForDate(date: String): Int = 0
        override suspend fun deleteMeal(id: Int) = Unit
        override suspend fun deleteMealsForDate(date: String) = Unit
        override suspend fun wipeMeals() = Unit
    }

    private class FakeWeightLogDao : WeightLogDao {
        override fun getAllWeights(): Flow<List<WeightLog>> = flowOf(emptyList())
        override fun getAllWeightsDesc(): Flow<List<WeightLog>> = flowOf(emptyList())
        override suspend fun getLatestWeightOnce(): WeightLog? = null
        override suspend fun insertWeight(weight: WeightLog): Long = 1L
        override suspend fun deleteWeightById(id: Int) = Unit
        override suspend fun wipeWeights() = Unit
        override suspend fun deleteLatestWeight() = Unit
    }

    private class FakeMealTemplateDao : MealTemplateDao {
        override fun getAllTemplatesWithItems(): Flow<List<MealTemplateWithItems>> = flowOf(emptyList())
        override suspend fun insertTemplate(template: MealTemplate): Long = 1L
        override suspend fun insertTemplateItems(items: List<MealTemplateItem>) = Unit
        override suspend fun deleteTemplate(templateId: Int) = Unit
        override suspend fun wipeTemplates() = Unit
    }

    private class FakeFoodPortionDao : FoodPortionDao {
        override suspend fun getPortionsForFood(foodId: Int): List<FoodPortion> = emptyList()
        override suspend fun insertPortions(portions: List<FoodPortion>) = Unit
        override suspend fun deletePortionsForFood(foodId: Int) = Unit
    }
}
