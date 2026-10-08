package com.example.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.FoodLabApplication
import com.example.data.GeminiFoodScanResult
import com.example.data.GeminiFoodScannerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GeminiFoodScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: FoodLabApplication
    private lateinit var fakeScannerService: FakeGeminiFoodScannerService
    private var widgetUpdatedCount = 0

    private val sampleScanResult = GeminiFoodScanResult(
        foodName = "Pasta al Pesto",
        estimatedGrams = 200,
        calories = 400,
        protein = 12.0f,
        carbs = 56.0f,
        fat = 14.0f,
        confidence = "HIGH",
        notes = "Pasta con pesto alla genovese fresca",
        detectedItems = listOf("Pasta", "Pesto", "Pinoli")
    )

    @Before
    fun setup() = runBlocking {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        app.repository.deleteMealsForDate("2026-10-08")
        app.repository.deleteMealsForDate("2026-10-09")
        fakeScannerService = FakeGeminiFoodScannerService(Result.success(sampleScanResult))
        widgetUpdatedCount = 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): GeminiFoodScanViewModel {
        return GeminiFoodScanViewModel(
            application = app,
            scannerService = fakeScannerService,
            repository = app.repository,
            widgetUpdater = { widgetUpdatedCount++ },
            defaultDispatcher = testDispatcher
        )
    }

    @Test
    fun testInitialStateIsIdle() {
        val viewModel = createViewModel()
        assertEquals(GeminiScanUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun testAnalyzeImageBytes_successFlow() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val dummyBytes = byteArrayOf(1, 2, 3, 4, 5)

        viewModel.analyzeImageBytes(dummyBytes)
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertTrue("Expected Success state but was $state", state is GeminiScanUiState.Success)
        val successState = state as GeminiScanUiState.Success

        assertEquals("Pasta al Pesto", successState.result.foodName)
        assertEquals(200, successState.adjustedGrams)
        assertEquals(400, successState.effectiveResult.calories)
        assertEquals(1, fakeScannerService.analyzeCallCount)
        assertEquals(dummyBytes, fakeScannerService.lastAnalyzedBytes)
    }

    @Test
    fun testAnalyzeImageBytes_emptyBytesResultsInErrorWithoutServiceCall() = runTest(testDispatcher) {
        val viewModel = createViewModel()

        viewModel.analyzeImageBytes(ByteArray(0))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is GeminiScanUiState.Error)
        val errorState = state as GeminiScanUiState.Error

        assertEquals("Immagine non valida o vuota", errorState.message)
        assertFalse(errorState.canRetry)
        assertEquals(0, fakeScannerService.analyzeCallCount)
    }

    @Test
    fun testAnalyzeImageBytes_serviceFailureResultsInErrorWithRetry() = runTest(testDispatcher) {
        fakeScannerService.resultToReturn = Result.failure(IOException("Connessione timeout"))
        val viewModel = createViewModel()

        viewModel.analyzeImageBytes(byteArrayOf(10, 20, 30))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val state = viewModel.uiState.value
        assertTrue("Expected Error state but was $state", state is GeminiScanUiState.Error)
        val errorState = state as GeminiScanUiState.Error

        assertEquals("Connessione timeout", errorState.message)
        assertTrue(errorState.canRetry)
    }

    @Test
    fun testUpdateAdjustedGrams_updatesUiStateAndScalesEffectiveResult() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.analyzeImageBytes(byteArrayOf(1, 2, 3))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // Initial scan: 200g -> 400 kcal, 12g P, 56g C, 14g F
        var state = viewModel.uiState.value as GeminiScanUiState.Success
        assertEquals(200, state.adjustedGrams)
        assertEquals(400, state.effectiveResult.calories)

        // Adjust to 300g (1.5x)
        viewModel.updateAdjustedGrams(300)
        state = viewModel.uiState.value as GeminiScanUiState.Success
        assertEquals(300, state.adjustedGrams)
        assertEquals(600, state.effectiveResult.calories)
        assertEquals(18.0f, state.effectiveResult.protein, 0.01f)
        assertEquals(84.0f, state.effectiveResult.carbs, 0.01f)
        assertEquals(21.0f, state.effectiveResult.fat, 0.01f)

        // Clamping check: <= 0 coerced to 1
        viewModel.updateAdjustedGrams(0)
        state = viewModel.uiState.value as GeminiScanUiState.Success
        assertEquals(1, state.adjustedGrams)

        // Clamping check: > 5000 coerced to 5000
        viewModel.updateAdjustedGrams(10_000)
        state = viewModel.uiState.value as GeminiScanUiState.Success
        assertEquals(5_000, state.adjustedGrams)
    }

    @Test
    fun testConfirmAndLogMeal_insertsMealAndFoodItemAndUpdatesWidgets() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.analyzeImageBytes(byteArrayOf(1, 2, 3))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        val targetDate = "2026-10-08"
        // Log adjusted 300g for Italian meal type "PRANZO" (canonical "LUNCH")
        val job = viewModel.confirmAndLogMeal(
            result = sampleScanResult,
            grams = 300,
            mealType = "PRANZO",
            dateString = targetDate
        )
        job.join()
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        // 1. Verify widgets were notified
        assertEquals(1, widgetUpdatedCount)

        // 2. Verify UI state was reset to Idle
        assertEquals(GeminiScanUiState.Idle, viewModel.uiState.value)

        // 3. Verify Room DB meal entries
        val mealsForDate = app.repository.getMealsForDate(targetDate).first()
        val loggedMeal = mealsForDate.find { it.foodName == "Pasta al Pesto" }
        assertNotNull("Expected logged MealEntry in Room DB", loggedMeal)

        loggedMeal!!.let {
            assertEquals("LUNCH", it.mealType)
            assertEquals(300, it.quantityGrams)
            assertEquals(600, it.calories)
            assertEquals(18.0f, it.protein, 0.01f)
            assertEquals(84.0f, it.carbs, 0.01f)
            assertEquals(21.0f, it.fat, 0.01f)
            assertTrue("Expected foodId > 0", it.foodId > 0)

            // 4. Verify catalog FoodItem was created
            val catalogFood = app.repository.getFoodById(it.foodId)
            assertNotNull("Catalog FoodItem should be persisted", catalogFood)
            assertEquals("Pasta al Pesto", catalogFood!!.name)
            assertEquals("Gemini AI", catalogFood.brand)
            assertEquals(200, catalogFood.caloriesPer100g)
        }
    }

    @Test
    fun testConfirmCurrentMeal_convenienceMethodLogsActiveState() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.analyzeImageBytes(byteArrayOf(1, 2, 3))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        viewModel.updateAdjustedGrams(250)

        val targetDate = "2026-10-09"
        val job = viewModel.confirmCurrentMeal(mealType = "CENA", dateString = targetDate)
        assertNotNull(job)
        job!!.join()
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, widgetUpdatedCount)
        assertEquals(GeminiScanUiState.Idle, viewModel.uiState.value)

        val meals = app.repository.getMealsForDate(targetDate).first()
        val loggedMeal = meals.find { it.foodName == "Pasta al Pesto" }
        assertNotNull(loggedMeal)
        assertEquals("DINNER", loggedMeal!!.mealType)
        assertEquals(250, loggedMeal.quantityGrams)
        // 200g had 400 kcal => 2 kcal/g * 250g = 500 kcal
        assertEquals(500, loggedMeal.calories)
    }

    @Test
    fun testRetryLastScan_reinvokesServiceAfterFailure() = runTest(testDispatcher) {
        fakeScannerService.resultToReturn = Result.failure(IOException("Errore rete"))
        val viewModel = createViewModel()

        viewModel.analyzeImageBytes(byteArrayOf(7, 8, 9))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(viewModel.uiState.value is GeminiScanUiState.Error)
        assertEquals(1, fakeScannerService.analyzeCallCount)

        // Fix service outcome and retry
        fakeScannerService.resultToReturn = Result.success(sampleScanResult)
        val retryJob = viewModel.retryLastScan()
        assertNotNull("retryLastScan should return an active Job", retryJob)
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(2, fakeScannerService.analyzeCallCount)
        assertTrue(viewModel.uiState.value is GeminiScanUiState.Success)
    }

    @Test
    fun testResetState_clearsStateAndCachedBytes() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        viewModel.analyzeImageBytes(byteArrayOf(1, 2, 3))
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(viewModel.uiState.value is GeminiScanUiState.Success)

        viewModel.resetState()
        assertEquals(GeminiScanUiState.Idle, viewModel.uiState.value)

        // Retry should be null after reset
        assertNull(viewModel.retryLastScan())
    }

    @Test
    fun testAnalyzeBitmap_compressesAndInvokesScanner() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)

        viewModel.analyzeBitmap(bitmap)
        testScheduler.advanceUntilIdle()
        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(viewModel.uiState.value is GeminiScanUiState.Success)
        assertEquals(1, fakeScannerService.analyzeCallCount)
        assertNotNull(fakeScannerService.lastAnalyzedBytes)
        assertTrue(fakeScannerService.lastAnalyzedBytes!!.isNotEmpty())

        // Verify JPEG magic header (0xFF, 0xD8)
        val bytes = fakeScannerService.lastAnalyzedBytes!!
        assertEquals(0xFF.toByte(), bytes[0])
        assertEquals(0xD8.toByte(), bytes[1])
    }

    @Test
    fun testCompressBitmapToJpeg_downscalesLargeBitmap() {
        val largeBitmap = Bitmap.createBitmap(2000, 1000, Bitmap.Config.ARGB_8888)
        val jpegBytes = GeminiFoodScanViewModel.compressBitmapToJpeg(largeBitmap, maxDimension = 500)

        assertTrue(jpegBytes.isNotEmpty())
        assertEquals(0xFF.toByte(), jpegBytes[0])
        assertEquals(0xD8.toByte(), jpegBytes[1])
    }
}

class FakeGeminiFoodScannerService(
    var resultToReturn: Result<GeminiFoodScanResult>
) : GeminiFoodScannerService {
    var lastAnalyzedBytes: ByteArray? = null
    var analyzeCallCount: Int = 0

    override suspend fun analyzeFoodImage(jpegBytes: ByteArray): Result<GeminiFoodScanResult> {
        analyzeCallCount++
        lastAnalyzedBytes = jpegBytes
        return resultToReturn
    }
}
