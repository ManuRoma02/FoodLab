package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class OtaUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val changelog: String,
    val releaseDate: String = "",
    val forceUpdate: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val sha256: String = ""
)

sealed interface OtaUpdateState {
    object Idle : OtaUpdateState
    object Checking : OtaUpdateState
    data class UpdateAvailable(val info: OtaUpdateInfo) : OtaUpdateState
    object UpToDate : OtaUpdateState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : OtaUpdateState
    data class ReadyToInstall(val apkFile: File, val info: OtaUpdateInfo) : OtaUpdateState
    data class Error(val message: String) : OtaUpdateState
}

class OtaUpdateManager(
    private val client: OkHttpClient = defaultClient()
) {
    private val _state = MutableStateFlow<OtaUpdateState>(OtaUpdateState.Idle)
    val state: StateFlow<OtaUpdateState> = _state.asStateFlow()

    companion object {
        private const val TAG = "OtaUpdateManager"
        const val DEFAULT_MANIFEST_URL = "https://raw.githubusercontent.com/ManuRoma02/FoodLab/main/app-update.json"
        const val GITHUB_RELEASES_URL = "https://api.github.com/repos/ManuRoma02/FoodLab/releases/latest"

        @Volatile
        private var instance: OtaUpdateManager? = null

        fun getInstance(): OtaUpdateManager =
            instance ?: synchronized(this) {
                instance ?: OtaUpdateManager().also { instance = it }
            }

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
        }
    }

    suspend fun checkForUpdates(
        currentCode: Int = BuildConfig.VERSION_CODE,
        currentName: String = BuildConfig.VERSION_NAME,
        manifestUrl: String = DEFAULT_MANIFEST_URL
    ): Result<OtaUpdateInfo> = withContext(Dispatchers.IO) {
        _state.value = OtaUpdateState.Checking
        try {
            val info = fetchManifest(manifestUrl) ?: fetchFromGitHubReleases()
            if (info == null) {
                val err = "Impossibile reperire informazioni sulla nuova versione"
                _state.value = OtaUpdateState.Error(err)
                return@withContext Result.failure(Exception(err))
            }

            val isAvailable = info.versionCode > currentCode
            val resultInfo = info.copy(isUpdateAvailable = isAvailable)

            if (isAvailable) {
                _state.value = OtaUpdateState.UpdateAvailable(resultInfo)
            } else {
                _state.value = OtaUpdateState.UpToDate
            }
            Result.success(resultInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            _state.value = OtaUpdateState.Error(e.message ?: "Errore sconosciuto")
            Result.failure(e)
        }
    }

    private fun fetchManifest(url: String): OtaUpdateInfo? {
        return try {
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                OtaUpdateInfo(
                    versionCode = json.optInt("versionCode", 0),
                    versionName = json.optString("versionName", ""),
                    apkUrl = json.optString("apkUrl", ""),
                    changelog = json.optString("changelog", "Miglioramenti generali e correzioni di bug."),
                    releaseDate = json.optString("releaseDate", ""),
                    forceUpdate = json.optBoolean("forceUpdate", false),
                    sha256 = json.optString("sha256", "").trim()
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch from primary manifest: ${e.message}")
            null
        }
    }

    private fun fetchFromGitHubReleases(): OtaUpdateInfo? {
        return try {
            val req = Request.Builder()
                .url(GITHUB_RELEASES_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val tagName = json.optString("tag_name", "").removePrefix("v")
                val changelog = json.optString("body", "Nuovo aggiornamento disponibile.")
                val assets = json.optJSONArray("assets")
                var apkDownloadUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk")) {
                            apkDownloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                val codeMatch = Regex("""(?:versionCode[:=]|Build)\s*(\d+)""", RegexOption.IGNORE_CASE).find(changelog)
                val code = codeMatch?.groupValues?.get(1)?.toIntOrNull()
                    ?: tagName.substringAfterLast(".").toIntOrNull()
                    ?: 1
                val sha256Match = Regex("""(?:sha256[:=]|SHA-256:\s*)([a-fA-F0-9]{64})""", RegexOption.IGNORE_CASE).find(changelog)
                val sha256 = sha256Match?.groupValues?.get(1) ?: ""
                if (apkDownloadUrl.isNotBlank()) {
                    OtaUpdateInfo(
                        versionCode = code,
                        versionName = tagName,
                        apkUrl = apkDownloadUrl,
                        changelog = changelog,
                        sha256 = sha256
                    )
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch from GitHub Releases: ${e.message}")
            null
        }
    }

    suspend fun downloadUpdate(
        context: Context,
        info: OtaUpdateInfo,
        onProgress: ((Float, Long, Long) -> Unit)? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        _state.value = OtaUpdateState.Downloading(0f, 0L, 0L)
        val updatesDir = File(context.cacheDir, "ota_updates").apply { mkdirs() }
        val destination = File(updatesDir, "FoodLab_v${info.versionName}.apk")
        val tempDestination = File(updatesDir, "FoodLab_v${info.versionName}.apk.tmp")

        try {
            val req = Request.Builder().url(info.apkUrl).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = "Download fallito con codice HTTP ${resp.code}"
                    _state.value = OtaUpdateState.Error(err)
                    return@withContext Result.failure(Exception(err))
                }

                val body = resp.body ?: throw IllegalStateException("Corpo della risposta nullo")
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                var lastProgressPct = -1

                body.byteStream().use { input ->
                    FileOutputStream(tempDestination).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                            downloadedBytes += read
                            val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes else 0f
                            val currentPct = (progress * 100).toInt()
                            if (currentPct != lastProgressPct || (totalBytes > 0 && downloadedBytes == totalBytes)) {
                                lastProgressPct = currentPct
                                _state.value = OtaUpdateState.Downloading(progress, downloadedBytes, totalBytes)
                                onProgress?.invoke(progress, downloadedBytes, totalBytes)
                            }
                        }
                        output.flush()
                    }
                }
            }

            // Verify SHA-256 integrity if checksum is provided
            if (info.sha256.isNotBlank()) {
                val computedHash = calculateSha256(tempDestination)
                if (!computedHash.equals(info.sha256, ignoreCase = true)) {
                    try { tempDestination.delete() } catch (_: Exception) {}
                    val err = "Verifica integrità fallita: SHA-256 non corrispondente"
                    _state.value = OtaUpdateState.Error(err)
                    return@withContext Result.failure(SecurityException(err))
                }
            }

            // Cleanup older APKs to prevent cache bloat
            updatesDir.listFiles()?.filter { it.name.endsWith(".apk") && it.name != destination.name }?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }

            if (tempDestination.exists()) {
                if (destination.exists()) destination.delete()
                tempDestination.renameTo(destination)
            }

            _state.value = OtaUpdateState.ReadyToInstall(destination, info)
            Result.success(destination)
        } catch (e: Exception) {
            try {
                if (tempDestination.exists()) tempDestination.delete()
            } catch (_: Exception) {}
            Log.e(TAG, "Error downloading APK", e)
            _state.value = OtaUpdateState.Error("Errore durante il download: ${e.message}")
            Result.failure(e)
        }
    }

    fun installUpdate(context: Context, apkFile: File): Result<Unit> {
        return try {
            if (!apkFile.exists()) {
                val err = "File APK non trovato in memoria cache"
                _state.value = OtaUpdateState.Error(err)
                return Result.failure(Exception(err))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(manageIntent)
                    return Result.success(Unit)
                }
            }

            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
            _state.value = OtaUpdateState.Error("Impossibile avviare l'installazione: ${e.message}")
            Result.failure(e)
        }
    }

    fun resetState() {
        _state.value = OtaUpdateState.Idle
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
