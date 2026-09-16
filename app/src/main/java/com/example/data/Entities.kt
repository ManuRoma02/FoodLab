package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Embedded
import androidx.room.Relation

@Entity(
    tableName = "foods",
    indices = [
        Index(value = ["name"]),
        Index(value = ["brand"]),
        Index(value = ["isFavorite"]),
        Index(value = ["useCount"])
    ]
)
data class FoodItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val brand: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultServingSize: Int = 100,
    val isFavorite: Boolean = false,
    val useCount: Int = 0
)

@Entity(
    tableName = "food_portions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["foodId"])]
)
data class FoodPortion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val foodId: Int,
    val name: String,
    val weightGrams: Float
)

data class FoodItemWithPortions(
    @Embedded val food: FoodItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "foodId"
    )
    val portions: List<FoodPortion>
)


@Entity(
    tableName = "meal_entries",
    indices = [
        Index(value = ["dateString", "timestamp"]),
        Index(value = ["foodId"])
    ]
)
data class MealEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val mealType: String,
    val foodId: Int,
    val foodName: String,
    val quantityGrams: Int,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val caloriesPer100g: Int = 0,
    val proteinPer100g: Float = 0f,
    val carbsPer100g: Float = 0f,
    val fatPer100g: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_targets")
data class DailyTarget(
    @PrimaryKey val dateString: String,
    val targetCalories: Int = DEFAULT_TARGET_CALORIES,
    val targetProtein: Int = DEFAULT_TARGET_PROTEIN,
    val targetCarbs: Int = DEFAULT_TARGET_CARBS,
    val targetFat: Int = DEFAULT_TARGET_FAT
) {
    companion object {
        const val DEFAULT_TARGET_CALORIES = 2450
        const val DEFAULT_TARGET_PROTEIN = 180
        const val DEFAULT_TARGET_CARBS = 245
        const val DEFAULT_TARGET_FAT = 82
    }
}

@Entity(
    tableName = "weight_logs",
    indices = [Index(value = ["timestamp"])]
)
data class WeightLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateString: String,
    val weightKg: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "meal_templates")
data class MealTemplate(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val defaultMealType: String = "SNACK",
    val servings: Int = 1
)

@Entity(
    tableName = "meal_template_items",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MealTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["templateId"])]
)
data class MealTemplateItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val templateId: Int,
    val foodId: Int,
    val foodName: String,
    val brand: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Float,
    val carbsPer100g: Float,
    val fatPer100g: Float,
    val defaultServingSize: Int = 100,
    val quantityGrams: Int
) {
    private val nutritionResult get() = com.example.util.NutritionCalculator.fromPer100g(quantityGrams, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g)
    val calories: Int get() = nutritionResult.calories
    val protein: Float get() = nutritionResult.protein
    val carbs: Float get() = nutritionResult.carbs
    val fat: Float get() = nutritionResult.fat
}

data class MealTemplateWithItems(
    @Embedded val template: MealTemplate,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val items: List<MealTemplateItem>
)
