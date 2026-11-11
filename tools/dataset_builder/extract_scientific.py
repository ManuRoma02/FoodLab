"""Extractor for official scientific food composition databases (CIQUAL & USDA FoodData Central)."""

from __future__ import annotations

import csv
import json
import re
from pathlib import Path
from typing import Any, Dict, Iterator, List, Optional, Tuple, Union

from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.validator import validate_and_normalize

# Bilingual Italian mapping dictionary for common raw scientific terms
FRENCH_TO_ITALIAN = {
    "poulet": "pollo",
    "boeuf": "manzo",
    "porc": "maiale",
    "veau": "vitello",
    "saumon": "salmone",
    "thon": "tonno",
    "cabillaud": "merluzzo",
    "crevette": "gambero",
    "oeuf": "uovo",
    "lait": "latte",
    "fromage": "formaggio",
    "beurre": "burro",
    "huile d'olive": "olio d'oliva",
    "pomme": "mela",
    "banane": "banana",
    "orange": "arancia",
    "fraise": "fragola",
    "tomate": "pomodoro",
    "carotte": "carota",
    "pomme de terre": "patata",
    "riz": "riso",
    "pain": "pane",
    "pâtes": "pasta",
    "farine": "farina",
    "cuit": "cotto",
    "cru": "crudo",
    "grillé": "grigliato",
    "bouilli": "bollito",
    "rôti": "arrosto",
}

ENGLISH_TO_ITALIAN = {
    "chicken": "pollo",
    "beef": "manzo",
    "pork": "maiale",
    "salmon": "salmone",
    "tuna": "tonno",
    "egg": "uovo",
    "milk": "latte",
    "cheese": "formaggio",
    "butter": "burro",
    "olive oil": "olio d'oliva",
    "apple": "mela",
    "banana": "banana",
    "orange": "arancia",
    "strawberry": "fragola",
    "tomato": "pomodoro",
    "carrot": "carota",
    "potato": "patata",
    "rice": "riso",
    "bread": "pane",
    "pasta": "pasta",
    "flour": "farina",
    "cooked": "cotto",
    "raw": "crudo",
    "grilled": "grigliato",
    "boiled": "bollito",
    "roasted": "arrosto",
    "baked": "al forno",
}


def translate_food_name(name: str, source_lang: str = "fr") -> str:
    """Provide intelligent Italian translation for international scientific food terms."""
    clean = name.strip()
    mapping = FRENCH_TO_ITALIAN if source_lang == "fr" else ENGLISH_TO_ITALIAN
    lower = clean.lower()

    # Exact or phrase replacement
    for src, dst in mapping.items():
        pattern = r"\b" + re.escape(src) + r"\b"
        if re.search(pattern, lower, flags=re.IGNORECASE):
            clean = re.sub(pattern, dst, clean, flags=re.IGNORECASE)

    # Capitalize first letter
    return clean[0].upper() + clean[1:] if clean else name


def parse_ciqual_record(row: Dict[str, Any]) -> Optional[FoodRecord]:
    """
    Parse a single record from CIQUAL (ANSES) French/Mediterranean food table.
    Enforces Mandatory Macro Gate and preserves secondary nutrients.
    """
    orig_name = str(row.get("alim_nom_fr") or row.get("name") or "").strip()
    if not orig_name:
        return None

    it_name = translate_food_name(orig_name, source_lang="fr")

    raw_dict = {
        "name": it_name,
        "brand": "CIQUAL Scienza",
        "category": str(row.get("alim_grp_nom_fr") or "Alimenti Scientifici").strip(),
        "barcode": "",
        "calories_100g": row.get("Energie, Règlement UE N° 1169/2011 (kcal/100 g)")
        or row.get("calories")
        or row.get("energie_kcal"),
        "protein_100g": row.get("Protéines, N x 6.25 (g/100 g)")
        or row.get("protein")
        or row.get("proteines"),
        "carbs_100g": row.get("Glucides (g/100 g)")
        or row.get("carbs")
        or row.get("glucides"),
        "fat_100g": row.get("Lipides (g/100 g)")
        or row.get("fat")
        or row.get("lipides"),
        "sugars_100g": row.get("Sucres (g/100 g)") or row.get("sugars") or row.get("sucres"),
        "saturated_fat_100g": row.get("AG saturés (g/100 g)")
        or row.get("saturated_fat")
        or row.get("ag_satures"),
        "polyols_100g": row.get("Polyols totaux (g/100 g)") or row.get("polyols"),
        "fiber_100g": row.get("Fibres alimentaires (g/100 g)")
        or row.get("fiber")
        or row.get("fibres"),
        "salt_100g": row.get("Sel chlorure de sodium (g/100 g)")
        or row.get("salt")
        or row.get("sel"),
        "default_serving_size": 100,
    }

    return validate_and_normalize(raw_dict, source="CIQUAL")


