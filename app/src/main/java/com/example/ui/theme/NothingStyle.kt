package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Nothing OS 1px wireframe outline border.
 */
fun Modifier.nothingBorder(
    width: Dp = 1.dp,
    color: Color = NothingBorderVisible,
    shape: Shape = RoundedCornerShape(14.dp)
): Modifier = this.border(width, color, shape)

/**
 * Nothing OS subtle dot-matrix background grid rendered on GPU canvas.
 */
fun Modifier.nothingDotGridBackground(
    dotColor: Color = NothingBorderVisible.copy(alpha = 0.40f),
    dotRadius: Dp = 1.dp,
    gridSpacing: Dp = 16.dp
): Modifier = this.drawBehind {
    val stepPx = gridSpacing.toPx()
    val rPx = dotRadius.toPx()
    var x = stepPx / 2
    while (x < size.width) {
        var y = stepPx / 2
        while (y < size.height) {
            drawCircle(
                color = dotColor,
                radius = rPx,
                center = Offset(x, y)
            )
            y += stepPx
        }
        x += stepPx
    }
}

/**
 * Authentic Nothing OS 5x7 Dot-Matrix LED Digit Patterns (NDot style).
 */
private val NDOT_PATTERNS = mapOf(
    '0' to intArrayOf(0b01110, 0b10001, 0b10011, 0b10101, 0b11001, 0b10001, 0b01110),
    '1' to intArrayOf(0b00100, 0b01100, 0b00100, 0b00100, 0b00100, 0b00100, 0b01110),
    '2' to intArrayOf(0b01110, 0b10001, 0b00001, 0b00010, 0b00100, 0b01000, 0b11111),
    '3' to intArrayOf(0b11110, 0b00001, 0b00001, 0b01110, 0b00001, 0b00001, 0b11110),
    '4' to intArrayOf(0b00010, 0b00110, 0b01010, 0b10010, 0b11111, 0b00010, 0b00010),
    '5' to intArrayOf(0b11111, 0b10000, 0b11110, 0b00001, 0b00001, 0b10001, 0b01110),
    '6' to intArrayOf(0b01110, 0b10000, 0b11110, 0b10001, 0b10001, 0b10001, 0b01110),
    '7' to intArrayOf(0b11111, 0b00001, 0b00010, 0b00100, 0b01000, 0b01000, 0b01000),
    '8' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01110, 0b10001, 0b10001, 0b01110),
    '9' to intArrayOf(0b01110, 0b10001, 0b10001, 0b01111, 0b00001, 0b00001, 0b01110),
    '-' to intArrayOf(0b00000, 0b00000, 0b00000, 0b11111, 0b00000, 0b00000, 0b00000),
    ':' to intArrayOf(0b00000, 0b00100, 0b00000, 0b00000, 0b00100, 0b00000, 0b00000),
    '.' to intArrayOf(0b00000, 0b00000, 0b00000, 0b00000, 0b00000, 0b01100, 0b01100),
    '/' to intArrayOf(0b00001, 0b00010, 0b00100, 0b01000, 0b10000, 0b00000, 0b00000)
)

/**
 * Authentic Nothing OS Dot-Matrix (NDot) Digital Number Display.
 * Renders characters via LED dots on a 5x7 discrete hardware matrix.
 */
@Composable
fun NothingDotMatrixText(
    text: String,
    modifier: Modifier = Modifier,
    dotSize: Dp = 3.5.dp,
    dotSpacing: Dp = 1.5.dp,
    charSpacing: Dp = 6.dp,
    litColor: Color = NothingTextDisplay,
    unlitColor: Color = Color(0xFF1E1E1E)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(charSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEach { char ->
            val pattern = NDOT_PATTERNS[char]
            if (pattern != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dotSpacing)
                ) {
                    for (row in 0 until 7) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(dotSpacing)
                        ) {
                            val rowBits = pattern[row]
                            for (col in 0 until 5) {
                                val isLit = (rowBits and (1 shl (4 - col))) != 0
                                Box(
                                    modifier = Modifier
                                        .size(dotSize)
                                        .clip(CircleShape)
                                        .background(if (isLit) litColor else unlitColor)
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = char.toString(),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = litColor
                )
            }
        }
    }
}

/**
 * Nothing OS Signature Segmented Progress Bar.
 * Discrete rectangular blocks with 2px gaps, mechanical instrument feel.
 */
