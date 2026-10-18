package com.synfusion.attendance.live.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FloatingHeartParticle(
    val id: Long,
    val startXRatio: Float,
    val sizePx: Float,
    val color: Color,
    val driftXRatio: Float,
    val rotationDegrees: Float,
    val durationMillis: Int = Random.nextInt(1800, 2600),
    val startTime: Long = System.currentTimeMillis()
)

@Composable
fun FloatingHeartWaveOverlay(
    modifier: Modifier = Modifier,
    triggerHeartSignal: Long = 0L,
    onSendHearts: (Int) -> Unit = {}
) {
    var particles by remember { mutableStateOf<List<FloatingHeartParticle>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Colors: Pink, coral, red
    val heartColors = remember {
        listOf(
            Color(0xFFFF2D55),
            Color(0xFFFF5252),
            Color(0xFFFF4081),
            Color(0xFFE91E63),
            Color(0xFFFF7043)
        )
    }

    fun spawnHearts(count: Int) {
        val now = System.currentTimeMillis()
        val newParticles = (0 until count).map {
            FloatingHeartParticle(
                id = Random.nextLong(),
                startXRatio = Random.nextFloat().coerceIn(0.65f, 0.90f),
                sizePx = Random.nextInt(18, 36).toFloat(),
                color = heartColors.random(),
                driftXRatio = (Random.nextFloat() - 0.5f) * 0.15f,
                rotationDegrees = (Random.nextFloat() - 0.5f) * 40f
            )
        }
        particles = (particles + newParticles).takeLast(30)
        onSendHearts(count)
    }

    LaunchedEffect(triggerHeartSignal) {
        if (triggerHeartSignal > 0) {
            spawnHearts(Random.nextInt(2, 5))
        }
    }

    // Cleanup expired particles
    LaunchedEffect(particles) {
        if (particles.isNotEmpty()) {
            delay(100)
            val now = System.currentTimeMillis()
            particles = particles.filter { now - it.startTime < it.durationMillis }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                spawnHearts(Random.nextInt(2, 4))
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val now = System.currentTimeMillis()

            particles.forEach { particle ->
                val elapsed = (now - particle.startTime).coerceAtLeast(0)
                val progress = (elapsed.toFloat() / particle.durationMillis).coerceIn(0f, 1f)

                if (progress < 1f) {
                    val currentY = canvasHeight * (0.85f - (progress * 0.70f))
                    val currentX = canvasWidth * (particle.startXRatio + (particle.driftXRatio * progress))
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    val scaleFactor = (0.5f + (progress * 0.6f)).coerceIn(0.5f, 1.2f)

                    rotate(
                        degrees = particle.rotationDegrees * progress,
                        pivot = Offset(currentX, currentY)
                    ) {
                        scale(
                            scale = scaleFactor,
                            pivot = Offset(currentX, currentY)
                        ) {
                            drawHeartShape(
                                center = Offset(currentX, currentY),
                                size = particle.sizePx,
                                color = particle.color.copy(alpha = alpha)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeartShape(
    center: Offset,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        val width = size
        val height = size
        val left = center.x - width / 2
        val top = center.y - height / 2

        moveTo(center.x, top + height * 0.35f)
        cubicTo(
            center.x - width * 0.35f, top,
            left, top + height * 0.35f,
            left, top + height * 0.55f
        )
        cubicTo(
            left, top + height * 0.75f,
            center.x - width * 0.25f, top + height * 0.90f,
            center.x, top + height
        )
        cubicTo(
            center.x + width * 0.25f, top + height * 0.90f,
            left + width, top + height * 0.75f,
            left + width, top + height * 0.55f
        )
        cubicTo(
            left + width, top + height * 0.35f,
            center.x + width * 0.35f, top,
            center.x, top + height * 0.35f
        )
        close()
    }

    drawPath(path = path, color = color)
}