def parse_usda_foundation_record(raw: Dict[str, Any]) -> Optional[FoodRecord]:
    """
    Parse a single record from USDA FoodData Central Foundation / SR Legacy foods.
    """
    orig_name = str(raw.get("description") or raw.get("name") or "").strip()
    if not orig_name:
        return None

    it_name = translate_food_name(orig_name, source_lang="en")

    # Map USDA nutrient IDs or names
    nutrients = raw.get("foodNutrients") or []
    kcal = None
    protein = None
    carbs = None
    fat = None
    sugars = None
    sat_fat = None
    polyols = None
    fiber = None
    sodium = None

    if isinstance(nutrients, list):
        for n in nutrients:
            if not isinstance(n, dict):
                continue
            name_n = str(
                n.get("nutrientName") or n.get("name") or (n.get("nutrient") or {}).get("name") or ""
            ).lower()
            val = n.get("amount") if n.get("amount") is not None else n.get("value")

            if "energy" in name_n and ("kcal" in name_n or n.get("unitName") == "KCAL"):
                kcal = val
            elif "protein" in name_n:
                protein = val
            elif "carbohydrate" in name_n:
                carbs = val
            elif "total lipid" in name_n or name_n == "fat":
                fat = val
            elif "sugars, total" in name_n or name_n == "sugars":
                sugars = val
            elif "fatty acids, total saturated" in name_n or "saturated fat" in name_n:
                sat_fat = val
            elif "polyols" in name_n:
                polyols = val
            elif "fiber, total dietary" in name_n or name_n == "fiber":
                fiber = val
            elif "sodium" in name_n:
                sodium = val
    elif isinstance(nutrients, dict):
        kcal = nutrients.get("calories") or nutrients.get("kcal")
        protein = nutrients.get("protein")
        carbs = nutrients.get("carbs")
        fat = nutrients.get("fat")
        sugars = nutrients.get("sugars")
        sat_fat = nutrients.get("saturated_fat")
        polyols = nutrients.get("polyols")
        fiber = nutrients.get("fiber")
        sodium = nutrients.get("sodium")

    raw_dict = {
        "name": it_name,
        "brand": "USDA Foundation",
        "category": str(raw.get("foodCategory") or "Materie Prime").strip(),
        "barcode": str(raw.get("gtinUpc") or raw.get("barcode") or "").strip(),
        "calories_100g": kcal if kcal is not None else raw.get("calories"),
        "protein_100g": protein if protein is not None else raw.get("protein"),
        "carbs_100g": carbs if carbs is not None else raw.get("carbs"),
        "fat_100g": fat if fat is not None else raw.get("fat"),
        "sugars_100g": sugars if sugars is not None else raw.get("sugars"),
        "saturated_fat_100g": sat_fat if sat_fat is not None else raw.get("saturated_fat"),
        "polyols_100g": polyols,
        "fiber_100g": fiber if fiber is not None else raw.get("fiber"),
        "sodium_100g": sodium if sodium is not None else raw.get("sodium"),
        "default_serving_size": 100,
    }

    return validate_and_normalize(raw_dict, source="USDA")


