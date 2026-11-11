"""Tests for Open Food Facts Italy streaming ingestion module."""

import gzip
import json
import tempfile
from pathlib import Path
import pytest

from tools.dataset_builder.extract_off import (
    parse_off_product_json,
    stream_off_italian_products,
)


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
            "salt_100g": 0.107,
        },
    }
    record = parse_off_product_json(valid_off_raw)
    assert record is not None
    assert record.barcode == "8000500310427"
    assert record.name == "Nutella"
    assert record.brand == "Ferrero"
    assert record.calories_100g == 539
    assert record.protein_100g == 6.3
    assert record.carbs_100g == 57.5
    assert record.fat_100g == 30.9
    assert record.sugars_100g == 56.3
    assert record.source == "openfoodfacts"


def test_parse_off_product_rejects_missing_macros():
    missing_macros = {
        "code": "8000000000001",
        "product_name": "Prodotto Incompleto",
        "countries_tags": ["en:italy"],
        "nutriments": {"energy-kcal_100g": 200},
    }
    assert parse_off_product_json(missing_macros) is None


def test_parse_off_product_rejects_non_italian():
    french_product = {
        "code": "3017620422003",
        "product_name": "Nutella France",
        "countries_tags": ["en:france"],
        "lang": "fr",
        "nutriments": {
            "energy-kcal_100g": 539,
            "proteins_100g": 6.3,
            "carbohydrates_100g": 57.5,
            "fat_100g": 30.9,
        },
    }
    # Should reject when filtering for Italy
    assert parse_off_product_json(french_product, filter_italian=True) is None
    # Should parse when filter_italian is disabled
    record = parse_off_product_json(french_product, filter_italian=False)
    assert record is not None
    assert record.barcode == "3017620422003"


def test_parse_off_product_accepts_italian_indicators():
    base = {
        "code": "8000000000002",
        "product_name": "Prodotto Italiano",
        "nutriments": {
            "energy-kcal_100g": 100,
            "proteins_100g": 5.0,
            "carbohydrates_100g": 10.0,
            "fat_100g": 2.0,
        },
    }

    # Case 1: countries contains "Italia"
    case_country = dict(base, countries="Italia, San Marino")
    assert parse_off_product_json(case_country) is not None

    # Case 2: lang is "it"
    case_lang = dict(base, lang="it")
    assert parse_off_product_json(case_lang) is not None

    # Case 3: languages_tags has "en:italian"
    case_ltag = dict(base, languages_tags=["en:italian"])
    assert parse_off_product_json(case_ltag) is not None

    # Case 4: countries_tags has "it"
    case_ctag = dict(base, countries_tags=["it"])
    assert parse_off_product_json(case_ctag) is not None


def test_parse_off_product_prefers_localized_italian_name():
    raw = {
        "code": "8000000000003",
        "product_name": "Generic English Name",
        "product_name_it": "Nome Specifico Italiano",
        "countries_tags": ["en:italy"],
        "nutriments": {
            "energy-kcal_100g": 150,
            "proteins_100g": 10.0,
            "carbohydrates_100g": 15.0,
            "fat_100g": 3.0,
        },
    }
    record = parse_off_product_json(raw)
    assert record is not None
    assert record.name == "Nome Specifico Italiano"


def test_parse_off_product_energy_kj_conversion():
    # 2255 kJ / 4.184 = 538.95 -> 539 kcal
    raw = {
        "code": "8000000000004",
        "product_name": "Prodotto con solo kJ",
        "countries_tags": ["en:italy"],
        "nutriments": {
            "energy_100g": 2255,
            "energy_unit": "kJ",
            "proteins_100g": 6.3,
            "carbohydrates_100g": 57.5,
            "fat_100g": 30.9,
        },
    }
    record = parse_off_product_json(raw)
    assert record is not None
    assert record.calories_100g == 539


