# tools/dataset_builder/tests/test_validator.py
from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.validator import validate_and_normalize, MacroQualityGate

def test_rejects_missing_protein():
    raw = {"name": "Test", "calories": 100, "carbs": 20.0, "fat": 2.0} # protein missing
    assert validate_and_normalize(raw, "TEST") is None

def test_rejects_missing_carbs_or_fat_or_calories():
    assert validate_and_normalize({"name": "Test", "protein": 10.0, "fat": 2.0, "calories": 100}, "TEST") is None
    assert validate_and_normalize({"name": "Test", "protein": 10.0, "carbs": 20.0, "calories": 100}, "TEST") is None
    assert validate_and_normalize({"name": "Test", "protein": 10.0, "carbs": 20.0, "fat": 2.0}, "TEST") is None

def test_rejects_sum_macros_exceeding_100g():
    raw = {"name": "Test", "calories": 900, "protein": 50.0, "carbs": 50.0, "fat": 50.0} # 150g > 102g
    assert validate_and_normalize(raw, "TEST") is None

def test_macro_sum_boundary_102g():
    # 102.0g should pass (within rounding tolerance)
    raw_pass = {"name": "Test Boundary", "calories": 500, "protein": 34.0, "carbs": 34.0, "fat": 34.0}
    assert validate_and_normalize(raw_pass, "TEST") is not None

    # 102.5g should fail
    raw_fail = {"name": "Test Boundary Fail", "calories": 500, "protein": 34.5, "carbs": 34.0, "fat": 34.0}
    assert validate_and_normalize(raw_fail, "TEST") is None

def test_rejects_single_macro_exceeding_100g():
    raw = {"name": "Test Single Macro", "calories": 404, "protein": 101.0, "carbs": 0.0, "fat": 0.0}
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
    assert food.saturated_fat_100g == 5.0
    assert food.fiber_100g == 3.5
    assert food.salt_100g == 0.4
    assert food.sodium_100g == 0.16
    assert food.source == "OFF_IT"

def test_rejects_empty_or_whitespace_name():
    raw = {"name": "   ", "calories": 100, "protein": 5.0, "carbs": 10.0, "fat": 2.0}
    assert validate_and_normalize(raw, "TEST") is None

def test_rejects_negative_macros_or_calories():
    raw = {"name": "Negative Test", "calories": -50, "protein": 10.0, "carbs": 10.0, "fat": 2.0}
    assert validate_and_normalize(raw, "TEST") is None
    raw2 = {"name": "Negative Macro", "calories": 100, "protein": -1.0, "carbs": 10.0, "fat": 2.0}
    assert validate_and_normalize(raw2, "TEST") is None

def test_rejects_drastic_energy_hallucination():
    # 0 kcal with 50g fat is physically impossible
    raw = {"name": "Ghost Calories", "calories": 0, "protein": 0.0, "carbs": 0.0, "fat": 50.0}
    assert validate_and_normalize(raw, "TEST") is None

def test_handles_string_values_and_european_formatting():
    raw = {
        "name": "Pasta Integrale",
        "calories": "350 kcal",
        "protein": "12,5 g",
        "carbs": "65,0 g",
        "fat": "2,2 g",
        "fiber": "6,0 g",
        "salt": "< 0,05 g"
    }
    food = validate_and_normalize(raw, "TEST")
    assert food is not None
    assert food.name == "Pasta Integrale"
    assert food.calories_100g == 350
    assert food.protein_100g == 12.5
    assert food.carbs_100g == 65.0
    assert food.fat_100g == 2.2
    assert food.fiber_100g == 6.0
    assert food.salt_100g == 0.05

def test_nested_nutriments_parsing():
    raw = {
        "product_name": "Nutella",
        "brands": "Ferrero",
        "code": "8000500310427",
        "nutriments": {
            "energy-kcal_100g": 539,
            "proteins_100g": 6.3,
            "carbohydrates_100g": 57.5,
            "fat_100g": 30.9,
            "sugars_100g": 56.3,
            "saturated-fat_100g": 10.6,
            "salt_100g": 0.107
        }
    }
    food = MacroQualityGate.validate(raw, "OFF_IT")
    assert food is not None
    assert food.name == "Nutella"
    assert food.brand == "Ferrero"
    assert food.barcode == "8000500310427"
    assert food.calories_100g == 539
    assert food.protein_100g == 6.3
    assert food.carbs_100g == 57.5
    assert food.fat_100g == 30.9
    assert food.sugars_100g == 56.3
    assert food.saturated_fat_100g == 10.6
    assert food.salt_100g == 0.107

def test_portion_record_creation():
    portion = PortionRecord(food_barcode_or_key="8076809513753", name="1 biscotto", weight_grams=12.5)
    assert portion.food_barcode_or_key == "8076809513753"
    assert portion.name == "1 biscotto"
    assert portion.weight_grams == 12.5
