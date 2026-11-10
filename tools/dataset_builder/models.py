"""Data models for FoodLab Italian Food & Nutrition Dataset Pipeline."""

from dataclasses import dataclass
from typing import Optional


@dataclass
class FoodRecord:
    """Normalized food item with 100g macronutrient and micronutrient values."""

    name: str
    brand: str = ""
    category: str = ""
    barcode: str = ""
    calories_100g: int = 0
    protein_100g: float = 0.0
    carbs_100g: float = 0.0
    fat_100g: float = 0.0
    sugars_100g: Optional[float] = None
    saturated_fat_100g: Optional[float] = None
    polyols_100g: Optional[float] = None
    fiber_100g: Optional[float] = None
    salt_100g: Optional[float] = None
    sodium_100g: Optional[float] = None
    default_serving_size: int = 100
    source: str = ""


@dataclass
class PortionRecord:
    """Serving size portion associated with a food record by barcode or identifier key."""

    food_barcode_or_key: str
    name: str
    weight_grams: float
