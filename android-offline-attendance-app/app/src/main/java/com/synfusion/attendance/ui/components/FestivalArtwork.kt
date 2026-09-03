package com.synfusion.attendance.ui.components

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.synfusion.attendance.data.FestivalAnimationFamily
import com.synfusion.attendance.data.FestivalEvent
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-quality vector artwork composable rendered per festival.
 * Renders high-fidelity vector illustrations embedded in the hero header.
 */
@Composable
fun FestivalArtwork(
    event: FestivalEvent,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(event.primaryColorHex)
    val secondaryColor = Color(event.secondaryColorHex)
    val accentColor = Color(event.accentColorHex)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            if (width <= 0f || height <= 0f) return@Canvas

            when {
                event.id.contains("chhath") || event.animationFamily == FestivalAnimationFamily.SUNRISE_RIVER -> {
                    drawChhathPujaArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("diwali") -> {
                    drawDiwaliArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("holi") || event.animationFamily == FestivalAnimationFamily.COLOR_BURST -> {
                    drawHoliArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("teachers") || event.animationFamily == FestivalAnimationFamily.EDUCATION_FLOAT -> {
                    drawTeachersDayArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("makar_sankranti") || event.animationFamily == FestivalAnimationFamily.KITE_FLY -> {
                    drawMakarSankrantiArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("republic") || event.id.contains("independence") || event.animationFamily == FestivalAnimationFamily.PATRIOTIC_RIBBON -> {
                    drawPatrioticArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("eid") || event.animationFamily == FestivalAnimationFamily.MOON_STAR_TWINKLE -> {
                    drawEidArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("christmas") || event.animationFamily == FestivalAnimationFamily.SNOW_TWINKLE -> {
                    drawChristmasArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("onam") || event.animationFamily == FestivalAnimationFamily.FLORAL_PETALS -> {
                    drawOnamArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("ganesh") || event.animationFamily == FestivalAnimationFamily.FESTIVE_AUGMENT -> {
                    drawGaneshArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("janmashtami") || event.animationFamily == FestivalAnimationFamily.PLAYFUL_DIVINE -> {
                    drawJanmashtamiArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("raksha") || event.animationFamily == FestivalAnimationFamily.CIRCULAR_THREAD -> {
                    drawRakshaBandhanArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("shivratri") -> {
                    drawMahaShivratriArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("ram_navami") || event.id.contains("dussehra") -> {
                    drawBowAndArrowArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("mahavir") || event.id.contains("buddha") -> {
                    drawLotusArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("gandhi") || event.animationFamily == FestivalAnimationFamily.PEACE_LIGHT -> {
                    drawGandhiArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("children") || event.animationFamily == FestivalAnimationFamily.YOUTHFUL_PLAY -> {
                    drawChildrensDayArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                event.id.contains("guru_nanak") -> {
                    drawGuruNanakArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
                else -> {
                    drawGenericFestiveArtwork(width, height, primaryColor, secondaryColor, accentColor)
                }
            }
        }
    }
}

// 1. Chhath Puja Artwork — Sunrise, Holy River, Golden Rays, Floating Diya
private fun DrawScope.drawChhathPujaArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.75f
    val cy = h * 0.45f

    // Radiant Sunrise Disc
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.9f), primary.copy(alpha = 0.6f), Color.Transparent),
            center = Offset(cx, cy),
            radius = h * 0.55f
        )
    )

    // Sun Core
    drawCircle(
        color = Color(0xFFFFF59D),
        radius = h * 0.18f,
        center = Offset(cx, cy)
    )

    // Sun Rays
    for (i in 0 until 12) {
        val angle = i * 30f * (Math.PI / 180.0)
        val rx1 = cx + (h * 0.22f) * cos(angle).toFloat()
        val ry1 = cy + (h * 0.22f) * sin(angle).toFloat()
        val rx2 = cx + (h * 0.42f) * cos(angle).toFloat()
        val ry2 = cy + (h * 0.42f) * sin(angle).toFloat()
        drawLine(
            color = accent.copy(alpha = 0.5f),
            start = Offset(rx1, ry1),
            end = Offset(rx2, ry2),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )
    }

    // River Water Waves
    val riverY = h * 0.72f
    val riverPath = Path().apply {
        moveTo(0f, riverY)
        cubicTo(w * 0.25f, riverY - 12f, w * 0.5f, riverY + 12f, w, riverY - 6f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        path = riverPath,
        brush = Brush.verticalGradient(
            colors = listOf(secondary.copy(alpha = 0.45f), primary.copy(alpha = 0.35f))
        )
    )

    // Golden Water Reflection Lines
    for (i in 0..3) {
        val lineY = riverY + 14f + (i * 12f)
        drawLine(
            color = accent.copy(alpha = 0.6f - i * 0.12f),
            start = Offset(cx - (60f - i * 8f), lineY),
            end = Offset(cx + (60f - i * 8f), lineY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round
        )
    }

    // Floating Diya / Arghya Basket Motif
    val diyaX = cx - 30f
    val diyaY = riverY - 10f

    // Diya Base Bowl
    val diyaPath = Path().apply {
        moveTo(diyaX - 22f, diyaY)
        quadraticBezierTo(diyaX, diyaY + 18f, diyaX + 22f, diyaY)
        close()
    }
    drawPath(path = diyaPath, color = primary)

    // Diya Flame
    val flamePath = Path().apply {
        moveTo(diyaX, diyaY - 2f)
        cubicTo(diyaX + 12f, diyaY - 14f, diyaX, diyaY - 28f, diyaX, diyaY - 28f)
        cubicTo(diyaX, diyaY - 28f, diyaX - 12f, diyaY - 14f, diyaX, diyaY - 2f)
        close()
    }
    drawPath(path = flamePath, color = accent)
}

// 2. Diwali Artwork — Ornate Diya, Flame Glow, Rangoli Rings
private fun DrawScope.drawDiwaliArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.78f
    val cy = h * 0.55f

    // Outer Rangoli Ring
    drawCircle(
        color = accent.copy(alpha = 0.15f),
        radius = h * 0.42f,
        center = Offset(cx, cy),
        style = Stroke(width = 2f)
    )
    drawCircle(
        color = primary.copy(alpha = 0.25f),
        radius = h * 0.32f,
        center = Offset(cx, cy),
        style = Stroke(width = 3f)
    )

    // Rangoli Petals
    for (i in 0 until 8) {
        val angle = i * 45f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawCircle(
                color = accent.copy(alpha = 0.35f),
                radius = 10f,
                center = Offset(cx, cy - h * 0.32f)
            )
        }
    }

    // Flame Radial Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.8f), primary.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(cx, cy - 20f),
            radius = h * 0.35f
        )
    )

    // Main Diya Clay Bowl
    val diyaPath = Path().apply {
        moveTo(cx - 36f, cy + 10f)
        cubicTo(cx - 20f, cy + 38f, cx + 20f, cy + 38f, cx + 36f, cy + 10f)
        quadraticBezierTo(cx, cy + 16f, cx - 36f, cy + 10f)
        close()
    }
    drawPath(path = diyaPath, color = primary)

    // Diya Rim Accent
    drawPath(
        path = diyaPath,
        color = accent,
        style = Stroke(width = 2.5f)
    )

    // Radiant Flame
    val flamePath = Path().apply {
        moveTo(cx, cy + 8f)
        cubicTo(cx + 18f, cy - 15f, cx + 2f, cy - 45f, cx, cy - 45f)
        cubicTo(cx - 2f, cy - 45f, cx - 18f, cy - 15f, cx, cy + 8f)
        close()
    }
    drawPath(path = flamePath, color = accent)

    // Inner Flame Core
    val innerFlame = Path().apply {
        moveTo(cx, cy + 4f)
        cubicTo(cx + 8f, cy - 10f, cx + 1f, cy - 28f, cx, cy - 28f)
        cubicTo(cx - 1f, cy - 28f, cx - 8f, cy - 10f, cx, cy + 4f)
        close()
    }
    drawPath(path = innerFlame, color = Color.White)
}

// 3. Holi Artwork — Color Powder Bursts & Splashes
private fun DrawScope.drawHoliArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.72f
    val cy = h * 0.5f

    val colors = listOf(primary, secondary, accent, Color(0xFF9C27B0), Color(0xFFFFEB3B))

    // Multiple Overlapping Color Bursts
    colors.forEachIndexed { idx, color ->
        val offsetX = cx + (idx % 3 - 1) * 35f
        val offsetY = cy + (idx / 2 - 1) * 25f
        val radius = 30f + (idx * 8f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.55f), color.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(offsetX, offsetY),
                radius = radius * 1.8f
            )
        )
    }

    // Powder Splatter Dots
    for (i in 0 until 18) {
        val angle = i * 20f * (Math.PI / 180.0)
        val dist = 30f + (i * 7) % 55
        val dotX = cx + dist * cos(angle).toFloat()
        val dotY = cy + dist * sin(angle).toFloat()
        val dotRadius = 3f + (i % 4) * 2.5f

        drawCircle(
            color = colors[i % colors.size].copy(alpha = 0.7f),
            radius = dotRadius,
            center = Offset(dotX, dotY)
        )
    }
}

// 4. Teachers' Day Artwork — Graduation Cap, Stacked Books, Pencil
private fun DrawScope.drawTeachersDayArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.52f

    // Stacked Books
    // Book 1 (Bottom)
    drawRoundRect(
        color = secondary.copy(alpha = 0.85f),
        topLeft = Offset(cx - 45f, cy + 15f),
        size = Size(90f, 16f),
        cornerRadius = CornerRadius(4f)
    )
    // Book 2 (Middle)
    drawRoundRect(
        color = primary.copy(alpha = 0.85f),
        topLeft = Offset(cx - 40f, cy - 3f),
        size = Size(80f, 16f),
        cornerRadius = CornerRadius(4f)
    )

    // Graduation Cap Top Diamond
    val capPath = Path().apply {
        moveTo(cx, cy - 40f)
        lineTo(cx + 42f, cy - 25f)
        lineTo(cx, cy - 10f)
        lineTo(cx - 42f, cy - 25f)
        close()
    }
    drawPath(path = capPath, color = primary)

    // Cap Skull Cap
    val capBase = Path().apply {
        moveTo(cx - 20f, cy - 22f)
        lineTo(cx - 20f, cy - 12f)
        quadraticBezierTo(cx, cy - 6f, cx + 20f, cy - 12f)
        lineTo(cx + 20f, cy - 22f)
        close()
    }
    drawPath(path = capBase, color = primary)

    // Cap Tassel
    drawLine(
        color = accent,
        start = Offset(cx, cy - 25f),
        end = Offset(cx + 36f, cy - 15f),
        strokeWidth = 2.5f
    )
    drawCircle(
        color = accent,
        radius = 4.5f,
        center = Offset(cx + 36f, cy - 12f)
    )
}

// 5. Makar Sankranti Artwork — Colorful Flying Kite
private fun DrawScope.drawMakarSankrantiArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.75f
    val cy = h * 0.42f
    val kiteSize = 38f

    rotate(degrees = 18f, pivot = Offset(cx, cy)) {
        // Kite Body
        val kitePath = Path().apply {
            moveTo(cx, cy - kiteSize)
            lineTo(cx + kiteSize, cy)
            lineTo(cx, cy + kiteSize)
            lineTo(cx - kiteSize, cy)
            close()
        }
        drawPath(path = kitePath, color = primary)

        // Top Triangle Contrast Color
        val topTriangle = Path().apply {
            moveTo(cx, cy - kiteSize)
            lineTo(cx + kiteSize, cy)
            lineTo(cx - kiteSize, cy)
            close()
        }
        drawPath(path = topTriangle, color = secondary)

        // Spine & Crossbow Ribs
        drawLine(color = accent, start = Offset(cx, cy - kiteSize), end = Offset(cx, cy + kiteSize), strokeWidth = 2.5f)
        drawLine(color = accent, start = Offset(cx - kiteSize, cy), end = Offset(cx + kiteSize, cy), strokeWidth = 2.5f)

        // Kite Tail Bows
        val tailPath = Path().apply {
            moveTo(cx, cy + kiteSize)
            quadraticBezierTo(cx + 12f, cy + kiteSize + 20f, cx - 8f, cy + kiteSize + 40f)
        }
        drawPath(path = tailPath, color = accent, style = Stroke(width = 2f))
    }
}

// 6. Patriotic Ribbon / Flag Artwork — Flowing Tricolor Ribbons & Ashoka Chakra
private fun DrawScope.drawPatrioticArtwork(
    w: Float, h: Float,
    saffron: Color, green: Color, navy: Color
) {
    val cx = w * 0.78f
    val cy = h * 0.5f

    // Ashoka Chakra Wheel Circle
    val radius = h * 0.28f
    drawCircle(color = navy.copy(alpha = 0.8f), radius = radius, center = Offset(cx, cy), style = Stroke(width = 2.5f))

    // 24 Spokes
    for (i in 0 until 24) {
        val angle = i * 15f * (Math.PI / 180.0)
        val x2 = cx + radius * cos(angle).toFloat()
        val y2 = cy + radius * sin(angle).toFloat()
        drawLine(color = navy.copy(alpha = 0.7f), start = Offset(cx, cy), end = Offset(x2, y2), strokeWidth = 1.5f)
    }

    // Saffron Wave Banner (Top Right)
    val saffronWave = Path().apply {
        moveTo(cx - 30f, cy - radius - 15f)
        quadraticBezierTo(cx + 20f, cy - radius - 30f, w, cy - radius - 15f)
        lineTo(w, cy - radius + 10f)
        quadraticBezierTo(cx + 20f, cy - radius - 5f, cx - 30f, cy - radius + 10f)
        close()
    }
    drawPath(path = saffronWave, color = saffron.copy(alpha = 0.75f))

    // Green Wave Banner (Bottom Right)
    val greenWave = Path().apply {
        moveTo(cx - 30f, cy + radius - 10f)
        quadraticBezierTo(cx + 20f, cy + radius - 5f, w, cy + radius - 10f)
        lineTo(w, cy + radius + 15f)
        quadraticBezierTo(cx + 20f, cy + radius + 20f, cx - 30f, cy + radius + 15f)
        close()
    }
    drawPath(path = greenWave, color = green.copy(alpha = 0.75f))
}

// 7. Eid Artwork — Crescent Moon & Hanging Lantern
private fun DrawScope.drawEidArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.42f
    val moonRadius = h * 0.3f

    // Crescent Moon
    val moonPath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - moonRadius, cy - moonRadius, cx + moonRadius, cy + moonRadius))
    }
    drawPath(path = moonPath, color = accent)

    val moonCutout = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(cx - moonRadius * 0.6f, cy - moonRadius * 1.1f, cx + moonRadius * 1.2f, cy + moonRadius * 0.9f))
    }
    drawPath(path = moonCutout, color = Color.Transparent, blendMode = androidx.compose.ui.graphics.BlendMode.Clear)

    // Star beside Crescent
    val starX = cx - moonRadius * 0.5f
    val starY = cy - moonRadius * 0.2f
    drawCircle(color = accent, radius = 6f, center = Offset(starX, starY))
}

