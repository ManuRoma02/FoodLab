# FoodLab Italian Food & Nutrition Dataset Builder

Comprehensive, high-performance dataset pipeline for assembling, validating, and compiling the official FoodLab Italian Food & Nutrition Database.

The pipeline combines official institutional databases (**CREA** / **BDA-IEO**), large-scale crowdsourced datasets (**Open Food Facts Italy**), and real-world supermarket product data (**Scrapling GDO**). Every record is strictly validated through the **Mandatory Macro Gate**, deduplicated using a strict **Source Precedence Hierarchy**, and compiled into an optimized **SQLite database** with **FTS5 full-text search** and optional **Apache Parquet** export.

---

## Architecture Overview

```
                      +-----------------------------+
                      |   CREA / BDA-IEO Tables     | (Priority 3 - Highest)
                      +--------------+--------------+
                                     |
+--------------------------+         |         +---------------------------+
|  Open Food Facts Italy   |         |         |  Scrapling GDO Supermarket|
| (.jsonl.gz / streams)    |         |         |  (Esselunga, Conad, Coop) |
| (Priority 1)             |         |         | (Priority 2)              |
+------------+-------------+         |         +-------------+-------------+
             |                       |                       |
             v                       v                       v
      +-------------------------------------------------------------+
      |               MANDATORY MACRO QUALITY GATE                  |
      |   - Calories, Protein, Carbs, Fat (Strictly mandatory)      |
      |   - Bound checks: individual macros <= 100.5g               |
      |   - Physical sum: (P + C + F) <= 102.0g                     |
      |   - Atwater energy factor sanity check                      |
      +------------------------------+------------------------------+
                                     | Valid FoodRecords
                                     v
      +-------------------------------------------------------------+
      |             SOURCE-PRECEDENCE DEDUPLICATION                 |
      |             CREA (3) > SCRAPLING (2) > OFF (1)              |
      |   - Primary key: Barcode (>= 4 digits)                      |
      |   - Secondary key: Normalized Brand + Name                  |
      |   - Dynamic nutrient enrichment (backfills secondary stats) |
      +------------------------------+------------------------------+
                                     |
                     +---------------+---------------+
                     |                               |
                     v                               v
      +------------------------------+ +----------------------------+
      |     SQLite 3 Database        | |   Apache Parquet Export    |
      | - foods table                | | (Snappy compressed columnar|
      | - food_portions table        | |  for analytical pipelines) |
      | - foods_fts (FTS5 table)     | +----------------------------+
      | - Synchronized triggers      |
      +------------------------------+
```

---

## Key Modules

| Module | Description |
| :--- | :--- |
| [`models.py`](models.py) | Canonical dataclasses (`FoodRecord`, `PortionRecord`) matching FoodLab Room conventions. |
| [`validator.py`](validator.py) | **Mandatory Macro Gate**: rejects missing macros, negative numbers, macro sums $>102$g, and Atwater hallucinations. |
| [`extract_crea.py`](extract_crea.py) | Ingestion of official Italian nutrition tables (CREA and BDA-IEO) and serving portions. |
| [`extract_off.py`](extract_off.py) | Low-memory streaming ingestion for Open Food Facts Italian products (`.jsonl`, `.jsonl.gz`, `.json`). |
| [`scraper_scrapling.py`](scraper_scrapling.py) | Supermarket crawler with CSS/XPath adaptors, JSON-LD Schema.org parsing, and HTML table extraction. |
| [`compiler.py`](compiler.py) | Master SQLite compiler, source precedence deduplication, FTS5 virtual table, and Parquet exporter. |
| [`main.py`](main.py) | CLI pipeline orchestrator with progress logging, source dispatching, and comparison reports. |

---

## Installation & Setup

Ensure Python 3.10+ is installed. Install the dataset builder dependencies:

```bash
# Basic installation
pip install pytest requests

# Full installation with high-performance parsing and Parquet export
pip install pyarrow beautifulsoup4 scrapling
```

---

## CLI Usage Guide

Run the pipeline using Python's module syntax:

```bash
python -m tools.dataset_builder.main [OPTIONS]
```

### CLI Options

