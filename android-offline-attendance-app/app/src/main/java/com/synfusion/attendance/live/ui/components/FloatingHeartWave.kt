package com.synfusion.attendance.live.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.random.Random

data class FloatingHeartParticle(
    val id: Long,
    val startXRatio: Float,
    val sizePx: Float,
    val color: Color,
    val driftXRatio: Float,
    val rotationDegrees: Float,
    val durationMillis: Long = Random.nextLong(1800, 2600),
    val startTimeNanos: Long = System.nanoTime()
)

/**
 * Floating Heart Wave Overlay Component.
 * Purely decorative transparent canvas layer for floating heart animations.
 *
 * CRITICAL FIX: Does NOT intercept pointer events or click touches.
 * Uses continuous frame-driven animation (withFrameNanos) for smooth particle lifecycle.
 */
@Composable
fun FloatingHeartWaveOverlay(
    modifier: Modifier = Modifier,
    triggerHeartSignal: Long = 0L,
    onSendHearts: (Int) -> Unit = {}
) {
    var particles by remember { mutableStateOf<List<FloatingHeartParticle>>(emptyList()) }

    val heartColors = remember {
        listOf(
            Color(0xFFFF2D55),
            Color(0xFFFF5252),
            Color(0xFFFF4081),
            Color(0xFFE91E63),
            Color(0xFFFF7043)
        )
    }

    var frameTimeNanos by remember { mutableLongStateOf(System.nanoTime()) }

    // Frame ticker loop for smooth, continuous animation frame updates
    LaunchedEffect(particles.isNotEmpty()) {
        if (particles.isNotEmpty()) {
            while (true) {
                withFrameNanos { time ->
                    frameTimeNanos = time
                }
                // Filter out completed particles smoothly
                val nowNanos = System.nanoTime()
                particles = particles.filter { particle ->
                    val elapsedMillis = (nowNanos - particle.startTimeNanos) / 1_000_000L
                    elapsedMillis < particle.durationMillis
                }
                if (particles.isEmpty()) break
            }
        }
    }

    // Spawn hearts when signal triggers
    LaunchedEffect(triggerHeartSignal) {
        if (triggerHeartSignal > 0) {
            val count = Random.nextInt(2, 5)
            val nowNanos = System.nanoTime()
            val newParticles = (0 until count).map {
                FloatingHeartParticle(
                    id = Random.nextLong(),
                    startXRatio = Random.nextFloat().coerceIn(0.70f, 0.90f),
                    sizePx = Random.nextInt(22, 38).toFloat(),
                    color = heartColors.random(),
                    driftXRatio = (Random.nextFloat() - 0.5f) * 0.18f,
                    rotationDegrees = (Random.nextFloat() - 0.5f) * 45f,
                    startTimeNanos = nowNanos
                )
            }
            particles = (particles + newParticles).takeLast(40)
            onSendHearts(count)
        }
    }

    // Non-clickable Box layer: allows underlying or overlaying controls to receive touches cleanly
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val currentNanos = frameTimeNanos

            particles.forEach { particle ->
                val elapsedMillis = ((currentNanos - particle.startTimeNanos) / 1_000_000L).coerceAtLeast(0L)
                val progress = (elapsedMillis.toFloat() / particle.durationMillis).coerceIn(0f, 1f)

                if (progress < 1f) {
                    val currentY = canvasHeight * (0.85f - (progress * 0.72f))
                    val currentX = canvasWidth * (particle.startXRatio + (particle.driftXRatio * progress))
                    val alpha = (1f - progress).coerceIn(0f, 1f)
                    val scaleFactor = (0.6f + (progress * 0.5f)).coerceIn(0.6f, 1.25f)

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