// 8. Christmas Artwork — Pine Tree & Star
private fun DrawScope.drawChristmasArtwork(
    w: Float, h: Float,
    primary: Color, green: Color, gold: Color
) {
    val cx = w * 0.78f
    val cy = h * 0.55f

    // Tree Tiers (3 Triangles)
    val t1 = Path().apply {
        moveTo(cx, cy - 40f)
        lineTo(cx + 20f, cy - 15f)
        lineTo(cx - 20f, cy - 15f)
        close()
    }
    val t2 = Path().apply {
        moveTo(cx, cy - 20f)
        lineTo(cx + 30f, cy + 10f)
        lineTo(cx - 30f, cy + 10f)
        close()
    }
    val t3 = Path().apply {
        moveTo(cx, cy + 5f)
        lineTo(cx + 40f, cy + 35f)
        lineTo(cx - 40f, cy + 35f)
        close()
    }
    drawPath(path = t1, color = green)
    drawPath(path = t2, color = green)
    drawPath(path = t3, color = green)

    // Tree Trunk
    drawRect(color = Color(0xFF795548), topLeft = Offset(cx - 6f, cy + 35f), size = Size(12f, 15f))

    // Top Star
    drawCircle(color = gold, radius = 8f, center = Offset(cx, cy - 42f))
}

