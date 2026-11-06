package com.example.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

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
                // Monospace fallback for letters not in NDOT digit map
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
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
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
