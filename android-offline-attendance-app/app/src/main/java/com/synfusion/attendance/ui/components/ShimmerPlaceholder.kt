package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // Automatically adapt to Light / Dark theme color schemes
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)

    val shimmerColors = listOf(
        baseColor,
        highlightColor,
        baseColor
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )

    this.background(brush)
}

@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    height: Dp,
    width: Dp? = null,
    cornerRadius: Dp = 8.dp
) {
    val boxModifier = modifier
        .fillMaxWidth(if (width == null) 1f else 1f)
        .let { if (width != null) it.width(width) else it }
        .height(height)
        .clip(RoundedCornerShape(cornerRadius))
        .shimmer()

    Box(modifier = boxModifier)
}

@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .shimmer()
    )
}

@Composable
fun ShimmerTextLine(
    modifier: Modifier = Modifier,
    height: Dp = 16.dp,
    fraction: Float = 0.8f
) {
    Box(
        modifier = modifier
            .fillMaxWidth(fraction)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .shimmer()
    )
}
