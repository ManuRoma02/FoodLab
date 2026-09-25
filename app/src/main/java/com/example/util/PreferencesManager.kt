package com.example.util

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeSkin(val displayName: String) {
    ORIGINAL("Originale"),
    RETRO_DARK("Midnight"),
    TITANIUM("Titanium Pro"),
    NORD("Nordic Frost"),
    CATPPUCCIN("Deep Mauve"),
    WARM_EMBER("Warm Ember");

    companion object {
        fun fromString(name: String?): ThemeSkin {
            return when (name) {
                "ORIGINAL" -> ORIGINAL
                "MATRIX", "TITANIUM" -> TITANIUM
                "GAMEBOY", "NORD" -> NORD
                "CYBERPUNK", "CATPPUCCIN" -> CATPPUCCIN
                "AMBER_CRT", "WARM_EMBER" -> WARM_EMBER
                "RETRO_DARK" -> RETRO_DARK
                else -> values().firstOrNull { it.name == name } ?: ORIGINAL
            }
        }
    }
}

class PreferencesManager private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("foodlab_prefs", Context.MODE_PRIVATE)

    private val _themeSkin = MutableStateFlow(loadThemeSkin())
    val themeSkin: StateFlow<ThemeSkin> = _themeSkin.asStateFlow()

    private val _syncSteps = MutableStateFlow(loadSyncSteps())
    val syncSteps: StateFlow<Boolean> = _syncSteps.asStateFlow()

    private val _soundEnabled = MutableStateFlow(loadSoundEnabled())
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, false))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false))
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    private val _waterGoal = MutableStateFlow(prefs.getInt("water_goal_ml", 2500))
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()

    private val _stepGoal = MutableStateFlow(prefs.getInt("step_goal_steps", 10000))
    val stepGoal: StateFlow<Int> = _stepGoal.asStateFlow()

    fun setWaterGoal(goalMl: Int) {
        prefs.edit { putInt("water_goal_ml", goalMl) }
        _waterGoal.value = goalMl
    }

    fun setStepGoal(steps: Int) {
        prefs.edit { putInt("step_goal_steps", steps) }
        _stepGoal.value = steps
    }

    // Card Visibility Preferences
    private val _showCalorieDeficit = MutableStateFlow(prefs.getBoolean("show_calorie_deficit", true))
    val showCalorieDeficit: StateFlow<Boolean> = _showCalorieDeficit.asStateFlow()

    private val _showCalorieChart = MutableStateFlow(prefs.getBoolean("show_calorie_chart", true))
    val showCalorieChart: StateFlow<Boolean> = _showCalorieChart.asStateFlow()

    private val _showMacroBreakdown = MutableStateFlow(prefs.getBoolean("show_macro_breakdown", true))
    val showMacroBreakdown: StateFlow<Boolean> = _showMacroBreakdown.asStateFlow()

    private val _showProteinChart = MutableStateFlow(prefs.getBoolean("show_protein_chart", true))
    val showProteinChart: StateFlow<Boolean> = _showProteinChart.asStateFlow()

    private val _showCarbsChart = MutableStateFlow(prefs.getBoolean("show_carbs_chart", true))
    val showCarbsChart: StateFlow<Boolean> = _showCarbsChart.asStateFlow()

    private val _showFatChart = MutableStateFlow(prefs.getBoolean("show_fat_chart", true))
    val showFatChart: StateFlow<Boolean> = _showFatChart.asStateFlow()

    private val _showWeightChart = MutableStateFlow(prefs.getBoolean("show_weight_chart", true))
    val showWeightChart: StateFlow<Boolean> = _showWeightChart.asStateFlow()

    private val _showStepsChart = MutableStateFlow(prefs.getBoolean("show_steps_chart", true))
    val showStepsChart: StateFlow<Boolean> = _showStepsChart.asStateFlow()

    private val _showStreakCard = MutableStateFlow(prefs.getBoolean("show_streak_card", true))
    val showStreakCard: StateFlow<Boolean> = _showStreakCard.asStateFlow()

    fun setCardVisibility(key: String, visible: Boolean) {
        prefs.edit { putBoolean(key, visible) }
        when (key) {
            "show_calorie_deficit" -> _showCalorieDeficit.value = visible
            "show_calorie_chart" -> _showCalorieChart.value = visible
            "show_macro_breakdown" -> _showMacroBreakdown.value = visible
            "show_protein_chart" -> _showProteinChart.value = visible
            "show_carbs_chart" -> _showCarbsChart.value = visible
            "show_fat_chart" -> _showFatChart.value = visible
            "show_weight_chart" -> _showWeightChart.value = visible
            "show_steps_chart" -> _showStepsChart.value = visible
            "show_streak_card" -> _showStreakCard.value = visible
        }
    }

    private fun loadThemeSkin(): ThemeSkin {
        val name = prefs.getString("theme_skin", ThemeSkin.ORIGINAL.name)
        return ThemeSkin.fromString(name)
    }

    fun setThemeSkin(skin: ThemeSkin) {
        prefs.edit { putString("theme_skin", skin.name) }
        _themeSkin.value = skin
    }

    private fun loadSyncSteps(): Boolean {
        return prefs.getBoolean("sync_steps", true)
    }

    fun setSyncSteps(enabled: Boolean) {
        prefs.edit { putBoolean("sync_steps", enabled) }
        _syncSteps.value = enabled
    }

    private fun loadSoundEnabled(): Boolean {
        return prefs.getBoolean("sound_enabled", false)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean("sound_enabled", enabled) }
        _soundEnabled.value = enabled
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }

    fun completeOnboarding() {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, true) }
        _hasCompletedOnboarding.value = true
    }

    fun reset() {
        prefs.edit { clear() }
        _themeSkin.value = ThemeSkin.RETRO_DARK
        _syncSteps.value = false
        _soundEnabled.value = false
        _notificationsEnabled.value = false
        _hasCompletedOnboarding.value = false
        _waterGoal.value = 2500
        _stepGoal.value = 10000
    }

    companion object {
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

val LocalPreferencesManager = androidx.compose.runtime.staticCompositionLocalOf<PreferencesManager> {
    error("No PreferencesManager provided")
}
