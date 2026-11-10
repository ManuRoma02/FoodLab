"""Tests for Scrapling Italian Supermarket Crawler and Label Parser."""

import json
from pathlib import Path
import pytest

from tools.dataset_builder.models import FoodRecord
from tools.dataset_builder.scraper_scrapling import (
    SupermarketSpider,
    parse_supermarket_html_or_json,
    scrape_supermarket_catalog,
)


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
            "Sale": "0.013 g",
        },
    }
    record = parse_supermarket_html_or_json(mock_item, source="SCRAPLING_GDO")
    assert record is not None
    assert record.name == "Pasta di Semola Spaghetti N.5"
    assert record.brand == "Barilla"
    assert record.barcode == "8076809513753"
    assert record.calories_100g == 359
    assert record.protein_100g == 12.5
    assert record.carbs_100g == 71.2
    assert record.fat_100g == 2.0
    assert record.sugars_100g == 3.5
    assert record.saturated_fat_100g == 0.5
    assert record.fiber_100g == 3.0
    assert record.salt_100g == 0.013
    assert record.source == "SCRAPLING_GDO"


def test_parse_supermarket_html_table():
    html_content = """
    <!DOCTYPE html>
    <html>
    <head><title>Biscotti Frollini - Mulino Bianco</title></head>
    <body>
        <h1 class="product-title">Biscotti Frollini Tarallucci</h1>
        <div class="product-brand">Mulino Bianco</div>
        <div class="barcode" data-ean="8012345678901">EAN: 8012345678901</div>
        <table class="nutrition-table">
            <thead>
                <tr><th>Valori nutrizionali medi</th><th>Per 100g</th></tr>
            </thead>
            <tbody>
                <tr><td>Energia</td><td>1985 kJ / 473 kcal</td></tr>
                <tr><td>Grassi</td><td>19,0 g</td></tr>
                <tr><td>- di cui acidi grassi saturi</td><td>5,3 g</td></tr>
                <tr><td>Carboidrati</td><td>67,0 g</td></tr>
                <tr><td>- di cui zuccheri</td><td>24,0 g</td></tr>
                <tr><td>Fibre</td><td>3,5 g</td></tr>
                <tr><td>Proteine</td><td>7,0 g</td></tr>
                <tr><td>Sale</td><td>0,625 g</td></tr>
            </tbody>
        </table>
    </body>
    </html>
    """
    record = parse_supermarket_html_or_json(html_content, source="SCRAPLING_GDO")
    assert record is not None
    assert "Tarallucci" in record.name
    assert record.brand == "Mulino Bianco"
    assert record.barcode == "8012345678901"
    assert record.calories_100g == 473
    assert record.fat_100g == 19.0
    assert record.saturated_fat_100g == 5.3
    assert record.carbs_100g == 67.0
    assert record.sugars_100g == 24.0
    assert record.fiber_100g == 3.5
    assert record.protein_100g == 7.0
    assert record.salt_100g == 0.625


def test_parse_supermarket_json_ld_html():
    html_with_jsonld = """
    <html>
    <head>
        <script type="application/ld+json">
        {
            "@context": "https://schema.org",
            "@type": "Product",
            "name": "Passata di Pomodoro Classica 700g",
            "brand": {"@type": "Brand", "name": "Cirio"},
            "gtin13": "8000500123456",
            "nutrition": {
                "@type": "NutritionInformation",
                "servingSize": "100g",
                "calories": "32 kcal",
                "proteinContent": "1.3 g",
                "carbohydrateContent": "5.2 g",
                "sugarContent": "4.5 g",
                "fatContent": "0.2 g",
                "saturatedFatContent": "0.1 g",
                "fiberContent": "1.2 g",
                "sodiumContent": "0.08 g"
            }
        }
        </script>
    </head>
    <body><h1>Passata di Pomodoro Classica</h1></body>
    </html>
    """
    record = parse_supermarket_html_or_json(html_with_jsonld)
    assert record is not None
    assert record.name == "Passata di Pomodoro Classica 700g"
    assert record.brand == "Cirio"
    assert record.barcode == "8000500123456"
    assert record.calories_100g == 32
    assert record.protein_100g == 1.3
    assert record.carbs_100g == 5.2
    assert record.fat_100g == 0.2
    assert record.sugars_100g == 4.5
    assert record.fiber_100g == 1.2


