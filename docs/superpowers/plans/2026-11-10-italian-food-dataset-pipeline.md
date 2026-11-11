# Italian Food & Nutrition Dataset Pipeline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a robust, offline-first Python data extraction and compilation pipeline that gathers, cleans, validates, and stores Italian foods with barcodes and full macronutrients/micronutrients into a high-performance SQLite database with FTS5 search.

**Architecture:** A modular Python pipeline under `tools/dataset_builder/` featuring discrete extractors (Open Food Facts Italy streamer, CREA/BDA-IEO extractor, and Scrapling stealth crawler for Italian supermarkets). Records pass through a strict `MacroQualityGate` validator (mandatory kcal, protein, carbs, fat; optional sugars, saturated fat, polyols, fiber, salt) and are compiled, deduplicated, and indexed into `foodlab_dataset.db` with SQLite FTS5 for sub-millisecond search.

**Tech Stack:** Python 3.13, Scrapling, Requests / HTTPX, DuckDB / SQLite3 (built-in), Pytest.

**Spec:** [`docs/superpowers/specs/2026-11-10-italian-food-dataset-pipeline-design.md`](file:///c:/Users/manue/Dropbox/FoodLab/docs/superpowers/specs/2026-11-10-italian-food-dataset-pipeline-design.md)

## Global Constraints
- Target directory: `tools/dataset_builder/`.
- Python 3.10+ compatibility.
- Mandatory Macro Gate: Every record must have valid non-negative `calories_100g`, `protein_100g`, `carbs_100g`, and `fat_100g`. If any of the four is missing or invalid, the record must be discarded immediately.
- Optional Nutrients: Capture and preserve `sugars_100g`, `saturated_fat_100g`, `polyols_100g`, `fiber_100g`, `salt_100g`, and `sodium_100g` when available.
- Database Schema: Must output SQLite tables `foods`, `food_portions`, and FTS5 virtual table `foods_fts` matching FoodLab's Room conventions.
- Zero impact on existing Android `app/` code during this dataset pipeline phase.

## Review Focus
1. **Invalid/Hallucinated Macros**: Products where `protein + carbs + fat > 102.0g` or calories deviate drastically from physical energy sums must be rejected.
2. **Missing Primary Macronutrient**: Any food missing protein, carbs, fat, or kcal must be filtered out without throwing an exception.
3. **Empty or Malformed Barcodes**: Barcodes containing non-digit characters or invalid lengths must be sanitized or ignored for barcode index lookups.
4. **Duplicate Products across Sources**: Deduplication must prefer high-fidelity scientific sources (CREA) over scraped/crowdsourced data (OFF) when barcodes or normalized names match.
5. **FTS5 Special Character Crashes**: Search index queries containing punctuation, hyphens, or quotes must not crash SQLite FTS5 syntax parser.

---

### Task 1: Environment, Data Models & Macro Quality Validator

**Files:**
- Create: `tools/dataset_builder/pyproject.toml`
- Create: `tools/dataset_builder/models.py`
- Create: `tools/dataset_builder/validator.py`
- Test: `tools/dataset_builder/tests/test_validator.py`

**Interfaces:**
- Produces:
  - `FoodRecord`: Dataclass representing a normalized food item (`name`, `brand`, `category`, `barcode`, `calories_100g`, `protein_100g`, `carbs_100g`, `fat_100g`, `sugars_100g`, `saturated_fat_100g`, `polyols_100g`, `fiber_100g`, `salt_100g`, `sodium_100g`, `default_serving_size`, `source`).
  - `PortionRecord`: Dataclass for serving sizes (`food_barcode_or_key`, `name`, `weight_grams`).
  - `validate_and_normalize(raw: dict, source: str) -> Optional[FoodRecord]`

- [ ] **Step 1: Write failing tests for MacroQualityGate and normalization**

```python
# tools/dataset_builder/tests/test_validator.py
from tools.dataset_builder.validator import validate_and_normalize

def test_rejects_missing_protein():
    raw = {"name": "Test", "calories": 100, "carbs": 20.0, "fat": 2.0} # protein missing
    assert validate_and_normalize(raw, "TEST") is None

def test_rejects_sum_macros_exceeding_100g():
    raw = {"name": "Test", "calories": 900, "protein": 50.0, "carbs": 50.0, "fat": 50.0} # 150g > 102g
    assert validate_and_normalize(raw, "TEST") is None

def test_accepts_valid_food_with_secondary_nutrients():
    raw = {
        "name": "Biscotti",
        "brand": "Mulino Bianco",
        "barcode": "8076809513753",
        "calories": 450,
        "protein": 7.5,
        "carbs": 68.0,
        "fat": 16.0,
        "sugars": 22.0,
        "saturated_fat": 5.0,
        "polyols": 1.2,
        "fiber": 3.5,
        "salt": 0.4
    }
    food = validate_and_normalize(raw, "OFF_IT")
    assert food is not None
    assert food.name == "Biscotti"
    assert food.calories_100g == 450
    assert food.sugars_100g == 22.0
    assert food.polyols_100g == 1.2
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_validator.py -v`
Expected: FAIL (ModuleNotFoundError or Import failure)

- [ ] **Step 3: Implement `models.py` and `validator.py`**

Define `FoodRecord`, `PortionRecord`, and `validate_and_normalize` handling bounds checking (`0 <= value <= 100`, total macro sanity check, float coercion).

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_validator.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/dataset_builder/
git commit -m "feat(dataset): implement core food models and macro quality validator"
```

---

### Task 2: CREA & BDA-IEO Ingestion Module

**Files:**
- Create: `tools/dataset_builder/extract_crea.py`
- Test: `tools/dataset_builder/tests/test_extract_crea.py`

**Interfaces:**
- Consumes: `FoodRecord`, `PortionRecord`, `validate_and_normalize` from `validator.py`.
- Produces: `extract_crea_foods(json_path: str) -> tuple[list[FoodRecord], list[PortionRecord]]`

- [ ] **Step 1: Write failing test for CREA ingestion**

```python
# tools/dataset_builder/tests/test_extract_crea.py
import json
import tempfile
from tools.dataset_builder.extract_crea import extract_crea_foods

def test_extract_crea_foods_parses_portions_and_macros():
    sample_data = [
        {"n": "Farro perlato, crudo", "c": "CREA - Cereali", "k": 353, "p": 14.6, "cb": 69.3, "f": 2.4, "pt": 80},
        {"n": "Olio extravergine", "c": "CREA - Grassi", "k": 899, "p": 0.0, "cb": 0.0, "f": 99.9, "pt": 10}
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name
    
    foods, portions = extract_crea_foods(path)
    assert len(foods) == 2
    assert foods[0].name == "Farro perlato, crudo"
    assert foods[0].protein_100g == 14.6
    assert len(portions) == 2
    assert portions[0].weight_grams == 80.0
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_extract_crea.py -v`
Expected: FAIL

- [ ] **Step 3: Implement `extract_crea.py`**

Support minified keys (`n`, `c`, `k`, `p`, `cb`, `f`, `pt`) and standard CREA schema keys, creating `FoodRecord` and linked `PortionRecord`.

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_extract_crea.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/dataset_builder/extract_crea.py tools/dataset_builder/tests/test_extract_crea.py
git commit -m "feat(dataset): implement CREA and BDA-IEO data extractor"
```

---

### Task 3: Open Food Facts Italy Stream Ingestion Module

**Files:**
- Create: `tools/dataset_builder/extract_off.py`
- Test: `tools/dataset_builder/tests/test_extract_off.py`

**Interfaces:**
- Consumes: `FoodRecord`, `validate_and_normalize`.
- Produces: `stream_off_italian_products(file_or_url: str, limit: Optional[int] = None) -> Iterator[FoodRecord]`

- [ ] **Step 1: Write failing test for OFF Italy parsing**

```python
# tools/dataset_builder/tests/test_extract_off.py
import json
import gzip
import tempfile
from tools.dataset_builder.extract_off import parse_off_product_json

def test_parse_off_product_filters_and_extracts_macros():
    valid_off_raw = {
        "code": "8000500310427",
        "product_name": "Nutella",
        "brands": "Ferrero",
        "countries_tags": ["en:italy"],
        "nutriments": {
            "energy-kcal_100g": 539,
            "proteins_100g": 6.3,
            "carbohydrates_100g": 57.5,
            "fat_100g": 30.9,
            "sugars_100g": 56.3,
            "saturated-fat_100g": 10.6,
            "fiber_100g": 3.0,
            "salt_100g": 0.107
        }
    }
    record = parse_off_product_json(valid_off_raw)
    assert record is not None
    assert record.barcode == "8000500310427"
    assert record.name == "Nutella"
    assert record.sugars_100g == 56.3

def test_parse_off_product_rejects_missing_macros():
    missing_macros = {
        "code": "8000000000001",
        "product_name": "Prodotto Incompleto",
        "countries_tags": ["en:italy"],
        "nutriments": {"energy-kcal_100g": 200}
    }
    assert parse_off_product_json(missing_macros) is None
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_extract_off.py -v`
Expected: FAIL

- [ ] **Step 3: Implement `extract_off.py`**

Implement streaming reader (JSON Lines `.jsonl.gz` / iterator) and `parse_off_product_json` mapping OFF's `nutriments` dictionary into `FoodRecord`.

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_extract_off.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/dataset_builder/extract_off.py tools/dataset_builder/tests/test_extract_off.py
git commit -m "feat(dataset): implement Open Food Facts Italy streaming extractor"
```

---

### Task 4: Scrapling Italian Supermarket Crawler

**Files:**
- Create: `tools/dataset_builder/scraper_scrapling.py`
- Test: `tools/dataset_builder/tests/test_scraper_scrapling.py`

**Interfaces:**
- Consumes: `FoodRecord`, `validate_and_normalize`.
- Produces: `SupermarketSpider(base_url, parser_fn)` and `scrape_supermarket_catalog(target: str, limit: int = 50) -> list[FoodRecord]`

- [ ] **Step 1: Write failing test for Scrapling parser mapping**

```python
# tools/dataset_builder/tests/test_scraper_scrapling.py
from tools.dataset_builder.scraper_scrapling import parse_supermarket_html_or_json

def test_parse_supermarket_product_extracts_correct_fields():
    mock_item = {
        "title": "Pasta di Semola Spaghetti N.5",
        "brand": "Barilla",
        "barcode": "8076809513753",
        "nutrition_table": {
            "Energia": "359 kcal",
            "Grassi": "2.0 g",
            "di cui acidi grassi saturi": "0.5 g",
            "Carboidrati": "71.2 g",
            "di cui zuccheri": "3.5 g",
            "Fibre": "3.0 g",
            "Proteine": "12.5 g",
            "Sale": "0.013 g"
        }
    }
    record = parse_supermarket_html_or_json(mock_item, source="SCRAPLING_GDO")
    assert record is not None
    assert record.calories_100g == 359
    assert record.protein_100g == 12.5
    assert record.saturated_fat_100g == 0.5
    assert record.source == "SCRAPLING_GDO"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_scraper_scrapling.py -v`
Expected: FAIL

- [ ] **Step 3: Implement `scraper_scrapling.py`**

Integrate `scrapling` (or fallback stealth fetcher with custom user-agents), italian nutrition label regex/text extractor (handling `"di cui zuccheri"`, `"di cui grassi saturi"`, etc.), and structured `FoodRecord` builder.

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_scraper_scrapling.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/dataset_builder/scraper_scrapling.py tools/dataset_builder/tests/test_scraper_scrapling.py
git commit -m "feat(dataset): implement Scrapling supermarket crawler and label parser"
```

---

### Task 5: Master Compiler, Deduplication & SQLite + FTS5 Generation

**Files:**
- Create: `tools/dataset_builder/compiler.py`
- Test: `tools/dataset_builder/tests/test_compiler.py`

**Interfaces:**
- Consumes: `Iterable[FoodRecord]`, `Iterable[PortionRecord]`.
- Produces: `DatasetCompiler(db_path: str)` with `compile(foods, portions) -> tuple[int, int]` (returns total foods and portions inserted).

- [ ] **Step 1: Write failing test for SQLite compilation and FTS5 indexing**

```python
# tools/dataset_builder/tests/test_compiler.py
import sqlite3
import tempfile
from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.compiler import DatasetCompiler

def test_compiler_deduplicates_and_indexes():
    foods = [
        FoodRecord(
            name="Spaghetti", brand="Barilla", category="Pasta", barcode="8076809513753",
            calories_100g=359, protein_100g=12.5, carbs_100g=71.2, fat_100g=2.0,
            sugars_100g=3.5, saturated_fat_100g=0.5, polyols_100g=None, fiber_100g=3.0,
            salt_100g=0.013, sodium_100g=None, default_serving_size=80, source="CREA"
        ),
        FoodRecord(
            name="Spaghetti N.5", brand="Barilla", category="Pasta", barcode="8076809513753",
            calories_100g=359, protein_100g=12.5, carbs_100g=71.2, fat_100g=2.0,
            sugars_100g=3.5, saturated_fat_100g=0.5, polyols_100g=None, fiber_100g=3.0,
            salt_100g=0.013, sodium_100g=None, default_serving_size=80, source="OFF_IT"
        ) # Duplicate barcode -> CREA must win
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name
    
    compiler = DatasetCompiler(db_path)
    count_foods, _ = compiler.compile(foods, [])
    assert count_foods == 1
    
    # Test FTS5 instant search
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT name, brand FROM foods_fts WHERE foods_fts MATCH 'Spaghetti'")
    results = cursor.fetchall()
    assert len(results) == 1
    assert results[0][0] == "Spaghetti"
    conn.close()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_compiler.py -v`
Expected: FAIL

- [ ] **Step 3: Implement `compiler.py`**

Implement schema creation (`foods`, `food_portions`, indexes, `foods_fts` FTS5 virtual table + triggers), deduplication logic (barcode unique index, source precedence: `CREA > SCRAPLING > OFF`), and batch inserts with transactions for peak performance.

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_compiler.py -v`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add tools/dataset_builder/compiler.py tools/dataset_builder/tests/test_compiler.py
git commit -m "feat(dataset): implement SQLite compiler with FTS5 virtual search table"
```

---

### Task 6: CLI Pipeline Orchestrator & End-to-End Integration

**Files:**
- Create: `tools/dataset_builder/main.py`
- Create: `tools/dataset_builder/README.md`
- Test: `tools/dataset_builder/tests/test_e2e_pipeline.py`

**Interfaces:**
- Produces: CLI script `python -m tools.dataset_builder.main [--sources crea,off,scrapling] [--output path.db] [--limit N]`.

- [ ] **Step 1: Write integration test for CLI pipeline runner**

```python
# tools/dataset_builder/tests/test_e2e_pipeline.py
import subprocess
import sys
import tempfile
import sqlite3

def test_cli_pipeline_execution():
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    cmd = [
        sys.executable, "-m", "tools.dataset_builder.main",
        "--sources", "crea",
        "--output-db", db_path,
        "--limit", "20"
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    assert res.returncode == 0
    assert "Dataset compilation complete" in res.stdout

    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()
    cursor.execute("SELECT COUNT(*) FROM foods")
    count = cursor.fetchone()[0]
    assert count > 0
    conn.close()
```

- [ ] **Step 2: Run test to verify it fails**

Run: `pytest tools/dataset_builder/tests/test_e2e_pipeline.py -v`
Expected: FAIL

- [ ] **Step 3: Implement `main.py` and `README.md`**

Add argument parsing (`argparse`), source execution dispatching, logging with progress counters (`tqdm` or standard output), and documentation on running the scraper.

- [ ] **Step 4: Run test to verify it passes**

Run: `pytest tools/dataset_builder/tests/test_e2e_pipeline.py -v`
Expected: PASS

- [ ] **Step 5: Run full test suite across `tools/dataset_builder/`**

Run: `pytest tools/dataset_builder/tests/ -v`
Expected: ALL PASS

- [ ] **Step 6: Commit**

```bash
git add tools/dataset_builder/main.py tools/dataset_builder/README.md tools/dataset_builder/tests/
git commit -m "feat(dataset): complete dataset builder pipeline orchestrator and CLI"
```