// 9. Onam Artwork — Pookalam Floral Rings
private fun DrawScope.drawOnamArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    drawCircle(color = secondary.copy(alpha = 0.3f), radius = h * 0.42f, center = Offset(cx, cy))
    drawCircle(color = primary.copy(alpha = 0.4f), radius = h * 0.32f, center = Offset(cx, cy))
    drawCircle(color = accent.copy(alpha = 0.7f), radius = h * 0.2f, center = Offset(cx, cy))

    for (i in 0 until 12) {
        val angle = i * 30f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            drawCircle(color = accent, radius = 6f, center = Offset(cx, cy - h * 0.32f))
        }
    }
}

// 10. Ganesh Chaturthi Artwork — Ganesha Mukut Contour & Aura
private fun DrawScope.drawGaneshArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.5f), accent.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(cx, cy),
            radius = h * 0.45f
        )
    )

    // Crown / Mukut Contour
    val crownPath = Path().apply {
        moveTo(cx, cy - 42f)
        lineTo(cx + 22f, cy - 10f)
        lineTo(cx - 22f, cy - 10f)
        close()
    }
    drawPath(path = crownPath, color = accent)

    // Trunk Curve Line
    val trunkPath = Path().apply {
        moveTo(cx, cy - 5f)
        cubicTo(cx - 15f, cy + 15f, cx - 12f, cy + 32f, cx + 5f, cy + 30f)
    }
    drawPath(path = trunkPath, color = primary, style = Stroke(width = 4f, cap = StrokeCap.Round))
}