def test_parse_supermarket_rejects_incomplete_or_invalid_macros():
    # Missing protein
    item_missing_protein = {
        "name": "Acqua Minerale Naturale",
        "nutrition_table": {
            "Energia": "0 kcal",
            "Grassi": "0 g",
            "Carboidrati": "0 g",
        },
    }
    assert parse_supermarket_html_or_json(item_missing_protein) is None

    # Physically impossible (>102g macros)
    item_over_100g = {
        "name": "Fake Superfood",
        "nutrition_table": {
            "Energia": "800 kcal",
            "Grassi": "60 g",
            "Carboidrati": "60 g",
            "Proteine": "60 g",
        },
    }
    assert parse_supermarket_html_or_json(item_over_100g) is None


def test_parse_supermarket_energy_kj_only():
    item = {
        "name": "Olio Extra Vergine di Oliva 1L",
        "brand": "Monini",
        "nutrition_table": {
            "Energia": "3404 kJ",
            "Grassi": "92 g",
            "di cui acidi grassi saturi": "14 g",
            "Carboidrati": "0 g",
            "di cui zuccheri": "0 g",
            "Proteine": "0 g",
            "Sale": "0 g",
        },
    }
    record = parse_supermarket_html_or_json(item)
    assert record is not None
    assert record.calories_100g == int(round(3404 / 4.184))  # ~814 kcal
    assert record.fat_100g == 92.0


def test_supermarket_spider_crawl_with_mock_fetcher():
    catalog_html = """
    <html><body>
        <div class="product-grid">
            <a href="/prodotti/pasta-1">Pasta 1</a>
            <a href="/prodotti/pasta-2">Pasta 2</a>
        </div>
    </body></html>
    """
    prod1_html = """
    <html><body>
        <h1>Penne Rigate N.73</h1>
        <div class="brand">De Cecco</div>
        <div class="barcode">8001250120732</div>
        <table>
            <tr><td>Energia</td><td>351 kcal</td></tr>
            <tr><td>Grassi</td><td>1.5 g</td></tr>
            <tr><td>- saturi</td><td>0.3 g</td></tr>
            <tr><td>Carboidrati</td><td>69.0 g</td></tr>
            <tr><td>- zuccheri</td><td>3.4 g</td></tr>
            <tr><td>Fibre</td><td>2.9 g</td></tr>
            <tr><td>Proteine</td><td>14.0 g</td></tr>
            <tr><td>Sale</td><td>0.0 g</td></tr>
        </table>
    </body></html>
    """
    prod2_html = """
    <html><body>
        <h1>Fusilli N.34</h1>
        <div class="brand">De Cecco</div>
        <div class="barcode">8001250120343</div>
        <table>
            <tr><td>Energia</td><td>351 kcal</td></tr>
            <tr><td>Grassi</td><td>1.5 g</td></tr>
            <tr><td>- saturi</td><td>0.3 g</td></tr>
            <tr><td>Carboidrati</td><td>69.0 g</td></tr>
            <tr><td>- zuccheri</td><td>3.4 g</td></tr>
            <tr><td>Fibre</td><td>2.9 g</td></tr>
            <tr><td>Proteine</td><td>14.0 g</td></tr>
            <tr><td>Sale</td><td>0.0 g</td></tr>
        </table>
    </body></html>
    """

    mock_pages = {
        "https://spesa.mock/catalog": catalog_html,
        "https://spesa.mock/prodotti/pasta-1": prod1_html,
        "https://spesa.mock/prodotti/pasta-2": prod2_html,
    }

    spider = SupermarketSpider(base_url="https://spesa.mock")
    # Custom fetcher returning mock pages
    spider.fetch = lambda url: mock_pages.get(url, "")

    records = spider.crawl("https://spesa.mock/catalog", limit=10)
    assert len(records) == 2
    assert records[0].name == "Penne Rigate N.73"
    assert records[0].brand == "De Cecco"
    assert records[1].name == "Fusilli N.34"


