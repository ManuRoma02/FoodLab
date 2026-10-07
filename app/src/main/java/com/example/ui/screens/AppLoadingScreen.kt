package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.foundation.appleSpring
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val QuantumBgColor = Color(0xFF0C0E12)
private val MacroProtein = Color(0xFF22C55E)
private val MacroCarb = Color(0xFFF59E0B)
private val MacroFat = Color(0xFF06B6D4)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)
private val ProgressBarBg = Color(0xFF1E293B)

private val AmbientGlowColors = listOf(
    MacroProtein.copy(alpha = 0.15f),
    MacroFat.copy(alpha = 0.08f),
    Color.Transparent
)

private val ProteinPhotonColors = listOf(
    Color.White,
    MacroProtein.copy(alpha = 0.85f),
    Color.Transparent
)

private val CarbPhotonColors = listOf(
    Color.White,
    MacroCarb.copy(alpha = 0.85f),
    Color.Transparent
)

private val FatPhotonColors = listOf(
    Color.White,
    MacroFat.copy(alpha = 0.85f),
    Color.Transparent
)

private val BloomColors = listOf(
    Color.White,
    MacroProtein.copy(alpha = 0.70f),
    MacroFat.copy(alpha = 0.30f),
    Color.Transparent
)

private val ProgressBarColors = listOf(
    MacroProtein,
    MacroFat,
    MacroCarb
)

private val ProgressBarBrush = Brush.horizontalGradient(ProgressBarColors)

/**
 * High-Precision Quantum Nutrient Orbit Startup Cinematic for FoodLab.
 *
 * Implements Apple Design foundations and Emil Kowalski animation physics:
 * 1. Deep OLED container (#0C0E12) running scale and alpha strictly on GPU RenderThread.
 * 2. FoodLab launcher symbol core with harmonic floating breathing.
 * 3. Volumetric radial glow breathing at the core.
 * 4. Three 3D inclined elliptical orbits (Protein +35°, Carb -40°, Fat 0°) with comet tails and rotating photons.
 * 5. Quantum lock convergence collapsing orbital radii to center accompanied by soft bloom flash and LongPress haptics.
 * 6. Smooth optical exit curve seamlessly transitioning into the app dashboard.
 */
