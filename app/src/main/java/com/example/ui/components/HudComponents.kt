package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BlinkingText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    durationMillis: Int = 800
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style
    )
}

@Composable
fun CrtScanlineOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val scanlineSpacing = 4.dp.toPx()
        val scanlineHeight = 1.dp.toPx()
        
        var y = 0f
        while (y < size.height) {
            drawRect(
                color = Color.Black.copy(alpha = 0.2f),
                topLeft = Offset(0f, y),
                size = androidx.compose.ui.geometry.Size(size.width, scanlineHeight)
            )
            y += scanlineSpacing
        }
    }
}

@Composable
fun RetroSegmentedProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ProgressBarAnimation"
    )

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
    ) {
        val cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = Color(0xFF121415),
            size = size,
            cornerRadius = cornerRadius
        )
        if (animatedProgress > 0f) {
            drawRoundRect(
                color = color,
                size = androidx.compose.ui.geometry.Size(size.width * animatedProgress, size.height),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun RpgStatBar(
    progress: Float,
    color: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                val percent = if (progress.isNaN() || progress.isInfinite()) 0 else (progress * 100).coerceIn(0f, 1000f).toInt()
                contentDescription = "$label macro progress: $value of target, status $percent% complete"
            }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1917))
            .border(1.dp, Color(0xFF2A2E30), RoundedCornerShape(14.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = color)
            Text(text = value, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(8.dp))
        RetroSegmentedProgressBar(
            progress = progress,
            color = color,
            modifier = Modifier.fillMaxWidth().height(10.dp)
        )
    }
}
