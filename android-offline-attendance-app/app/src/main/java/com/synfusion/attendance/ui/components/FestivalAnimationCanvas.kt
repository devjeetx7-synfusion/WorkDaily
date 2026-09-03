package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.synfusion.attendance.data.FestivalAnimationFamily
import com.synfusion.attendance.data.FestivalEvent
import com.synfusion.attendance.data.FestivalIntensity
import kotlin.math.cos
import kotlin.math.sin

private data class Particle(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val speed: Float,
    val alpha: Float,
    val phase: Float,
    val color: Color
)

@Composable
fun FestivalAnimationCanvas(
    event: FestivalEvent,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "festivalAnimation")

    val timeProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeProgress"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseProgress"
    )

    val primaryColor = remember(event.primaryColorHex) { Color(event.primaryColorHex) }
    val secondaryColor = remember(event.secondaryColorHex) { Color(event.secondaryColorHex) }
    val accentColor = remember(event.accentColorHex) { Color(event.accentColorHex) }

    // Controlled particle count strictly targetting 10–20 particles max
    val particleCount = when (event.intensity) {
        FestivalIntensity.SUBTLE -> 10
        FestivalIntensity.MEDIUM -> 15
        FestivalIntensity.STRONG -> 20
    }

    val particles = remember(event.id, particleCount) {
        val colors = listOf(primaryColor, secondaryColor, accentColor)
        List(particleCount) { index ->
            Particle(
                xRatio = ((index * 37) % 100) / 100f,
                yRatio = (((index * 53) % 100) / 100f) * 0.85f, // Confined strictly to upper hero region
                size = 3.5f + ((index * 11) % 8),
                speed = 0.15f + (((index * 17) % 6) / 10f),
                alpha = 0.15f + (((index * 29) % 4) / 10f),
                phase = (index * 0.5f),
                color = colors[index % colors.size]
            )
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        when (event.animationFamily) {
            FestivalAnimationFamily.LIGHT_GLOW -> {
                drawLightGlowAnimation(width, height, timeProgress, pulseProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.COLOR_BURST -> {
                drawColorBurstAnimation(width, height, timeProgress, pulseProgress, particles)
            }
            FestivalAnimationFamily.SNOW_TWINKLE -> {
                drawSnowTwinkleAnimation(width, height, timeProgress, particles, accentColor)
            }
            FestivalAnimationFamily.EDUCATION_FLOAT -> {
                drawEducationFloatAnimation(width, height, timeProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.SUNRISE_RIVER -> {
                drawSunriseRiverAnimation(width, height, timeProgress, pulseProgress, particles, primaryColor, secondaryColor, accentColor)
            }
            FestivalAnimationFamily.KITE_FLY -> {
                drawKiteFlyAnimation(width, height, timeProgress, particles, primaryColor, secondaryColor, accentColor)
            }
            FestivalAnimationFamily.SPIRITUAL_GLOW -> {
                drawSpiritualGlowAnimation(width, height, pulseProgress, timeProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.MOON_STAR_TWINKLE -> {
                drawMoonStarAnimation(width, height, timeProgress, pulseProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.PATRIOTIC_RIBBON -> {
                drawPatrioticRibbonAnimation(width, height, timeProgress, particles, primaryColor, secondaryColor, accentColor)
            }
            FestivalAnimationFamily.PLAYFUL_DIVINE -> {
                drawPlayfulDivineAnimation(width, height, timeProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.CIRCULAR_THREAD -> {
                drawCircularThreadAnimation(width, height, timeProgress, pulseProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.FESTIVE_AUGMENT -> {
                drawFestiveAugmentAnimation(width, height, timeProgress, pulseProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.FLORAL_PETALS -> {
                drawFloralPetalsAnimation(width, height, timeProgress, particles, primaryColor, secondaryColor, accentColor)
            }
            FestivalAnimationFamily.PEACE_LIGHT -> {
                drawPeaceLightAnimation(width, height, pulseProgress, particles, primaryColor, accentColor)
            }
            FestivalAnimationFamily.YOUTHFUL_PLAY -> {
                drawYouthfulPlayAnimation(width, height, timeProgress, particles, primaryColor, secondaryColor, accentColor)
            }
        }
    }
}

private fun DrawScope.drawLightGlowAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.22f * pulse), primaryColor.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.4f),
            radius = width * 0.55f * pulse
        )
    )

    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 15f
        val currentAlpha = (p.alpha * sin((time * 6.28f + p.phase).toDouble()).toFloat().coerceIn(0.2f, 1f))
        drawCircle(
            color = p.color.copy(alpha = currentAlpha * 0.45f),
            radius = p.size,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawColorBurstAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>
) {
    particles.forEach { p ->
        val driftX = p.xRatio * width + sin((time * 6.28f * p.speed + p.phase).toDouble()).toFloat() * 25f
        val driftY = p.yRatio * height + cos((time * 6.28f * p.speed + p.phase).toDouble()).toFloat() * 25f
        val radius = p.size * 2.8f * pulse

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(p.color.copy(alpha = p.alpha * 0.35f), p.color.copy(alpha = 0.04f), Color.Transparent),
                center = Offset(driftX, driftY),
                radius = radius
            )
        )
    }
}

private fun DrawScope.drawSnowTwinkleAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    accentColor: Color
) {
    particles.forEach { p ->
        val y = ((p.yRatio + time * p.speed) % 1.0f) * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 12f
        drawCircle(
            color = Color.White.copy(alpha = p.alpha * 0.55f),
            radius = p.size * 0.8f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawEducationFloatAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    particles.forEachIndexed { i, p ->
        val y = ((p.yRatio - time * p.speed * 0.4f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 3.14f + p.phase).toDouble()).toFloat() * 20f

        if (i % 2 == 0) {
            val rectSize = p.size * 2.2f
            drawRoundRect(
                color = primaryColor.copy(alpha = p.alpha * 0.3f),
                topLeft = Offset(x, y),
                size = Size(rectSize, rectSize * 0.7f),
                cornerRadius = CornerRadius(3f)
            )
        } else {
            drawCircle(
                color = accentColor.copy(alpha = p.alpha * 0.4f),
                radius = p.size * 0.75f,
                center = Offset(x, y)
            )
        }
    }
}

private fun DrawScope.drawSunriseRiverAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.28f * pulse), primaryColor.copy(alpha = 0.1f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.2f),
            radius = width * 0.65f
        )
    )

    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed * 0.5f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width
        drawCircle(
            color = accentColor.copy(alpha = p.alpha * 0.38f),
            radius = p.size,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawKiteFlyAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    particles.forEach { p ->
        val x = ((p.xRatio + time * p.speed) % 1.0f) * width
        val y = p.yRatio * height
        drawCircle(
            color = accentColor.copy(alpha = p.alpha * 0.3f),
            radius = p.size,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawSpiritualGlowAnimation(
    width: Float,
    height: Float,
    pulse: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.18f * pulse), primaryColor.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.5f * pulse
        )
    )

    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed * 0.25f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 3.14f + p.phase).toDouble()).toFloat() * 12f
        drawCircle(
            color = primaryColor.copy(alpha = p.alpha * 0.35f),
            radius = p.size * 0.85f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawMoonStarAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height * 0.8f
        val starAlpha = (0.2f + 0.5f * sin((time * 12.56f * p.speed + p.phase).toDouble()).toFloat()).coerceIn(0.1f, 0.7f)
        drawCircle(
            color = accentColor.copy(alpha = starAlpha),
            radius = p.size * 0.65f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawPatrioticRibbonAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    saffron: Color,
    green: Color,
    white: Color
) {
    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(
            color = white.copy(alpha = p.alpha * 0.3f),
            radius = p.size * 0.6f,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawPlayfulDivineAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.3f),
            radius = width * 0.55f
        )
    )

    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed * 0.3f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.35f), radius = p.size, center = Offset(x, y))
    }
}

private fun DrawScope.drawCircularThreadAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.3f), radius = p.size * 0.75f, center = Offset(x, y))
    }
}