// 11. Janmashtami Artwork — Peacock Feather & Flute
private fun DrawScope.drawJanmashtamiArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    // Peacock Eye
    drawCircle(color = primary, radius = h * 0.28f, center = Offset(cx, cy - 10f))
    drawCircle(color = secondary, radius = h * 0.18f, center = Offset(cx, cy - 10f))
    drawCircle(color = accent, radius = h * 0.09f, center = Offset(cx, cy - 10f))

    // Flute Line
    drawLine(color = accent, start = Offset(cx - 45f, cy + 25f), end = Offset(cx + 45f, cy + 15f), strokeWidth = 5f, cap = StrokeCap.Round)
}

// 12. Raksha Bandhan Artwork — Rakhi Jewel & Threads
private fun DrawScope.drawRakshaBandhanArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    // Silk Threads extending left/right
    drawLine(color = primary, start = Offset(cx - 65f, cy), end = Offset(cx + 65f, cy), strokeWidth = 3f)

    // Circular Rakhi Motif
    drawCircle(color = primary, radius = h * 0.32f, center = Offset(cx, cy))
    drawCircle(color = accent, radius = h * 0.22f, center = Offset(cx, cy), style = Stroke(width = 3f))
    drawCircle(color = Color.White, radius = h * 0.12f, center = Offset(cx, cy))
}

