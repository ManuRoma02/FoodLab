package com.example.util

import android.content.Context
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var prefsManager: PreferencesManager

    private val cardVisibilityKeys = listOf(
        "show_calorie_deficit",
        "show_calorie_chart",
        "show_macro_breakdown",
        "show_protein_chart",
        "show_carbs_chart",
        "show_fat_chart",
        "show_weight_chart",
        "show_steps_chart",
        "show_streak_card"
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefsManager = PreferencesManager.getInstance(context)
        prefsManager.reset()
    }

    @Test
    fun reset_alignsSyncStepsWithPersistedDefaultAndResetsAllCardVisibilityFlows() {
        prefsManager.setThemeSkin(ThemeSkin.NOTHING_OS)
        prefsManager.setSyncSteps(false)
        prefsManager.setSoundEnabled(true)
        prefsManager.setNotificationsEnabled(true)
        prefsManager.completeOnboarding()
        prefsManager.setWaterGoal(3200)
        prefsManager.setStepGoal(12000)
        cardVisibilityKeys.forEach { key -> prefsManager.setCardVisibility(key, false) }

        assertFalse(prefsManager.syncSteps.value)
        assertFalse(prefsManager.showCalorieDeficit.value)
        assertFalse(prefsManager.showStreakCard.value)

        prefsManager.reset()

        assertEquals(ThemeSkin.ORIGINAL, prefsManager.themeSkin.value)
        assertTrue(prefsManager.syncSteps.value)
        assertEquals(
            true,
            context.getSharedPreferences("foodlab_prefs", Context.MODE_PRIVATE)
                .getBoolean("sync_steps", true)
        )
        assertFalse(prefsManager.soundEnabled.value)
        assertFalse(prefsManager.notificationsEnabled.value)
        assertFalse(prefsManager.hasCompletedOnboarding.value)
        assertEquals(2500, prefsManager.waterGoal.value)
        assertEquals(10000, prefsManager.stepGoal.value)

        assertTrue(prefsManager.showCalorieDeficit.value)
        assertTrue(prefsManager.showCalorieChart.value)
        assertTrue(prefsManager.showMacroBreakdown.value)
        assertTrue(prefsManager.showProteinChart.value)
        assertTrue(prefsManager.showCarbsChart.value)
        assertTrue(prefsManager.showFatChart.value)
        assertTrue(prefsManager.showWeightChart.value)
        assertTrue(prefsManager.showStepsChart.value)
        assertTrue(prefsManager.showStreakCard.value)
    }

    @Test
    fun reset_doesNotClobberCreaImportFlags() {
        val importPrefs = context.getSharedPreferences(
            CreaDatabaseImporter.IMPORT_PREFS_NAME,
            Context.MODE_PRIVATE
        )
        importPrefs.edit {
            putBoolean("foodlab_master_dataset_v5", true)
            putBoolean("crea_imported_v3_enhanced", true)
        }

        prefsManager.reset()

        assertTrue(importPrefs.getBoolean("foodlab_master_dataset_v5", false))
        assertTrue(importPrefs.getBoolean("crea_imported_v3_enhanced", false))
    }
}
