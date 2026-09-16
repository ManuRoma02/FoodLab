package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.core.content.edit
import com.example.R

class SoundManager(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<String, Int>()
    private val loadedSoundIds = mutableSetOf<Int>()
    private val pendingOneShots = mutableSetOf<String>()
    private val activeStreams = mutableSetOf<Int>()
    private val prefs = context.getSharedPreferences("sound_prefs", Context.MODE_PRIVATE)

    var volume: Float
        get() = prefs.getFloat("volume", 0.5f).coerceIn(0f, 1f)
        set(value) {
            val clampedVolume = value.coerceIn(0f, 1f)
            prefs.edit { putFloat("volume", clampedVolume) }
            activeStreams.forEach { streamId ->
                soundPool.setVolume(streamId, clampedVolume, clampedVolume)
            }
            if (clampedVolume <= 0f) {
                stopAllActiveStreams()
            }
        }

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSoundIds += sampleId
                val name = soundMap.entries.firstOrNull { it.value == sampleId }?.key
                if (name != null && pendingOneShots.remove(name)) playSound(name)
            }
        }

        // Only the common cues are warmed up; larger/rare assets load on first use.
        listOf("coin", "click", "error", "power_up").forEach(::loadSound)
    }

    private fun loadSound(name: String): Int? {
        return try {
            soundMap[name]?.let { return it }
            val resId = SOUND_RESOURCES[name] ?: return null
            soundPool.load(appContext, resId, 1).also { soundMap[name] = it }
        } catch (_: Exception) {
            null
        }
    }

    private var lastSelectIdx = 0
    private val prefManager by lazy { PreferencesManager.getInstance(appContext) }

    fun playSound(name: String): Int {
        if (!prefManager.soundEnabled.value) return 0
        val currentVolume = volume
        if (currentVolume <= 0f) return 0
        val soundId = loadSound(name) ?: return 0
        if (soundId !in loadedSoundIds) {
            pendingOneShots += name
            return 0
        }
        return soundPool.play(soundId, currentVolume, currentVolume, 1, 0, 1f)
    }

    fun playRandomSelect() {
        lastSelectIdx = (lastSelectIdx + 1) % 3
        val name = "blip_select_${lastSelectIdx + 1}"
        playSound(name)
    }

    fun playLoop(name: String): Int {
        if (!prefManager.soundEnabled.value) return 0
        val currentVolume = volume
        if (currentVolume <= 0f) return 0
        val soundId = loadSound(name) ?: return 0
        if (soundId !in loadedSoundIds) return 0
        return soundId.let {
            soundPool.play(it, currentVolume, currentVolume, 1, -1, 1f).also { streamId ->
                if (streamId != 0) {
                    activeStreams.add(streamId)
                }
            }
        } ?: 0
    }

    fun stopSound(streamId: Int) {
        activeStreams.remove(streamId)
        soundPool.stop(streamId)
    }

    private fun stopAllActiveStreams() {
        activeStreams.toList().forEach { streamId ->
            soundPool.stop(streamId)
        }
        activeStreams.clear()
    }

    fun release() {
        stopAllActiveStreams()
        soundPool.release()
    }

    private companion object {
        val SOUND_RESOURCES = mapOf(
            "blip_select_1" to R.raw.blip_select_1,
            "blip_select_2" to R.raw.blip_select_2,
            "blip_select_3" to R.raw.blip_select_3,
            "click" to R.raw.click,
            "coin" to R.raw.coin,
            "error" to R.raw.error,
            "power_up" to R.raw.power_up,
        )
    }
}

val LocalSoundManager = staticCompositionLocalOf<SoundManager> { 
    error("No SoundManager provided. Ensure your app is wrapped in CompositionLocalProvider(LocalSoundManager provides ...).") 
}