def test_scrape_supermarket_catalog_from_local_files(tmp_path: Path):
    item1 = {
        "title": "Riso Carnaroli 1kg",
        "brand": "Scotti",
        "barcode": "8001860123456",
        "nutrition_table": {
            "Energia": "349 kcal",
            "Grassi": "1.0 g",
            "di cui acidi grassi saturi": "0.3 g",
            "Carboidrati": "77.0 g",
            "di cui zuccheri": "0.2 g",
            "Fibre": "1.8 g",
            "Proteine": "6.9 g",
            "Sale": "0.01 g",
        },
    }
    file_path = tmp_path / "supermarket_products.json"
    file_path.write_text(json.dumps([item1]), encoding="utf-8")

    records = scrape_supermarket_catalog(str(file_path), limit=10)
    assert len(records) == 1
    assert records[0].name == "Riso Carnaroli 1kg"
    assert records[0].brand == "Scotti"
    assert records[0].calories_100g == 349


def test_parse_supermarket_multi_column_table_extracts_100g_column():
    html_content = """
    <html><body>
        <h1>Cereali Integrali Fitness</h1>
        <div class="brand">Nestlé</div>
        <div class="barcode">7613035123456</div>
        <table>
            <thead>
                <tr>
                    <th>Informazioni Nutrizionali</th>
                    <th>Per porzione (30g)</th>
                    <th>Per 100g</th>
                </tr>
            </thead>
            <tbody>
                <tr><td>Energia</td><td>112 kcal</td><td>373 kcal</td></tr>
                <tr><td>Grassi</td><td>0.5 g</td><td>1.7 g</td></tr>
                <tr><td>- di cui grassi saturi</td><td>0.2 g</td><td>0.6 g</td></tr>
                <tr><td>Carboidrati</td><td>22.5 g</td><td>75.1 g</td></tr>
                <tr><td>- di cui zuccheri</td><td>3.2 g</td><td>10.8 g</td></tr>
                <tr><td>Fibre</td><td>2.4 g</td><td>8.0 g</td></tr>
                <tr><td>Proteine</td><td>2.8 g</td><td>9.4 g</td></tr>
                <tr><td>Sale</td><td>0.25 g</td><td>0.83 g</td></tr>
            </tbody>
        </table>
    </body></html>
    """
    record = parse_supermarket_html_or_json(html_content)
    assert record is not None
    assert record.name == "Cereali Integrali Fitness"
    assert record.brand == "Nestlé"
    # Verify values are from the 100g column (373 kcal), NOT portion (112 kcal)
    assert record.calories_100g == 373
    assert record.carbs_100g == 75.1
    assert record.fat_100g == 1.7
    assert record.protein_100g == 9.4
    assert record.fiber_100g == 8.0
    assert record.salt_100g == 0.83


def test_parse_supermarket_definition_list_dl():
    html_content = """
    <html><body>
        <h1>Yogurt Greco 0%</h1>
        <div class="product-brand">Fage</div>
        <div class="barcode">5201054012345</div>
        <dl>
            <dt>Valore energetico</dt><dd>54 kcal</dd>
            <dt>Grassi</dt><dd>0,0 g</dd>
            <dt>di cui saturi</dt><dd>0,0 g</dd>
            <dt>Carboidrati</dt><dd>3,0 g</dd>
            <dt>di cui zuccheri</dt><dd>3,0 g</dd>
            <dt>Proteine</dt><dd>10,3 g</dd>
            <dt>Sale</dt><dd>0,10 g</dd>
        </dl>
    </body></html>
    """
    record = parse_supermarket_html_or_json(html_content)
    assert record is not None
    assert record.name == "Yogurt Greco 0%"
    assert record.brand == "Fage"
    assert record.calories_100g == 54
    assert record.protein_100g == 10.3
    assert record.fat_100g == 0.0
    assert record.carbs_100g == 3.0


