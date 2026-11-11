import json
import tempfile
from tools.dataset_builder.extract_scientific import (
    extract_scientific_foods,
    parse_ciqual_record,
    parse_usda_foundation_record,
    translate_food_name,
)

def test_translate_food_name():
    assert "Pollo" in translate_food_name("Poulet rôti", source_lang="fr")
    assert "Salmone" in translate_food_name("Salmon, grilled", source_lang="en")

def test_parse_ciqual_record():
    raw_ciqual = {
        "alim_nom_fr": "Poulet, filet, sans peau, cuit",
        "alim_grp_nom_fr": "Viandes cuites",
        "Energie, Règlement UE N° 1169/2011 (kcal/100 g)": "145",
        "Protéines, N x 6.25 (g/100 g)": "31,0",
        "Glucides (g/100 g)": "0,0",
        "Lipides (g/100 g)": "2,3",
        "Sucres (g/100 g)": "0,0",
        "AG saturés (g/100 g)": "0,6",
        "Fibres alimentaires (g/100 g)": "0,0",
        "Sel chlorure de sodium (g/100 g)": "0,15",
    }
    food = parse_ciqual_record(raw_ciqual)
    assert food is not None
    assert "Pollo" in food.name
    assert food.calories_100g == 145
    assert food.protein_100g == 31.0
    assert food.fat_100g == 2.3
    assert food.source == "CIQUAL"

def test_parse_usda_foundation_record():
    raw_usda = {
        "description": "Beef, tenderloin, steak, raw",
        "foodCategory": "Beef Products",
        "foodNutrients": [
            {"nutrientName": "Energy", "unitName": "KCAL", "amount": 198},
            {"nutrientName": "Protein", "unitName": "G", "amount": 21.5},
            {"nutrientName": "Carbohydrate, by difference", "unitName": "G", "amount": 0.0},
            {"nutrientName": "Total lipid (fat)", "unitName": "G", "amount": 12.4},
            {"nutrientName": "Fatty acids, total saturated", "unitName": "G", "amount": 4.8},
            {"nutrientName": "Sodium, Na", "unitName": "MG", "amount": 54},
        ],
    }
    food = parse_usda_foundation_record(raw_usda)
    assert food is not None
    assert "Manzo" in food.name
    assert food.calories_100g == 198
    assert food.protein_100g == 21.5
    assert food.sodium_100g == 54.0
    assert food.source == "USDA"

def test_extract_scientific_foods_file():
    sample_ciqual = [
        {
            "name": "Pomme, crue",
            "calories": 52,
            "protein": 0.3,
            "carbs": 12.0,
            "fat": 0.2,
        }
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(sample_ciqual, f)
        path = f.name

    foods = extract_scientific_foods(path, format_type="ciqual")
    assert len(foods) == 1
    assert "Mela" in foods[0].name