CANONICAL_SCIENTIFIC_FOODS: List[Dict[str, Any]] = [
    {
        "name": "Yogurt Greco 0% Grassi",
        "brand": "CIQUAL Scienza",
        "category": "Latticini & Uova",
        "barcode": "",
        "calories_100g": 59,
        "protein_100g": 10.3,
        "carbs_100g": 3.6,
        "fat_100g": 0.0,
        "sugars_100g": 3.6,
        "saturated_fat_100g": 0.0,
        "fiber_100g": 0.0,
        "salt_100g": 0.1,
        "default_serving_size": 170,
    },
    {
        "name": "Yogurt Greco 2% Grassi",
        "brand": "CIQUAL Scienza",
        "category": "Latticini & Uova",
        "barcode": "",
        "calories_100g": 73,
        "protein_100g": 9.8,
        "carbs_100g": 3.8,
        "fat_100g": 2.0,
        "sugars_100g": 3.8,
        "saturated_fat_100g": 1.3,
        "fiber_100g": 0.0,
        "salt_100g": 0.1,
        "default_serving_size": 170,
    },
    {
        "name": "Yogurt Greco 5% Grassi",
        "brand": "CIQUAL Scienza",
        "category": "Latticini & Uova",
        "barcode": "",
        "calories_100g": 96,
        "protein_100g": 9.0,
        "carbs_100g": 3.9,
        "fat_100g": 5.0,
        "sugars_100g": 3.9,
        "saturated_fat_100g": 3.2,
        "fiber_100g": 0.0,
        "salt_100g": 0.1,
        "default_serving_size": 150,
    },
    {
        "name": "Skyr Naturale Tradizionale",
        "brand": "CIQUAL Scienza",
        "category": "Latticini & Uova",
        "barcode": "",
        "calories_100g": 62,
        "protein_100g": 11.0,
        "carbs_100g": 4.0,
        "fat_100g": 0.2,
        "sugars_100g": 4.0,
        "saturated_fat_100g": 0.1,
        "fiber_100g": 0.0,
        "salt_100g": 0.12,
        "default_serving_size": 150,
    },
    {
        "name": "Proteine Whey Isolate 90% (Siero del latte)",
        "brand": "USDA Foundation",
        "category": "Integratori & Fitness",
        "barcode": "",
        "calories_100g": 375,
        "protein_100g": 88.0,
        "carbs_100g": 1.5,
        "fat_100g": 1.2,
        "sugars_100g": 0.8,
        "saturated_fat_100g": 0.5,
        "fiber_100g": 0.0,
        "salt_100g": 0.45,
        "default_serving_size": 30,
    },
    {
        "name": "Caseina Micellare 80%",
        "brand": "USDA Foundation",
        "category": "Integratori & Fitness",
        "barcode": "",
        "calories_100g": 362,
        "protein_100g": 80.0,
        "carbs_100g": 4.2,
        "fat_100g": 1.6,
        "sugars_100g": 3.0,
        "saturated_fat_100g": 0.9,
        "fiber_100g": 0.0,
        "salt_100g": 0.35,
        "default_serving_size": 30,
    },
    {
        "name": "Albume d'uovo pastorizzato",
        "brand": "CIQUAL Scienza",
        "category": "Latticini & Uova",
        "barcode": "",
        "calories_100g": 48,
        "protein_100g": 10.8,
        "carbs_100g": 0.7,
        "fat_100g": 0.2,
        "sugars_100g": 0.7,
        "saturated_fat_100g": 0.0,
        "fiber_100g": 0.0,
        "salt_100g": 0.4,
        "default_serving_size": 100,
    },
    {
        "name": "Edamame di soia lessati",
        "brand": "USDA Foundation",
        "category": "Legumi & Derivati",
        "barcode": "",
        "calories_100g": 122,
        "protein_100g": 11.9,
        "carbs_100g": 8.9,
        "fat_100g": 5.2,
        "sugars_100g": 2.2,
        "saturated_fat_100g": 0.6,
        "fiber_100g": 5.2,
        "salt_100g": 0.02,
        "default_serving_size": 150,
    },
    {
        "name": "Tofu al naturale biologico",
        "brand": "CIQUAL Scienza",
        "category": "Legumi & Derivati",
        "barcode": "",
        "calories_100g": 83,
        "protein_100g": 8.5,
        "carbs_100g": 1.2,
        "fat_100g": 4.8,
        "sugars_100g": 0.4,
        "saturated_fat_100g": 0.7,
        "fiber_100g": 1.2,
        "salt_100g": 0.05,
        "default_serving_size": 125,
    },
    {
        "name": "Tempeh di soia fermentato",
        "brand": "USDA Foundation",
        "category": "Legumi & Derivati",
        "barcode": "",
        "calories_100g": 192,
        "protein_100g": 20.3,
        "carbs_100g": 7.6,
        "fat_100g": 10.8,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 2.2,
        "fiber_100g": 4.0,
        "salt_100g": 0.03,
        "default_serving_size": 100,
    },
    {
        "name": "Seitan al naturale",
        "brand": "CIQUAL Scienza",
        "category": "Cereali & Derivati",
        "barcode": "",
        "calories_100g": 140,
        "protein_100g": 25.0,
        "carbs_100g": 5.5,
        "fat_100g": 1.5,
        "sugars_100g": 0.5,
        "saturated_fat_100g": 0.3,
        "fiber_100g": 1.5,
        "salt_100g": 0.8,
        "default_serving_size": 120,
    },
    {
        "name": "Farina d'avena integrale micronizzata",
        "brand": "USDA Foundation",
        "category": "Cereali & Derivati",
        "barcode": "",
        "calories_100g": 389,
        "protein_100g": 14.5,
        "carbs_100g": 66.0,
        "fat_100g": 6.9,
        "sugars_100g": 1.2,
        "saturated_fat_100g": 1.2,
        "fiber_100g": 10.6,
        "salt_100g": 0.02,
        "default_serving_size": 50,
    },
    {
        "name": "Burro di arachidi 100% puro",
        "brand": "USDA Foundation",
        "category": "Frutta Secca & Semi",
        "barcode": "",
        "calories_100g": 588,
        "protein_100g": 25.0,
        "carbs_100g": 20.0,
        "fat_100g": 50.0,
        "sugars_100g": 6.0,
        "saturated_fat_100g": 10.0,
        "fiber_100g": 8.0,
        "salt_100g": 0.01,
        "default_serving_size": 25,
    },
    {
        "name": "Semi di chia crudi",
        "brand": "USDA Foundation",
        "category": "Frutta Secca & Semi",
        "barcode": "",
        "calories_100g": 486,
        "protein_100g": 16.5,
        "carbs_100g": 42.1,
        "fat_100g": 30.7,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 3.3,
        "fiber_100g": 34.4,
        "salt_100g": 0.04,
        "default_serving_size": 15,
    },
    {
        "name": "Salmone selvaggio dell'Alaska",
        "brand": "USDA Foundation",
        "category": "Pesce & Frutti di Mare",
        "barcode": "",
        "calories_100g": 142,
        "protein_100g": 19.8,
        "carbs_100g": 0.0,
        "fat_100g": 6.3,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 1.0,
        "fiber_100g": 0.0,
        "salt_100g": 0.12,
        "default_serving_size": 150,
    },
    {
        "name": "Petto di tacchino al forno a fette",
        "brand": "USDA Foundation",
        "category": "Carne & Pollame",
        "barcode": "",
        "calories_100g": 108,
        "protein_100g": 24.0,
        "carbs_100g": 0.5,
        "fat_100g": 1.2,
        "sugars_100g": 0.2,
        "saturated_fat_100g": 0.4,
        "fiber_100g": 0.0,
        "salt_100g": 1.2,
        "default_serving_size": 100,
    },
    {
        "name": "Carne bovina macinata magra (<5% grassi)",
        "brand": "USDA Foundation",
        "category": "Carne & Pollame",
        "barcode": "",
        "calories_100g": 137,
        "protein_100g": 21.4,
        "carbs_100g": 0.0,
        "fat_100g": 5.0,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 2.1,
        "fiber_100g": 0.0,
        "salt_100g": 0.15,
        "default_serving_size": 150,
    },
    {
        "name": "Quinoa bianca cotta",
        "brand": "USDA Foundation",
        "category": "Cereali & Derivati",
        "barcode": "",
        "calories_100g": 120,
        "protein_100g": 4.4,
        "carbs_100g": 21.3,
        "fat_100g": 1.9,
        "sugars_100g": 0.9,
        "saturated_fat_100g": 0.2,
        "fiber_100g": 2.8,
        "salt_100g": 0.02,
        "default_serving_size": 150,
    },
    {
        "name": "Avocado Hass fresco maturo",
        "brand": "CIQUAL Scienza",
        "category": "Frutta Fresca",
        "barcode": "",
        "calories_100g": 160,
        "protein_100g": 2.0,
        "carbs_100g": 8.5,
        "fat_100g": 14.7,
        "sugars_100g": 0.7,
        "saturated_fat_100g": 2.1,
        "fiber_100g": 6.7,
        "salt_100g": 0.02,
        "default_serving_size": 100,
    },
]


