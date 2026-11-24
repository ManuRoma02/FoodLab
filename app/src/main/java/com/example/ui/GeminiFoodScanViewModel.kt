package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FoodLabApplication
import com.example.data.FoodItem
import com.example.data.GeminiFoodScanResult
import com.example.data.GeminiFoodScannerService
import com.example.data.MealEntry
import com.example.data.Repository
import com.example.util.MealTypes
import com.example.util.WidgetUpdater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.Date
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

/**
 * UI states for Gemini Food Vision Scanner.
 */
sealed interface GeminiScanUiState {
    data object Idle : GeminiScanUiState

    data class Analyzing(
        val progressHint: String = "Analisi del piatto in corso..."
    ) : GeminiScanUiState

    data class Success(
        val result: GeminiFoodScanResult,
        val adjustedGrams: Int,
        val isSaving: Boolean = false
    ) : GeminiScanUiState {
        /**
         * Convenience accessor returning the scan result scaled to [adjustedGrams].
         */
        val effectiveResult: GeminiFoodScanResult
            get() = if (adjustedGrams != result.estimatedGrams) {
                result.scaledToGrams(adjustedGrams)
            } else {
                result
            }
    }

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : GeminiScanUiState
}

/**
 * ViewModel orchestrating the camera/photo food scanning with Gemini Vision API,
 * portion adjustment, and logging the meal directly into Room database and home widgets.
 */
