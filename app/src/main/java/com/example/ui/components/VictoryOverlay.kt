package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.example.util.LocalSoundManager
import kotlin.random.Random

private data class Particle(
    val x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    val color: Color
)

@Composable
fun VictoryOverlay(
    isVisible: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current

    LaunchedEffect(isVisible) {
        if (isVisible) {
            soundManager.playSound("power_up")
            kotlinx.coroutines.delay(2800)
            onFinished()
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(400))
    ) {
        val transition = rememberInfiniteTransition(label = "VictoryParticles")
        val progress by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
            label = "Progress"
        )

        val particles = remember {
            List(40) {
                Particle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * -0.5f,
                    radius = Random.nextFloat() * 6f + 4f,
                    speed = Random.nextFloat() * 0.8f + 0.4f,
                    color = listOf(
                        Color(0xFF4ADE80), Color(0xFFFEF08A), Color(0xFF60A5FA), Color(0xFFF43F5E)
                    ).random()
                )
            }
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            particles.forEach { p ->
                val currentY = ((p.y + progress * p.speed) % 1.2f) * h
                val currentX = p.x * w
                drawCircle(
                    color = p.color,
                    radius = p.radius,
                    center = Offset(currentX, currentY)
                )
            }
        }
    }
}