def extract_scientific_foods(
    file_path: Optional[Union[str, Path]] = None, format_type: str = "ciqual", limit: Optional[int] = None
) -> List[FoodRecord]:
    """
    Extract foods from a CIQUAL or USDA dataset file (JSON or CSV).
    If file_path is None, returns verified canonical scientific foods.
    """
    if file_path is None:
        records: List[FoodRecord] = []
        for item in CANONICAL_SCIENTIFIC_FOODS:
            food = validate_and_normalize(item, source=item.get("brand", "SCIENTIFIC"))
            if food:
                records.append(food)
                if limit and len(records) >= limit:
                    break
        return records

    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"Scientific dataset file not found: {path}")

    records: List[FoodRecord] = []

    if path.suffix.lower() == ".json":
        with open(path, "r", encoding="utf-8") as f:
            data = json.load(f)
            items = data if isinstance(data, list) else data.get("foods") or data.get("items") or []
            for item in items:
                if not isinstance(item, dict):
                    continue
                food = (
                    parse_ciqual_record(item)
                    if format_type.lower() == "ciqual"
                    else parse_usda_foundation_record(item)
                )
                if food:
                    records.append(food)
                    if limit and len(records) >= limit:
                        break
    elif path.suffix.lower() == ".csv":
        with open(path, "r", encoding="utf-8", errors="replace") as f:
            reader = csv.DictReader(f, delimiter=";" if format_type.lower() == "ciqual" else ",")
            for row in reader:
                food = (
                    parse_ciqual_record(row)
                    if format_type.lower() == "ciqual"
                    else parse_usda_foundation_record(row)
                )
                if food:
                    records.append(food)
                    if limit and len(records) >= limit:
                        break

    return records
