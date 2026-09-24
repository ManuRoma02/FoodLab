package com.example.ui.foundation

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Standard Apple physics spring curve.
 * Damping ratio = 0.75f (controlled deceleration, minimal bounce).
 * Stiffness = 300f (natural fluid response).
 */
fun <T> appleSpring(
    dampingRatio: Float = 0.75f,
    stiffness: Float = 300f,
    visibilityThreshold: T? = null
): SpringSpec<T> = spring(
    dampingRatio = dampingRatio,
    stiffness = stiffness,
    visibilityThreshold = visibilityThreshold
)

object AppleSprings {
    // Standard UI Spring Spec
    val DefaultFloat: SpringSpec<Float> = appleSpring(dampingRatio = 0.75f, stiffness = 300f)
    val DefaultDp: SpringSpec<Dp> = appleSpring(dampingRatio = 0.75f, stiffness = 300f)
    val DefaultColor: SpringSpec<Color> = appleSpring(dampingRatio = 1.0f, stiffness = 300f)
    val DefaultOffset: SpringSpec<IntOffset> = appleSpring(dampingRatio = 0.75f, stiffness = 300f)
    val DefaultSize: SpringSpec<IntSize> = appleSpring(dampingRatio = 0.75f, stiffness = 300f)

    // Snappy Micro-interactions (steppers, chips, toggles)
    val SnappyFloat: SpringSpec<Float> = appleSpring(dampingRatio = 0.85f, stiffness = 500f)
    val SnappyDp: SpringSpec<Dp> = appleSpring(dampingRatio = 0.85f, stiffness = 500f)

    // Critically Damped (1.0 damping, zero overshoot)
    val CriticallyDampedFloat: SpringSpec<Float> = appleSpring(dampingRatio = 1.0f, stiffness = 350f)
    val CriticallyDampedDp: SpringSpec<Dp> = appleSpring(dampingRatio = 1.0f, stiffness = 350f)

    // Sheet and Modal entry
    val SheetFloat: SpringSpec<Float> = appleSpring(dampingRatio = 0.80f, stiffness = 300f)
    val SheetOffset: SpringSpec<IntOffset> = appleSpring(dampingRatio = 0.80f, stiffness = 300f)
}
