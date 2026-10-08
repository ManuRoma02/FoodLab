package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Locale
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class GeminiFoodScanResult(
    @param:Json(name = "foodName") val foodName: String = "Pasto stimato",
    @param:Json(name = "estimatedGrams") val estimatedGrams: Int = 100,
    @param:Json(name = "calories") val calories: Int = 0,
    @param:Json(name = "protein") val protein: Float = 0f,
    @param:Json(name = "carbs") val carbs: Float = 0f,
    @param:Json(name = "fat") val fat: Float = 0f,
    @param:Json(name = "confidence") val confidence: String = "MEDIUM",
    @param:Json(name = "notes") val notes: String? = null,
    @param:Json(name = "detectedItems") val detectedItems: List<String> = emptyList()
) {
    /**
     * Calculates estimated calories from macronutrients using the Atwater 4-4-9 formula:
     * 4 kcal/g protein, 4 kcal/g carbohydrates, 9 kcal/g fat.
     */
    fun calculateCaloriesFromMacros(): Int {
        return (protein * 4f + carbs * 4f + fat * 9f).roundToInt()
    }

    /**
     * Calories per 100g of estimated portion.
     */
    val caloriesPer100g: Int
        get() = if (estimatedGrams > 0) {
            ((calories.toFloat() / estimatedGrams.toFloat()) * 100f).roundToInt()
        } else {
            calories
        }

    /**
     * Protein in grams per 100g of estimated portion.
     */
    val proteinPer100g: Float
        get() = if (estimatedGrams > 0) {
            (protein / estimatedGrams.toFloat()) * 100f
        } else {
            protein
        }

    /**
     * Carbohydrates in grams per 100g of estimated portion.
     */
    val carbsPer100g: Float
        get() = if (estimatedGrams > 0) {
            (carbs / estimatedGrams.toFloat()) * 100f
        } else {
            carbs
        }

    /**
     * Fat in grams per 100g of estimated portion.
     */
    val fatPer100g: Float
        get() = if (estimatedGrams > 0) {
            (fat / estimatedGrams.toFloat()) * 100f
        } else {
            fat
        }

    /**
     * Scale calories and macros proportionally to a target weight in grams.
     */
    fun scaledToGrams(targetGrams: Int): GeminiFoodScanResult {
        if (targetGrams <= 0 || estimatedGrams <= 0) {
            return copy(estimatedGrams = targetGrams.coerceAtLeast(0))
        }
        val ratio = targetGrams.toFloat() / estimatedGrams.toFloat()
        return copy(
            estimatedGrams = targetGrams,
            calories = (calories * ratio).roundToInt(),
            protein = protein * ratio,
            carbs = carbs * ratio,
            fat = fat * ratio
        )
    }

    /**
     * Returns a normalized copy ensuring valid confidence and non-negative values.
     */
    fun normalized(): GeminiFoodScanResult {
        val normalizedConf = when (confidence.trim().uppercase(Locale.ROOT)) {
            "HIGH" -> "HIGH"
            "LOW" -> "LOW"
            else -> "MEDIUM"
        }
        val safeGrams = estimatedGrams.coerceAtLeast(1)
        val safeProtein = protein.coerceAtLeast(0f)
        val safeCarbs = carbs.coerceAtLeast(0f)
        val safeFat = fat.coerceAtLeast(0f)
        var safeCalories = calories.coerceAtLeast(0)
        if (safeCalories == 0 && (safeProtein > 0f || safeCarbs > 0f || safeFat > 0f)) {
            safeCalories = (safeProtein * 4f + safeCarbs * 4f + safeFat * 9f).roundToInt()
        }
        return copy(
            foodName = foodName.trim().ifEmpty { "Pasto stimato" },
            estimatedGrams = safeGrams,
            calories = safeCalories,
            protein = safeProtein,
            carbs = safeCarbs,
            fat = safeFat,
            confidence = normalizedConf,
            notes = notes?.trim()?.ifEmpty { null },
            detectedItems = detectedItems.mapNotNull { it.trim().ifEmpty { null } }
        )
    }
}

/**
 * Resilient DTO to gracefully handle varied or nullable types from Gemini JSON responses.
 */