@Composable
fun NothingSegmentedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    segments: Int = 20,
    segmentHeight: Dp = 8.dp,
    segmentGap: Dp = 2.dp,
    activeColor: Color = NothingTextDisplay,
    emptyColor: Color = NothingBorder,
    overflowColor: Color = NothingRed
) {
    val clampedProgress = progress.coerceAtLeast(0f)
    val isOverTarget = clampedProgress > 1.0f
    val filledCount = (clampedProgress.coerceIn(0f, 1f) * segments).roundToInt()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(segmentHeight),
        horizontalArrangement = Arrangement.spacedBy(segmentGap)
    ) {
        for (i in 0 until segments) {
            val isFilled = i < filledCount
            val barColor = when {
                isOverTarget -> overflowColor
                isFilled -> activeColor
                else -> emptyColor
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(segmentHeight)
                    .background(barColor)
            )
        }
    }
}

/**
 * Nothing OS High-Contrast Pill Button.
 * Primary: Pure White fill with OLED Black text.
 * Secondary: Outline with 1px border.
 * Destructive: Accent red outline.
 */
@Composable
fun NothingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = true,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val bgColor = when {
        !enabled -> NothingSurfaceRaised
        isDestructive -> Color.Transparent
        isPrimary -> NothingTextDisplay
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> NothingTextDisabled
        isDestructive -> NothingRed
        isPrimary -> NothingBlack
        else -> NothingTextDisplay
    }
    val borderStroke = when {
        !enabled -> BorderStroke(1.dp, NothingBorder)
        isDestructive -> BorderStroke(1.dp, NothingRed)
        !isPrimary -> BorderStroke(1.dp, NothingBorderVisible)
        else -> null
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor)
            .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(999.dp)) else Modifier)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp
            ),
            color = contentColor
        )
    }
}

/**
 * Technical uppercase chip in Nothing OS style: [ LABEL // VALUE ]
 */
@Composable
fun NothingTechnicalChip(
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    accentDot: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(NothingSurfaceRaised)
            .border(1.dp, NothingBorderVisible, RoundedCornerShape(999.dp))
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ) else Modifier
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (accentDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(NothingRed)
            )
        }
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.8.sp
            ),
            color = NothingTextSecondary
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                ),
                color = NothingTextDisplay
            )
        }
    }
}

/**
 * Iconic Nothing OS Glyph Progress Dial.
 * Inspired by Nothing Phone's circular Glyph Interface and Nothing OS 3.0 circular widgets.
 * Renders 32 discrete LED radial segments in a precise circular array on GPU Canvas.
 */
@Composable
fun NothingGlyphDial(
    remainingCalories: Int,
    targetCalories: Int,
    loggedCalories: Int,
    isOverLimit: Boolean,
    modifier: Modifier = Modifier
) {
    val progress = if (targetCalories > 0) loggedCalories.toFloat() / targetCalories.toFloat() else 0f
    val displayRemaining = if (remainingCalories >= 0) remainingCalories else -remainingCalories
    val totalTicks = 32
    val litTicks = (progress.coerceIn(0f, 1f) * totalTicks).roundToInt()
    val overflowTicks = if (isOverLimit) {
        (((progress - 1f).coerceIn(0f, 0.5f) / 0.5f) * totalTicks).roundToInt().coerceAtLeast(1)
    } else 0

    val infiniteTransition = rememberInfiniteTransition(label = "GlyphPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = modifier.size(126.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.width / 2f - 6.dp.toPx()
            val tickLength = 8.dp.toPx()
            val innerRadius = outerRadius - tickLength

            for (i in 0 until totalTicks) {
                val angleDeg = -90f + (i * 360f / totalTicks)
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val cosA = cos(angleRad).toFloat()
                val sinA = sin(angleRad).toFloat()

                val start = Offset(center.x + innerRadius * cosA, center.y + innerRadius * sinA)
                val end = Offset(center.x + outerRadius * cosA, center.y + outerRadius * sinA)

                val tickColor = when {
                    isOverLimit -> {
                        if (i < overflowTicks) NothingRed.copy(alpha = pulseAlpha)
                        else if (i < litTicks) NothingTextDisplay
                        else NothingBorderVisible
                    }
                    i < litTicks -> NothingTextDisplay
                    else -> NothingBorderVisible
                }

                drawLine(
                    color = tickColor,
                    start = start,
                    end = end,
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // 12 o'clock Nothing Red reference beacon
            val dotCenter = Offset(center.x, center.y - outerRadius - 3.dp.toPx())
            drawCircle(
                color = if (isOverLimit) NothingRed else NothingRed.copy(alpha = 0.85f),
                radius = 2.5.dp.toPx(),
                center = dotCenter
            )
        }

        // Center Hardware Readout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$displayRemaining",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                    letterSpacing = (-1.0).sp
                ),
                color = if (isOverLimit) NothingRed else NothingTextDisplay
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "KCAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.2.sp
                ),
                color = if (isOverLimit) NothingRed else NothingTextDisplay
            )
            Text(
                text = if (isOverLimit) "IN ECCESSO" else "RIMASTE",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.0.sp
                ),
                color = if (isOverLimit) NothingRed else NothingTextSecondary
            )
        }
    }
}