def test_parse_supermarket_nested_html_table_in_dict():
    table_html = """
    <table>
        <tr><td>Energia</td><td>250 kcal</td></tr>
        <tr><td>Grassi</td><td>10.0 g</td></tr>
        <tr><td>- di cui acidi grassi saturi</td><td>3.0 g</td></tr>
        <tr><td>Carboidrati</td><td>20.0 g</td></tr>
        <tr><td>- di cui zuccheri</td><td>2.0 g</td></tr>
        <tr><td>Proteine</td><td>18.0 g</td></tr>
        <tr><td>Sale</td><td>1.2 g</td></tr>
    </table>
    """
    item = {
        "title": "Tofu Naturale Biologico",
        "brand": "Compagnia Italiana",
        "barcode": "8012345000000",
        "nutrition_table": table_html,
    }
    record = parse_supermarket_html_or_json(item)
    assert record is not None
    assert record.name == "Tofu Naturale Biologico"
    assert record.calories_100g == 250
    assert record.protein_100g == 18.0
    assert record.fat_100g == 10.0


def test_parse_supermarket_special_values_and_formatting():
    item = {
        "name": "Fesa di Tacchino Arrosto",
        "brand": "Aia",
        "nutrition_table": {
            "Energia": "422 kJ / 100 kcal",
            "Grassi": "1,5 g",
            "di cui acidi grassi saturi": "< 0,5 g",
            "Carboidrati": "tracce",
            "di cui zuccheri": "assente",
            "Proteine": "21,5 g",
            "Sale": "1,8 g",
        },
    }
    record = parse_supermarket_html_or_json(item)
    assert record is not None
    assert record.calories_100g == 100
    assert record.protein_100g == 21.5
    assert record.fat_100g == 1.5
    assert record.saturated_fat_100g == 0.5
    assert record.carbs_100g == 0.0
    assert record.sugars_100g is None  # 'assente' treated as null or zero


def test_scrape_supermarket_catalog_directory_with_mixed_files(tmp_path: Path):
    d = tmp_path / "catalog_dump"
    d.mkdir()

    # File 1: JSON file
    (d / "item1.json").write_text(
        json.dumps({
            "title": "Latte Intero Fresco",
            "brand": "Granarolo",
            "barcode": "8002660123456",
            "nutrition_table": {
                "Energia": "64 kcal",
                "Grassi": "3.6 g",
                "di cui acidi grassi saturi": "2.4 g",
                "Carboidrati": "4.9 g",
                "di cui zuccheri": "4.9 g",
                "Proteine": "3.2 g",
                "Sale": "0.1 g",
            },
        }),
        encoding="utf-8",
    )

    # File 2: HTML file
    (d / "item2.html").write_text(
        """
        <html><body>
            <h1>Ricotta Fresca</h1>
            <div class="brand">Santa Lucia</div>
            <div class="barcode">8000430123456</div>
            <table>
                <tr><td>Energia</td><td>146 kcal</td></tr>
                <tr><td>Grassi</td><td>11.0 g</td></tr>
                <tr><td>- di cui acidi grassi saturi</td><td>7.5 g</td></tr>
                <tr><td>Carboidrati</td><td>3.5 g</td></tr>
                <tr><td>- di cui zuccheri</td><td>3.5 g</td></tr>
                <tr><td>Proteine</td><td>8.5 g</td></tr>
                <tr><td>Sale</td><td>0.3 g</td></tr>
            </table>
        </body></html>
        """,
        encoding="utf-8",
    )

    records = scrape_supermarket_catalog(str(d), limit=10)
    assert len(records) == 2
    names = {r.name for r in records}
    assert "Latte Intero Fresco" in names
    assert "Ricotta Fresca" in names


def test_supermarket_spider_stealth_headers():
    spider = SupermarketSpider(base_url="https://example.com")
    headers = getattr(spider.session, "headers", {})
    assert "Mozilla/5.0" in headers.get("User-Agent", "")
    assert "it-IT" in headers.get("Accept-Language", "")
