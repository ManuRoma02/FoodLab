"""Extract European and Italian-market sports nutrition and fitness foods from OFF dump."""

from __future__ import annotations

import csv
import gzip
import logging
from pathlib import Path
import re
from typing import Generator, Optional

from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.validator import validate_and_normalize

logger = logging.getLogger(__name__)

# Key sports nutrition & fitness brands popular among Italian gym/tracking users
FITNESS_BRANDS = [
    "myprotein", "prozis", "bulk powders", "gymbeam", "optimum nutrition",
    "yamamoto nutrition", "foodspring", "huel", "biotechusa", "biotech usa",
    "enervit", "enervit gymline", "namedsport", "named sport", "proaction",
    "multipower", "scitec", "scitec nutrition", "weider", "barebells",
    "isopure", "dymatize", "bsn", "cellucor", "muscletech"
]

BRAND_PATTERNS = [re.compile(rf"\b{re.escape(b)}\b", re.IGNORECASE) for b in FITNESS_BRANDS]

def is_fitness_product(brand_field: str) -> bool:
    if not brand_field:
        return False
    for pat in BRAND_PATTERNS:
        if pat.search(brand_field):
            return True
    return False

def stream_fitness_products_csv_gz(
    csv_gz_path: str,
    limit: Optional[int] = None
) -> Generator[tuple[FoodRecord, list[PortionRecord]], None, None]:
    path = Path(csv_gz_path)
    if not path.exists():
        logger.warning(f"Dump file {csv_gz_path} does not exist.")
        return

    count = 0
    with gzip.open(path, "rt", encoding="utf-8", errors="replace") as f:
        reader = csv.reader(f, delimiter="\t")
        try:
            headers = next(reader)
        except StopIteration:
            return

        col_map = {col.strip(): idx for idx, col in enumerate(headers)}
        c_code = col_map.get("code")
        c_name = col_map.get("product_name")
        c_brands = col_map.get("brands")
        c_energy_kcal = col_map.get("energy-kcal_100g")
        c_energy = col_map.get("energy_100g")
        c_proteins = col_map.get("proteins_100g")
        c_carbs = col_map.get("carbohydrates_100g")
        c_fat = col_map.get("fat_100g")
        c_sugars = col_map.get("sugars_100g")
        c_sat_fat = col_map.get("saturated-fat_100g")
        c_fiber = col_map.get("fiber_100g")
        c_salt = col_map.get("salt_100g")
        c_serving = col_map.get("serving_size")

        def val(row: list[str], idx: Optional[int]) -> Optional[str]:
            if idx is None or idx >= len(row):
                return None
            s = row[idx].strip()
            return s if s else None

        for row in reader:
            brands = val(row, c_brands) or ""
            if not is_fitness_product(brands):
                continue

            name = val(row, c_name) or ""
            if not name:
                continue

            kcal = val(row, c_energy_kcal)
            if not kcal:
                kj = val(row, c_energy)
                if kj:
                    try:
                        kcal = str(round(float(kj) / 4.184))
                    except ValueError:
                        pass

            raw_dict = {
                "barcode": val(row, c_code),
                "name": name,
                "brand": brands,
                "category": "Integrazione & Fitness",
                "calories_100g": kcal,
                "protein_100g": val(row, c_proteins),
                "carbs_100g": val(row, c_carbs),
                "fat_100g": val(row, c_fat),
                "sugars_100g": val(row, c_sugars),
                "saturated_fat_100g": val(row, c_sat_fat),
                "fiber_100g": val(row, c_fiber),
                "salt_100g": val(row, c_salt),
                "serving_size": val(row, c_serving),
            }

            food_rec = validate_and_normalize(raw_dict, source="FITNESS_EU")
            if food_rec:
                portions: list[PortionRecord] = []
                serving_str = val(row, c_serving)
                if serving_str:
                    match = re.search(r"(\d+(?:[.,]\d+)?)\s*g", serving_str, re.IGNORECASE)
                    if match:
                        try:
                            g = float(match.group(1).replace(",", "."))
                            if 5 <= g <= 2000:
                                portions.append(PortionRecord(
                                    food_barcode_or_key=food_rec.barcode or food_rec.name,
                                    name=f"Porzione ({int(round(g))}g)",
                                    weight_grams=round(g, 1)
                                ))
                        except ValueError:
                            pass
                yield food_rec, portions
                count += 1
                if limit and count >= limit:
                    break
