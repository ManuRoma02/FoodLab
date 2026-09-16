package com.example.data

import java.util.Locale

data class MappedOpenFoodFactsProduct(
    val food: FoodItem,
    val portion: FoodPortion?
)

object OpenFoodFactsMapper {
    private const val MAX_CALORIES_PER_100G = 900.0
    private const val MAX_MACRO_PER_100G = 100.0
    private const val DEFAULT_SERVING_GRAMS = 100

    fun map(
        product: OFFProduct,
        id: Int,
        fallbackName: String? = null
    ): MappedOpenFoodFactsProduct? {
        val name = product.product_name
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: fallbackName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return null

        val nutriments = product.nutriments
        val caloriesKcal = parseBounded(nutriments?.energy_kcal_100g, MAX_CALORIES_PER_100G)
        val caloriesFromKj = if (caloriesKcal == null) {
            val kj = parseBounded(nutriments?.energy_kj_100g ?: nutriments?.energy_100g, 4000.0)
            kj?.let { (it / 4.184).coerceIn(0.0, MAX_CALORIES_PER_100G) }
        } else null
        val calories = caloriesKcal ?: caloriesFromKj
        val protein = parseBounded(nutriments?.proteins_100g, MAX_MACRO_PER_100G)
        val carbs = parseBounded(nutriments?.carbohydrates_100g, MAX_MACRO_PER_100G)
        val fat = parseBounded(nutriments?.fat_100g, MAX_MACRO_PER_100G)

        val servingQuantity = parseBounded(product.serving_quantity, 5_000.0)
            ?.takeIf { it > 0.0 }
            ?.toInt()
            ?: DEFAULT_SERVING_GRAMS
        val parsedPortion = parseServingSize(product.serving_size, servingQuantity.toFloat())

        val food = FoodItem(
            id = id,
            name = name.take(160),
            brand = product.brands?.trim()?.takeIf { it.isNotEmpty() }?.take(160) ?: "Open Food Facts",
            caloriesPer100g = (calories ?: 0.0).toInt(),
            proteinPer100g = (protein ?: 0.0).toFloat(),
            carbsPer100g = (carbs ?: 0.0).toFloat(),
            fatPer100g = (fat ?: 0.0).toFloat(),
            defaultServingSize = parsedPortion?.second?.toInt()?.takeIf { it > 0 } ?: servingQuantity
        )

        return MappedOpenFoodFactsProduct(
            food = food,
            portion = parsedPortion?.let { (portionName, grams) ->
                FoodPortion(foodId = id, name = portionName, weightGrams = grams)
            }
        )
    }

    internal fun parseBounded(value: Any?, max: Double): Double? {
        val parsed = when (value) {
            is Number -> value.toDouble()
            is String -> value.trim().replace(',', '.').toDoubleOrNull()
            else -> value?.toString()?.trim()?.replace(',', '.')?.toDoubleOrNull()
        } ?: return null
        return parsed.takeIf { it.isFinite() && it in 0.0..max }
    }

    internal fun parseServingSize(servingSize: String?, fallbackGrams: Float): Pair<String, Float>? {
        val raw = servingSize?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val match = Regex("""(\d+(?:[\.,]\d+)?)\s*(kg|mg|g|ml|cl|l|oz)\b""", RegexOption.IGNORE_CASE)
            .findAll(raw)
            .lastOrNull()
            ?: return null
        val amount = match.groupValues[1].replace(',', '.').toFloatOrNull() ?: return null
        val grams = when (match.groupValues[2].lowercase(Locale.ROOT)) {
            "kg" -> amount * 1_000f
            "mg" -> amount / 1_000f
            "cl" -> amount * 10f
            "l" -> amount * 1_000f
            "oz" -> amount * 28.3495f
            else -> amount // g and ml use the conventional 1:1 food-tracking estimate.
        }.takeIf { it.isFinite() && it in 0.1f..5_000f } ?: fallbackGrams.takeIf { it > 0f } ?: return null

        val labelPrefix = raw.substring(0, match.range.first)
            .trim(' ', '(', '-', ':')
            .replace(Regex("""^[\d\s\./xX]+"""), "")
            .trim()
        val label = if (labelPrefix.isEmpty()) {
            "Serving"
        } else {
            labelPrefix.split(Regex("\\s+")).joinToString(" ") { word ->
                word.replaceFirstChar { character ->
                    if (character.isLowerCase()) character.titlecase(Locale.getDefault()) else character.toString()
                }
            }.take(60)
        }
        return label to grams
    }
}
