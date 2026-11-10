"""CREA (Consiglio per la ricerca in agricoltura e l'analisi dell'economia agraria)
and BDA-IEO (Banca Dati di Composizione degli Alimenti per Studi Epidemiologici in Italia)
ingestion module for FoodLab.

Parses official Italian nutrition tables, supporting both minified formats
(e.g., existing FoodLab crea_foods.json) and standard full-schema datasets.
"""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Tuple, Union

from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.validator import validate_and_normalize

# Mapping Italian / BDA-IEO synonyms to standardized keys recognized by validator
ITALIAN_KEY_MAPPINGS = {
    "nome": "name",
    "denominazione": "name",
    "alimento": "name",
    "categoria": "category",
    "gruppo": "category",
    "codice": "barcode",
    "marca": "brand",
    "energia_kcal": "calories_100g",
    "proteine_totali": "protein_100g",
    "carboidrati_disponibili": "carbs_100g",
    "lipidi": "fat_100g",
    "lipidi_totali": "fat_100g",
    "zuccheri_solubili": "sugars_100g",
    "acidi_grassi_saturi": "saturated_fat_100g",
    "saturi": "saturated_fat_100g",
    "polialcoli": "polyols_100g",
    "fibra": "fiber_100g",
    "fibra_alimentare": "fiber_100g",
    "peso_porzione": "portion_weight",
    "porzione_grammi": "portion_weight",
    "porzione": "portion_weight",
}


def _normalize_raw_dict(raw: Dict[str, Any]) -> Dict[str, Any]:
    """Map known Italian and domain-specific aliases into canonical dictionary keys."""
    normalized = dict(raw)
    for it_key, std_key in ITALIAN_KEY_MAPPINGS.items():
        if it_key in raw and raw[it_key] is not None and std_key not in normalized:
            normalized[std_key] = raw[it_key]
    return normalized


def _extract_portion_record(raw: Dict[str, Any], food: FoodRecord) -> PortionRecord | None:
    """Extract linked portion if explicit portion size is provided."""
    raw_portion = (
        raw.get("pt")
        or raw.get("peso_porzione")
        or raw.get("portion_weight")
        or raw.get("porzione_grammi")
        or raw.get("porzione")
        or raw.get("serving_size")
    )
    if raw_portion is None:
        return None

    try:
        if isinstance(raw_portion, str):
            cleaned = raw_portion.replace(",", ".").strip()
            import re
            m = re.search(r"[-+]?\d*\.?\d+", cleaned)
            if not m:
                return None
            weight = float(m.group(0))
        else:
            weight = float(raw_portion)

        if weight <= 0:
            return None
    except (ValueError, TypeError):
        return None

    portion_name = (
        raw.get("nome_porzione")
        or raw.get("portion_name")
        or raw.get("tipo_porzione")
        or raw.get("portion_desc")
        or "Porzione standard"
    )

    portion_key = food.barcode if food.barcode else food.name

    return PortionRecord(
        food_barcode_or_key=portion_key,
        name=str(portion_name).strip(),
        weight_grams=round(weight, 2),
    )


def extract_crea_foods(
    json_path: Union[str, Path],
    source: str = "CREA",
) -> Tuple[List[FoodRecord], List[PortionRecord]]:
    """
    Extract, validate, and normalize CREA / BDA-IEO food items and their portions from JSON.

    Supports both minified datasets (keys: n, c, k, p, cb, f, pt) and
    full standard/Italian schema datasets.

    Args:
        json_path: Path to the JSON or JSONL data file.
        source: Source attribution identifier (defaults to "CREA").

    Returns:
        A tuple of (foods, portions) containing valid FoodRecord and PortionRecord instances.
    """
    path = Path(json_path)
    if not path.is_file():
        raise FileNotFoundError(f"CREA dataset file not found: {json_path}")

    with open(path, "r", encoding="utf-8") as f:
        try:
            content = json.load(f)
        except json.JSONDecodeError:
            # Fallback for line-delimited JSON (.jsonl)
            f.seek(0)
            content = []
            for line in f:
                line_str = line.strip()
                if line_str:
                    content.append(json.loads(line_str))

    raw_items: List[Dict[str, Any]] = []
    if isinstance(content, list):
        raw_items = [item for item in content if isinstance(item, dict)]
    elif isinstance(content, dict):
        # Handle container dicts like {"alimenti": [...]}, {"foods": [...]}, etc.
        for container_key in ("alimenti", "foods", "data", "items", "products", "records"):
            if container_key in content and isinstance(content[container_key], list):
                raw_items = [item for item in content[container_key] if isinstance(item, dict)]
                break
        else:
            raw_items = [v for v in content.values() if isinstance(v, dict)]

    foods: List[FoodRecord] = []
    portions: List[PortionRecord] = []

    for item in raw_items:
        normalized = _normalize_raw_dict(item)
        food = validate_and_normalize(normalized, source=source)
        if food is None:
            continue

        foods.append(food)

        portion = _extract_portion_record(item, food)
        if portion is not None:
            portions.append(portion)

    return foods, portions


if __name__ == "__main__":
    import sys

    default_file = Path("app/src/main/assets/data/crea_foods.json")
    target_file = Path(sys.argv[1]) if len(sys.argv) > 1 else default_file

    if target_file.exists():
        foods_list, portions_list = extract_crea_foods(target_file)
        print(f"Extracted {len(foods_list)} foods and {len(portions_list)} portions from {target_file}")
    else:
        print(f"File not found: {target_file}")
