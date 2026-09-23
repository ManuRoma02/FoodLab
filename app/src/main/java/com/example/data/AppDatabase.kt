package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        FoodItem::class,
        MealEntry::class,
        DailyTarget::class,
        WeightLog::class,
        MealTemplate::class,
        MealTemplateItem::class,
        FoodPortion::class,
        ShoppingItem::class
    ],
    version = 12,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao
    abstract fun mealEntryDao(): MealEntryDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun foodPortionDao(): FoodPortionDao
    abstract fun shoppingItemDao(): ShoppingItemDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE foods ADD COLUMN useCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_name ON foods(name)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_brand ON foods(brand)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_isFavorite ON foods(isFavorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_useCount ON foods(useCount)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meal_templates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `defaultMealType` TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meal_template_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `templateId` INTEGER NOT NULL, 
                        `foodId` INTEGER NOT NULL, 
                        `foodName` TEXT NOT NULL, 
                        `brand` TEXT NOT NULL, 
                        `caloriesPer100g` INTEGER NOT NULL, 
                        `proteinPer100g` REAL NOT NULL, 
                        `carbsPer100g` REAL NOT NULL, 
                        `fatPer100g` REAL NOT NULL, 
                        `defaultServingSize` INTEGER NOT NULL, 
                        `quantityGrams` INTEGER NOT NULL,
                        FOREIGN KEY(`templateId`) REFERENCES `meal_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_template_items_templateId` ON `meal_template_items` (`templateId`)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `food_portions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `foodId` INTEGER NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `weightGrams` REAL NOT NULL,
                        FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_portions_foodId` ON `food_portions` (`foodId`)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_entries_dateString_timestamp` ON `meal_entries` (`dateString`, `timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_meal_entries_foodId` ON `meal_entries` (`foodId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_weight_logs_timestamp` ON `weight_logs` (`timestamp`)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `weight_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `date` TEXT NOT NULL, 
                        `weightKg` REAL NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `category` TEXT NOT NULL, 
                        `isChecked` INTEGER NOT NULL, 
                        `quantity` TEXT NOT NULL, 
                        `timestamp` INTEGER NOT NULL
                    )
                """)
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_templates ADD COLUMN servings INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("""
                        INSERT INTO `weight_logs` (`dateString`, `weightKg`, `timestamp`)
                        SELECT `date`, `weightKg`, `timestamp` FROM `weight_entries`
                        WHERE `timestamp` NOT IN (SELECT `timestamp` FROM `weight_logs`)
                    """)
                } catch (e: Exception) {
                    android.util.Log.w("AppDatabase", "Migration 9->10 notice", e)
                }
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN caloriesPer100g INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN proteinPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN carbsPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meal_entries ADD COLUMN fatPer100g REAL NOT NULL DEFAULT 0")
                db.execSQL("""
                    UPDATE meal_entries 
                    SET caloriesPer100g = CASE WHEN quantityGrams > 0 THEN CAST(ROUND((calories * 100.0) / quantityGrams) AS INTEGER) ELSE calories END,
                        proteinPer100g = CASE WHEN quantityGrams > 0 THEN (protein * 100.0) / quantityGrams ELSE protein END,
                        carbsPer100g = CASE WHEN quantityGrams > 0 THEN (carbs * 100.0) / quantityGrams ELSE carbs END,
                        fatPer100g = CASE WHEN quantityGrams > 0 THEN (fat * 100.0) / quantityGrams ELSE fat END
                """)
                db.execSQL("DROP TABLE IF EXISTS weight_entries")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE foods ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_foods_barcode ON foods(barcode)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "foodtrack_database"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