private fun DrawScope.drawFestiveAugmentAnimation(
    width: Float,
    height: Float,
    time: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = 0.22f * pulse), accentColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.6f * pulse
        )
    )

    particles.forEach { p ->
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 15f
        val y = p.yRatio * height + cos((time * 6.28f + p.phase).toDouble()).toFloat() * 15f
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.4f), radius = p.size, center = Offset(x, y))
    }
}

private fun DrawScope.drawFloralPetalsAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    particles.forEachIndexed { i, p ->
        val y = ((p.yRatio + time * p.speed * 0.8f) % 1.0f) * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 20f

        rotate(degrees = (time * 360f * p.speed + i * 45) % 360f, pivot = Offset(x, y)) {
            val petalPath = Path().apply {
                moveTo(x, y - p.size * 1.3f)
                quadraticBezierTo(x + p.size, y, x, y + p.size * 1.3f)
                quadraticBezierTo(x - p.size, y, x, y - p.size * 1.3f)
                close()
            }
            drawPath(path = petalPath, color = p.color.copy(alpha = p.alpha * 0.45f))
        }
    }
}

private fun DrawScope.drawPeaceLightAnimation(
    width: Float,
    height: Float,
    pulse: Float,
    particles: List<Particle>,
    primaryColor: Color,
    accentColor: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.15f * pulse), primaryColor.copy(alpha = 0.04f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.3f),
            radius = width * 0.45f
        )
    )

    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(color = primaryColor.copy(alpha = p.alpha * 0.2f), radius = p.size * 0.65f, center = Offset(x, y))
    }
}

private fun DrawScope.drawYouthfulPlayAnimation(
    width: Float,
    height: Float,
    time: Float,
    particles: List<Particle>,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    particles.forEachIndexed { i, p ->
        val y = ((p.yRatio - time * p.speed * 0.5f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 20f

        if (i % 3 == 0) {
            val planePath = Path().apply {
                moveTo(x, y - p.size * 1.3f)
                lineTo(x + p.size, y + p.size * 1.3f)
                lineTo(x, y + p.size)
                lineTo(x - p.size, y + p.size * 1.3f)
                close()
            }
            drawPath(path = planePath, color = primaryColor.copy(alpha = p.alpha * 0.35f))
        } else {
            drawCircle(color = p.color.copy(alpha = p.alpha * 0.4f), radius = p.size, center = Offset(x, y))
        }
    }
}
