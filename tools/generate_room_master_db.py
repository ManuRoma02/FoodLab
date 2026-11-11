import sqlite3
import gzip
import shutil
import os

source_db = "tools/dataset_builder/foodlab_italian_master.db"
target_db = "tools/dataset_builder/foodlab_room_master.db"
target_gz = "tools/dataset_builder/foodlab_room_master.db.gz"

if os.path.exists(target_db):
    os.remove(target_db)
if os.path.exists(target_gz):
    os.remove(target_gz)

conn = sqlite3.connect(target_db)
c = conn.cursor()

# Set fast pragmas for generation
c.execute("PRAGMA page_size = 4096")
c.execute("PRAGMA journal_mode = OFF")
c.execute("PRAGMA synchronous = OFF")

# Create Room-compatible foods table
c.execute("""
CREATE TABLE IF NOT EXISTS `foods` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `name` TEXT NOT NULL,
    `brand` TEXT NOT NULL,
    `caloriesPer100g` INTEGER NOT NULL,
    `proteinPer100g` REAL NOT NULL,
    `carbsPer100g` REAL NOT NULL,
    `fatPer100g` REAL NOT NULL,
    `defaultServingSize` INTEGER NOT NULL DEFAULT 100,
    `isFavorite` INTEGER NOT NULL DEFAULT 0,
    `useCount` INTEGER NOT NULL DEFAULT 0,
    `barcode` TEXT NOT NULL DEFAULT ''
)
""")

# Create Room-compatible food_portions table
c.execute("""
CREATE TABLE IF NOT EXISTS `food_portions` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `foodId` INTEGER NOT NULL,
    `name` TEXT NOT NULL,
    `weightGrams` REAL NOT NULL,
    FOREIGN KEY(`foodId`) REFERENCES `foods`(`id`) ON DELETE CASCADE
)
""")

# Attach source master db
c.execute(f"ATTACH DATABASE '{source_db}' AS src")

# Copy foods
c.execute("""
INSERT INTO foods (id, name, brand, caloriesPer100g, proteinPer100g, carbsPer100g, fatPer100g, defaultServingSize, isFavorite, useCount, barcode)
SELECT 
    id,
    name,
    COALESCE(brand, ''),
    CAST(ROUND(calories_100g) AS INTEGER),
    ROUND(protein_100g, 1),
    ROUND(carbs_100g, 1),
    ROUND(fat_100g, 1),
    COALESCE(default_serving_size, 100),
    0,
    0,
    COALESCE(barcode, '')
FROM src.foods
ORDER BY id ASC
""")

# Copy portions
c.execute("""
INSERT INTO food_portions (id, foodId, name, weightGrams)
SELECT id, food_id, name, weight_grams
FROM src.food_portions
ORDER BY id ASC
""")

# Build indexes
c.execute("CREATE INDEX IF NOT EXISTS `index_foods_name` ON `foods` (`name`)")
c.execute("CREATE INDEX IF NOT EXISTS `index_foods_brand` ON `foods` (`brand`)")
c.execute("CREATE INDEX IF NOT EXISTS `index_foods_barcode` ON `foods` (`barcode`)")
c.execute("CREATE INDEX IF NOT EXISTS `index_foods_isFavorite` ON `foods` (`isFavorite`)")
c.execute("CREATE INDEX IF NOT EXISTS `index_foods_useCount` ON `foods` (`useCount`)")
c.execute("CREATE INDEX IF NOT EXISTS `index_food_portions_foodId` ON `food_portions` (`foodId`)")

conn.commit()
c.execute("VACUUM")
conn.close()

db_size = os.path.getsize(target_db) / (1024 * 1024)
print(f"Room Master DB created: {db_size:.2f} MB")

with open(target_db, "rb") as f_in, gzip.open(target_gz, "wb", compresslevel=9) as f_out:
    shutil.copyfileobj(f_in, f_out)

gz_size = os.path.getsize(target_gz) / (1024 * 1024)
print(f"Room Master DB compressed (.gz): {gz_size:.2f} MB")
