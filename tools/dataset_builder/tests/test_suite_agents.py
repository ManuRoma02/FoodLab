"""Tests for Suite Agents: Fitness Extractor, Traditional Dishes, and User Export Ingestion."""

import json
import sqlite3
import tempfile

from tools.dataset_builder.extract_fitness import is_fitness_product
from tools.dataset_builder.extract_traditional_dishes import extract_traditional_dishes
from tools.dataset_builder.ingest_user_export import parse_user_export, merge_user_foods_into_db
from tools.dataset_builder.validator import validate_and_normalize


def test_is_fitness_product_matching():
    assert is_fitness_product("MyProtein")
    assert is_fitness_product("Prozis Sport")
    assert is_fitness_product("Yamamoto Nutrition")
    assert is_fitness_product("Optimum Nutrition Gold Standard")
    assert not is_fitness_product("Yamamotoyama Tea")
    assert not is_fitness_product("Barilla Pasta")
    assert not is_fitness_product("Mulino Bianco")


def test_extract_traditional_dishes_validates_and_has_portions():
    foods, portions = extract_traditional_dishes()
    assert len(foods) >= 30
    assert len(portions) >= 30

    names = {f.name for f in foods}
    assert "Spaghetti alla Carbonara" in names
    assert "Pizza Margherita al Piatto" in names
    assert "Tiramisù Tradizionale al Caffè e Mascarpone" in names

    # All dishes must satisfy the Mandatory Macro Gate
    for f in foods:
        assert f.calories_100g > 0
        assert f.protein_100g >= 0
        assert f.carbs_100g >= 0
        assert f.fat_100g >= 0
        assert (f.protein_100g + f.carbs_100g + f.fat_100g) <= 102.0


def test_user_export_ingestion_e2e():
    mock_user_foods = [
        {
            "name": "Frullato Proteico Banana e Burro d'Arachidi",
            "brand": "Ricetta Utente",
            "barcode": "",
            "caloriesPer100g": 180,
            "proteinPer100g": 15.0,
            "carbsPer100g": 20.0,
            "fatPer100g": 4.0,
            "defaultServingSize": 350,
            "portions": [
                {"name": "Bicchiere grande (350g)", "weightGrams": 350.0}
            ]
        },
        {
            "name": "Biscotto Avena e Cioccolato",
            "brand": "Forno Artigianale",
            "barcode": "8099887766554",
            "caloriesPer100g": 450,
            "proteinPer100g": 8.0,
            "carbsPer100g": 60.0,
            "fatPer100g": 18.0,
            "defaultServingSize": 25,
            "portions": [
                {"name": "1 biscotto (25g)", "weightGrams": 25.0}
            ]
        }
    ]

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(mock_user_foods, f)
        json_path = f.name

    foods, portions = parse_user_export(json_path)
    assert len(foods) == 2
    assert len(portions) == 2

    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as db_file:
        db_path = db_file.name

    conn = sqlite3.connect(db_path)
    conn.execute("""
        CREATE TABLE foods (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL,
            brand TEXT NOT NULL,
            category TEXT NOT NULL,
            barcode TEXT,
            calories_100g REAL NOT NULL,
            protein_100g REAL NOT NULL,
            carbs_100g REAL NOT NULL,
            fat_100g REAL NOT NULL,
            default_serving_size INTEGER NOT NULL,
            source TEXT NOT NULL
        )
    """)
    conn.execute("""
        CREATE TABLE food_portions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            food_id INTEGER NOT NULL,
            name TEXT NOT NULL,
            weight_grams REAL NOT NULL
        )
    """)
    conn.commit()
    conn.close()

    inserted = merge_user_foods_into_db(db_path, foods, portions)
    assert inserted == 2

    conn = sqlite3.connect(db_path)
    c = conn.cursor()
    c.execute("SELECT COUNT(*) FROM foods")
    assert c.fetchone()[0] == 2
    c.execute("SELECT COUNT(*) FROM food_portions")
    assert c.fetchone()[0] == 2
    conn.close()
