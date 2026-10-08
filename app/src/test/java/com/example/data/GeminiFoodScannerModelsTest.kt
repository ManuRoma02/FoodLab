package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiFoodScannerModelsTest {

    private val moshi = GeminiFoodScannerService.moshi

    @Test
    fun parseValidGeminiFoodScanResultDirectly() {
        val json = """
            {
              "foodName": "Petto di pollo con riso",
              "estimatedGrams": 250,
              "calories": 360,
              "protein": 42.0,
              "carbs": 38.0,
              "fat": 4.5,
              "confidence": "HIGH",
              "notes": "Riso cotto e pollo ai ferri",
              "detectedItems": ["Pollo", "Riso basmati"]
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResult(json)

        assertEquals("Petto di pollo con riso", result.foodName)
        assertEquals(250, result.estimatedGrams)
        assertEquals(360, result.calories)
        assertEquals(42.0f, result.protein, 0.01f)
        assertEquals(38.0f, result.carbs, 0.01f)
        assertEquals(4.5f, result.fat, 0.01f)
        assertEquals("HIGH", result.confidence)
        assertEquals("Riso cotto e pollo ai ferri", result.notes)
        assertEquals(listOf("Pollo", "Riso basmati"), result.detectedItems)
    }

    @Test
    fun parseGeminiGenerateContentEnvelopeResponse() {
        val envelopeJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"foodName\":\"Salmone al forno con patate\",\"estimatedGrams\":300,\"calories\":450,\"protein\":35.0,\"carbs\":25.0,\"fat\":20.0,\"confidence\":\"HIGH\",\"notes\":\"Filetto di salmone e patate novelle\",\"detectedItems\":[\"Salmone\",\"Patate\"]}"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP",
                  "index": 0
                }
              ]
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResponse(envelopeJson)

        assertEquals("Salmone al forno con patate", result.foodName)
        assertEquals(300, result.estimatedGrams)
        assertEquals(450, result.calories)
        assertEquals(35.0f, result.protein, 0.01f)
        assertEquals(25.0f, result.carbs, 0.01f)
        assertEquals(20.0f, result.fat, 0.01f)
        assertEquals("HIGH", result.confidence)
        assertEquals("Filetto di salmone e patate novelle", result.notes)
        assertEquals(listOf("Salmone", "Patate"), result.detectedItems)
    }

    @Test
    fun parseMarkdownCodeFencedJsonInEnvelope() {
        val envelopeJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\n  \"foodName\": \"Insalata greca\",\n  \"estimatedGrams\": 200,\n  \"calories\": 220,\n  \"protein\": 8.0,\n  \"carbs\": 10.0,\n  \"fat\": 16.0,\n  \"confidence\": \"MEDIUM\",\n  \"notes\": \"Feta e olive kalamata\",\n  \"detectedItems\": [\"Feta\", \"Pomodori\", \"Olive\"]\n}\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResponse(envelopeJson)

        assertEquals("Insalata greca", result.foodName)
        assertEquals(200, result.estimatedGrams)
        assertEquals(220, result.calories)
        assertEquals(8.0f, result.protein, 0.01f)
        assertEquals(10.0f, result.carbs, 0.01f)
        assertEquals(16.0f, result.fat, 0.01f)
        assertEquals("MEDIUM", result.confidence)
        assertEquals(listOf("Feta", "Pomodori", "Olive"), result.detectedItems)
    }

    @Test
    fun fallbackAndHandlingOfMissingFields() {
        val partialJson = """
            {
              "foodName": "Mela Fuji"
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResult(partialJson)

        assertEquals("Mela Fuji", result.foodName)
        assertTrue("Estimated grams should have reasonable default", result.estimatedGrams > 0)
        assertEquals(0, result.calories)
        assertEquals(0f, result.protein, 0.01f)
        assertEquals(0f, result.carbs, 0.01f)
        assertEquals(0f, result.fat, 0.01f)
        assertEquals("MEDIUM", result.confidence)
        assertEquals(emptyList<String>(), result.detectedItems)
    }

    @Test
    fun fallbackAndHandlingOfNullFields() {
        val nullFieldsJson = """
            {
              "foodName": null,
              "estimatedGrams": null,
              "calories": null,
              "protein": null,
              "carbs": null,
              "fat": null,
              "confidence": null,
              "notes": null,
              "detectedItems": null
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResult(nullFieldsJson)

        assertTrue(result.foodName.isNotBlank())
        assertTrue(result.estimatedGrams >= 0)
        assertEquals(0, result.calories)
        assertEquals(0f, result.protein, 0.01f)
        assertEquals(0f, result.carbs, 0.01f)
        assertEquals(0f, result.fat, 0.01f)
        assertEquals("MEDIUM", result.confidence)
        assertEquals(emptyList<String>(), result.detectedItems)
    }

    @Test
    fun calculationConsistencyCaloriesFromMacrosWhenZero() {
        // When calories are 0 or omitted but macros are present,
        // calories should reconcile from macros: 4 * protein + 4 * carbs + 9 * fat
        val json = """
            {
              "foodName": "Shake Proteico",
              "estimatedGrams": 300,
              "calories": 0,
              "protein": 30.0,
              "carbs": 20.0,
              "fat": 5.0,
              "confidence": "HIGH"
            }
        """.trimIndent()

        val result = GeminiFoodScannerService.parseScanResult(json)

        // 30*4 + 20*4 + 5*9 = 120 + 80 + 45 = 245
        assertEquals(245, result.calories)
        assertEquals(245, result.calculateCaloriesFromMacros())
    }

    @Test
    fun calculationConsistencyScalingAndPer100g() {
        val original = GeminiFoodScanResult(
            foodName = "Pasta al pomodoro",
            estimatedGrams = 200,
            calories = 300,
            protein = 10f,
            carbs = 58f,
            fat = 2f,
            confidence = "HIGH",
            notes = "Spaghetti con sugo semplice",
            detectedItems = listOf("Pasta", "Pomodoro")
        )

        // Per 100g
        assertEquals(150, original.caloriesPer100g)
        assertEquals(5.0f, original.proteinPer100g, 0.01f)
        assertEquals(29.0f, original.carbsPer100g, 0.01f)
        assertEquals(1.0f, original.fatPer100g, 0.01f)

        // Scaled to 300g (1.5x)
        val scaled = original.scaledToGrams(300)
        assertEquals(300, scaled.estimatedGrams)
        assertEquals(450, scaled.calories)
        assertEquals(15.0f, scaled.protein, 0.01f)
        assertEquals(87.0f, scaled.carbs, 0.01f)
        assertEquals(3.0f, scaled.fat, 0.01f)
    }

    @Test
    fun confidenceNormalization() {
        val highResult = GeminiFoodScannerService.parseScanResult("""{"foodName":"Riso","confidence":"high"}""")
        assertEquals("HIGH", highResult.confidence)

        val lowResult = GeminiFoodScannerService.parseScanResult("""{"foodName":"Riso","confidence":"low"}""")
        assertEquals("LOW", lowResult.confidence)

        val unknownResult = GeminiFoodScannerService.parseScanResult("""{"foodName":"Riso","confidence":"VERY_HIGH"}""")
        assertEquals("MEDIUM", unknownResult.confidence)
    }

    @Test
    fun geminiRequestBuilderProducesValidPayloadWithBase64Image() {
        val dummyImageBytes = byteArrayOf(1, 2, 3, 4, 5)
        val request = GeminiFoodScannerService.buildGenerateContentRequest(dummyImageBytes)

        assertEquals(1, request.contents.size)
        val parts = request.contents[0].parts
        assertEquals(2, parts.size)
        assertNotNull(parts[0].text)
        assertNotNull(parts[1].inlineData)
        assertEquals("image/jpeg", parts[1].inlineData?.mimeType)
        assertEquals("application/json", request.generationConfig?.responseMimeType)
        assertNotNull(request.generationConfig?.responseSchema)
    }

    @Test
    fun gracefulMockOnMissingOrPlaceholderApiKey() {
        val service = GeminiFoodScannerService.create(
            apiKey = "MY_GEMINI_API_KEY",
            enableMockOnMissingKey = true
        )
        val result = kotlinx.coroutines.runBlocking {
            service.analyzeFoodImage(byteArrayOf(1, 2, 3))
        }

        assertTrue("Should return success mock result", result.isSuccess)
        val scan = result.getOrThrow()
        assertTrue(scan.foodName.isNotEmpty())
        assertTrue(scan.calories > 0)
        assertTrue(scan.estimatedGrams > 0)
    }
}
