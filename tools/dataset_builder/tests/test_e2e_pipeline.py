"""End-to-end integration tests for FoodLab Dataset Builder CLI pipeline."""

from __future__ import annotations

import json
from pathlib import Path
import sqlite3
import subprocess
import sys
import tempfile
import pytest


def test_cli_pipeline_execution():
    """Verify basic CLI execution with CREA source, limit, and database creation."""
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".db") as f:
        db_path = f.name

    try:
        cmd = [
            sys.executable,
            "-m",
            "tools.dataset_builder.main",
            "--sources",
            "crea",
            "--output-db",
            db_path,
            "--limit",
            "20",
        ]
        res = subprocess.run(cmd, capture_output=True, text=True)
        assert res.returncode == 0, f"STDOUT: {res.stdout}\nSTDERR: {res.stderr}"
        assert "Dataset compilation complete" in res.stdout

        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM foods")
        count = cursor.fetchone()[0]
        assert 0 < count <= 20

        # Verify FTS5 virtual table was created and searchable
        cursor.execute("SELECT COUNT(*) FROM foods_fts")
        fts_count = cursor.fetchone()[0]
        assert fts_count == count
        conn.close()
    finally:
        Path(db_path).unlink(missing_ok=True)


def test_cli_multi_source_and_parquet_export():
    """Verify end-to-end pipeline with CREA, OFF, Scrapling, and Parquet export."""
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        db_path = tmp_path / "test_out.db"
        parquet_path = tmp_path / "test_out.parquet"

        # 1. Mock CREA JSON
        crea_file = tmp_path / "mock_crea.json"
        crea_data = [
            {
                "n": "Pasta di Semola",
                "b": "Barilla",
                "c": "Cereali",
                "k": 355,
                "p": 12.5,
                "cb": 71.2,
                "f": 1.5,
                "pt": "80g",
            },
            {
                "n": "Olio Extravergine",
                "b": "Monini",
                "c": "Condimenti",
                "k": 824,
                "p": 0.0,
                "cb": 0.0,
                "f": 91.6,
                "pt": "10g",
            },
        ]
        crea_file.write_text(json.dumps(crea_data), encoding="utf-8")

        # 2. Mock Open Food Facts JSON array (containing 1 clean valid, 1 invalid macro gate, 1 non-Italian)
        off_file = tmp_path / "mock_off.json"
        off_data = [
            # Valid Italian product
            {
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
                },
            },
            # Rejected by Macro Gate (missing protein, carbs, fat)
            {
                "code": "8000000000999",
                "product_name": "Prodotto Incompleto",
                "countries_tags": ["en:italy"],
                "nutriments": {
                    "energy-kcal_100g": 200,
                },
            },
            # Rejected: non-Italian
            {
                "code": "3017620422003",
                "product_name": "Produit Francais",
                "countries_tags": ["en:france"],
                "lang": "fr",
                "nutriments": {
                    "energy-kcal_100g": 400,
                    "proteins_100g": 10.0,
                    "carbohydrates_100g": 50.0,
                    "fat_100g": 15.0,
                },
            },
        ]
        off_file.write_text(json.dumps(off_data), encoding="utf-8")

        # 3. Mock Scrapling Supermarket HTML product file
        scrapling_dir = tmp_path / "supermarket_pages"
        scrapling_dir.mkdir()
        scrapling_html = scrapling_dir / "mozzarella.html"
        scrapling_html.write_text(
            """
            <html>
                <head><title>Mozzarella Fresca 125g</title></head>
                <body>
                    <h1 class="product-title">Mozzarella Fresca</h1>
                    <span class="brand">Santa Lucia</span>
                    <span class="barcode">8001234567890</span>
                    <table>
                        <tr><th>Valori Nutrizionali</th><th>per 100g</th></tr>
                        <tr><td>Energia</td><td>240 kcal</td></tr>
                        <tr><td>Grassi</td><td>18.5 g</td></tr>
                        <tr><td>Carboidrati</td><td>1.2 g</td></tr>
                        <tr><td>Proteine</td><td>17.0 g</td></tr>
                    </table>
                </body>
            </html>
            """,
            encoding="utf-8",
        )

        cmd = [
            sys.executable,
            "-m",
            "tools.dataset_builder.main",
            "--sources",
            "crea,off,scrapling",
            "--crea-path",
            str(crea_file),
            "--off-path",
            str(off_file),
            "--scrapling-target",
            str(scrapling_dir),
            "--output-db",
            str(db_path),
            "--output-parquet",
            str(parquet_path),
            "--limit",
            "50",
        ]

        res = subprocess.run(cmd, capture_output=True, text=True)
        assert res.returncode == 0, f"STDOUT: {res.stdout}\nSTDERR: {res.stderr}"
        assert "Dataset compilation complete" in res.stdout

        # Verify database contents
        conn = sqlite3.connect(str(db_path))
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM foods")
        total_foods = cursor.fetchone()[0]
        assert total_foods == 4  # 2 CREA + 1 OFF + 1 Scrapling

        cursor.execute("SELECT COUNT(*) FROM food_portions")
        total_portions = cursor.fetchone()[0]
        assert total_portions == 2  # 2 portions from CREA

        # Verify Parquet export
        assert parquet_path.exists()
        assert parquet_path.stat().st_size > 0
        conn.close()