def test_parse_off_product_zero_energy_kcal_not_falsy():
    # Water or zero calorie drink with energy_100g = 0
    raw = {
        "code": "8000000000005",
        "product_name": "Acqua Minerale Naturale",
        "countries_tags": ["en:italy"],
        "nutriments": {
            "energy_100g": 0,
            "energy_unit": "kcal",
            "proteins_100g": 0.0,
            "carbohydrates_100g": 0.0,
            "fat_100g": 0.0,
        },
    }
    record = parse_off_product_json(raw)
    assert record is not None
    assert record.calories_100g == 0



def test_stream_off_italian_products_jsonl_gz():
    items = [
        # Valid Italian item 1
        {
            "code": "8000500310427",
            "product_name": "Nutella",
            "countries_tags": ["en:italy"],
            "nutriments": {
                "energy-kcal_100g": 539,
                "proteins_100g": 6.3,
                "carbohydrates_100g": 57.5,
                "fat_100g": 30.9,
            },
        },
        # Non-Italian item (should be skipped)
        {
            "code": "3017620422003",
            "product_name": "French Product",
            "countries_tags": ["en:france"],
            "lang": "fr",
            "nutriments": {
                "energy-kcal_100g": 500,
                "proteins_100g": 5.0,
                "carbohydrates_100g": 50.0,
                "fat_100g": 20.0,
            },
        },
        # Invalid item (missing carbs, should be skipped)
        {
            "code": "8000000000005",
            "product_name": "Incompleto",
            "countries_tags": ["en:italy"],
            "nutriments": {
                "energy-kcal_100g": 200,
                "proteins_100g": 10.0,
            },
        },
        # Valid Italian item 2
        {
            "code": "8000000000006",
            "product_name": "Pasta Barilla",
            "brands": "Barilla",
            "countries_tags": ["en:italy"],
            "nutriments": {
                "energy-kcal_100g": 359,
                "proteins_100g": 12.5,
                "carbohydrates_100g": 71.2,
                "fat_100g": 2.0,
                "fiber_100g": 3.0,
            },
        },
    ]

    with tempfile.NamedTemporaryFile(suffix=".jsonl.gz", delete=False) as f:
        gz_path = f.name

    try:
        with gzip.open(gz_path, "wt", encoding="utf-8") as gz_file:
            for item in items:
                gz_file.write(json.dumps(item) + "\n")
            # Also write a corrupted line to ensure parser resilience
            gz_file.write("{ corrupted line ...\n")

        records = list(stream_off_italian_products(gz_path))
        assert len(records) == 2
        assert records[0].name == "Nutella"
        assert records[0].barcode == "8000500310427"
        assert records[1].name == "Pasta Barilla"
        assert records[1].barcode == "8000000000006"
        assert records[1].fiber_100g == 3.0

    finally:
        Path(gz_path).unlink(missing_ok=True)


def test_stream_off_italian_products_with_limit():
    items = [
        {
            "code": f"80000000000{i}",
            "product_name": f"Prodotto {i}",
            "countries_tags": ["en:italy"],
            "nutriments": {
                "energy-kcal_100g": 100 + i,
                "proteins_100g": 5.0,
                "carbohydrates_100g": 10.0,
                "fat_100g": 2.0,
            },
        }
        for i in range(5)
    ]

    with tempfile.NamedTemporaryFile(suffix=".jsonl", delete=False, mode="w", encoding="utf-8") as f:
        for item in items:
            f.write(json.dumps(item) + "\n")
        jsonl_path = f.name

    try:
        records = list(stream_off_italian_products(jsonl_path, limit=2))
        assert len(records) == 2
        assert records[0].barcode == "800000000000"
        assert records[1].barcode == "800000000001"
    finally:
        Path(jsonl_path).unlink(missing_ok=True)


