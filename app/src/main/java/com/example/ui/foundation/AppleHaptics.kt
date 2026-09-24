package com.example.ui.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Apple Tactile Haptic Feedback Helpers.
 * Provides crisp, immediate haptic feedback (HapticFeedbackType.LongPress / tactile click)
 * aligned with Apple multimodal harmony principles.
 */
class AppleHaptics(private val haptic: HapticFeedback) {
    /**
     * Tactile click feedback for touch-down, button presses, stepper adjustments, and tab switches.
     */
    fun tactileClick() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /**
     * Subtle tick feedback for sliders, picker detents, and selection changes.
     */
    fun selectionTick() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    /**
     * Impact feedback for card clicks and significant state triggers.
     */
    fun impact() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}

/**
 * Helper to obtain and remember AppleHaptics in any Composable.
 */
@Composable
fun rememberAppleHaptics(): AppleHaptics {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) { AppleHaptics(haptic) }
}

/**
 * Direct tactile feedback invocation helper.
 */
fun performTactileFeedback(haptic: HapticFeedback) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
}