def test_cli_statistics_and_off_comparison_report():
    """Verify that Open Food Facts vs FoodLab comparison statistics are generated and printed."""
    with tempfile.TemporaryDirectory() as tmp_dir:
        tmp_path = Path(tmp_dir)
        db_path = tmp_path / "stats_test.db"

        # Mock OFF data with known counts: 4 total evaluated
        # 1 valid Italian
        # 2 rejected by Macro Gate (1 missing macros, 1 impossible macro sum > 102g)
        # 1 rejected non-Italian
        off_file = tmp_path / "stats_off.json"
        off_data = [
            {
                "code": "8000500123456",
                "product_name": "Biscotti Buoni",
                "countries_tags": ["en:italy"],
                "nutriments": {
                    "energy-kcal_100g": 450,
                    "proteins_100g": 7.0,
                    "carbohydrates_100g": 68.0,
                    "fat_100g": 16.0,
                },
            },
            {
                "code": "8000500123457",
                "product_name": "Senza Macro",
                "countries_tags": ["en:italy"],
                "nutriments": {"energy-kcal_100g": 300},
            },
            {
                "code": "8000500123458",
                "product_name": "Macro Impossibili",
                "countries_tags": ["en:italy"],
                "nutriments": {
                    "energy-kcal_100g": 500,
                    "proteins_100g": 80.0,
                    "carbohydrates_100g": 80.0,
                    "fat_100g": 80.0,
                },
            },
            {
                "code": "4000123456789",
                "product_name": "Deutsches Produkt",
                "countries_tags": ["en:germany"],
                "lang": "de",
                "nutriments": {
                    "energy-kcal_100g": 200,
                    "proteins_100g": 5.0,
                    "carbohydrates_100g": 20.0,
                    "fat_100g": 10.0,
                },
            },
        ]
        off_file.write_text(json.dumps(off_data), encoding="utf-8")

        cmd = [
            sys.executable,
            "-m",
            "tools.dataset_builder.main",
            "--sources",
            "off",
            "--off-path",
            str(off_file),
            "--output-db",
            str(db_path),
        ]

        res = subprocess.run(cmd, capture_output=True, text=True)
        assert res.returncode == 0, f"STDOUT: {res.stdout}\nSTDERR: {res.stderr}"

        # Assert report sections and comparison numbers
        stdout = res.stdout
        assert "STATISTICS & COMPARISON REPORT" in stdout or "RESOCONTO COMPARATIVO" in stdout.upper()
        assert "Open Food Facts" in stdout
        assert "FoodLab" in stdout
        # Should mention macro gate rejections
        assert "Macro Gate" in stdout or "gate" in stdout.lower()


def test_cli_invalid_source():
    """Verify that specifying an unsupported source produces a non-zero exit code."""
    cmd = [
        sys.executable,
        "-m",
        "tools.dataset_builder.main",
        "--sources",
        "unsupported_source",
    ]
    res = subprocess.run(cmd, capture_output=True, text=True)
    assert res.returncode != 0
    assert "Unknown sources" in res.stdout or "Unknown sources" in res.stderr


def test_cli_help_flag():
    """Verify that --help exits with 0 and prints usage options."""
    cmd = [sys.executable, "-m", "tools.dataset_builder.main", "--help"]
    res = subprocess.run(cmd, capture_output=True, text=True)
    assert res.returncode == 0
    assert "--sources" in res.stdout
    assert "--crea-path" in res.stdout
    assert "--off-path" in res.stdout
    assert "--scrapling-target" in res.stdout
    assert "--output-db" in res.stdout
    assert "--output-parquet" in res.stdout

