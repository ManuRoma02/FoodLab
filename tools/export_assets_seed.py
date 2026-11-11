"""Export compiled FoodLab foods and portions into the Android app asset seed."""

from __future__ import annotations

import json
from pathlib import Path
import sqlite3

def export_to_android_assets(db_path: str = "tools/dataset_builder/foodlab_dataset.db", asset_path: str = "app/src/main/assets/data/crea_foods.json"):
    conn = sqlite3.connect(db_path)
    cursor = conn.cursor()

    # Query foods joined with their primary portion if available
    cursor.execute("""
        SELECT f.id, f.name, f.brand, f.category, f.barcode, f.calories_100g, f.protein_100g, f.carbs_100g, f.fat_100g, f.default_serving_size,
               p.name, p.weight_grams
        FROM foods f
        LEFT JOIN food_portions p ON p.food_id = f.id
        ORDER BY f.id ASC
    """)
    rows = cursor.fetchall()
    conn.close()

    exported_items = []
    seen_ids = set()

    for row in rows:
        fid, name, brand, cat, barcode, kcal, p, cb, f, def_serving, p_name, p_weight = row
        if fid in seen_ids:
            continue
        seen_ids.add(fid)

        item = {
            "n": name.strip(),
            "c": (brand or cat or "CREA Italia").strip(),
            "k": int(round(kcal)),
            "p": round(float(p), 1),
            "cb": round(float(cb), 1),
            "f": round(float(f), 1),
        }

        serving = p_weight if p_weight and p_weight > 0 else (def_serving if def_serving and def_serving > 0 else 100)
        item["pt"] = int(round(serving))

        if p_name and p_name.strip() and p_name.strip() != f"Porzione {int(round(serving))}g":
            item["pn"] = p_name.strip()

        if barcode and str(barcode).strip():
            item["b"] = str(barcode).strip()

        exported_items.append(item)

    out_file = Path(asset_path)
    out_file.parent.mkdir(parents=True, exist_ok=True)
    with open(out_file, "w", encoding="utf-8") as out:
        json.dump(exported_items, out, ensure_ascii=False, separators=(',', ':'))

    print(f"Successfully exported {len(exported_items)} foods into {asset_path} ({out_file.stat().st_size / 1024:.1f} KB)")

if __name__ == "__main__":
    export_to_android_assets()
