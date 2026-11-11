# Design Document: Italian Food & Nutrition Dataset Pipeline for FoodLab

**Date:** 2026-11-10  
**Status:** Draft / Review  
**Target:** `tools/dataset_builder/`  

---

## 1. Intent and The "Why"
FoodLab is an offline-first Android calorie and macro tracking application. Currently, FoodLab's local database only contains a miniature seed (`crea_foods.json`, ~93 KB, ~700 items). To provide a seamless user experience comparable to commercial trackers without requiring external online API calls at runtime, FoodLab needs an exhaustive, clean, verified offline Italian food dataset.

This pipeline will extract, scrape, clean, deduplicate, and compile:
1. All packaged supermarket foods sold in Italy with barcodes (EAN-13).
2. All raw ingredients, PDO/PGI traditional products, cuts of meat, fish, and traditional dishes from Italian official sources.
3. Private-label supermarket products scraped via Scrapling.

---

## 2. Goals & Success Criteria
1. **Automated Extraction Pipeline**: A standalone Python toolchain in `tools/dataset_builder/` that can be run on-demand or incrementally.
2. **Multi-Source Ingestion**:
   - **Open Food Facts (OFF) Italy**: Stream and filter the official daily dump for products sold in Italy (`countries_tags` containing `en:italy` or `it`).
   - **CREA AlimentiNutrizione & BDA-IEO**: Complete ingestion of official Italian nutritional tables (~3,000 raw foods, dishes, portion weights).
   - **Scrapling Spider**: Dedicated crawler using Scrapling (stealth browser / HTTP client) targeting major Italian supermarket chains (e.g. Conad, Coop, Esselunga) to extract missing private-label items.
3. **Strict Macro Quality Gate**:
   - `calories_100g`, `protein_100g`, `carbs_100g`, `fat_100g` are **strictly mandatory**.
   - If ANY of these 4 primary macronutrients is missing, null, or invalid, the record is **discarded immediately**.
   - Consistency check: `(protein_100g + carbs_100g + fat_100g) <= 102.0g` (allowing minor measurement rounding) and `calories_100g` roughly consistent with Atwater factors ($\pm 25\%$).
4. **Rich Nutritional Attributes**:
   - Capture optional secondary nutrients when present:
     - `sugars_100g` (float, nullable)
     - `saturated_fat_100g` (float, nullable)
     - `polyols_100g` (float, nullable)
     - `fiber_100g` (float, nullable)
     - `salt_100g` (float, nullable)
     - `sodium_100g` (float, nullable)
   - Store portion metadata (name, weight in grams) for `food_portions` table.
5. **Output Deliverables**:
   - Master SQLite database (`foodlab_dataset.db`) containing:
     - `foods`: canonical schema matching FoodLab + extra nutrient columns.
     - `food_portions`: linked portion weights.
     - `foods_fts`: SQLite FTS5 virtual table for instantaneous (<5ms) sub-string & prefix search across 200,000+ items.
   - Master Parquet export (`foodlab_italian_foods.parquet`) for archival, compression, and analysis.

---

## 3. Non-Goals & Anti-Goals
- **Non-Goals (Deferred to next milestone)**:
  - Modifying FoodLab's Android Compose UI or Room schema in this phase (user directive: focus strictly on the dataset pipeline first).
  - Real-time in-app scraping over Android network connections.
  - Ingestion of non-food items (pet food, cosmetics, supplements, pharmaceuticals).
- **Anti-Goals**:
  - Ingesting unverified junk data (e.g. 0 kcal food with 50g fat, negative numbers, missing protein).
  - Scraping fragile dynamic sites without anti-bot protection (Scrapling is mandated to avoid IP blocks).
  - Massive unindexed dumps that cause UI freezing when querying.

---

## 4. Architecture & Technical Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        DATA EXTRACTION LAYER                           │
├──────────────────────┬──────────────────────┬──────────────────────────┤
│ Open Food Facts IT   │ CREA & BDA-IEO       │ Scrapling Spider         │
│ (Bulk Dump / Parquet)│ (Official Tables)    │ (Italian GDO Chains)     │
└──────────┬───────────┴──────────┬───────────┴────────────┬─────────────┘
           │                      │                        │
           ▼                      ▼                        ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   NORMALIZATION & CLEANING ENGINE                      │
│ - Mandatory Macro Gate: Kcal + Prot + Carb + Fat present               │
│ - Physical sanity filter: sum(macros) <= 100g                          │
│ - Extracted: Sugars, SatFat, Polyols, Fiber, Salt, Barcode             │
│ - Deduplication by Barcode & normalized Brand + Name                   │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       STORAGE & COMPILATION                            │
├──────────────────────────────────┬─────────────────────────────────────┤
│ Master SQLite (foodlab_dataset.db│ Parquet Export                      │
│ - foods + extra nutrients        │ (foodlab_italian_foods.parquet)     │
│ - food_portions                  │ Compressed archival copy            │
│ - foods_fts (FTS5 search index)  │                                     │
└──────────────────────────────────┴─────────────────────────────────────┘
```

### 4.1 Master Database Schema

```sql
CREATE TABLE foods (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    brand TEXT NOT NULL DEFAULT '',
    category TEXT NOT NULL DEFAULT '',
    barcode TEXT NOT NULL DEFAULT '',
    calories_100g INTEGER NOT NULL,
    protein_100g REAL NOT NULL,
    carbs_100g REAL NOT NULL,
    fat_100g REAL NOT NULL,
    sugars_100g REAL,
    saturated_fat_100g REAL,
    polyols_100g REAL,
    fiber_100g REAL,
    salt_100g REAL,
    sodium_100g REAL,
    default_serving_size INTEGER NOT NULL DEFAULT 100,
    source TEXT NOT NULL, -- 'OFF_IT', 'CREA', 'SCRAPLING_ESSELUNGA', etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_foods_barcode ON foods(barcode);
CREATE INDEX idx_foods_name ON foods(name);
CREATE INDEX idx_foods_brand ON foods(brand);

CREATE TABLE food_portions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    weight_grams REAL NOT NULL
);

-- Full Text Search Index for instant search
CREATE VIRTUAL TABLE foods_fts USING fts5(
    name,
    brand,
    content='foods',
    content_rowid='id'
);
```

---

## 5. What's Left to The Builder
1. Implement the Python environment and package management under `tools/dataset_builder/` (e.g. `scrapling`, `polars` / `pandas`, `duckdb` or `sqlite3`, `tqdm`).
2. Implement `extract_off.py` (stream or download OFF Italy dump with zero RAM overload).
3. Implement `extract_crea.py` (parse CREA alimentinutrizione tables into normalized schema).
4. Implement `scraper_scrapling.py` (spiders for Italian supermarket catalogs using Scrapling).
5. Implement `build_dataset.py` (pipeline coordinator: cleans, deduplicates, validates, and builds `foodlab_dataset.db` + FTS5).
