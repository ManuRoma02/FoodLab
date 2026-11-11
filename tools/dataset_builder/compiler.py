"""Master SQLite compiler, deduplication engine, and FTS5 index generator.

Compiles validated FoodRecords and PortionRecords into an optimized SQLite
database with FTS5 virtual table and triggers for instantaneous offline search.
Also supports optional export to Apache Parquet format.
"""

from __future__ import annotations

import re
import sqlite3
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Set, Tuple, Union

from tools.dataset_builder.models import FoodRecord, PortionRecord

# SQL Schema Definitions
CREATE_FOODS_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS foods (
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
"""

CREATE_INDEXES_SQL = """
CREATE INDEX IF NOT EXISTS idx_foods_barcode ON foods(barcode);
CREATE INDEX IF NOT EXISTS idx_foods_name ON foods(name);
CREATE INDEX IF NOT EXISTS idx_foods_brand ON foods(brand);
"""

CREATE_PORTIONS_TABLE_SQL = """
CREATE TABLE IF NOT EXISTS food_portions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    food_id INTEGER NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    weight_grams REAL NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_food_portions_food_id ON food_portions(food_id);
"""

CREATE_FTS5_TABLE_SQL = """
CREATE VIRTUAL TABLE IF NOT EXISTS foods_fts USING fts5(
    name,
    brand,
    content='foods',
    content_rowid='id'
);
"""

CREATE_FTS5_TRIGGERS_SQL = """
CREATE TRIGGER IF NOT EXISTS foods_ai AFTER INSERT ON foods BEGIN
    INSERT INTO foods_fts(rowid, name, brand) VALUES (new.id, new.name, new.brand);
END;

CREATE TRIGGER IF NOT EXISTS foods_ad AFTER DELETE ON foods BEGIN
    INSERT INTO foods_fts(foods_fts, rowid, name, brand) VALUES('delete', old.id, old.name, old.brand);
END;

CREATE TRIGGER IF NOT EXISTS foods_au AFTER UPDATE ON foods BEGIN
    INSERT INTO foods_fts(foods_fts, rowid, name, brand) VALUES('delete', old.id, old.name, old.brand);
    INSERT INTO foods_fts(rowid, name, brand) VALUES (new.id, new.name, new.brand);
END;
"""

INSERT_FOOD_SQL = """
INSERT INTO foods (
    name, brand, category, barcode, calories_100g, protein_100g, carbs_100g, fat_100g,
    sugars_100g, saturated_fat_100g, polyols_100g, fiber_100g, salt_100g, sodium_100g,
    default_serving_size, source
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
"""

INSERT_PORTION_SQL = """
INSERT INTO food_portions (food_id, name, weight_grams) VALUES (?, ?, ?);
"""


def _normalize_key_text(text: str) -> str:
    """Normalize text for collision matching (lowercase, stripped, collapsed whitespace)."""
    if not text:
        return ""
    cleaned = re.sub(r"[^\w\s]", " ", text.lower())
    return " ".join(cleaned.split())


def _source_priority(source: str) -> int:
    """
    Return numerical priority for food source.
    Precedence hierarchy: CREA / BDA / CIQUAL / CHAINS (3) > SCRAPLING / USDA (2) > OFF (1) > OTHER (0).
    """
    s = (source or "").upper().strip()
    if any(k in s for k in ("CREA", "BDA", "CIQUAL", "CHAIN", "RISTO")):
        return 3
    elif any(k in s for k in ("SCRAP", "SUPERMARKET", "ESSELUNGA", "CONAD", "COOP", "USDA")):
        return 2
    elif "OFF" in s or "OPENFOODFACTS" in s:
        return 1
    return 0


def _record_quality_score(record: FoodRecord) -> Tuple[int, int, int]:
    """Calculate tuple score for record ranking: (priority, nutrient_completeness, meta_presence)."""
    priority = _source_priority(record.source)
    secondary_nutrients = [
        record.sugars_100g,
        record.saturated_fat_100g,
        record.polyols_100g,
        record.fiber_100g,
        record.salt_100g,
        record.sodium_100g,
    ]
    nutrient_completeness = sum(1 for v in secondary_nutrients if v is not None)
    meta_presence = int(bool(record.brand and record.brand.strip())) + int(
        bool(record.category and record.category.strip())
    )
    return (priority, nutrient_completeness, meta_presence)


def _enrich_record(winning: FoodRecord, donor: FoodRecord) -> FoodRecord:
    """Enrich the winning record with any missing secondary/metadata fields from donor."""
    return FoodRecord(
        name=winning.name,
        brand=winning.brand or donor.brand,
        category=winning.category or donor.category,
        barcode=winning.barcode or donor.barcode,
        calories_100g=winning.calories_100g,
        protein_100g=winning.protein_100g,
        carbs_100g=winning.carbs_100g,
        fat_100g=winning.fat_100g,
        sugars_100g=winning.sugars_100g if winning.sugars_100g is not None else donor.sugars_100g,
        saturated_fat_100g=winning.saturated_fat_100g
        if winning.saturated_fat_100g is not None
        else donor.saturated_fat_100g,
        polyols_100g=winning.polyols_100g if winning.polyols_100g is not None else donor.polyols_100g,
        fiber_100g=winning.fiber_100g if winning.fiber_100g is not None else donor.fiber_100g,
        salt_100g=winning.salt_100g if winning.salt_100g is not None else donor.salt_100g,
        sodium_100g=winning.sodium_100g if winning.sodium_100g is not None else donor.sodium_100g,
        default_serving_size=winning.default_serving_size,
        source=winning.source,
    )


def sanitize_fts_query(query: str) -> str:
    """
    Sanitize a user search string into a valid SQLite FTS5 MATCH expression.

    Extracts alphanumeric tokens and encloses them in double quotes with prefix asterisks
    to prevent syntax errors caused by special characters (hyphens, asterisks, boolean terms).
    """
    if not query or not isinstance(query, str):
        return ""
    tokens = re.findall(r"\w+", query)
    if not tokens:
        return ""
    return " ".join(f'"{t}"*' for t in tokens)


def export_parquet(db_path: Union[str, Path], parquet_path: Union[str, Path]) -> int:
    """
    Export the foods table from an existing SQLite database into an Apache Parquet file.
    Prefers pyarrow, falling back to pandas or duckdb if available.
    """
    p_path = Path(parquet_path)
    p_path.parent.mkdir(parents=True, exist_ok=True)

    conn = sqlite3.connect(str(db_path))
    conn.row_factory = sqlite3.Row
    cursor = conn.cursor()
    cursor.execute("SELECT * FROM foods")
    rows = cursor.fetchall()

    if not rows:
        conn.close()
        try:
            import pyarrow as pa
            import pyarrow.parquet as pq

            empty_table = pa.Table.from_arrays(
                [],
                names=[],
            )
            pq.write_table(empty_table, str(p_path))
        except ImportError:
            pass
        return 0

    columns = rows[0].keys()
    data = {col: [row[col] for row in rows] for col in columns}
    conn.close()

    # Try pyarrow
    try:
        import pyarrow as pa
        import pyarrow.parquet as pq

        table = pa.Table.from_pydict(data)
        pq.write_table(table, str(p_path), compression="snappy")
        return len(rows)
    except ImportError:
        pass

    # Try pandas fallback
    try:
        import pandas as pd

        df = pd.DataFrame(data)
        df.to_parquet(str(p_path), index=False)
        return len(rows)
    except ImportError:
        pass

    raise RuntimeError("Neither pyarrow nor pandas is available for Parquet export.")


class DatasetCompiler:
    """
    Master compiler for the Italian food & nutrition dataset.

    Manages deduplication across disparate sources, schema generation,
    fast batch inserts, and FTS5 search indexing.
    """

    def __init__(self, db_path: Union[str, Path]):
        self.db_path = Path(db_path)
        if str(self.db_path) != ":memory:":
            self.db_path.parent.mkdir(parents=True, exist_ok=True)

    def _get_connection(self) -> sqlite3.Connection:
        """Create and configure a high-performance SQLite connection."""
        conn = sqlite3.connect(str(self.db_path))
        conn.execute("PRAGMA journal_mode = WAL;")
        conn.execute("PRAGMA synchronous = NORMAL;")
        conn.execute("PRAGMA foreign_keys = ON;")
        conn.execute("PRAGMA temp_store = MEMORY;")
        return conn

    def _init_schema(self, conn: sqlite3.Connection) -> None:
        """Create tables, indexes, virtual tables, and triggers if they do not exist."""
        cursor = conn.cursor()
        cursor.execute(CREATE_FOODS_TABLE_SQL)
        cursor.executescript(CREATE_INDEXES_SQL)
        cursor.executescript(CREATE_PORTIONS_TABLE_SQL)
        cursor.execute(CREATE_FTS5_TABLE_SQL)
        cursor.executescript(CREATE_FTS5_TRIGGERS_SQL)

    def _deduplicate_foods(self, foods: Iterable[FoodRecord]) -> List[FoodRecord]:
        """
        Deduplicate food records according to source precedence:
        CREA > SCRAPLING > OFF.
        Uses barcode as primary unique identifier; normalized brand + name for unbarcoded items.
        """
        dedup_map: Dict[Tuple[str, ...], FoodRecord] = {}

        for record in foods:
            if not isinstance(record, FoodRecord):
                continue
            if not record.name or not record.name.strip():
                continue

            # Defense-in-depth: ensure mandatory macronutrients are valid and physically consistent
            if (
                record.calories_100g is None
                or record.calories_100g < 0
                or record.calories_100g > 950
                or record.protein_100g is None
                or record.protein_100g < 0
                or record.protein_100g > 100.5
                or record.carbs_100g is None
                or record.carbs_100g < 0
                or record.carbs_100g > 100.5
                or record.fat_100g is None
                or record.fat_100g < 0
                or record.fat_100g > 100.5
                or (record.protein_100g + record.carbs_100g + record.fat_100g > 102.0)
            ):
                continue

            cleaned_bc = "".join(c for c in record.barcode if c.isdigit()) if record.barcode else ""
            if len(cleaned_bc) >= 4:
                dedup_key = ("barcode", cleaned_bc)
            else:
                dedup_key = (
                    "name",
                    _normalize_key_text(record.brand),
                    _normalize_key_text(record.name),
                )

            if dedup_key not in dedup_map:
                dedup_map[dedup_key] = record
            else:
                existing = dedup_map[dedup_key]
                if _record_quality_score(record) > _record_quality_score(existing):
                    dedup_map[dedup_key] = _enrich_record(record, existing)
                else:
                    dedup_map[dedup_key] = _enrich_record(existing, record)

        return list(dedup_map.values())

    def compile(
        self,
        foods: Iterable[FoodRecord],
        portions: Iterable[PortionRecord] = (),
    ) -> Tuple[int, int]:
        """
        Compile validated foods and portions into the SQLite database.

        Performs source-precedence deduplication, batch insertion inside an atomic
        transaction, and links portions by barcode or item name.

        Returns:
            Tuple[int, int]: (total_foods_inserted, total_portions_inserted)
        """
        deduped_foods = self._deduplicate_foods(foods)

        conn = self._get_connection()
        try:
            self._init_schema(conn)
            cursor = conn.cursor()

            # Prepare batch for foods
            food_rows = [
                (
                    f.name.strip(),
                    (f.brand or "").strip(),
                    (f.category or "").strip(),
                    (f.barcode or "").strip(),
                    int(f.calories_100g),
                    float(f.protein_100g),
                    float(f.carbs_100g),
                    float(f.fat_100g),
                    float(f.sugars_100g) if f.sugars_100g is not None else None,
                    float(f.saturated_fat_100g) if f.saturated_fat_100g is not None else None,
                    float(f.polyols_100g) if f.polyols_100g is not None else None,
                    float(f.fiber_100g) if f.fiber_100g is not None else None,
                    float(f.salt_100g) if f.salt_100g is not None else None,
                    float(f.sodium_100g) if f.sodium_100g is not None else None,
                    int(f.default_serving_size),
                    (f.source or "").strip(),
                )
                for f in deduped_foods
            ]

            portion_rows: List[Tuple[int, str, float]] = []

            with conn:
                if food_rows:
                    cursor.executemany(INSERT_FOOD_SQL, food_rows)

                # Link portions if any provided
                portions_list = list(portions)
                if portions_list and food_rows:
                    cursor.execute("SELECT id, barcode, name FROM foods")
                    all_foods = cursor.fetchall()

                    barcode_to_id: Dict[str, int] = {}
                    name_to_id: Dict[str, int] = {}
                    norm_name_to_id: Dict[str, int] = {}
                    all_food_ids: Set[int] = set()

                    for fid, bc, fname in all_foods:
                        all_food_ids.add(fid)
                        if bc:
                            barcode_to_id[bc] = fid
                        if fname:
                            name_to_id[fname] = fid
                            norm_name_to_id[_normalize_key_text(fname)] = fid

                    seen_portions = set()
                    for p in portions_list:
                        if (
                            not isinstance(p, PortionRecord)
                            or not p.name
                            or not p.name.strip()
                            or p.weight_grams <= 0
                        ):
                            continue

                        key = str(p.food_barcode_or_key).strip()
                        food_id: Optional[int] = None

                        if key in barcode_to_id:
                            food_id = barcode_to_id[key]
                        elif key in name_to_id:
                            food_id = name_to_id[key]
                        elif _normalize_key_text(key) in norm_name_to_id:
                            food_id = norm_name_to_id[_normalize_key_text(key)]
                        elif key.isdigit() and int(key) in all_food_ids:
                            food_id = int(key)

                        if food_id is not None:
                            dedup_p_key = (
                                food_id,
                                p.name.strip().lower(),
                                round(float(p.weight_grams), 2),
                            )
                            if dedup_p_key not in seen_portions:
                                seen_portions.add(dedup_p_key)
                                portion_rows.append(
                                    (food_id, p.name.strip(), float(p.weight_grams))
                                )

                    if portion_rows:
                        cursor.executemany(INSERT_PORTION_SQL, portion_rows)

            return (len(food_rows), len(portion_rows))
        finally:
            conn.close()

    def search(self, query: str, limit: int = 50) -> List[Dict[str, Any]]:
        """
        Execute safe FTS5 full-text search against the foods dataset.
        Prevents crashes from special characters and punctuation.
        """
        sanitized = sanitize_fts_query(query)
        if not sanitized:
            return []

        conn = self._get_connection()
        conn.row_factory = sqlite3.Row
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT f.*
                FROM foods_fts fts
                JOIN foods f ON fts.rowid = f.id
                WHERE foods_fts MATCH ?
                ORDER BY rank
                LIMIT ?;
                """,
                (sanitized, limit),
            )
            return [dict(row) for row in cursor.fetchall()]
        finally:
            conn.close()

    def export_parquet(self, parquet_path: Union[str, Path]) -> int:
        """Export compiled dataset to Apache Parquet."""
        return export_parquet(self.db_path, parquet_path)