// 12x12 discrete 1-bit pixel patterns for Teenage Engineering Pocket Operator LCD
private val PO_PET_IDLE_A = intArrayOf(
    0b000000000000,
    0b001000001000, // antennae
    0b001111111100, // head
    0b011111111110,
    0b011011110110, // eyes
    0b011111111110,
    0b011100001110, // mouth
    0b001111111100, // chin
    0b000111111000, // neck
    0b001111111100, // body
    0b010111111010, // arms
    0b000100001000  // feet
)

private val PO_PET_IDLE_B = intArrayOf(
    0b000000000000,
    0b000100010000, // antennae sway
    0b001111111100, // head
    0b011111111110,
    0b011111111110, // eyes blink
    0b011111111110,
    0b011011110110, // grin
    0b001111111100,
    0b000111111000,
    0b001111111100,
    0b011111111110, // arms raised
    0b001000000100  // step
)

private val PO_PET_ALERT = intArrayOf(
    0b000001100000, // ! top
    0b000001100000,
    0b000000000000, // ! dot
    0b001111111100,
    0b011011110110, // big eyes
    0b011111111110,
    0b011100001110, // open mouth
    0b001111111100,
    0b001111111100,
    0b011111111110,
    0b000111111000,
    0b001000000100
)

/**
 * Teenage Engineering Pocket Operator 1-bit LCD Mascot Screen.
 * Replaces generic cartoons with an authentic hardware LCD synthesizer companion.
 * Features animated 1-bit pixel pet, audio VU-meter bars, and interactive haptic taps.
 */
@Composable
fun NothingPocketOperatorLcd(
    isOverLimit: Boolean,
    isTyping: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var tapCounter by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "PocketOperatorClock")
    val frameSwitch by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "LcdFrame"
    )
    val vuMeterPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "VuMeter"
    )

    val currentPattern = when {
        isOverLimit -> PO_PET_ALERT
        (tapCounter % 2 != 0) -> PO_PET_IDLE_B
        frameSwitch > 1.0f -> PO_PET_IDLE_B
        else -> PO_PET_IDLE_A
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0C0C0C))
            .border(1.dp, NothingBorderVisible, RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                tapCounter++
            }
            .padding(10.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LCD Top Title Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(if (isOverLimit) NothingRed else NothingTextDisplay)
                    )
                    Text(
                        text = "PO-20",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = NothingTextSecondary
                    )
                }
                Text(
                    text = if (isOverLimit) "ALRT" else if (isTyping) "SYNC" else "LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = if (isOverLimit) NothingRed else NothingTextDisplay
                )
            }

            // 1-Bit Pixel Canvas Grid
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Canvas(modifier = Modifier.size(54.dp)) {
                    val gridSize = 12
                    val pixelSizePx = size.width / gridSize
                    val pixelColor = if (isOverLimit) NothingRed else NothingTextDisplay
                    val lcdBgGrid = Color(0xFF141414)

                    // Draw subtle backing grid
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            drawRect(
                                color = lcdBgGrid,
                                topLeft = Offset(c * pixelSizePx + 0.5f, r * pixelSizePx + 0.5f),
                                size = Size(pixelSizePx - 1f, pixelSizePx - 1f)
                            )
                        }
                    }

                    // Draw active 1-bit sprite pixels
                    for (r in 0 until gridSize) {
                        val rowBits = currentPattern[r]
                        for (c in 0 until gridSize) {
                            val isBitSet = (rowBits and (1 shl (gridSize - 1 - c))) != 0
                            if (isBitSet) {
                                drawRect(
                                    color = pixelColor,
                                    topLeft = Offset(c * pixelSizePx + 0.5f, r * pixelSizePx + 0.5f),
                                    size = Size(pixelSizePx - 1f, pixelSizePx - 1f)
                                )
                            }
                        }
                    }
                }

                // Mini Audio VU-Meter bars
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    val activeBars = vuMeterPulse.roundToInt()
                    for (bar in 4 downTo 1) {
                        Box(
                            modifier = Modifier
                                .width((bar * 5).dp)
                                .height(3.dp)
                                .background(
                                    if (bar <= activeBars) (if (bar == 4) NothingRed else NothingTextDisplay)
                                    else NothingBorderVisible
                                )
                        )
                    }
                }
            }

            // LCD Bottom Status Code
            Text(
                text = "LCD // MONO.DSP",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Normal,
                    fontSize = 7.sp,
                    letterSpacing = 0.5.sp
                ),
                color = NothingTextDisabled
            )
        }
    }
}
