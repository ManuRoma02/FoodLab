"""Tests for Master Compiler, Deduplication & SQLite + FTS5 Generation."""

import sqlite3
import tempfile
from pathlib import Path

import pytest

from tools.dataset_builder.compiler import (
    DatasetCompiler,
    export_parquet,
    sanitize_fts_query,
)
from tools.dataset_builder.models import FoodRecord, PortionRecord


def test_compiler_deduplicates_and_indexes():
    foods = [
        FoodRecord(
            name="Spaghetti",
            brand="Barilla",
            category="Pasta",
            barcode="8076809513753",
            calories_100g=359,
            protein_100g=12.5,
            carbs_100g=71.2,
            fat_100g=2.0,
            sugars_100g=3.5,
            saturated_fat_100g=0.5,
            polyols_100g=None,
            fiber_100g=3.0,
            salt_100g=0.013,
            sodium_100g=None,
            default_serving_size=80,
            source="CREA",
        ),
        FoodRecord(
            name="Spaghetti N.5",
            brand="Barilla",
            category="Pasta",
            barcode="8076809513753",
            calories_100g=359,
            protein_100g=12.5,
            carbs_100g=71.2,
            fat_100g=2.0,
            sugars_100g=3.5,
            saturated_fat_100g=0.5,
            polyols_100g=None,
            fiber_100g=3.0,
            salt_100g=0.013,
            sodium_100g=None,
            default_serving_size=80,
            source="OFF_IT",
        ),  # Duplicate barcode -> CREA must win
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        compiler = DatasetCompiler(db_path)
        count_foods, count_portions = compiler.compile(foods, [])
        assert count_foods == 1
        assert count_portions == 0

        # Test FTS5 instant search
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT name, brand FROM foods_fts WHERE foods_fts MATCH 'Spaghetti'")
        results = cursor.fetchall()
        assert len(results) == 1
        assert results[0][0] == "Spaghetti"
        conn.close()
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_deduplication_priority_crea_over_scrapling_over_off():
    # 3 records with the same barcode across OFF, SCRAPLING, and CREA
    off_item = FoodRecord(
        name="Biscotti OFF",
        brand="Mulino Bianco",
        barcode="8000500123456",
        calories_100g=420,
        protein_100g=7.0,
        carbs_100g=70.0,
        fat_100g=12.0,
        source="OFF_IT",
    )
    scrapling_item = FoodRecord(
        name="Biscotti Scrapling",
        brand="Mulino Bianco",
        barcode="8000500123456",
        calories_100g=430,
        protein_100g=7.5,
        carbs_100g=68.0,
        fat_100g=14.0,
        source="SCRAPLING_GDO",
    )
    crea_item = FoodRecord(
        name="Biscotti Frollini CREA",
        brand="Mulino Bianco",
        barcode="8000500123456",
        calories_100g=426,
        protein_100g=7.2,
        carbs_100g=71.7,
        fat_100g=13.8,
        source="CREA",
    )

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        # Pass OFF first, then Scrapling, then CREA
        compiler = DatasetCompiler(db_path)
        count_foods, _ = compiler.compile([off_item, scrapling_item, crea_item], [])
        assert count_foods == 1

        conn = sqlite3.connect(db_path)
        cur = conn.cursor()
        cur.execute("SELECT name, source FROM foods WHERE barcode = '8000500123456'")
        row = cur.fetchone()
        assert row is not None
        assert row[0] == "Biscotti Frollini CREA"
        assert row[1] == "CREA"
        conn.close()

        # Test SCRAPLING wins over OFF when CREA is absent
        with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f2:
            db_path2 = f2.name
        try:
            compiler2 = DatasetCompiler(db_path2)
            c2, _ = compiler2.compile([off_item, scrapling_item], [])
            assert c2 == 1
            conn2 = sqlite3.connect(db_path2)
            cur2 = conn2.cursor()
            cur2.execute("SELECT name, source FROM foods WHERE barcode = '8000500123456'")
            row2 = cur2.fetchone()
            assert row2[0] == "Biscotti Scrapling"
            assert "SCRAPLING" in row2[1]
            conn2.close()
        finally:
            Path(db_path2).unlink(missing_ok=True)
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_deduplication_without_barcode():
    # Deduplication for generic foods without barcode (matching normalized brand and name)
    foods = [
        FoodRecord(
            name="Farro perlato, crudo",
            brand="",
            category="Cereali",
            barcode="",
            calories_100g=353,
            protein_100g=14.6,
            carbs_100g=69.3,
            fat_100g=2.4,
            source="openfoodfacts",
        ),
        FoodRecord(
            name="Farro perlato, crudo",
            brand="",
            category="CREA - Cereali e derivati",
            barcode="",
            calories_100g=353,
            protein_100g=14.6,
            carbs_100g=69.3,
            fat_100g=2.4,
            source="CREA",
        ),
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        compiler = DatasetCompiler(db_path)
        count_foods, _ = compiler.compile(foods)
        assert count_foods == 1

        conn = sqlite3.connect(db_path)
        cur = conn.cursor()
        cur.execute("SELECT name, source, category FROM foods")
        row = cur.fetchone()
        assert row is not None
        assert row[0] == "Farro perlato, crudo"
        assert row[1] == "CREA"
        conn.close()
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_portion_linking_with_food_id():
    foods = [
        FoodRecord(
            name="Pasta di semola",
            brand="De Cecco",
            category="Pasta",
            barcode="8001250120128",
            calories_100g=351,
            protein_100g=14.0,
            carbs_100g=69.0,
            fat_100g=1.5,
            source="CREA",
        ),
        FoodRecord(
            name="Mela Golden",
            brand="",
            category="Frutta",
            barcode="",
            calories_100g=46,
            protein_100g=0.4,
            carbs_100g=10.7,
            fat_100g=0.1,
            source="CREA",
        ),
    ]
    portions = [
        # Link via barcode
        PortionRecord(
            food_barcode_or_key="8001250120128",
            name="Piatto standard",
            weight_grams=80.0,
        ),
        PortionRecord(
            food_barcode_or_key="8001250120128",
            name="Porzione abbondante",
            weight_grams=120.0,
        ),
        # Link via food name
        PortionRecord(
            food_barcode_or_key="Mela Golden",
            name="1 mela media",
            weight_grams=150.0,
        ),
        # Unlinkable portion should be ignored without error
        PortionRecord(
            food_barcode_or_key="UnknownNonExistentKey",
            name="Ghost portion",
            weight_grams=50.0,
        ),
    ]

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        compiler = DatasetCompiler(db_path)
        count_foods, count_portions = compiler.compile(foods, portions)
        assert count_foods == 2
        assert count_portions == 3

        conn = sqlite3.connect(db_path)
        cur = conn.cursor()

        # Check pasta portions
        cur.execute(
            """
            SELECT p.name, p.weight_grams
            FROM food_portions p
            JOIN foods f ON p.food_id = f.id
            WHERE f.barcode = '8001250120128'
            ORDER BY p.weight_grams ASC
            """
        )
        pasta_portions = cur.fetchall()
        assert len(pasta_portions) == 2
        assert pasta_portions[0] == ("Piatto standard", 80.0)
        assert pasta_portions[1] == ("Porzione abbondante", 120.0)

        # Check apple portion linked by name
        cur.execute(
            """
            SELECT p.name, p.weight_grams
            FROM food_portions p
            JOIN foods f ON p.food_id = f.id
            WHERE f.name = 'Mela Golden'
            """
        )
        apple_portions = cur.fetchall()
        assert len(apple_portions) == 1
        assert apple_portions[0] == ("1 mela media", 150.0)

        conn.close()
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_fts5_safe_search_special_characters():
    assert sanitize_fts_query("Coca-Cola") == '"Coca"* "Cola"*'
    assert sanitize_fts_query('Pasta "Barilla" (N.5)') == '"Pasta"* "Barilla"* "N"* "5"*'
    assert sanitize_fts_query("AND OR NOT") == '"AND"* "OR"* "NOT"*'
    assert sanitize_fts_query("***") == ""

    foods = [
        FoodRecord(
            name="Coca-Cola Original Taste",
            brand="The Coca-Cola Company",
            category="Bevande",
            barcode="5449000000996",
            calories_100g=37,
            protein_100g=0.0,
            carbs_100g=9.0,
            fat_100g=0.0,
            source="SCRAPLING_GDO",
        ),
        FoodRecord(
            name="Nutella & Go!",
            brand="Ferrero",
            category="Snack",
            barcode="8000500199994",
            calories_100g=511,
            protein_100g=7.3,
            carbs_100g=61.7,
            fat_100g=25.4,
            source="OFF_IT",
        ),
    ]

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        compiler = DatasetCompiler(db_path)
        compiler.compile(foods)

        # Direct search with special characters via search helper
        coca_results = compiler.search("Coca-Cola")
        assert len(coca_results) == 1
        assert coca_results[0]["name"] == "Coca-Cola Original Taste"

        nutella_results = compiler.search("Nutella & Go!")
        assert len(nutella_results) == 1
        assert nutella_results[0]["name"] == "Nutella & Go!"

        # Prefix search
        prefix_results = compiler.search("Ferr")
        assert len(prefix_results) == 1
        assert prefix_results[0]["brand"] == "Ferrero"

        # Special characters / booleans that would crash raw FTS
        crash_prone_queries = ['"unclosed', 'AND OR', 'Coca-Cola (Taste)', '***', 'test*']
        for q in crash_prone_queries:
            # Must never raise sqlite3.OperationalError
            res = compiler.search(q)
            assert isinstance(res, list)
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_batch_performance_and_transactional_integrity():
    # Test batch compile of 1000 items
    foods = [
        FoodRecord(
            name=f"Prodotto Test {i:04d}",
            brand="TestBrand",
            category="TestCat",
            barcode=f"800000{i:04d}",
            calories_100g=100 + (i % 200),
            protein_100g=10.0,
            carbs_100g=20.0,
            fat_100g=2.0,
            sugars_100g=5.0,
            source="OFF_IT",
        )
        for i in range(1000)
    ]

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        compiler = DatasetCompiler(db_path)
        count_foods, count_portions = compiler.compile(foods)
        assert count_foods == 1000
        assert count_portions == 0

        # Verify index count
        conn = sqlite3.connect(db_path)
        cur = conn.cursor()
        cur.execute("SELECT count(*) FROM foods")
        assert cur.fetchone()[0] == 1000

        # Verify FTS5 is fully populated
        cur.execute("SELECT count(*) FROM foods_fts")
        assert cur.fetchone()[0] == 1000

        # Query single item via FTS5
        cur.execute("SELECT name FROM foods_fts WHERE foods_fts MATCH '\"Prodotto\"* \"0500\"*'")
        match = cur.fetchall()
        assert len(match) == 1
        assert match[0][0] == "Prodotto Test 0500"
        conn.close()
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_export_parquet():
    foods = [
        FoodRecord(
            name="Spaghetti",
            brand="Barilla",
            category="Pasta",
            barcode="8076809513753",
            calories_100g=359,
            protein_100g=12.5,
            carbs_100g=71.2,
            fat_100g=2.0,
            sugars_100g=3.5,
            saturated_fat_100g=0.5,
            polyols_100g=None,
            fiber_100g=3.0,
            salt_100g=0.013,
            sodium_100g=0.0052,
            default_serving_size=80,
            source="CREA",
        )
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f_db:
        db_path = f_db.name
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".parquet") as f_pq:
        parquet_path = f_pq.name

    try:
        compiler = DatasetCompiler(db_path)
        compiler.compile(foods)

        # Export via standalone helper
        exported_count = export_parquet(db_path, parquet_path)
        assert exported_count == 1
        assert Path(parquet_path).exists()
        assert Path(parquet_path).stat().st_size > 0

        # Verify pyarrow can read the parquet file
        import pyarrow.parquet as pq

        table = pq.read_table(parquet_path)
        assert table.num_rows == 1
        assert "name" in table.column_names
        assert "calories_100g" in table.column_names
        assert table["name"][0].as_py() == "Spaghetti"

        # Export via compiler instance method
        with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".parquet") as f_pq2:
            parquet_path2 = f_pq2.name
        try:
            c2 = compiler.export_parquet(parquet_path2)
            assert c2 == 1
            t2 = pq.read_table(parquet_path2)
            assert t2.num_rows == 1
        finally:
            Path(parquet_path2).unlink(missing_ok=True)
    finally:
        Path(db_path).unlink(missing_ok=True)
        Path(parquet_path).unlink(missing_ok=True)
