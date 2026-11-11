import json
import tempfile
from tools.dataset_builder.extract_chains import CANONICAL_ITALIAN_CHAINS, extract_chains_foods

def test_extract_canonical_italian_chains():
    foods, portions = extract_chains_foods()
    assert len(foods) >= len(CANONICAL_ITALIAN_CHAINS)
    assert len(portions) == len(foods)

    big_mac = next((f for f in foods if f.name == "Big Mac"), None)
    assert big_mac is not None
    assert big_mac.brand == "McDonald's"
    assert big_mac.calories_100g == 235
    assert big_mac.protein_100g == 12.0
    assert big_mac.source == "CHAINS_IT"

    big_mac_portion = next((p for p in portions if p.food_barcode_or_key == "Big Mac"), None)
    assert big_mac_portion is not None
    assert big_mac_portion.weight_grams == 215.0

    whopper = next((f for f in foods if f.name == "Whopper"), None)
    assert whopper is not None
    assert whopper.brand == "Burger King"

    poke = next((f for f in foods if f.name == "Sunny Salmon Poke"), None)
    assert poke is not None
    assert poke.brand == "Poke House"

def test_extract_chains_with_custom_file_and_limit():
    custom = [
        {
            "name": "Custom Panino Gourmet",
            "brand": "Paninoteca Locale",
            "calories": 250,
            "protein": 14.0,
            "carbs": 25.0,
            "fat": 10.0,
            "portion_weight": 200,
        }
    ]
    with tempfile.NamedTemporaryFile("w+", delete=False, suffix=".json", encoding="utf-8") as f:
        json.dump(custom, f)
        path = f.name

    foods, portions = extract_chains_foods(custom_json_path=path, limit=3)
    assert len(foods) == 3
    assert len(portions) == 3
    for f in foods:
        assert f.source == "CHAINS_IT"
