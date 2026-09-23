package com.example.data

data class DefaultFoodSeed(
    val food: FoodItem,
    val portions: List<Pair<String, Float>> = emptyList()
)

object DefaultFoodCatalog {
    val items: List<DefaultFoodSeed> = listOf(
        // Carboidrati e Cereali
        DefaultFoodSeed(
            FoodItem(
                name = "Pasta di semola",
                brand = "Base",
                caloriesPer100g = 356,
                proteinPer100g = 12.5f,
                carbsPer100g = 72.0f,
                fatPer100g = 1.5f,
                defaultServingSize = 80
            ),
            portions = listOf("Porzione (80g)" to 80f, "Piatto abbondante (100g)" to 100f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Riso Basmati",
                brand = "Base",
                caloriesPer100g = 350,
                proteinPer100g = 8.5f,
                carbsPer100g = 77.0f,
                fatPer100g = 0.9f,
                defaultServingSize = 80
            ),
            portions = listOf("Porzione (80g)" to 80f, "Piatto abbondante (100g)" to 100f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Pane comune",
                brand = "Base",
                caloriesPer100g = 265,
                proteinPer100g = 9.0f,
                carbsPer100g = 49.0f,
                fatPer100g = 3.2f,
                defaultServingSize = 50
            ),
            portions = listOf("Fetta (30g)" to 30f, "Panino piccolo (50g)" to 50f, "Panino medio (80g)" to 80f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Fiocchi d'Avena",
                brand = "Base",
                caloriesPer100g = 372,
                proteinPer100g = 13.5f,
                carbsPer100g = 59.0f,
                fatPer100g = 7.0f,
                defaultServingSize = 40
            ),
            portions = listOf("Cucchiaio (15g)" to 15f, "Porzione porridge (40g)" to 40f, "Porzione abbondante (60g)" to 60f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Patate",
                brand = "Base",
                caloriesPer100g = 77,
                proteinPer100g = 2.0f,
                carbsPer100g = 17.5f,
                fatPer100g = 0.1f,
                defaultServingSize = 200
            ),
            portions = listOf("Patata piccola (150g)" to 150f, "Patata media (200g)" to 200f)
        ),
        // Proteine
        DefaultFoodSeed(
            FoodItem(
                name = "Petto di Pollo",
                brand = "Base",
                caloriesPer100g = 110,
                proteinPer100g = 23.5f,
                carbsPer100g = 0.0f,
                fatPer100g = 1.5f,
                defaultServingSize = 150
            ),
            portions = listOf("Fetta piccola (100g)" to 100f, "Fetta media (150g)" to 150f, "Fetta grande (200g)" to 200f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Uovo intero",
                brand = "Base",
                caloriesPer100g = 143,
                proteinPer100g = 12.8f,
                carbsPer100g = 0.7f,
                fatPer100g = 9.5f,
                defaultServingSize = 60
            ),
            portions = listOf("1 Uovo medio (60g)" to 60f, "2 Uova medie (120g)" to 120f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Albume d'Uovo",
                brand = "Base",
                caloriesPer100g = 52,
                proteinPer100g = 11.0f,
                carbsPer100g = 0.7f,
                fatPer100g = 0.2f,
                defaultServingSize = 100
            ),
            portions = listOf("Albume di 1 uovo (35g)" to 35f, "Porzione (100g)" to 100f, "Bicchiere (150g)" to 150f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Salmone fresco",
                brand = "Base",
                caloriesPer100g = 208,
                proteinPer100g = 20.0f,
                carbsPer100g = 0.0f,
                fatPer100g = 13.5f,
                defaultServingSize = 150
            ),
            portions = listOf("Trancio piccolo (120g)" to 120f, "Trancio medio (150g)" to 150f, "Filetto (200g)" to 200f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Tonno al naturale",
                brand = "Base",
                caloriesPer100g = 101,
                proteinPer100g = 24.0f,
                carbsPer100g = 0.0f,
                fatPer100g = 0.6f,
                defaultServingSize = 80
            ),
            portions = listOf("Scatoletta sgocciolata (56g)" to 56f, "Scatoletta grande (80g)" to 80f, "2 Scatolette (112g)" to 112f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Bresaola",
                brand = "Base",
                caloriesPer100g = 151,
                proteinPer100g = 32.0f,
                carbsPer100g = 0.5f,
                fatPer100g = 2.0f,
                defaultServingSize = 60
            ),
            portions = listOf("Monoporzione (50g)" to 50f, "Porzione (70g)" to 70f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Yogurt Greco 0%",
                brand = "Base",
                caloriesPer100g = 57,
                proteinPer100g = 10.3f,
                carbsPer100g = 3.6f,
                fatPer100g = 0.2f,
                defaultServingSize = 150
            ),
            portions = listOf("Vasetto (150g)" to 150f, "Vasetto grande (170g)" to 170f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Parmigiano Reggiano",
                brand = "Base",
                caloriesPer100g = 431,
                proteinPer100g = 38.0f,
                carbsPer100g = 0.0f,
                fatPer100g = 29.0f,
                defaultServingSize = 25
            ),
            portions = listOf("Cucchiaio grattugiato (10g)" to 10f, "Spicchio snack (25g)" to 25f, "Porzione (40g)" to 40f)
        ),
        // Grassi e Condimenti
        DefaultFoodSeed(
            FoodItem(
                name = "Olio Extravergine d'Oliva",
                brand = "Base",
                caloriesPer100g = 884,
                proteinPer100g = 0.0f,
                carbsPer100g = 0.0f,
                fatPer100g = 100.0f,
                defaultServingSize = 10
            ),
            portions = listOf("Cucchiaino (5g)" to 5f, "Cucchiaio (10g)" to 10f, "2 Cucchiai (20g)" to 20f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Noci sgusciate",
                brand = "Base",
                caloriesPer100g = 654,
                proteinPer100g = 15.0f,
                carbsPer100g = 14.0f,
                fatPer100g = 65.0f,
                defaultServingSize = 30
            ),
            portions = listOf("3 noci (15g)" to 15f, "Manciata (30g)" to 30f)
        ),
        // Frutta e Verdura
        DefaultFoodSeed(
            FoodItem(
                name = "Mela",
                brand = "Base",
                caloriesPer100g = 52,
                proteinPer100g = 0.3f,
                carbsPer100g = 13.8f,
                fatPer100g = 0.2f,
                defaultServingSize = 180
            ),
            portions = listOf("Mela piccola (150g)" to 150f, "Mela media (180g)" to 180f, "Mela grande (220g)" to 220f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Banana",
                brand = "Base",
                caloriesPer100g = 89,
                proteinPer100g = 1.1f,
                carbsPer100g = 22.8f,
                fatPer100g = 0.3f,
                defaultServingSize = 120
            ),
            portions = listOf("Banana piccola (100g)" to 100f, "Banana media sbucciata (120g)" to 120f, "Banana grande (150g)" to 150f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Pomodori",
                brand = "Base",
                caloriesPer100g = 18,
                proteinPer100g = 0.9f,
                carbsPer100g = 3.9f,
                fatPer100g = 0.2f,
                defaultServingSize = 150
            ),
            portions = listOf("Pomodoro medio (100g)" to 100f, "Insalata di pomodori (150g)" to 150f)
        ),
        DefaultFoodSeed(
            FoodItem(
                name = "Zucchine",
                brand = "Base",
                caloriesPer100g = 17,
                proteinPer100g = 1.2f,
                carbsPer100g = 3.1f,
                fatPer100g = 0.3f,
                defaultServingSize = 200
            ),
            portions = listOf("1 Zucchina media (150g)" to 150f, "Porzione contorno (200g)" to 200f)
        ),
        // Latticini e Bevande
        DefaultFoodSeed(
            FoodItem(
                name = "Latte Parzialmente Scremato",
                brand = "Base",
                caloriesPer100g = 47,
                proteinPer100g = 3.4f,
                carbsPer100g = 4.9f,
                fatPer100g = 1.6f,
                defaultServingSize = 200
            ),
            portions = listOf("Tazzina caffè (50g)" to 50f, "Bicchiere (150g)" to 150f, "Tazza colazione (200g)" to 200f, "Tazza grande (250g)" to 250f)
        )
    )
}
