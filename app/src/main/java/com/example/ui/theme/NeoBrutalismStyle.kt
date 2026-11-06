package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.screens.applePressEffect

/**
 * Neo-Brutalist 0-blur hard drop shadow rendered on GPU via drawBehind.
 */
fun Modifier.neoHardShadow(
    offset: Dp = 4.dp,
    color: Color = Color.Black,
    cornerRadius: Dp = 12.dp
): Modifier = this.drawBehind {
    drawRoundRect(
        color = color,
        topLeft = Offset(offset.toPx(), offset.toPx()),
        size = size,
        cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
    )
}

/**
 * Neo-Brutalist crisp thick outline border.
 */
fun Modifier.neoBorder(
    width: Dp = 2.dp,
    color: Color = Color.Black,
    cornerRadius: Dp = 12.dp
): Modifier = this.border(width, color, RoundedCornerShape(cornerRadius))

/**
 * Adaptive container modifier supporting Apple elevation and Neo-Brutalist shadow styles.
 */
@Composable
fun Modifier.foodLabCardModifier(
    shape: Shape = RoundedCornerShape(22.dp),
    neoCornerRadius: Dp = 12.dp,
    elevation: Dp = 4.dp,
    neoOffset: Dp = 4.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
): Modifier {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
    return when {
        isNothing -> {
            this.border(1.dp, NothingBorderVisible, shape)
        }
        isNeo -> {
            this
                .neoHardShadow(offset = neoOffset, color = Color.Black, cornerRadius = neoCornerRadius)
                .border(2.dp, Color.Black, RoundedCornerShape(neoCornerRadius))
        }
        isBento -> {
            this
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = BentoCocoa.copy(alpha = 0.12f),
                    ambientColor = BentoCocoa.copy(alpha = 0.08f)
                )
                .border(2.dp, BentoBorder, RoundedCornerShape(22.dp))
        }
        else -> {
            this
                .shadow(
                    elevation = elevation,
                    shape = shape,
                    spotColor = Color.Black.copy(alpha = 0.18f),
                    ambientColor = Color.Black.copy(alpha = 0.10f)
                )
                .border(borderWidth, borderColor, shape)
        }
    }
}

/**
 * Adaptive click/press effect: smooth spring scale in Apple themes,
 * tactile physical offset translation (pressing into the shadow) in Neo-Brutalism.
 */
@Composable
fun Modifier.foodLabPressEffect(
    pressedScale: Float = 0.94f,
    offsetAmount: Dp = 3.dp,
    onClick: (() -> Unit)? = null
): Modifier {
    val isNeo = MaterialTheme.isNeoBrutalist
    val isBento = MaterialTheme.isBentoCraft
    val isNothing = MaterialTheme.isNothingOs
    if (isNothing) {
        return this.applePressEffect(pressedScale = 0.97f, onClick = onClick)
    }
    if (isBento) {
        return this.applePressEffect(pressedScale = 0.93f, onClick = onClick)
    }
    if (!isNeo) {
        return this.applePressEffect(pressedScale = pressedScale, onClick = onClick)
    }
    var isPressed by remember { mutableStateOf(false) }
    return this
        .offset(
            x = if (isPressed) offsetAmount else 0.dp,
            y = if (isPressed) offsetAmount else 0.dp
        )
        .pointerInput(onClick) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = { onClick?.invoke() }
            )
        }
}

@Composable
fun foodLabCardShape(
    defaultShape: Shape = RoundedCornerShape(22.dp),
    neoCornerRadius: Dp = 12.dp
): Shape {
    return when {
        MaterialTheme.isNothingOs -> RoundedCornerShape(14.dp)
        MaterialTheme.isNeoBrutalist -> RoundedCornerShape(neoCornerRadius)
        MaterialTheme.isBentoCraft -> RoundedCornerShape(22.dp)
        else -> defaultShape
    }
}

@Composable
fun foodLabCardBorder(
    defaultBorder: BorderStroke = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    neoWidth: Dp = 2.dp,
    neoColor: Color = Color.Black
): BorderStroke {
    return when {
        MaterialTheme.isNothingOs -> BorderStroke(1.dp, NothingBorderVisible)
        MaterialTheme.isNeoBrutalist -> BorderStroke(neoWidth, neoColor)
        MaterialTheme.isBentoCraft -> BorderStroke(2.dp, BentoBorder)
        else -> defaultBorder
    }
}