@Composable
fun AppLoadingScreen(
    onLoadingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Lifecycles: Entrance & Exit Animators
    val entranceScale = remember { Animatable(0.93f) }
    val entranceAlpha = remember { Animatable(0f) }
    val exitScale = remember { Animatable(1f) }
    val exitAlpha = remember { Animatable(1f) }

    // Progress, Quantum Convergence & Bloom Animators
    val progress = remember { Animatable(0f) }
    val orbitRadiusMultiplier = remember { Animatable(1f) }
    val bloomAlpha = remember { Animatable(0f) }

    // Ensure onLoadingComplete is called exactly once
    val currentOnLoadingComplete by rememberUpdatedState(onLoadingComplete)
    var completedCalled by remember { mutableStateOf(false) }
    val completeOnce = remember {
        {
            if (!completedCalled) {
                completedCalled = true
                currentOnLoadingComplete()
            }
        }
    }

    // Continuous 60/120 FPS Celestial Motion Engine
    val infiniteTransition = rememberInfiniteTransition(label = "quantum_engine")

    val t by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "core_t"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )

    val proteinOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "protein_angle"
    )

    val carbOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "carb_angle"
    )

    val fatOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fat_angle"
    )

    // Master Timeline Sequencing Pipeline
    LaunchedEffect(Unit) {
        // Safety guard: Fallback timeout ensuring completion even if animations are interrupted
        val safetyTimeoutJob = launch {
            delay(2200)
            completeOnce()
        }

        try {
            // Phase 1 (0 - 280ms): Entrance scale (0.93f -> 1.0f) and alpha (0f -> 1f)
            val entranceSpring = appleSpring<Float>(dampingRatio = 0.72f, stiffness = 280f)
            launch {
                entranceScale.animateTo(
                    targetValue = 1f,
                    animationSpec = entranceSpring
                )
            }
            launch {
                entranceAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = entranceSpring
                )
            }

            // Phase 2 (280 - 1300ms): Continuous rotation and progress (0f -> 1f)
            delay(280)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1020, easing = FastOutSlowInEasing)
            )

            // Phase 3 (1300 - 1550ms): Quantum lock convergence
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}

            launch {
                orbitRadiusMultiplier.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                )
            }
            launch {
                bloomAlpha.animateTo(
                    targetValue = 0.7f,
                    animationSpec = tween(durationMillis = 100, easing = FastOutLinearInEasing)
                )
                bloomAlpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 150, easing = LinearOutSlowInEasing)
                )
            }
            delay(250)

            // Phase 4 (1550 - 1750ms): Optical exit
            launch {
                exitScale.animateTo(
                    targetValue = 1.06f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
                )
            }
            exitAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
            )

            // Completion
            safetyTimeoutJob.cancel()
            completeOnce()
        } catch (_: Exception) {
            completeOnce()
        }
    }

    // Cached stroke and brush objects to eliminate draw allocations
    val orbitStroke = remember(density) {
        Stroke(width = density.run { 1.5.dp.toPx() })
    }
    val cometStroke = remember(density) {
        Stroke(width = density.run { 2.2.dp.toPx() }, cap = StrokeCap.Round)
    }

    val glowRadiusPx = remember(density) { density.run { 140.dp.toPx() } }
    val bloomRadiusPx = remember(density) { density.run { 130.dp.toPx() } }
    val photonGlowRadiusPx = remember(density) { density.run { 8.5.dp.toPx() } }
    val photonCoreRadiusPx = remember(density) { density.run { 2.6.dp.toPx() } }

    val ambientGlowBrush = remember(density) {
        Brush.radialGradient(
            colors = AmbientGlowColors,
            center = Offset.Zero,
            radius = density.run { 140.dp.toPx() }
        )
    }

    val bloomBrush = remember(density) {
        Brush.radialGradient(
            colors = BloomColors,
            center = Offset.Zero,
            radius = density.run { 130.dp.toPx() }
        )
    }

    val proteinPhotonBrush = remember(density) {
        Brush.radialGradient(
            colors = ProteinPhotonColors,
            center = Offset.Zero,
            radius = density.run { 8.5.dp.toPx() }
        )
    }

    val carbPhotonBrush = remember(density) {
        Brush.radialGradient(
            colors = CarbPhotonColors,
            center = Offset.Zero,
            radius = density.run { 8.5.dp.toPx() }
        )
    }

    val fatPhotonBrush = remember(density) {
        Brush.radialGradient(
            colors = FatPhotonColors,
            center = Offset.Zero,
            radius = density.run { 8.5.dp.toPx() }
        )
    }

    // Root Container running GPU transforms
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("quantum_loading_root")
            .background(QuantumBgColor)
            .graphicsLayer {
                val scale = exitScale.value * entranceScale.value
                scaleX = scale
                scaleY = scale
                alpha = exitAlpha.value * entranceAlpha.value
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            // Core & 3D Quantum Orbit System
            Box(
                modifier = Modifier.size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background Glow, Orbits and Bloom Flash
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val coreBobPx = (sin(t) * 4.dp.toPx())
                    val centerOffset = Offset(size.width / 2f, size.height / 2f + coreBobPx)
                    val mult = orbitRadiusMultiplier.value

                    // 1. Volumetric Ambient Backlight
                    withTransform({
                        translate(centerOffset.x, centerOffset.y)
                    }) {
                        drawCircle(
                            brush = ambientGlowBrush,
                            radius = glowRadiusPx,
                            center = Offset.Zero,
                            alpha = glowPulse
                        )
                    }

                    // Helper to render an inclined quantum orbit
                    fun drawQuantumOrbit(
                        color: Color,
                        photonBrush: Brush,
                        inclinationDeg: Float,
                        rxDp: Float,
                        ryDp: Float,
                        particleAngleDeg: Float,
                        trailLengthDeg: Float = 50f
                    ) {
                        val rx = rxDp.dp.toPx() * mult
                        val ry = ryDp.dp.toPx() * mult
                        if (rx <= 0.5f || ry <= 0.5f) return

                        withTransform({
                            rotate(degrees = inclinationDeg, pivot = centerOffset)
                        }) {
                            val topLeft = Offset(centerOffset.x - rx, centerOffset.y - ry)
                            val ovalSize = Size(rx * 2f, ry * 2f)

                            // Base Elliptical Track
                            drawOval(
                                color = color.copy(alpha = 0.32f * mult),
                                topLeft = topLeft,
                                size = ovalSize,
                                style = orbitStroke
                            )

                            // Comet Trail Arc
                            val arcStart = particleAngleDeg - trailLengthDeg
                            drawArc(
                                color = color.copy(alpha = 0.65f * mult),
                                startAngle = arcStart,
                                sweepAngle = trailLengthDeg,
                                useCenter = false,
                                topLeft = topLeft,
                                size = ovalSize,
                                style = cometStroke
                            )

                            // Orbiting Photon Head
                            val rad = Math.toRadians(particleAngleDeg.toDouble())
                            val headX = centerOffset.x + (rx * cos(rad)).toFloat()
                            val headY = centerOffset.y + (ry * sin(rad)).toFloat()
                            val headPos = Offset(headX, headY)

                            withTransform({
                                translate(headX, headY)
                            }) {
                                drawCircle(
                                    brush = photonBrush,
                                    center = Offset.Zero,
                                    radius = photonGlowRadiusPx,
                                    alpha = mult
                                )
                            }

                            drawCircle(
                                color = Color.White.copy(alpha = mult),
                                center = headPos,
                                radius = photonCoreRadiusPx
                            )

                            // Trailing Comet Sparks
                            for (i in 1..4) {
                                val sparkAngle = particleAngleDeg - (i * 10f)
                                val sparkRad = Math.toRadians(sparkAngle.toDouble())
                                val sparkX = centerOffset.x + (rx * cos(sparkRad)).toFloat()
                                val sparkY = centerOffset.y + (ry * sin(sparkRad)).toFloat()
                                val sparkAlpha = (0.50f - i * 0.10f).coerceAtLeast(0f) * mult
                                drawCircle(
                                    color = color.copy(alpha = sparkAlpha),
                                    center = Offset(sparkX, sparkY),
                                    radius = (2.2f - i * 0.35f).dp.toPx()
                                )
                            }
                        }
                    }

                    // 2. Three 3D Quantum Nutrient Orbits
                    // Protein: +35°, rx 100.dp, ry 38.dp
                    drawQuantumOrbit(
                        color = MacroProtein,
                        photonBrush = proteinPhotonBrush,
                        inclinationDeg = 35f,
                        rxDp = 100f,
                        ryDp = 38f,
                        particleAngleDeg = proteinOrbitAngle + progress.value * 180f
                    )

                    // Carb: -40°, rx 90.dp, ry 34.dp
                    drawQuantumOrbit(
                        color = MacroCarb,
                        photonBrush = carbPhotonBrush,
                        inclinationDeg = -40f,
                        rxDp = 90f,
                        ryDp = 34f,
                        particleAngleDeg = carbOrbitAngle - progress.value * 180f
                    )

                    // Fat: 0° equatorial, rx 110.dp, ry 30.dp
                    drawQuantumOrbit(
                        color = MacroFat,
                        photonBrush = fatPhotonBrush,
                        inclinationDeg = 0f,
                        rxDp = 110f,
                        ryDp = 30f,
                        particleAngleDeg = fatOrbitAngle + progress.value * 240f
                    )

                    // 3. Convergence Bloom Flash
                    if (bloomAlpha.value > 0.001f) {
                        withTransform({
                            translate(centerOffset.x, centerOffset.y)
                        }) {
                            drawCircle(
                                brush = bloomBrush,
                                center = Offset.Zero,
                                radius = bloomRadiusPx,
                                alpha = bloomAlpha.value
                            )
                        }
                    }
                }

                // Central Core Floating Symbol
                Image(
                    painter = painterResource(id = R.drawable.foodlab_launcher_symbol),
                    contentDescription = "FoodLab Quantum Core",
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            translationY = sin(t) * 4.dp.toPx()
                        },
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Minimalist Apple-style Typography
            Text(
                text = "FOODLAB",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                letterSpacing = 2.sp,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "QUANTUM SYNC",
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Slim Gradient Progress Bar / Pill
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(ProgressBarBg.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = progress.value.coerceIn(0f, 1f)
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .background(ProgressBarBrush)
                )
            }
        }
    }
}
