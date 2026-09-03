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
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "timeProgress"
    )

    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseProgress"
    )

    val primaryColor = remember(event.primaryColorHex) { Color(event.primaryColorHex) }
    val secondaryColor = remember(event.secondaryColorHex) { Color(event.secondaryColorHex) }
    val accentColor = remember(event.accentColorHex) { Color(event.accentColorHex) }

    val particleCount = when (event.intensity) {
        FestivalIntensity.SUBTLE -> 12
        FestivalIntensity.MEDIUM -> 22
        FestivalIntensity.STRONG -> 32
    }

    val particles = remember(event.id, particleCount) {
        val colors = listOf(primaryColor, secondaryColor, accentColor)
        List(particleCount) { index ->
            Particle(
                xRatio = ((index * 37) % 100) / 100f,
                yRatio = ((index * 53) % 100) / 100f,
                size = 4f + ((index * 13) % 12),
                speed = 0.2f + (((index * 17) % 8) / 10f),
                alpha = 0.2f + (((index * 29) % 5) / 10f),
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
    // Bottom aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.25f * pulse), primaryColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.85f),
            radius = width * 0.6f * pulse
        )
    )

    // Rising golden sparks
    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 20f
        val currentAlpha = (p.alpha * sin((time * 6.28f + p.phase).toDouble()).toFloat().coerceIn(0.2f, 1f))
        drawCircle(
            color = p.color.copy(alpha = currentAlpha * 0.5f),
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
    particles.forEachIndexed { i, p ->
        val driftX = p.xRatio * width + sin((time * 6.28f * p.speed + p.phase).toDouble()).toFloat() * 35f
        val driftY = p.yRatio * height + cos((time * 6.28f * p.speed + p.phase).toDouble()).toFloat() * 35f
        val radius = p.size * 3.5f * pulse

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(p.color.copy(alpha = p.alpha * 0.4f), p.color.copy(alpha = 0.05f), Color.Transparent),
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
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 15f
        drawCircle(
            color = Color.White.copy(alpha = p.alpha * 0.6f),
            radius = p.size * 0.8f,
            center = Offset(x, y)
        )
    }

    // Top twinkling stars
    for (i in 0..4) {
        val starX = width * (0.15f + i * 0.2f)
        val starY = height * 0.08f + sin((time * 6.28f + i).toDouble()).toFloat() * 5f
        val glintAlpha = (0.3f + 0.5f * sin((time * 12.56f + i * 1.5f).toDouble()).toFloat()).coerceIn(0.1f, 0.8f)

        drawCircle(
            color = accentColor.copy(alpha = glintAlpha),
            radius = 6f,
            center = Offset(starX, starY)
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
        val y = ((p.yRatio - time * p.speed * 0.5f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 3.14f + p.phase).toDouble()).toFloat() * 25f

        if (i % 2 == 0) {
            // Floating book icon (rectangle with line)
            val rectSize = p.size * 2.5f
            drawRoundRect(
                color = primaryColor.copy(alpha = p.alpha * 0.35f),
                topLeft = Offset(x, y),
                size = Size(rectSize, rectSize * 0.7f),
                cornerRadius = CornerRadius(4f)
            )
        } else {
            // Floating star glint
            drawCircle(
                color = accentColor.copy(alpha = p.alpha * 0.45f),
                radius = p.size * 0.8f,
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
    // Sunrise aura top center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.3f * pulse), primaryColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.15f),
            radius = width * 0.7f
        )
    )

    // Water ripple lines near bottom
    val wavePath = Path()
    for (wave in 0..2) {
        wavePath.reset()
        val startY = height * (0.8f + wave * 0.06f)
        wavePath.moveTo(0f, startY)
        val waveShift = time * 6.28f + wave * 2.0f
        for (x in 0..width.toInt() step 20) {
            val y = startY + sin((x * 0.015f + waveShift).toDouble()).toFloat() * 8f
            wavePath.lineTo(x.toFloat(), y)
        }
        wavePath.lineTo(width, height)
        wavePath.lineTo(0f, height)
        wavePath.close()

        drawPath(
            path = wavePath,
            color = secondaryColor.copy(alpha = 0.05f + wave * 0.03f)
        )
    }

    // Golden sunbeams floating upwards
    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width
        drawCircle(
            color = accentColor.copy(alpha = p.alpha * 0.4f),
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
    // Sky sunlight glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(secondaryColor.copy(alpha = 0.12f), Color.Transparent),
            center = Offset(width * 0.8f, height * 0.2f),
            radius = width * 0.5f
        )
    )

    // Animated Drifting Kite
    val kiteX = width * 0.3f + sin((time * 6.28f).toDouble()).toFloat() * (width * 0.25f)
    val kiteY = height * 0.2f + cos((time * 12.56f).toDouble()).toFloat() * (height * 0.08f)
    val kiteSize = 28f

    rotate(degrees = 15f + sin((time * 6.28f).toDouble()).toFloat() * 10f, pivot = Offset(kiteX, kiteY)) {
        val kitePath = Path().apply {
            moveTo(kiteX, kiteY - kiteSize)
            lineTo(kiteX + kiteSize, kiteY)
            lineTo(kiteX, kiteY + kiteSize)
            lineTo(kiteX - kiteSize, kiteY)
            close()
        }
        drawPath(path = kitePath, color = primaryColor.copy(alpha = 0.5f))

        // Kite Cross Ribs
        drawLine(
            color = accentColor.copy(alpha = 0.7f),
            start = Offset(kiteX, kiteY - kiteSize),
            end = Offset(kiteX, kiteY + kiteSize),
            strokeWidth = 2f
        )
        drawLine(
            color = accentColor.copy(alpha = 0.7f),
            start = Offset(kiteX - kiteSize, kiteY),
            end = Offset(kiteX + kiteSize, kiteY),
            strokeWidth = 2f
        )
    }

    // Drifting sunlight motes
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
    // Deep spiritual aura ring
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.2f * pulse), primaryColor.copy(alpha = 0.08f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.4f),
            radius = width * 0.55f * pulse
        )
    )

    // Gentle floating light motes
    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed * 0.3f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 3.14f + p.phase).toDouble()).toFloat() * 15f
        drawCircle(
            color = primaryColor.copy(alpha = p.alpha * 0.4f),
            radius = p.size * 0.9f,
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
    // Crescent Moon in top right
    val moonX = width * 0.8f
    val moonY = height * 0.12f
    val moonRadius = 24f

    drawCircle(
        color = accentColor.copy(alpha = 0.7f),
        radius = moonRadius,
        center = Offset(moonX, moonY)
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.15f), // subtle cutout overlay
        radius = moonRadius * 0.85f,
        center = Offset(moonX + 8f, moonY - 4f)
    )

    // Twinkling stars
    particles.forEachIndexed { i, p ->
        val x = p.xRatio * width
        val y = p.yRatio * height * 0.6f
        val starAlpha = (0.2f + 0.6f * sin((time * 12.56f * p.speed + p.phase).toDouble()).toFloat()).coerceIn(0.1f, 0.8f)
        drawCircle(
            color = accentColor.copy(alpha = starAlpha),
            radius = p.size * 0.7f,
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
    val pathSaffron = Path()
    val pathWhite = Path()
    val pathGreen = Path()

    val waveShift = time * 6.28f

    // Saffron Top Ribbon
    pathSaffron.moveTo(0f, 0f)
    for (x in 0..width.toInt() step 20) {
        val y = height * 0.05f + sin((x * 0.01f + waveShift).toDouble()).toFloat() * 12f
        pathSaffron.lineTo(x.toFloat(), y)
    }
    pathSaffron.lineTo(width, 0f)
    pathSaffron.close()
    drawPath(pathSaffron, color = saffron.copy(alpha = 0.2f))

    // Green Bottom Ribbon
    pathGreen.moveTo(0f, height)
    for (x in 0..width.toInt() step 20) {
        val y = height * 0.95f + sin((x * 0.01f + waveShift + 1.5f).toDouble()).toFloat() * 12f
        pathGreen.lineTo(x.toFloat(), y)
    }
    pathGreen.lineTo(width, height)
    pathGreen.close()
    drawPath(pathGreen, color = green.copy(alpha = 0.18f))

    // Subtle sparkles
    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(
            color = white.copy(alpha = p.alpha * 0.35f),
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
    // Peacock Blue Aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primaryColor.copy(alpha = 0.22f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.3f),
            radius = width * 0.6f
        )
    )

    // Gentle swaying peacock feather curve
    val featherPath = Path()
    val startX = width * 0.85f
    val startY = height * 0.15f
    featherPath.moveTo(startX, startY)
    val sway = sin((time * 3.14f).toDouble()).toFloat() * 20f
    featherPath.cubicTo(
        startX - 40f, startY + 60f + sway,
        startX - 80f, startY + 120f,
        startX - 120f, startY + 180f
    )
    drawPath(
        path = featherPath,
        color = accentColor.copy(alpha = 0.35f),
        style = Stroke(width = 3f)
    )

    // Motes
    particles.forEach { p ->
        val y = ((p.yRatio - time * p.speed * 0.4f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.4f), radius = p.size, center = Offset(x, y))
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
    // Rakhi geometry motif in center
    val centerX = width * 0.5f
    val centerY = height * 0.25f

    drawCircle(
        color = primaryColor.copy(alpha = 0.12f * pulse),
        radius = 50f * pulse,
        center = Offset(centerX, centerY)
    )
    drawCircle(
        color = accentColor.copy(alpha = 0.3f),
        radius = 25f,
        center = Offset(centerX, centerY),
        style = Stroke(width = 2f)
    )

    // Soft thread line extending
    drawLine(
        color = primaryColor.copy(alpha = 0.2f),
        start = Offset(0f, centerY),
        end = Offset(width, centerY),
        strokeWidth = 2f
    )

    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.35f), radius = p.size * 0.8f, center = Offset(x, y))
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
            colors = listOf(primaryColor.copy(alpha = 0.28f * pulse), accentColor.copy(alpha = 0.1f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.35f),
            radius = width * 0.65f * pulse
        )
    )

    particles.forEach { p ->
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 20f
        val y = p.yRatio * height + cos((time * 6.28f + p.phase).toDouble()).toFloat() * 20f
        drawCircle(color = accentColor.copy(alpha = p.alpha * 0.45f), radius = p.size * 1.1f, center = Offset(x, y))
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
        val y = ((p.yRatio + time * p.speed) % 1.0f) * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 25f

        rotate(degrees = (time * 360f * p.speed + i * 45) % 360f, pivot = Offset(x, y)) {
            // Teardrop petal shape
            val petalPath = Path().apply {
                moveTo(x, y - p.size * 1.5f)
                quadraticBezierTo(x + p.size, y, x, y + p.size * 1.5f)
                quadraticBezierTo(x - p.size, y, x, y - p.size * 1.5f)
                close()
            }
            drawPath(path = petalPath, color = p.color.copy(alpha = p.alpha * 0.5f))
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
    // Tranquil subtle warm glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accentColor.copy(alpha = 0.18f * pulse), primaryColor.copy(alpha = 0.05f), Color.Transparent),
            center = Offset(width * 0.5f, height * 0.3f),
            radius = width * 0.5f
        )
    )

    particles.forEach { p ->
        val x = p.xRatio * width
        val y = p.yRatio * height
        drawCircle(color = primaryColor.copy(alpha = p.alpha * 0.25f), radius = p.size * 0.7f, center = Offset(x, y))
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
        val y = ((p.yRatio - time * p.speed * 0.6f) % 1.0f + 1.0f) % 1.0f * height
        val x = p.xRatio * width + sin((time * 6.28f + p.phase).toDouble()).toFloat() * 30f

        if (i % 3 == 0) {
            // Paper plane triangle
            val planePath = Path().apply {
                moveTo(x, y - p.size * 1.5f)
                lineTo(x + p.size * 1.2f, y + p.size * 1.5f)
                lineTo(x, y + p.size)
                lineTo(x - p.size * 1.2f, y + p.size * 1.5f)
                close()
            }
            drawPath(path = planePath, color = primaryColor.copy(alpha = p.alpha * 0.4f))
        } else {
            drawCircle(color = p.color.copy(alpha = p.alpha * 0.45f), radius = p.size, center = Offset(x, y))
        }
    }
}
