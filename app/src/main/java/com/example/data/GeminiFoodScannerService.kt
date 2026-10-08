package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.ByteString.Companion.toByteString
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiFoodScannerService {

    /**
     * Analyzes meal photo bytes (JPEG) using Gemini Vision API with structured JSON output.
     * Returns a [Result] wrapping [GeminiFoodScanResult].
     */
    suspend fun analyzeFoodImage(jpegBytes: ByteArray): Result<GeminiFoodScanResult>

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

        val moshi: Moshi by lazy {
            Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        }

        fun create(
            apiKey: String = BuildConfig.GEMINI_API_KEY,
            okHttpClient: OkHttpClient? = null,
            model: String = DEFAULT_MODEL,
            enableMockOnMissingKey: Boolean = true
        ): GeminiFoodScannerService {
            return RealGeminiFoodScannerService(
                apiKey = apiKey,
                okHttpClient = okHttpClient ?: defaultOkHttpClient(),
                model = model,
                enableMockOnMissingKey = enableMockOnMissingKey
            )
        }

        fun defaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }

        fun isApiKeyConfigured(apiKey: String?): Boolean {
            return !apiKey.isNullOrBlank() &&
                    apiKey != "MY_GEMINI_API_KEY" &&
                    !apiKey.startsWith("placeholder", ignoreCase = true)
        }

        fun createMockResult(foodName: String = "Piatto bilanciato (Demo)"): GeminiFoodScanResult {
            return GeminiFoodScanResult(
                foodName = foodName,
                estimatedGrams = 320,
                calories = 480,
                protein = 36.0f,
                carbs = 50.0f,
                fat = 14.0f,
                confidence = "MEDIUM",
                notes = "Modalità dimostrativa: GEMINI_API_KEY non configurata. Risultato stimato di esempio.",
                detectedItems = listOf("Carboidrati complessi", "Proteine magre", "Verdure di stagione")
            )
        }

        fun buildGenerateContentRequest(jpegBytes: ByteArray): GeminiGenerateContentRequest {
            val base64Data = jpegBytes.toByteString().base64()

            val systemPrompt = "Sei un nutrizionista esperto. Analizza l'immagine fornita del piatto o del cibo. " +
                    "Stima con la massima accuratezza possibile il nome del piatto in italiano, il peso totale della porzione visibile in grammi, " +
                    "le calorie totali (kcal), i macronutrienti (proteine, carboidrati, grassi), il livello di confidenza e i singoli ingredienti identificati. " +
                    "Rispondi esclusivamente con un oggetto JSON valido strutturato."

            val userPrompt = "Analizza questo piatto e restituisci la stima nutrizionale completa per la porzione visibile sul piatto."

            val schema = GeminiSchema(
                type = "OBJECT",
                properties = mapOf(
                    "foodName" to GeminiSchemaProperty(type = "STRING", description = "Nome del piatto o cibo identificato"),
                    "estimatedGrams" to GeminiSchemaProperty(type = "INTEGER", description = "Peso stimato in grammi della porzione visibile"),
                    "calories" to GeminiSchemaProperty(type = "INTEGER", description = "Calorie totali stimate in kcal"),
                    "protein" to GeminiSchemaProperty(type = "NUMBER", description = "Proteine in grammi"),
                    "carbs" to GeminiSchemaProperty(type = "NUMBER", description = "Carboidrati in grammi"),
                    "fat" to GeminiSchemaProperty(type = "NUMBER", description = "Grassi in grammi"),
                    "confidence" to GeminiSchemaProperty(type = "STRING", enumValues = listOf("HIGH", "MEDIUM", "LOW"), description = "Confidenza della stima"),
                    "notes" to GeminiSchemaProperty(type = "STRING", description = "Note su ingredienti, metodo di cottura o condimenti"),
                    "detectedItems" to GeminiSchemaProperty(
                        type = "ARRAY",
                        description = "Lista degli alimenti o ingredienti identificati",
                        items = GeminiSchema(type = "STRING")
                    )
                ),
                required = listOf("foodName", "estimatedGrams", "calories", "protein", "carbs", "fat", "confidence", "detectedItems")
            )

            return GeminiGenerateContentRequest(
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemPrompt))
                ),
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = userPrompt),
                            GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data))
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(
                    responseMimeType = "application/json",
                    responseSchema = schema,
                    temperature = 0.2f
                )
            )
        }

        fun parseScanResult(jsonString: String): GeminiFoodScanResult {
            val cleanJson = stripMarkdownCodeFences(jsonString)
            // First try parsing via resilient RawDto
            val rawAdapter = moshi.adapter(GeminiFoodScanRawDto::class.java)
            val parsedRaw = try {
                rawAdapter.fromJson(cleanJson)
            } catch (_: Exception) {
                null
            }

            if (parsedRaw != null) {
                return parsedRaw.toScanResult()
            }

            // Direct fallback
            val directAdapter = moshi.adapter(GeminiFoodScanResult::class.java)
            val directResult = directAdapter.fromJson(cleanJson) ?: createMockResult("Pasto non riconosciuto")
            return directResult.normalized()
        }

        fun parseScanResponse(responseJson: String): GeminiFoodScanResult {
            val cleanJson = stripMarkdownCodeFences(responseJson)
            // Check if response is wrapped in Gemini envelope
            val envelopeAdapter = moshi.adapter(GeminiGenerateContentResponse::class.java)
            val envelope = try {
                envelopeAdapter.fromJson(cleanJson)
            } catch (_: Exception) {
                null
            }

            val candidateText = envelope?.candidates?.firstOrNull()?.content?.parts?.firstOrNull { !it.text.isNullOrBlank() }?.text
            if (!candidateText.isNullOrBlank()) {
                return parseScanResult(candidateText)
            }

            // If not envelope or candidate parts empty, parse directly
            return parseScanResult(cleanJson)
        }

        private fun stripMarkdownCodeFences(text: String): String {
            var trimmed = text.trim()
            if (trimmed.startsWith("```json")) {
                trimmed = trimmed.removePrefix("```json")
            } else if (trimmed.startsWith("```")) {
                trimmed = trimmed.removePrefix("```")
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.removeSuffix("```")
            }
            return trimmed.trim()
        }
    }
}