def test_stream_off_italian_products_json_array():
    items = [
        {
            "code": "8000000000099",
            "product_name": "Prodotto in JSON Array",
            "countries_tags": ["en:italy"],
            "nutriments": {
                "energy-kcal_100g": 250,
                "proteins_100g": 8.0,
                "carbohydrates_100g": 30.0,
                "fat_100g": 10.0,
            },
        }
    ]

    with tempfile.NamedTemporaryFile(suffix=".json", delete=False, mode="w", encoding="utf-8") as f:
        json.dump(items, f)
        json_path = f.name

    try:
        records = list(stream_off_italian_products(json_path))
        assert len(records) == 1
        assert records[0].name == "Prodotto in JSON Array"
        assert records[0].barcode == "8000000000099"
    finally:
        Path(json_path).unlink(missing_ok=True)


def test_stream_off_file_not_found():
    with pytest.raises(FileNotFoundError):
        list(stream_off_italian_products("non_existent_file_path_xyz.jsonl"))


def test_stream_off_italian_products_csv_gz():
    header = [
        "code", "product_name", "brands", "stores", "categories", "countries_tags",
        "serving_size", "energy-kcal_100g", "fat_100g", "saturated-fat_100g",
        "carbohydrates_100g", "sugars_100g", "proteins_100g", "salt_100g", "fiber_100g"
    ]
    rows = [
        # 1. Barilla Pasta
        ["8000500310427", "Spaghetti n.5", "Barilla", "Esselunga", "Pasta", "en:italy", "80g", "359", "2.0", "0.5", "71.2", "3.5", "12.5", "0.01", "3.0"],
        # 2. Esselunga Smart Latte
        ["200000000001", "Latte Parzialmente Scremato", "Smart", "Esselunga", "Latticini", "en:italy", "200ml", "46", "1.5", "1.0", "4.9", "4.9", "3.2", "0.1", "0.0"],
        # 3. Lidl Milbona Yogurt
        ["200000000002", "Yogurt Greco", "Milbona", "Lidl", "Yogurt", "en:italy", "150g", "59", "0.0", "0.0", "3.6", "3.6", "10.3", "0.1", "0.0"],
        # 4. MD Vivo Meglio Gallette
        ["200000000003", "Gallette di Mais", "Vivo Meglio", "MD", "Snack", "en:italy", "20g", "380", "1.2", "0.2", "82.0", "0.5", "8.0", "0.2", "2.5"],
        # 5. Eurospin Tre Mulini Farina
        ["200000000004", "Farina Tipo 00", "Tre Mulini", "Eurospin", "Farine", "en:italy", "100g", "345", "1.0", "0.2", "72.0", "1.0", "11.0", "0.01", "2.0"],
        # 6. Non-Italian French product (should be rejected)
        ["300000000005", "Fromage Français", "Marque FR", "Carrefour FR", "Fromages", "en:france", "30g", "350", "28.0", "18.0", "0.5", "0.5", "25.0", "1.5", "0.0"],
        # 7. Rejected by Macro Gate (missing protein)
        ["8000500310499", "Prodotto Incompleto", "Barilla", "Coop", "Pasta", "en:italy", "80g", "350", "2.0", "0.5", "70.0", "2.0", "", "0.01", "2.0"]
    ]

    with tempfile.NamedTemporaryFile(suffix=".csv.gz", delete=False) as f:
        gz_path = f.name

    try:
        with gzip.open(gz_path, "wt", encoding="utf-8") as gz:
            gz.write("\t".join(header) + "\n")
            for r in rows:
                gz.write("\t".join(r) + "\n")

        records = list(stream_off_italian_products(gz_path))
        assert len(records) == 5
        names = [r.name for r in records]
        assert "Spaghetti n.5" in names
        assert "Latte Parzialmente Scremato" in names
        assert "Yogurt Greco" in names
        assert "Gallette di Mais" in names
        assert "Farina Tipo 00" in names
        assert records[0].barcode == "8000500310427"
    finally:
        Path(gz_path).unlink(missing_ok=True)