// 13. Maha Shivratri Artwork — Mystic Trishul Contour & Moon
private fun DrawScope.drawMahaShivratriArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.4f), primary.copy(alpha = 0.15f), Color.Transparent),
            center = Offset(cx, cy),
            radius = h * 0.45f
        )
    )

    // Center Trishul Line
    drawLine(color = accent, start = Offset(cx, cy - 35f), end = Offset(cx, cy + 35f), strokeWidth = 3.5f)
    // Left Prongs
    val leftProng = Path().apply {
        moveTo(cx, cy + 10f)
        cubicTo(cx - 20f, cy + 5f, cx - 20f, cy - 25f, cx - 18f, cy - 30f)
    }
    drawPath(path = leftProng, color = accent, style = Stroke(width = 3f))
    // Right Prongs
    val rightProng = Path().apply {
        moveTo(cx, cy + 10f)
        cubicTo(cx + 20f, cy + 5f, cx + 20f, cy - 25f, cx + 18f, cy - 30f)
    }
    drawPath(path = rightProng, color = accent, style = Stroke(width = 3f))
}

// 14. Bow and Arrow Artwork — Dussehra / Ram Navami Victory Bow
private fun DrawScope.drawBowAndArrowArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    // Bow Curve
    val bowPath = Path().apply {
        moveTo(cx - 30f, cy - 35f)
        quadraticBezierTo(cx + 20f, cy, cx - 30f, cy + 35f)
    }
    drawPath(path = bowPath, color = primary, style = Stroke(width = 4f))

    // Bow String
    drawLine(color = accent, start = Offset(cx - 30f, cy - 35f), end = Offset(cx - 30f, cy + 35f), strokeWidth = 1.5f)

    // Arrow Shaft
    drawLine(color = accent, start = Offset(cx - 40f, cy), end = Offset(cx + 35f, cy), strokeWidth = 3f)

    // Arrowhead
    val head = Path().apply {
        moveTo(cx + 35f, cy - 8f)
        lineTo(cx + 48f, cy)
        lineTo(cx + 35f, cy + 8f)
        close()
    }
    drawPath(path = head, color = accent)
}

