package com.synfusion.attendance.rewards.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
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

// ==========================================
// CONSTRUCTION ITEM CANVAS ILLUSTRATION DRAWING FUNCTIONS
// ==========================================

fun DrawScope.drawSafetyHelmet(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    val domePath = Path().apply {
        moveTo(cx - r * 0.85f, cy + r * 0.2f)
        cubicTo(
            cx - r * 0.85f, cy - r * 0.9f,
            cx + r * 0.85f, cy - r * 0.9f,
            cx + r * 0.85f, cy + r * 0.2f
        )
        close()
    }
    drawPath(
        path = domePath,
        brush = Brush.verticalGradient(
            listOf(Color(0xFFFFF1A8), Color(0xFFFFBE0B), Color(0xFFD97706)),
            startY = cy - r, endY = cy + r * 0.2f
        )
    )
    drawRoundRect(
        color = Color(0xFFF59E0B),
        topLeft = Offset(cx - r * 1.05f, cy + r * 0.15f),
        size = Size(r * 2.1f, r * 0.35f),
        cornerRadius = CornerRadius(r * 0.15f)
    )
    drawRoundRect(
        color = Color(0xFFFFF1A8),
        topLeft = Offset(cx - r * 0.2f, cy - r * 0.75f),
        size = Size(r * 0.4f, r * 0.8f),
        cornerRadius = CornerRadius(r * 0.1f)
    )
}

fun DrawScope.drawHammer(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    val handlePath = Path().apply {
        moveTo(cx - r * 0.15f, cy - r * 0.1f)
        lineTo(cx + r * 0.45f, cy + r * 0.85f)
        lineTo(cx + r * 0.30f, cy + r * 0.95f)
        lineTo(cx - r * 0.30f, cy + r * 0.05f)
        close()
    }
    drawPath(handlePath, color = Color(0xFF8B4513))
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569))),
        topLeft = Offset(cx - r * 0.8f, cy - r * 0.6f),
        size = Size(r * 1.2f, r * 0.5f),
        cornerRadius = CornerRadius(r * 0.1f)
    )
    val clawPath = Path().apply {
        moveTo(cx + r * 0.4f, cy - r * 0.6f)
        quadraticBezierTo(cx + r * 0.8f, cy - r * 0.3f, cx + r * 0.7f, cy + r * 0.1f)
        lineTo(cx + r * 0.4f, cy - r * 0.1f)
        close()
    }
    drawPath(clawPath, color = Color(0xFF64748B))
}

fun DrawScope.drawBricks(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    val bw = r * 0.85f
    val bh = r * 0.35f
    val brickColor = Color(0xFFEA580C)
    val brickDark = Color(0xFFC2410C)

    drawRoundRect(
        color = brickColor,
        topLeft = Offset(cx - bw * 1.05f, cy + bh * 0.2f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(r * 0.05f)
    )
    drawRoundRect(
        color = brickDark,
        topLeft = Offset(cx + bw * 0.05f, cy + bh * 0.2f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(r * 0.05f)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFC2410C))),
        topLeft = Offset(cx - bw * 0.5f, cy - bh * 0.9f),
        size = Size(bw, bh),
        cornerRadius = CornerRadius(r * 0.05f)
    )
}

fun DrawScope.drawToolbox(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF873FFF), Color(0xFF4F35E8))),
        topLeft = Offset(cx - r * 0.85f, cy - r * 0.3f),
        size = Size(r * 1.7f, r * 1.0f),
        cornerRadius = CornerRadius(r * 0.15f)
    )
    drawRoundRect(
        color = Color(0xFFFFBE0B),
        topLeft = Offset(cx - r * 0.9f, cy - r * 0.45f),
        size = Size(r * 1.8f, r * 0.3f),
        cornerRadius = CornerRadius(r * 0.08f)
    )
    val handlePath = Path().apply {
        moveTo(cx - r * 0.35f, cy - r * 0.45f)
        lineTo(cx - r * 0.35f, cy - r * 0.75f)
        lineTo(cx + r * 0.35f, cy - r * 0.75f)
        lineTo(cx + r * 0.35f, cy - r * 0.45f)
    }
    drawPath(handlePath, color = Color(0xFFFFD54A), style = Stroke(width = r * 0.18f))
    drawCircle(color = Color.White, radius = r * 0.12f, center = Offset(cx - r * 0.4f, cy - r * 0.15f))
    drawCircle(color = Color.White, radius = r * 0.12f, center = Offset(cx + r * 0.4f, cy - r * 0.15f))
}

