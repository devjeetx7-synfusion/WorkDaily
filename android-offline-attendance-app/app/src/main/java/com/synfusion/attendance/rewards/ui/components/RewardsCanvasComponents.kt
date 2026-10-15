package com.synfusion.attendance.rewards.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

/**
 * Reusable Canvas Graphics System for WorkDaily Premium Rewards Experience.
 */

/**
 * Draws a 4-point tapered star at the specified center position.
 */
fun DrawScope.drawFourPointStar(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val halfSize = size / 2f
    val innerRadius = size * 0.18f

    val path = Path().apply {
        moveTo(centerX, centerY - halfSize) // Top tip
        quadraticBezierTo(centerX, centerY, centerX + innerRadius, centerY - innerRadius)
        lineTo(centerX + halfSize, centerY) // Right tip
        quadraticBezierTo(centerX, centerY, centerX + innerRadius, centerY + innerRadius)
        lineTo(centerX, centerY + halfSize) // Bottom tip
        quadraticBezierTo(centerX, centerY, centerX - innerRadius, centerY + innerRadius)
        lineTo(centerX - halfSize, centerY) // Left tip
        quadraticBezierTo(centerX, centerY, centerX - innerRadius, centerY - innerRadius)
        close()
    }

    drawPath(path = path, color = color, style = Fill)
}

/**
 * Draws floating gold/lavender particles (dots, diamonds, mini stars) around hero illustrations.
 */
@Composable
fun RewardsSparkleLayer(
    modifier: Modifier = Modifier,
    starColor: Color = GoldBright,
    sparklePositions: List<Offset> = listOf(
        Offset(0.15f, 0.25f),
        Offset(0.85f, 0.20f),
        Offset(0.78f, 0.70f),
        Offset(0.12f, 0.75f),
        Offset(0.50f, 0.12f)
    )
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        sparklePositions.forEachIndexed { index, pos ->
            val cx = width * pos.x
            val cy = height * pos.y
            val starSize = when (index % 3) {
                0 -> 18.dp.toPx()
                1 -> 12.dp.toPx()
                else -> 15.dp.toPx()
            }
            val color = when (index % 3) {
                0 -> starColor
                1 -> Color.White.copy(alpha = 0.9f)
                else -> HighlightLavender
            }
            drawFourPointStar(centerX = cx, centerY = cy, size = starSize, color = color)
        }
    }
}

/**
 * Soft radial spotlight behind worker, badge, or trophy artwork.
 */
@Composable
fun RewardsRadialGlow(
    modifier: Modifier = Modifier,
    glowColor: Color = GoldPrimary,
    centerOffset: Offset = Offset(0.5f, 0.5f),
    radiusFraction: Float = 0.6f,
    alpha: Float = 0.35f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val center = Offset(size.width * centerOffset.x, size.height * centerOffset.y)
        val radius = size.minDimension * radiusFraction

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = alpha),
                    glowColor.copy(alpha = alpha * 0.4f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            center = center,
            radius = radius
        )
    }
}

/**
 * Floating gold coins / particle dots accent.
 */
@Composable
fun RewardsCoinParticles(
    modifier: Modifier = Modifier,
    particleColor: Color = GoldBright
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Top right floating particles
        drawCircle(
            color = particleColor.copy(alpha = 0.85f),
            radius = 5.dp.toPx(),
            center = Offset(width * 0.88f, height * 0.25f)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = 3.dp.toPx(),
            center = Offset(width * 0.92f, height * 0.35f)
        )

        // Bottom left floating particles
        drawCircle(
            color = particleColor.copy(alpha = 0.75f),
            radius = 4.dp.toPx(),
            center = Offset(width * 0.10f, height * 0.65f)
        )
        drawCircle(
            color = HighlightLavender.copy(alpha = 0.8f),
            radius = 3.dp.toPx(),
            center = Offset(width * 0.16f, height * 0.78f)
        )
    }
}

/**
 * Curved highlights and decorative waves across hero background corners.
 */
@Composable
fun RewardsCurvedHighlights(
    modifier: Modifier = Modifier,
    waveColor: Color = Color.White.copy(alpha = 0.08f)
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Top-left curved wave
        val wavePath1 = Path().apply {
            moveTo(0f, h * 0.4f)
            quadraticBezierTo(w * 0.3f, h * 0.1f, w * 0.6f, 0f)
            lineTo(0f, 0f)
            close()
        }
        drawPath(path = wavePath1, color = waveColor)

        // Bottom-right subtle wave
        val wavePath2 = Path().apply {
            moveTo(w * 0.4f, h)
            quadraticBezierTo(w * 0.7f, h * 0.8f, w, h * 0.6f)
            lineTo(w, h)
            close()
        }
        drawPath(path = wavePath2, color = waveColor)
    }
}

/**
 * Full decorative Hero Background combining base gradient, waves, glow, and sparkles.
 */
@Composable
fun RewardsHeroBackground(
    modifier: Modifier = Modifier,
    baseGradient: Brush = RewardsPrimaryGradient,
    showGlow: Boolean = true,
    glowColor: Color = GoldPrimary,
    glowCenter: Offset = Offset(0.75f, 0.4f),
    showSparkles: Boolean = true
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = baseGradient)
        }
        RewardsCurvedHighlights(modifier = Modifier.fillMaxSize())
        if (showGlow) {
            RewardsRadialGlow(
                modifier = Modifier.fillMaxSize(),
                glowColor = glowColor,
                centerOffset = glowCenter,
                radiusFraction = 0.55f,
                alpha = 0.3f
            )
        }
        if (showSparkles) {
            RewardsSparkleLayer(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * Celebration confetti particles for Leaderboard or Unlocked Badges.
 */
@Composable
fun RewardsCelebrationEffects(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val confettiColors = listOf(GoldBright, RoyalPurple, HighlightLavender, GoldPrimary, Color.White)

        val positions = listOf(
            Offset(0.15f, 0.15f), Offset(0.30f, 0.10f), Offset(0.50f, 0.08f),
            Offset(0.70f, 0.12f), Offset(0.85f, 0.18f), Offset(0.20f, 0.35f),
            Offset(0.80f, 0.32f), Offset(0.08f, 0.45f), Offset(0.92f, 0.48f)
        )

        positions.forEachIndexed { i, pos ->
            val color = confettiColors[i % confettiColors.size]
            val x = w * pos.x
            val y = h * pos.y

            if (i % 2 == 0) {
                drawCircle(color = color.copy(alpha = 0.85f), radius = 3.5.dp.toPx(), center = Offset(x, y))
            } else {
                drawRect(
                    color = color.copy(alpha = 0.85f),
                    topLeft = Offset(x - 3.dp.toPx(), y - 3.dp.toPx()),
                    size = Size(6.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}