@JsonClass(generateAdapter = true)
data class GeminiFoodScanRawDto(
    @param:Json(name = "foodName") val foodName: String? = null,
    @param:Json(name = "estimatedGrams") val estimatedGrams: Any? = null,
    @param:Json(name = "calories") val calories: Any? = null,
    @param:Json(name = "protein") val protein: Any? = null,
    @param:Json(name = "carbs") val carbs: Any? = null,
    @param:Json(name = "fat") val fat: Any? = null,
    @param:Json(name = "confidence") val confidence: String? = null,
    @param:Json(name = "notes") val notes: String? = null,
    @param:Json(name = "detectedItems") val detectedItems: List<Any?>? = null
) {
    fun toScanResult(): GeminiFoodScanResult {
        val name = foodName?.trim()?.ifEmpty { null } ?: "Pasto stimato"
        val grams = parseNumber(estimatedGrams)?.roundToInt()?.coerceAtLeast(1) ?: 100
        var cals = parseNumber(calories)?.roundToInt()?.coerceAtLeast(0) ?: 0
        val prot = parseNumber(protein)?.coerceAtLeast(0f) ?: 0f
        val carb = parseNumber(carbs)?.coerceAtLeast(0f) ?: 0f
        val fats = parseNumber(fat)?.coerceAtLeast(0f) ?: 0f

        if (cals == 0 && (prot > 0f || carb > 0f || fats > 0f)) {
            cals = (prot * 4f + carb * 4f + fats * 9f).roundToInt()
        }

        val conf = when (confidence?.trim()?.uppercase(Locale.ROOT)) {
            "HIGH" -> "HIGH"
            "LOW" -> "LOW"
            else -> "MEDIUM"
        }

        val items = detectedItems?.mapNotNull { item ->
            when (item) {
                is String -> item.trim().ifEmpty { null }
                null -> null
                else -> item.toString().trim().ifEmpty { null }
            }
        } ?: emptyList()

        return GeminiFoodScanResult(
            foodName = name,
            estimatedGrams = grams,
            calories = cals,
            protein = prot,
            carbs = carb,
            fat = fats,
            confidence = conf,
            notes = notes?.trim()?.ifEmpty { null },
            detectedItems = items
        )
    }

    private fun parseNumber(value: Any?): Float? {
        if (value == null) return null
        return when (value) {
            is Number -> value.toFloat()
            is String -> {
                val clean = value.trim().replace(',', '.')
                clean.toFloatOrNull()
            }
            else -> null
        }
    }
}

// -------------------------------------------------------------
// Gemini REST API GenerateContent Request & Response Models
// -------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentRequest(
    @param:Json(name = "contents") val contents: List<GeminiContent>,
    @param:Json(name = "system_instruction") val systemInstruction: GeminiContent? = null,
    @param:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @param:Json(name = "role") val role: String? = null,
    @param:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @param:Json(name = "text") val text: String? = null,
    @param:Json(name = "inline_data") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @param:Json(name = "mime_type") val mimeType: String,
    @param:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @param:Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @param:Json(name = "responseSchema") val responseSchema: GeminiSchema? = null,
    @param:Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchema(
    @param:Json(name = "type") val type: String,
    @param:Json(name = "properties") val properties: Map<String, GeminiSchemaProperty>? = null,
    @param:Json(name = "required") val required: List<String>? = null,
    @param:Json(name = "items") val items: GeminiSchema? = null,
    @param:Json(name = "enum") val enumValues: List<String>? = null,
    @param:Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiSchemaProperty(
    @param:Json(name = "type") val type: String,
    @param:Json(name = "description") val description: String? = null,
    @param:Json(name = "enum") val enumValues: List<String>? = null,
    @param:Json(name = "items") val items: GeminiSchema? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerateContentResponse(
    @param:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null,
    @param:Json(name = "promptFeedback") val promptFeedback: GeminiPromptFeedback? = null,
    @param:Json(name = "error") val error: GeminiApiError? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @param:Json(name = "content") val content: GeminiContent? = null,
    @param:Json(name = "finishReason") val finishReason: String? = null,
    @param:Json(name = "index") val index: Int? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPromptFeedback(
    @param:Json(name = "blockReason") val blockReason: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiApiError(
    @param:Json(name = "code") val code: Int? = null,
    @param:Json(name = "message") val message: String? = null,
    @param:Json(name = "status") val status: String? = null
)