| Flag | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `--sources` | `str` | `crea,off,scrapling` | Comma-separated list of sources to run (`crea`, `off`, `scrapling`). |
| `--crea-path` | `str` | `app/src/main/assets/data/crea_foods.json` | Path to CREA / BDA-IEO JSON data file. |
| `--off-path` | `str` | `None` | Path or HTTP URL to Open Food Facts dump (`.jsonl`, `.jsonl.gz`, `.json`). |
| `--scrapling-target`| `str` | `None` | Path to local directory of HTML/JSON files or starting URL for web crawling. |
| `--output-db` | `str` | `foodlab_dataset.db` | Destination path for the compiled SQLite database. |
| `--output-parquet`| `str` | `None` | Optional destination path for Apache Parquet dataset export. |
| `--limit` | `int` | `None` | Optional maximum record limit per source (ideal for quick verification). |
| `-v`, `--verbose` | `flag` | `False` | Enables detailed debug log output. |

---

## Practical Examples

### 1. Fast Build: CREA Institutional Tables Only
Uses the verified Italian CREA baseline included in FoodLab:

```bash
python -m tools.dataset_builder.main \
  --sources crea \
  --output-db foodlab_dataset.db
```

### 2. Multi-Source Pipeline with Open Food Facts
Streams Italian products from an Open Food Facts gzip dump:

```bash
python -m tools.dataset_builder.main \
  --sources crea,off \
  --off-path path/to/openfoodfacts-italy.jsonl.gz \
  --output-db foodlab_dataset.db
```

### 3. Supermarket Crawling + Parquet Export
Ingests CREA, local supermarket HTML pages, and exports both SQLite and Parquet:

```bash
python -m tools.dataset_builder.main \
  --sources crea,scrapling \
  --scrapling-target data/supermarket_html/ \
  --output-db foodlab_dataset.db \
  --output-parquet foodlab_dataset.parquet
```

### 4. Quick Test Run with Record Limits
Limits ingestion to 100 items per source for rapid validation:

```bash
python -m tools.dataset_builder.main \
  --sources crea,off,scrapling \
  --off-path data/off_sample.json \
  --scrapling-target data/supermarket_sample/ \
  --output-db test_dataset.db \
  --limit 100
```

---

## Open Food Facts vs FoodLab Quality Guarantee

Open Food Facts is a valuable community initiative, but raw Open Food Facts dumps suffer from severe quality and consistency issues when used directly in a nutrition tracking application:

1. **Crowdsourced Missing Macros**: A large percentage of Italian products in Open Food Facts are submitted with incomplete nutrition labels (e.g. only calories, or calories + protein, without carbs or fats).
2. **Physically Impossible Values**: Products frequently contain typos such as `fat: 80g`, `carbs: 80g`, `protein: 80g` on a 100g basis ($\Sigma > 102$g), or negative energy values.
3. **Severe Atwater Hallucinations**: Products listing 0 kcal despite containing 50g of fat.

### How FoodLab Resolves This:
- **Mandatory Macro Gate**: Every single food in FoodLab is guaranteed to have valid, non-null values for **Calories**, **Protein**, **Carbohydrates**, and **Fat**.
- **Physical Sum Check**: The sum of macronutrients cannot exceed $102.0\text{ g}$ per $100\text{ g}$ (accounting for normal measurement tolerance).
- **Source Precedence**: Where institutional data exists (CREA), it automatically takes precedence over crowdsourced entries for the same barcode or product.
- **Portions & Serving Sizes**: FoodLab attaches standardized portion sizes (e.g. `1 porzione (80g)`, `1 vasetto (125g)`) directly linked via foreign keys.

At the end of every pipeline run, `main.py` outputs a side-by-side comparison table detailing total evaluated products vs items rejected by the quality gate.

---

## SQLite Database Schema

The output SQLite database is engineered for high-performance offline lookup:

### `foods` Table
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
    source TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_foods_barcode ON foods(barcode);
CREATE INDEX idx_foods_name ON foods(name);
CREATE INDEX idx_foods_brand ON foods(brand);
```

### `food_portions` Table
```sql
CREATE TABLE food_portions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    weight_grams REAL NOT NULL
);
CREATE INDEX idx_food_portions_food_id ON food_portions(food_id);
```

### `foods_fts` FTS5 Full-Text Search
```sql
CREATE VIRTUAL TABLE foods_fts USING fts5(
    name,
    brand,
    content='foods',
    content_rowid='id'
);
```
Automatic `AFTER INSERT`, `AFTER DELETE`, and `AFTER UPDATE` triggers ensure `foods_fts` stays atomically synchronized without manual rebuilds.

---

## Testing

Execute the complete test suite across all pipeline components:

```bash
# Run all tests
python -m pytest tools/dataset_builder/tests/ -v

# Run end-to-end pipeline integration tests only
python -m pytest tools/dataset_builder/tests/test_e2e_pipeline.py -v
```