fun DrawScope.drawCrane(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    val craneColor = Color(0xFFFFBE0B)
    val steelColor = Color(0xFF475569)

    drawLine(craneColor, Offset(cx - r * 0.3f, cy + r * 0.9f), Offset(cx - r * 0.3f, cy - r * 0.7f), strokeWidth = r * 0.15f)
    drawLine(steelColor, Offset(cx - r * 0.3f, cy + r * 0.5f), Offset(cx - r * 0.1f, cy + r * 0.1f), strokeWidth = r * 0.08f)
    drawLine(steelColor, Offset(cx - r * 0.3f, cy + r * 0.1f), Offset(cx - r * 0.1f, cy - r * 0.3f), strokeWidth = r * 0.08f)
    drawLine(craneColor, Offset(cx - r * 0.7f, cy - r * 0.6f), Offset(cx + r * 0.9f, cy - r * 0.6f), strokeWidth = r * 0.15f)
    drawLine(steelColor, Offset(cx + r * 0.6f, cy - r * 0.6f), Offset(cx + r * 0.6f, cy + r * 0.2f), strokeWidth = r * 0.08f)
    drawCircle(Color(0xFFF59E0B), radius = r * 0.15f, center = Offset(cx + r * 0.6f, cy + r * 0.25f))
}

fun DrawScope.drawCementTruck(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    drawRoundRect(
        color = Color(0xFFFFBE0B),
        topLeft = Offset(cx - r * 0.9f, cy - r * 0.1f),
        size = Size(r * 0.6f, r * 0.7f),
        cornerRadius = CornerRadius(r * 0.1f)
    )
    val drumPath = Path().apply {
        moveTo(cx - r * 0.2f, cy - r * 0.5f)
        lineTo(cx + r * 0.8f, cy - r * 0.2f)
        lineTo(cx + r * 0.6f, cy + r * 0.3f)
        lineTo(cx - r * 0.4f, cy + r * 0.0f)
        close()
    }
    drawPath(
        path = drumPath,
        brush = Brush.horizontalGradient(listOf(Color(0xFF873FFF), Color(0xFFA64CFF), Color(0xFF6737F5)))
    )
    drawCircle(Color(0xFF1E293B), radius = r * 0.22f, center = Offset(cx - r * 0.6f, cy + r * 0.65f))
    drawCircle(Color(0xFF1E293B), radius = r * 0.22f, center = Offset(cx + r * 0.2f, cy + r * 0.65f))
    drawCircle(Color.White, radius = r * 0.08f, center = Offset(cx - r * 0.6f, cy + r * 0.65f))
    drawCircle(Color.White, radius = r * 0.08f, center = Offset(cx + r * 0.2f, cy + r * 0.65f))
}

fun DrawScope.drawGoldenBlueprint(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFFFFF1A8), Color(0xFFFFBE0B), Color(0xFFF59E0B))),
        topLeft = Offset(cx - r * 0.7f, cy - r * 0.6f),
        size = Size(r * 1.4f, r * 1.1f),
        cornerRadius = CornerRadius(r * 0.12f)
    )
    val gridColor = Color(0xFF8D5B00).copy(alpha = 0.5f)
    drawLine(gridColor, Offset(cx - r * 0.4f, cy - r * 0.4f), Offset(cx - r * 0.4f, cy + r * 0.3f), strokeWidth = r * 0.06f)
    drawLine(gridColor, Offset(cx, cy - r * 0.4f), Offset(cx, cy + r * 0.3f), strokeWidth = r * 0.06f)
    drawLine(gridColor, Offset(cx + r * 0.4f, cy - r * 0.4f), Offset(cx + r * 0.4f, cy + r * 0.3f), strokeWidth = r * 0.06f)
    drawLine(gridColor, Offset(cx - r * 0.5f, cy - r * 0.1f), Offset(cx + r * 0.5f, cy - r * 0.1f), strokeWidth = r * 0.06f)
    drawRoundRect(
        color = Color(0xFF6737F5),
        topLeft = Offset(cx - r * 0.15f, cy - r * 0.65f),
        size = Size(r * 0.3f, r * 1.2f),
        cornerRadius = CornerRadius(r * 0.05f)
    )
}

fun DrawScope.drawForemansPass(cx: Float, cy: Float, itemSize: Float) {
    val r = itemSize / 2f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF1A8), Color(0xFFFFBE0B))),
        topLeft = Offset(cx - r * 0.65f, cy - r * 0.7f),
        size = Size(r * 1.3f, r * 1.4f),
        cornerRadius = CornerRadius(r * 0.15f)
    )
    drawRoundRect(
        color = Color(0xFFD97706),
        topLeft = Offset(cx - r * 0.65f, cy - r * 0.7f),
        size = Size(r * 1.3f, r * 1.4f),
        cornerRadius = CornerRadius(r * 0.15f),
        style = Stroke(width = r * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFF6737F5),
        topLeft = Offset(cx - r * 0.55f, cy - r * 0.6f),
        size = Size(r * 1.1f, r * 0.35f),
        cornerRadius = CornerRadius(r * 0.08f)
    )
    drawRoundRect(
        color = Color(0xFF1E293B),
        topLeft = Offset(cx - r * 0.2f, cy - r * 0.85f),
        size = Size(r * 0.4f, r * 0.2f),
        cornerRadius = CornerRadius(r * 0.05f)
    )
    drawFourPointStar(centerX = cx, centerY = cy + r * 0.15f, size = r * 0.6f, color = Color(0xFF6737F5))
}