class GeminiFoodScanViewModel @JvmOverloads constructor(
    application: Application,
    private val scannerService: GeminiFoodScannerService = GeminiFoodScannerService.create(),
    private val repository: Repository = (application as? FoodLabApplication)?.repository
        ?: throw IllegalArgumentException("Application must be FoodLabApplication or repository must be provided explicitly"),
    private val widgetUpdater: (Context) -> Unit = { WidgetUpdater.updateAllWidgets(it) },
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<GeminiScanUiState>(GeminiScanUiState.Idle)
    val uiState: StateFlow<GeminiScanUiState> = _uiState.asStateFlow()

    private var lastAnalyzedBytes: ByteArray? = null
    private var activeScanJob: Job? = null
    private var activeSaveJob: Job? = null

    /**
     * Updates the portion grams for the currently analyzed meal before confirming.
     */
    fun updateAdjustedGrams(grams: Int) {
        val currentState = _uiState.value
        if (currentState is GeminiScanUiState.Success && !currentState.isSaving) {
            val safeGrams = grams.coerceIn(1, 5_000)
            _uiState.value = currentState.copy(adjustedGrams = safeGrams)
        }
    }

    /**
     * Resets the scan UI state back to [GeminiScanUiState.Idle] and clears cached photo data.
     */
    fun resetState() {
        activeScanJob?.cancel()
        activeScanJob = null
        _uiState.value = GeminiScanUiState.Idle
        lastAnalyzedBytes = null
    }

    /**
     * Retries scanning the last analyzed photo bytes if available.
     */
    fun retryLastScan(): Job? {
        val bytes = lastAnalyzedBytes ?: return null
        return analyzeImageBytes(bytes)
    }

    /**
     * Analyzes raw JPEG byte array with Gemini Vision service.
     */
    fun analyzeImageBytes(jpegBytes: ByteArray): Job {
        activeScanJob?.cancel()
        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            analyzeImageBytesInternal(jpegBytes, job)
        }
        activeScanJob = job
        job.start()
        return job
    }

    /**
     * Compresses the provided bitmap to JPEG bytes (downscaling if necessary)
     * and forwards it to [analyzeImageBytes].
     */
    fun analyzeBitmap(
        bitmap: Bitmap,
        maxDimension: Int = 1024,
        quality: Int = 85
    ): Job {
        activeScanJob?.cancel()
        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            if (bitmap.isRecycled) {
                if (activeScanJob === job) {
                    _uiState.value = GeminiScanUiState.Error(
                        message = "Immagine riciclata o non valida",
                        canRetry = false
                    )
                    activeScanJob = null
                }
                return@launch
            }

            _uiState.value = GeminiScanUiState.Analyzing("Preparazione e compressione immagine...")
            val bytes = withContext(defaultDispatcher) {
                compressBitmapToJpeg(bitmap, maxDimension, quality)
            }
            if (!isActive || activeScanJob !== job) return@launch
            analyzeImageBytesInternal(bytes, job)
        }
        activeScanJob = job
        job.start()
        return job
    }

    private suspend fun analyzeImageBytesInternal(jpegBytes: ByteArray, scanJob: Job) {
        if (!coroutineContext.isActive || activeScanJob !== scanJob) return
        if (jpegBytes.isEmpty()) {
            if (activeScanJob === scanJob) {
                _uiState.value = GeminiScanUiState.Error(
                    message = "Immagine non valida o vuota",
                    canRetry = false
                )
                activeScanJob = null
            }
            return
        }

        lastAnalyzedBytes = jpegBytes
        _uiState.value = GeminiScanUiState.Analyzing("Analisi del piatto in corso...")

        val outcome = scannerService.analyzeFoodImage(jpegBytes)
        if (!coroutineContext.isActive || activeScanJob !== scanJob) return

        outcome.fold(
            onSuccess = { result ->
                val normalized = result.normalized()
                _uiState.value = GeminiScanUiState.Success(
                    result = normalized,
                    adjustedGrams = normalized.estimatedGrams
                )
            },
            onFailure = { error ->
                if (error is CancellationException) throw error
                val message = error.message?.takeIf { it.isNotBlank() }
                    ?: "Errore durante l'analisi del piatto"
                _uiState.value = GeminiScanUiState.Error(
                    message = message,
                    canRetry = true
                )
            }
        )
        if (activeScanJob === scanJob) {
            activeScanJob = null
        }
    }

    /**
     * Confirms the meal analysis and logs it directly into the database as a [MealEntry],
     * inserting or updating the [FoodItem] catalog entry, refreshing all home widgets,
     * and resetting the scanner state to [GeminiScanUiState.Idle].
     */
    fun confirmAndLogMeal(
        result: GeminiFoodScanResult,
        grams: Int,
        mealType: String,
        dateString: String
    ): Job {
        activeSaveJob?.takeIf { it.isActive }?.let { return it }
        val currentState = _uiState.value
        if (currentState is GeminiScanUiState.Success) {
            if (currentState.isSaving) {
                return activeSaveJob ?: Job().apply { complete() }
            }
            _uiState.value = currentState.copy(isSaving = true)
        }
        lateinit var job: Job
        job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                val safeGrams = grams.coerceIn(1, 5_000)
                val scaledResult = if (safeGrams != result.estimatedGrams) {
                    result.scaledToGrams(safeGrams)
                } else {
                    result
                }.normalized()

                // 1. Create or persist FoodItem in catalog for historical tracking and reuse
                val foodItem = FoodItem(
                    name = scaledResult.foodName,
                    brand = "Gemini AI",
                    caloriesPer100g = scaledResult.caloriesPer100g,
                    proteinPer100g = scaledResult.proteinPer100g,
                    carbsPer100g = scaledResult.carbsPer100g,
                    fatPer100g = scaledResult.fatPer100g,
                    defaultServingSize = safeGrams,
                    useCount = 1,
                    isUserCreated = true
                )
                val insertedFoodId = repository.insertFood(foodItem).toInt()

                // 2. Canonicalize meal type and ensure valid date
                val canonicalMealType = MealTypes.canonicalize(mealType)
                val finalDateString = dateString.trim().ifEmpty {
                    SessionManager.formatDate(Date())
                }

                // 3. Insert MealEntry with exact scaled macros
                val entry = MealEntry(
                    dateString = finalDateString,
                    mealType = canonicalMealType,
                    foodId = insertedFoodId,
                    foodName = scaledResult.foodName,
                    quantityGrams = safeGrams,
                    calories = scaledResult.calories,
                    protein = scaledResult.protein,
                    carbs = scaledResult.carbs,
                    fat = scaledResult.fat,
                    caloriesPer100g = scaledResult.caloriesPer100g,
                    proteinPer100g = scaledResult.proteinPer100g,
                    carbsPer100g = scaledResult.carbsPer100g,
                    fatPer100g = scaledResult.fatPer100g,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertMeal(entry)

                // 4. Update home widgets
                widgetUpdater(getApplication())

                // 5. Reset state
                _uiState.value = GeminiScanUiState.Idle
                lastAnalyzedBytes = null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = GeminiScanUiState.Error(
                    message = e.localizedMessage ?: "Errore durante il salvataggio del pasto",
                    canRetry = false
                )
            } finally {
                if (activeSaveJob === job) {
                    activeSaveJob = null
                }
            }
        }
        activeSaveJob = job
        job.start()
        return job
    }

    /**
     * Convenience method to confirm and log the current [GeminiScanUiState.Success] state.
     */
    fun confirmCurrentMeal(
        mealType: String,
        dateString: String = SessionManager.formatDate(Date())
    ): Job? {
        val current = _uiState.value as? GeminiScanUiState.Success ?: return null
        if (current.isSaving) return activeSaveJob
        return confirmAndLogMeal(
            result = current.result,
            grams = current.adjustedGrams,
            mealType = mealType,
            dateString = dateString
        )
    }

    companion object {
        /**
         * Compresses a [Bitmap] to JPEG byte array, scaling it down if dimensions exceed [maxDimension].
         */
        fun compressBitmapToJpeg(
            bitmap: Bitmap,
            maxDimension: Int = 1024,
            quality: Int = 85
        ): ByteArray {
            if (bitmap.isRecycled) return ByteArray(0)

            val width = bitmap.width
            val height = bitmap.height
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val scale = maxDimension.toFloat() / maxOf(width, height).toFloat()
                val targetWidth = (width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (height * scale).roundToInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
            } else {
                bitmap
            }

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), outputStream)
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            return outputStream.toByteArray()
        }
    }
}
