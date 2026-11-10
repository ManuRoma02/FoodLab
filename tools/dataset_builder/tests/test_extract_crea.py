"""Tests for CREA and BDA-IEO food and portion extractor."""

import json
import os
import tempfile
import pytest
from tools.dataset_builder.extract_crea import extract_crea_foods


def test_extract_crea_foods_parses_portions_and_macros():
    """Verify minified CREA schema parsing with macros and portions."""
    sample_data = [
        {"n": "Farro perlato, crudo", "c": "CREA - Cereali", "k": 353, "p": 14.6, "cb": 69.3, "f": 2.4, "pt": 80},
        {"n": "Olio extravergine", "c": "CREA - Grassi", "k": 899, "p": 0.0, "cb": 0.0, "f": 99.9, "pt": 10},
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name

    try:
        foods, portions = extract_crea_foods(path)
        assert len(foods) == 2
        assert foods[0].name == "Farro perlato, crudo"
        assert foods[0].protein_100g == 14.6
        assert foods[0].category == "CREA - Cereali"
        assert foods[0].source == "CREA"
        assert len(portions) == 2
        assert portions[0].weight_grams == 80.0
        assert portions[0].food_barcode_or_key == "Farro perlato, crudo"
        assert portions[1].weight_grams == 10.0
    finally:
        os.remove(path)


def test_extract_crea_foods_standard_and_bda_schema():
    """Verify standard CREA / BDA-IEO schema with full Italian key names."""
    sample_data = [
        {
            "nome": "Parmigiano Reggiano DOP",
            "categoria": "Formaggi",
            "codice": "8001234567890",
            "energia_kcal": 402,
            "proteine": 32.4,
            "carboidrati": 0.0,
            "lipidi": 29.7,
            "zuccheri": 0.0,
            "acidi_grassi_saturi": 19.6,
            "fibra_alimentare": 0.0,
            "sale": 1.6,
            "peso_porzione": 50,
            "nome_porzione": "Scaglia standard",
        }
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name

    try:
        foods, portions = extract_crea_foods(path, source="BDA-IEO")
        assert len(foods) == 1
        assert foods[0].name == "Parmigiano Reggiano DOP"
        assert foods[0].barcode == "8001234567890"
        assert foods[0].calories_100g == 402
        assert foods[0].protein_100g == 32.4
        assert foods[0].fat_100g == 29.7
        assert foods[0].saturated_fat_100g == 19.6
        assert foods[0].salt_100g == 1.6
        assert foods[0].source == "BDA-IEO"

        assert len(portions) == 1
        assert portions[0].weight_grams == 50.0
        assert portions[0].name == "Scaglia standard"
        assert portions[0].food_barcode_or_key == "8001234567890"
    finally:
        os.remove(path)


def test_extract_crea_foods_nested_container():
    """Verify extraction when foods are enclosed in a root dictionary object."""
    sample_data = {
        "alimenti": [
            {"n": "Mela con buccia", "c": "Frutta", "k": 52, "p": 0.3, "cb": 13.8, "f": 0.2, "pt": 150}
        ]
    }
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name

    try:
        foods, portions = extract_crea_foods(path)
        assert len(foods) == 1
        assert foods[0].name == "Mela con buccia"
        assert len(portions) == 1
        assert portions[0].weight_grams == 150.0
    finally:
        os.remove(path)


def test_extract_crea_foods_skips_invalid_records():
    """Verify that records failing the macro quality gate are cleanly discarded."""
    sample_data = [
        {"n": "Valido", "k": 100, "p": 10.0, "cb": 10.0, "f": 2.0},
        {"n": "Invalido Kcal Negative", "k": -50, "p": 10.0, "cb": 10.0, "f": 2.0},
        {"n": "Invalido No Proteine", "k": 100, "cb": 10.0, "f": 2.0},
        {"n": "Invalido Somma Macro Eccessiva", "k": 500, "p": 60.0, "cb": 50.0, "f": 10.0},
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name

    try:
        foods, portions = extract_crea_foods(path)
        assert len(foods) == 1
        assert foods[0].name == "Valido"
        # No portions defined in sample_data
        assert len(portions) == 0
    finally:
        os.remove(path)


def test_extract_crea_foods_real_assets_file():
    """Verify parser operates on the real app/src/main/assets/data/crea_foods.json asset."""
    asset_path = os.path.join("app", "src", "main", "assets", "data", "crea_foods.json")
    if os.path.exists(asset_path):
        foods, portions = extract_crea_foods(asset_path)
        assert len(foods) >= 800
        assert len(portions) >= 800
        assert all(f.source == "CREA" for f in foods)


def test_extract_crea_portion_regex_gram_priority():
    """Verify gram-unit regex extracts 80g instead of leading '1' in '1 porzione (80g)'."""
    sample_data = [
        {"n": "Pasta", "k": 350, "p": 12.0, "cb": 72.0, "f": 1.5, "pt": "1 porzione (80g)"},
        {"n": "Riso", "k": 350, "p": 7.0, "cb": 78.0, "f": 0.5, "pt": "1 porzione (80 grammi)"},
        {"n": "Pane", "k": 260, "p": 8.0, "cb": 50.0, "f": 1.0, "pt": "50"},
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_data, f)
        path = f.name

    try:
        foods, portions = extract_crea_foods(path)
        assert len(foods) == 3
        assert len(portions) == 3
        assert portions[0].weight_grams == 80.0
        assert portions[1].weight_grams == 80.0
        assert portions[2].weight_grams == 50.0
    finally:
        os.remove(path)

