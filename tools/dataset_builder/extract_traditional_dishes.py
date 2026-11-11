"""Italian Traditional Dishes and Prepared Meals Dataset for FoodLab.

Provides standardized macro and portion profiles for Italian home and restaurant dishes
(pastas, pizzas, meats, fish, vegetable bakes, regional specialties, and desserts)
based on official Italian culinary and nutritional standards (CREA / INRAN).
"""

from __future__ import annotations

from typing import List, Tuple
from tools.dataset_builder.models import FoodRecord, PortionRecord
from tools.dataset_builder.validator import validate_and_normalize

TRADITIONAL_DISHES_DATA = [
    # --- PRIMI PIATTI ---
    {
        "name": "Spaghetti alla Carbonara",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 245,
        "protein_100g": 11.8,
        "carbs_100g": 26.5,
        "fat_100g": 10.4,
        "sugars_100g": 1.1,
        "saturated_fat_100g": 3.8,
        "salt_100g": 0.9,
        "portion_name": "Piatto standard (300g)",
        "portion_grams": 300.0
    },
    {
        "name": "Bucatini all'Amatriciana",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 210,
        "protein_100g": 8.9,
        "carbs_100g": 25.2,
        "fat_100g": 8.1,
        "sugars_100g": 2.2,
        "saturated_fat_100g": 2.7,
        "salt_100g": 0.8,
        "portion_name": "Piatto standard (300g)",
        "portion_grams": 300.0
    },
    {
        "name": "Spaghetti Cacio e Pepe",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 230,
        "protein_100g": 9.8,
        "carbs_100g": 28.5,
        "fat_100g": 8.4,
        "sugars_100g": 1.0,
        "saturated_fat_100g": 4.2,
        "salt_100g": 0.7,
        "portion_name": "Piatto standard (280g)",
        "portion_grams": 280.0
    },
    {
        "name": "Tagliatelle al Ragù Bolognese",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 195,
        "protein_100g": 9.2,
        "carbs_100g": 23.4,
        "fat_100g": 7.1,
        "sugars_100g": 2.0,
        "saturated_fat_100g": 2.4,
        "salt_100g": 0.8,
        "portion_name": "Piatto standard (320g)",
        "portion_grams": 320.0
    },
    {
        "name": "Lasagne alla Bolognese al Forno",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 185,
        "protein_100g": 8.8,
        "carbs_100g": 18.5,
        "fat_100g": 8.4,
        "sugars_100g": 2.5,
        "saturated_fat_100g": 3.5,
        "salt_100g": 0.9,
        "portion_name": "Porzione media (300g)",
        "portion_grams": 300.0
    },
    {
        "name": "Pasta al Pomodoro e Basilico",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 140,
        "protein_100g": 4.5,
        "carbs_100g": 25.8,
        "fat_100g": 2.1,
        "sugars_100g": 2.4,
        "saturated_fat_100g": 0.4,
        "salt_100g": 0.5,
        "portion_name": "Piatto standard (280g)",
        "portion_grams": 280.0
    },
    {
        "name": "Pasta al Pesto Genovese",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 235,
        "protein_100g": 6.8,
        "carbs_100g": 27.2,
        "fat_100g": 10.9,
        "sugars_100g": 1.2,
        "saturated_fat_100g": 2.1,
        "salt_100g": 0.7,
        "portion_name": "Piatto standard (280g)",
        "portion_grams": 280.0
    },
    {
        "name": "Risotto allo Zafferano (alla Milanese)",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 175,
        "protein_100g": 4.8,
        "carbs_100g": 25.6,
        "fat_100g": 5.9,
        "sugars_100g": 0.5,
        "saturated_fat_100g": 2.8,
        "salt_100g": 0.6,
        "portion_name": "Piatto standard (280g)",
        "portion_grams": 280.0
    },
    {
        "name": "Risotto ai Funghi Porcini",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 160,
        "protein_100g": 4.2,
        "carbs_100g": 24.5,
        "fat_100g": 5.1,
        "sugars_100g": 0.8,
        "saturated_fat_100g": 2.2,
        "salt_100g": 0.6,
        "portion_name": "Piatto standard (280g)",
        "portion_grams": 280.0
    },
    {
        "name": "Gnocchi alla Sorrentina",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 165,
        "protein_100g": 6.2,
        "carbs_100g": 22.8,
        "fat_100g": 5.4,
        "sugars_100g": 1.8,
        "saturated_fat_100g": 2.6,
        "salt_100g": 0.7,
        "portion_name": "Piatto standard (320g)",
        "portion_grams": 320.0
    },
    {
        "name": "Tortellini di Carne in Brodo",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 95,
        "protein_100g": 5.8,
        "carbs_100g": 12.4,
        "fat_100g": 2.4,
        "sugars_100g": 0.4,
        "saturated_fat_100g": 0.8,
        "salt_100g": 0.8,
        "portion_name": "Piatto fondo (350g)",
        "portion_grams": 350.0
    },
    {
        "name": "Minestrone di Verdure con Legumi",
        "category": "Primi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 55,
        "protein_100g": 2.8,
        "carbs_100g": 8.5,
        "fat_100g": 1.1,
        "sugars_100g": 2.1,
        "saturated_fat_100g": 0.2,
        "salt_100g": 0.5,
        "portion_name": "Porzione media (350g)",
        "portion_grams": 350.0
    },

    # --- PIZZE & RUSTICI ---
    {
        "name": "Pizza Margherita al Piatto",
        "category": "Pizze & Rustici",
        "brand": "Pizzeria Tradizionale",
        "calories_100g": 240,
        "protein_100g": 9.5,
        "carbs_100g": 34.0,
        "fat_100g": 7.2,
        "sugars_100g": 2.4,
        "saturated_fat_100g": 2.9,
        "salt_100g": 1.2,
        "portion_name": "Pizza intera (350g)",
        "portion_grams": 350.0
    },
    {
        "name": "Pizza Marinara al Piatto",
        "category": "Pizze & Rustici",
        "brand": "Pizzeria Tradizionale",
        "calories_100g": 205,
        "protein_100g": 6.8,
        "carbs_100g": 36.5,
        "fat_100g": 3.6,
        "sugars_100g": 2.2,
        "saturated_fat_100g": 0.6,
        "salt_100g": 1.1,
        "portion_name": "Pizza intera (320g)",
        "portion_grams": 320.0
    },
    {
        "name": "Pizza Diavola (con Salame Piccante)",
        "category": "Pizze & Rustici",
        "brand": "Pizzeria Tradizionale",
        "calories_100g": 265,
        "protein_100g": 11.2,
        "carbs_100g": 32.5,
        "fat_100g": 9.8,
        "sugars_100g": 2.1,
        "saturated_fat_100g": 3.9,
        "salt_100g": 1.5,
        "portion_name": "Pizza intera (370g)",
        "portion_grams": 370.0
    },
    {
        "name": "Pizza Quattro Formaggi",
        "category": "Pizze & Rustici",
        "brand": "Pizzeria Tradizionale",
        "calories_100g": 280,
        "protein_100g": 12.5,
        "carbs_100g": 31.0,
        "fat_100g": 11.8,
        "sugars_100g": 1.8,
        "saturated_fat_100g": 5.8,
        "salt_100g": 1.4,
        "portion_name": "Pizza intera (360g)",
        "portion_grams": 360.0
    },
    {
        "name": "Pizza Capricciosa",
        "category": "Pizze & Rustici",
        "brand": "Pizzeria Tradizionale",
        "calories_100g": 245,
        "protein_100g": 10.4,
        "carbs_100g": 30.5,
        "fat_100g": 8.7,
        "sugars_100g": 2.0,
        "saturated_fat_100g": 3.1,
        "salt_100g": 1.3,
        "portion_name": "Pizza intera (390g)",
        "portion_grams": 390.0
    },
    {
        "name": "Focaccia Genovese all'Olio",
        "category": "Pizze & Rustici",
        "brand": "Panetteria Tradizionale",
        "calories_100g": 310,
        "protein_100g": 7.5,
        "carbs_100g": 48.0,
        "fat_100g": 9.8,
        "sugars_100g": 1.5,
        "saturated_fat_100g": 1.4,
        "salt_100g": 1.6,
        "portion_name": "Trancio (150g)",
        "portion_grams": 150.0
    },
    {
        "name": "Focaccia di Recco col Formaggio",
        "category": "Pizze & Rustici",
        "brand": "Panetteria Tradizionale",
        "calories_100g": 320,
        "protein_100g": 11.0,
        "carbs_100g": 32.0,
        "fat_100g": 16.5,
        "sugars_100g": 1.2,
        "saturated_fat_100g": 7.8,
        "salt_100g": 1.2,
        "portion_name": "Porzione (180g)",
        "portion_grams": 180.0
    },
    {
        "name": "Piadina Romagnola (Crudo, Squacquerone e Rucola)",
        "category": "Pizze & Rustici",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 265,
        "protein_100g": 11.5,
        "carbs_100g": 31.0,
        "fat_100g": 10.4,
        "sugars_100g": 1.4,
        "saturated_fat_100g": 4.5,
        "salt_100g": 1.4,
        "portion_name": "Piadina farcita (220g)",
        "portion_grams": 220.0
    },
    {
        "name": "Parmigiana di Melanzane",
        "category": "Pizze & Rustici",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 175,
        "protein_100g": 6.8,
        "carbs_100g": 7.5,
        "fat_100g": 13.2,
        "sugars_100g": 3.8,
        "saturated_fat_100g": 4.5,
        "salt_100g": 0.8,
        "portion_name": "Porzione media (250g)",
        "portion_grams": 250.0
    },
    {
        "name": "Arancino al Ragù Siciliano",
        "category": "Pizze & Rustici",
        "brand": "Rosticceria Tradizionale",
        "calories_100g": 215,
        "protein_100g": 6.5,
        "carbs_100g": 28.0,
        "fat_100g": 8.6,
        "sugars_100g": 1.5,
        "saturated_fat_100g": 2.8,
        "salt_100g": 0.9,
        "portion_name": "1 pezzo (200g)",
        "portion_grams": 200.0
    },
    {
        "name": "Supplì al Telefono Romano",
        "category": "Pizze & Rustici",
        "brand": "Rosticceria Tradizionale",
        "calories_100g": 210,
        "protein_100g": 6.2,
        "carbs_100g": 27.5,
        "fat_100g": 8.4,
        "sugars_100g": 1.4,
        "saturated_fat_100g": 2.5,
        "salt_100g": 0.9,
        "portion_name": "1 pezzo (120g)",
        "portion_grams": 120.0
    },

    # --- SECONDI PIATTI ---
    {
        "name": "Cotoletta alla Milanese",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 255,
        "protein_100g": 19.5,
        "carbs_100g": 11.2,
        "fat_100g": 14.8,
        "sugars_100g": 0.5,
        "saturated_fat_100g": 4.8,
        "salt_100g": 0.8,
        "portion_name": "Porzione media (180g)",
        "portion_grams": 180.0
    },
    {
        "name": "Pollo Arrosto con Patate al Forno",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 165,
        "protein_100g": 15.8,
        "carbs_100g": 8.5,
        "fat_100g": 7.5,
        "sugars_100g": 0.5,
        "saturated_fat_100g": 1.8,
        "salt_100g": 0.7,
        "portion_name": "Porzione media (350g)",
        "portion_grams": 350.0
    },
    {
        "name": "Polpette di Manzo al Sugo",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 175,
        "protein_100g": 14.2,
        "carbs_100g": 7.2,
        "fat_100g": 9.8,
        "sugars_100g": 2.5,
        "saturated_fat_100g": 3.2,
        "salt_100g": 0.8,
        "portion_name": "Porzione (220g - 4 polpette)",
        "portion_grams": 220.0
    },
    {
        "name": "Tagliata di Manzo al Rosmarino",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 180,
        "protein_100g": 24.5,
        "carbs_100g": 0.2,
        "fat_100g": 9.1,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 3.4,
        "salt_100g": 0.6,
        "portion_name": "Porzione standard (250g)",
        "portion_grams": 250.0
    },
    {
        "name": "Scaloppine al Limone",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 160,
        "protein_100g": 18.2,
        "carbs_100g": 3.5,
        "fat_100g": 8.1,
        "sugars_100g": 0.5,
        "saturated_fat_100g": 2.1,
        "salt_100g": 0.7,
        "portion_name": "Porzione (180g)",
        "portion_grams": 180.0
    },
    {
        "name": "Orata al Forno con Patate e Pomodorini",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 125,
        "protein_100g": 14.5,
        "carbs_100g": 6.8,
        "fat_100g": 4.2,
        "sugars_100g": 1.1,
        "saturated_fat_100g": 0.8,
        "salt_100g": 0.6,
        "portion_name": "Porzione media (300g)",
        "portion_grams": 300.0
    },
    {
        "name": "Calamari alla Griglia con Limone e Prezzemolo",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 105,
        "protein_100g": 16.8,
        "carbs_100g": 1.2,
        "fat_100g": 3.4,
        "sugars_100g": 0.0,
        "saturated_fat_100g": 0.6,
        "salt_100g": 0.7,
        "portion_name": "Porzione (200g)",
        "portion_grams": 200.0
    },
    {
        "name": "Frittata di Zucchine",
        "category": "Secondi Piatti Tradizionali",
        "brand": "Cucina Tradizionale Italiana",
        "calories_100g": 145,
        "protein_100g": 9.5,
        "carbs_100g": 3.2,
        "fat_100g": 10.4,
        "sugars_100g": 1.8,
        "saturated_fat_100g": 2.8,
        "salt_100g": 0.6,
        "portion_name": "Porzione (180g)",
        "portion_grams": 180.0
    },

    # --- DOLCI TRADIZIONALI ---
    {
        "name": "Tiramisù Tradizionale al Caffè e Mascarpone",
        "category": "Dolci Tradizionali",
        "brand": "Pasticceria Tradizionale",
        "calories_100g": 285,
        "protein_100g": 5.2,
        "carbs_100g": 31.0,
        "fat_100g": 15.8,
        "sugars_100g": 22.5,
        "saturated_fat_100g": 9.8,
        "salt_100g": 0.3,
        "portion_name": "Porzione (150g)",
        "portion_grams": 150.0
    },
    {
        "name": "Panna Cotta con Frutti di Bosco",
        "category": "Dolci Tradizionali",
        "brand": "Pasticceria Tradizionale",
        "calories_100g": 240,
        "protein_100g": 3.8,
        "carbs_100g": 24.5,
        "fat_100g": 14.2,
        "sugars_100g": 21.0,
        "saturated_fat_100g": 8.9,
        "salt_100g": 0.2,
        "portion_name": "Coppetta (130g)",
        "portion_grams": 130.0
    },
    {
        "name": "Crostata alla Marmellata di Albicocche",
        "category": "Dolci Tradizionali",
        "brand": "Pasticceria Tradizionale",
        "calories_100g": 360,
        "protein_100g": 5.1,
        "carbs_100g": 62.0,
        "fat_100g": 10.5,
        "sugars_100g": 34.0,
        "saturated_fat_100g": 4.8,
        "salt_100g": 0.3,
        "portion_name": "Fetta media (100g)",
        "portion_grams": 100.0
    },
    {
        "name": "Cannolo Siciliano con Ricotta e Gocce di Cioccolato",
        "category": "Dolci Tradizionali",
        "brand": "Pasticceria Tradizionale",
        "calories_100g": 320,
        "protein_100g": 7.8,
        "carbs_100g": 38.5,
        "fat_100g": 14.8,
        "sugars_100g": 24.0,
        "saturated_fat_100g": 7.2,
        "salt_100g": 0.4,
        "portion_name": "1 cannolo (120g)",
        "portion_grams": 120.0
    }
]

def extract_traditional_dishes() -> Tuple[List[FoodRecord], List[PortionRecord]]:
    foods: List[FoodRecord] = []
    portions: List[PortionRecord] = []

    for item in TRADITIONAL_DISHES_DATA:
        food_rec = validate_and_normalize(item, source="TRADITIONAL_IT")
        if food_rec:
            foods.append(food_rec)
            p_name = item.get("portion_name")
            p_grams = item.get("portion_grams")
            if p_name and p_grams:
                portions.append(PortionRecord(
                    food_barcode_or_key=food_rec.name,
                    name=p_name,
                    weight_grams=float(p_grams)
                ))

    return foods, portions