class RealGeminiFoodScannerService(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val okHttpClient: OkHttpClient = GeminiFoodScannerService.defaultOkHttpClient(),
    private val model: String = GeminiFoodScannerService.DEFAULT_MODEL,
    private val enableMockOnMissingKey: Boolean = true
) : GeminiFoodScannerService {

    override suspend fun analyzeFoodImage(jpegBytes: ByteArray): Result<GeminiFoodScanResult> = withContext(Dispatchers.IO) {
        if (!GeminiFoodScannerService.isApiKeyConfigured(apiKey)) {
            return@withContext if (enableMockOnMissingKey) {
                Result.success(GeminiFoodScannerService.createMockResult())
            } else {
                Result.failure(
                    IllegalStateException("Chiave API Gemini mancante o segnaposto in BuildConfig.GEMINI_API_KEY")
                )
            }
        }

        try {
            val requestPayload = GeminiFoodScannerService.buildGenerateContentRequest(jpegBytes)
            val requestJson = GeminiFoodScannerService.moshi
                .adapter(GeminiGenerateContentRequest::class.java)
                .toJson(requestPayload)

            val url = "${GeminiFoodScannerService.BASE_URL}$model:generateContent?key=$apiKey"
            val requestBody = requestJson.toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .build()

            val scanResult = okHttpClient.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                    val errorMsg = "Gemini API error (HTTP ${response.code}): ${responseBody ?: response.message}"
                    throw IOException(errorMsg)
                }

                GeminiFoodScannerService.parseScanResponse(responseBody)
            }
            Result.success(scanResult)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
