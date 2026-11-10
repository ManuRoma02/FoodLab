"""Nutrient validation and normalization engine for FoodLab."""

import re
from typing import Any, Dict, List, Optional
from tools.dataset_builder.models import FoodRecord


def _parse_numeric(val: Any) -> Optional[float]:
    """Parse numeric values from int, float, or string representations."""
    if val is None:
        return None
    if isinstance(val, (int, float)):
        import math
        if math.isnan(val) or math.isinf(val):
            return None
        return float(val)
    if isinstance(val, str):
        cleaned = val.strip().lower()
        if not cleaned or cleaned in ("-", "--", "n/a", "na"):
            return None
        if cleaned in ("tr", "traces", "tracce"):
            return 0.0

        # Handle '< 0,1' or '<0.5'
        if cleaned.startswith("<"):
            cleaned = cleaned[1:].strip()

        # Handle units
        is_mg = False
        if cleaned.endswith("mg"):
            is_mg = True
            cleaned = cleaned[:-2].strip()
        elif cleaned.endswith("g"):
            cleaned = cleaned[:-1].strip()
        elif cleaned.endswith("kcal"):
            cleaned = cleaned[:-4].strip()
        elif cleaned.endswith("kj"):
            cleaned = cleaned[:-2].strip()

        # Replace European decimal comma
        cleaned = cleaned.replace(",", ".")

        # Extract first valid decimal number
        match = re.search(r"[-+]?\d*\.?\d+", cleaned)
        if match:
            try:
                num = float(match.group(0))
                if is_mg:
                    num /= 1000.0
                return num
            except ValueError:
                return None
    return None


def _get_field(raw: Dict[str, Any], keys: List[str]) -> Any:
    """Retrieve field value checking multiple aliases and nested dictionaries."""
    for key in keys:
        if key in raw and raw[key] is not None:
            return raw[key]

    # Check nested 'nutriments' (Open Food Facts format)
    nutriments = raw.get("nutriments")
    if isinstance(nutriments, dict):
        for key in keys:
            if key in nutriments and nutriments[key] is not None:
                return nutriments[key]

    # Check nested 'nutrition_table' (Scraper format)
    table = raw.get("nutrition_table")
    if isinstance(table, dict):
        for key in keys:
            if key in table and table[key] is not None:
                return table[key]

    return None