// 15. Lotus Blossom Artwork — Mahavir Jayanti / Buddha Purnima
private fun DrawScope.drawLotusArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.52f

    // Lotus Petals
    for (i in -2..2) {
        val angle = i * 20f
        rotate(degrees = angle, pivot = Offset(cx, cy)) {
            val petal = Path().apply {
                moveTo(cx, cy)
                quadraticBezierTo(cx + 15f, cy - 25f, cx, cy - 42f)
                quadraticBezierTo(cx - 15f, cy - 25f, cx, cy)
                close()
            }
            drawPath(path = petal, color = primary.copy(alpha = 0.7f))
        }
    }
}

// 16. Gandhi Jayanti Spectacles Artwork
private fun DrawScope.drawGandhiArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    // Round Spectacle Lenses
    drawCircle(color = accent, radius = 18f, center = Offset(cx - 22f, cy), style = Stroke(width = 3f))
    drawCircle(color = accent, radius = 18f, center = Offset(cx + 22f, cy), style = Stroke(width = 3f))

    // Bridge Line
    drawLine(color = accent, start = Offset(cx - 4f, cy - 4f), end = Offset(cx + 4f, cy - 4f), strokeWidth = 3f)
}

// 17. Children's Day Paper Plane Artwork
private fun DrawScope.drawChildrensDayArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    rotate(degrees = -15f, pivot = Offset(cx, cy)) {
        val plane = Path().apply {
            moveTo(cx + 35f, cy)
            lineTo(cx - 35f, cy - 22f)
            lineTo(cx - 15f, cy)
            lineTo(cx - 35f, cy + 22f)
            close()
        }
        drawPath(path = plane, color = primary)
    }
}

// 18. Guru Nanak Jayanti Artwork
private fun DrawScope.drawGuruNanakArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = 0.8f), primary.copy(alpha = 0.3f), Color.Transparent),
            center = Offset(cx, cy),
            radius = h * 0.45f
        )
    )

    drawCircle(color = accent, radius = 20f, center = Offset(cx, cy), style = Stroke(width = 3f))
}

// Fallback Festive Artwork
private fun DrawScope.drawGenericFestiveArtwork(
    w: Float, h: Float,
    primary: Color, secondary: Color, accent: Color
) {
    val cx = w * 0.76f
    val cy = h * 0.5f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = 0.4f), accent.copy(alpha = 0.2f), Color.Transparent),
            center = Offset(cx, cy),
            radius = h * 0.45f
        )
    )
}
