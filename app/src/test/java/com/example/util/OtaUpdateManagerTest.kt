package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OtaUpdateManagerTest {

    private fun createMockClient(statusCode: Int, jsonBody: String): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(statusCode)
                    .message(if (statusCode == 200) "OK" else "Error")
                    .body(jsonBody.toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
    }

    @Test
    fun testCheckForUpdates_whenRemoteIsHigher_returnsUpdateAvailable() = runTest {
        val json = """
            {
                "versionCode": 50,
                "versionName": "1.0.50",
                "apkUrl": "https://example.com/app.apk",
                "changelog": "Test update notes"
            }
        """.trimIndent()

        val manager = OtaUpdateManager(client = createMockClient(200, json))
        val result = manager.checkForUpdates(currentCode = 30, currentName = "1.0.30")

        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertEquals(50, info?.versionCode)
        assertEquals("1.0.50", info?.versionName)
        assertTrue(info?.isUpdateAvailable == true)

        val currentState = manager.state.value
        assertTrue("State should be UpdateAvailable", currentState is OtaUpdateState.UpdateAvailable)
    }

    @Test
    fun testCheckForUpdates_whenRemoteIsEqualOrLower_returnsUpToDate() = runTest {
        val json = """
            {
                "versionCode": 30,
                "versionName": "1.0.30",
                "apkUrl": "https://example.com/app.apk",
                "changelog": "No new features"
            }
        """.trimIndent()

        val manager = OtaUpdateManager(client = createMockClient(200, json))
        val result = manager.checkForUpdates(currentCode = 30, currentName = "1.0.30")

        assertTrue(result.isSuccess)
        val info = result.getOrNull()
        assertNotNull(info)
        assertFalse(info?.isUpdateAvailable == true)

        val currentState = manager.state.value
        assertEquals(OtaUpdateState.UpToDate, currentState)
    }

    @Test
    fun testCheckForUpdates_whenNetworkFails_setsErrorState() = runTest {
        val manager = OtaUpdateManager(client = createMockClient(500, "Server Error"))
        val result = manager.checkForUpdates(currentCode = 30)

        assertTrue(result.isFailure)
        val currentState = manager.state.value
        assertTrue("State should be Error", currentState is OtaUpdateState.Error)
    }

    @Test
    fun testResetState_setsStateToIdle() {
        val manager = OtaUpdateManager()
        manager.resetState()
        assertEquals(OtaUpdateState.Idle, manager.state.value)
    }

    @Test
    fun testDownloadUpdate_createsAtomicFileAndCleansUpStaleApks() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val updatesDir = File(context.cacheDir, "ota_updates").apply { mkdirs() }
        // Create an old stale APK
        val oldApk = File(updatesDir, "FoodLab_v1.0.10.apk").apply { writeText("old apk content") }
        assertTrue(oldApk.exists())

        val mockBytes = "mock apk binary payload".toByteArray()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(mockBytes.toResponseBody("application/vnd.android.package-archive".toMediaType()))
                    .build()
            }
            .build()

        val manager = OtaUpdateManager(client = client)
        val info = OtaUpdateInfo(
            versionCode = 35,
            versionName = "1.0.35",
            apkUrl = "https://example.com/app-v1.0.35.apk",
            changelog = "Test new release"
        )

        val result = manager.downloadUpdate(context, info)
        assertTrue(result.isSuccess)
        val downloadedFile = result.getOrNull()
        assertNotNull(downloadedFile)
        assertTrue(downloadedFile!!.exists())
        assertEquals("FoodLab_v1.0.35.apk", downloadedFile.name)
        // Verify temp file does not linger
        val tempFile = File(updatesDir, "FoodLab_v1.0.35.apk.tmp")
        assertFalse(tempFile.exists())
        // Verify stale APK was deleted
        assertFalse(oldApk.exists())
        // Verify state is ReadyToInstall
        val state = manager.state.value
        assertTrue("State should be ReadyToInstall", state is OtaUpdateState.ReadyToInstall)
    }

    @Test
    fun testInstallUpdate_whenFileDoesNotExist_returnsFailure() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = OtaUpdateManager()
        val nonExistentFile = File(context.cacheDir, "non_existent.apk")

        val result = manager.installUpdate(context, nonExistentFile)
        assertTrue(result.isFailure)
        assertTrue(manager.state.value is OtaUpdateState.Error)
    }
}