def validate_and_normalize(raw: Dict[str, Any], source: str = "") -> Optional[FoodRecord]:
    """
    Validate and normalize raw food data into a FoodRecord.

    Rejects records missing mandatory macronutrients (calories, protein, carbs, fat)
    or violating physical consistency (sum(macros) > 102g, negative values, severe
    discrepancy with Atwater factors).
    """
    if not isinstance(raw, dict):
        return None

    # 1. Product Name (Strictly mandatory)
    raw_name = _get_field(raw, ["name", "product_name", "title", "n"])
    if not isinstance(raw_name, str):
        return None
    name = raw_name.strip()
    if not name:
        return None

    # 2. Metadata (Brand, Category, Barcode)
    raw_brand = _get_field(raw, ["brand", "brands", "b"])
    brand = str(raw_brand).strip() if raw_brand is not None else ""

    raw_category = _get_field(raw, ["category", "categories", "c"])
    category = str(raw_category).strip() if raw_category is not None else ""

    raw_barcode = _get_field(raw, ["barcode", "code"])
    barcode = ""
    if raw_barcode is not None:
        cleaned_barcode = "".join(c for c in str(raw_barcode) if c.isdigit())
        if len(cleaned_barcode) >= 4:
            barcode = cleaned_barcode

    # 3. Serving size
    raw_serving = _get_field(raw, ["default_serving_size", "serving_size", "pt", "portion_weight"])
    default_serving_size = 100
    if raw_serving is not None:
        parsed_serving = _parse_numeric(raw_serving)
        if parsed_serving is not None and 1 <= parsed_serving <= 5000:
            default_serving_size = int(round(parsed_serving))

    # 4. Mandatory Macronutrients: Calories, Protein, Carbs, Fat
    raw_kcal = _get_field(raw, ["calories_100g", "calories", "energy-kcal_100g", "energy-kcal", "energy_kcal", "kcal", "k", "energia"])
    raw_protein = _get_field(raw, ["protein_100g", "protein", "proteins_100g", "proteins", "p", "proteine"])
    raw_carbs = _get_field(raw, ["carbs_100g", "carbs", "carbohydrates_100g", "carbohydrates", "cb", "carboidrati"])
    raw_fat = _get_field(raw, ["fat_100g", "fat", "f", "grassi"])

    kcal_val = _parse_numeric(raw_kcal)
    protein_val = _parse_numeric(raw_protein)
    carbs_val = _parse_numeric(raw_carbs)
    fat_val = _parse_numeric(raw_fat)

    if kcal_val is None or protein_val is None or carbs_val is None or fat_val is None:
        return None

    # Non-negative validation
    if kcal_val < 0 or protein_val < 0 or carbs_val < 0 or fat_val < 0:
        return None

    # Bound check for individual macros in 100g
    if protein_val > 100.5 or carbs_val > 100.5 or fat_val > 100.5:
        return None

    # Max physically possible calories per 100g (pure fat ~900 kcal)
    if kcal_val > 950:
        return None

    # Sum of primary macronutrients must not exceed 102.0g (allowing minor measurement rounding)
    total_macros = protein_val + carbs_val + fat_val
    if total_macros > 102.0:
        return None

    # 5. Secondary nutrients
    raw_sugars = _get_field(raw, ["sugars_100g", "sugars", "sugar", "di cui zuccheri", "zuccheri"])
    raw_sat_fat = _get_field(raw, ["saturated_fat_100g", "saturated_fat", "saturated-fat_100g", "sat_fat", "di cui acidi grassi saturi", "grassi saturi"])
    raw_polyols = _get_field(raw, ["polyols_100g", "polyols", "polioli"])
    raw_fiber = _get_field(raw, ["fiber_100g", "fiber", "fibres_100g", "fibre"])
    raw_salt = _get_field(raw, ["salt_100g", "salt", "sale"])
    raw_sodium = _get_field(raw, ["sodium_100g", "sodium", "sodio"])

    sugars_val = _parse_numeric(raw_sugars)
    sat_fat_val = _parse_numeric(raw_sat_fat)
    polyols_val = _parse_numeric(raw_polyols)
    fiber_val = _parse_numeric(raw_fiber)
    salt_val = _parse_numeric(raw_salt)
    sodium_val = _parse_numeric(raw_sodium)

    # Sanity checks on secondary nutrients
    if sugars_val is not None:
        if sugars_val < 0 or sugars_val > 100.5:
            return None
        if sugars_val > carbs_val + 2.0:
            return None
        sugars_val = round(sugars_val, 2)

    if sat_fat_val is not None:
        if sat_fat_val < 0 or sat_fat_val > 100.5:
            return None
        if sat_fat_val > fat_val + 2.0:
            return None
        sat_fat_val = round(sat_fat_val, 2)

    if polyols_val is not None:
        if polyols_val < 0 or polyols_val > 100.5:
            return None
        polyols_val = round(polyols_val, 2)

    if fiber_val is not None:
        if fiber_val < 0 or fiber_val > 100.5:
            return None
        fiber_val = round(fiber_val, 2)

    # Correlate Salt <-> Sodium (Salt ≈ Sodium * 2.5)
    if salt_val is not None and (salt_val < 0 or salt_val > 100.0):
        return None
    if sodium_val is not None and (sodium_val < 0 or sodium_val > 100.0):
        return None

    if salt_val is not None and sodium_val is None:
        sodium_val = round(salt_val / 2.5, 4)
    elif sodium_val is not None and salt_val is None:
        salt_val = round(sodium_val * 2.5, 3)

    if salt_val is not None:
        salt_val = round(salt_val, 3)
    if sodium_val is not None:
        sodium_val = round(sodium_val, 4)

    # 6. Physical energy consistency (Atwater check)
    # 4 kcal/g for protein and carbs, 9 kcal/g for fat
    effective_carbs = max(0.0, carbs_val - (polyols_val if polyols_val else 0.0))
    min_base_energy = protein_val * 4.0 + effective_carbs * 4.0 + fat_val * 9.0

    # Reject zero calories when substantial macros exist (e.g. 0 kcal with 50g fat)
    if kcal_val < 15 and min_base_energy > 30:
        return None

    # Reject high calories when macros are near zero
    if total_macros < 0.5 and kcal_val > 25:
        return None

    # Reject severe discrepancies (> 50% lower than theoretical min energy when energy is significant)
    if min_base_energy > 50 and kcal_val < min_base_energy * 0.40 - 20:
        return None

    return FoodRecord(
        name=name,
        brand=brand,
        category=category,
        barcode=barcode,
        calories_100g=int(round(kcal_val)),
        protein_100g=round(protein_val, 2),
        carbs_100g=round(carbs_val, 2),
        fat_100g=round(fat_val, 2),
        sugars_100g=sugars_val,
        saturated_fat_100g=sat_fat_val,
        polyols_100g=polyols_val,
        fiber_100g=fiber_val,
        salt_100g=salt_val,
        sodium_100g=sodium_val,
        default_serving_size=default_serving_size,
        source=source.strip(),
    )


class MacroQualityGate:
    """Quality gate validating macronutrient integrity and physical constraints."""

    @staticmethod
    def validate(raw: Dict[str, Any], source: str = "") -> Optional[FoodRecord]:
        return validate_and_normalize(raw, source)
