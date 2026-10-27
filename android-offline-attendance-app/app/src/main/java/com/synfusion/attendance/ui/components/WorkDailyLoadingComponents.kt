package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Explicit UI state model distinguishing initial loading, background refresh with valid content,
 * empty content, and recoverable error states.
 */
sealed class WorkDailyLoadState<out T> {
    object InitialLoading : WorkDailyLoadState<Nothing>()
    data class Content<out T>(
        val data: T,
        val isRefreshing: Boolean = false
    ) : WorkDailyLoadState<T>()
    object Empty : WorkDailyLoadState<Nothing>()
    data class Error<out T>(
        val message: String,
        val cachedData: T? = null
    ) : WorkDailyLoadState<T>()
}

/**
 * Lightweight, low-cost 3-dot animated brand indicator.
 * Uses a single float transition and pure graphicsLayer scale/alpha (zero layout passes per frame).
 */
@Composable
fun WorkDailyPulseDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    dotSize: Dp = 5.dp,
    spacing: Dp = 4.dp
) {
    val transition = rememberInfiniteTransition(label = "wd_pulse_dots")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wd_pulse_phase"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val offsetPhase = phase - index * 0.85f
            val wave = ((sin(offsetPhase.toDouble()) + 1.0) * 0.5).toFloat()
            val scale = 0.72f + (0.28f * wave)
            val alpha = 0.35f + (0.65f * wave)

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/**
 * Ultra-subtle 2dp animated sweep bar for background refreshes when valid cached content is already visible.
 */
@Composable
fun WorkDailySubtleRefreshBar(
    visible: Boolean,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (!visible) return

    val transition = rememberInfiniteTransition(label = "wd_refresh_bar")
    val progress by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wd_refresh_progress"
    )
    val trackColor = color.copy(alpha = 0.10f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawRect(color = trackColor)
        val segmentWidth = size.width * 0.35f
        val startX = size.width * progress - segmentWidth * 0.5f
        val endX = startX + segmentWidth
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, color, Color.Transparent),
                startX = startX,
                endX = endX
            ),
            topLeft = Offset(startX.coerceAtLeast(0f), 0f),
            size = size.copy(
                width = (endX.coerceAtMost(size.width) - startX.coerceAtLeast(0f)).coerceAtLeast(0f)
            )
        )
    }
}

/**
 * Compact skeleton placeholder for a single profile metric (Followers or Following) when not yet cached.
 */
@Composable
fun ProfileCountValueSkeleton(
    modifier: Modifier = Modifier
) {
    ShimmerBox(
        modifier = modifier,
        height = 20.dp,
        width = 40.dp,
        cornerRadius = 6.dp
    )
}

/**
 * Compact skeleton placeholder for initial loading of another user's profile screen.
 */
@Composable
fun PublicProfileScreenSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerCircle(size = 88.dp)
            Spacer(modifier = Modifier.height(2.dp))
            ShimmerBox(height = 20.dp, width = 148.dp, cornerRadius = 6.dp)
            ShimmerBox(height = 14.dp, width = 96.dp, cornerRadius = 5.dp)
            ShimmerBox(height = 22.dp, width = 88.dp, cornerRadius = 8.dp)
            ShimmerBox(height = 14.dp, width = 210.dp, cornerRadius = 5.dp)
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(height = 20.dp, width = 36.dp, cornerRadius = 6.dp)
                    ShimmerBox(height = 12.dp, width = 56.dp, cornerRadius = 4.dp)
                }
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(height = 20.dp, width = 36.dp, cornerRadius = 6.dp)
                    ShimmerBox(height = 12.dp, width = 56.dp, cornerRadius = 4.dp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerBox(modifier = Modifier.weight(1f), height = 46.dp, cornerRadius = 23.dp)
            ShimmerBox(modifier = Modifier.weight(1f), height = 46.dp, cornerRadius = 23.dp)
        }
    }
}

/**
 * Lightweight user-row placeholder with circular avatar, display name, @username, and Follow pill shape.
 * Used across Discover search, Followers, and Following lists during initial loading.
 */
@Composable
fun UserRowSkeleton(
    showActionButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerCircle(size = 44.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ShimmerTextLine(height = 14.dp, fraction = 0.52f)
            ShimmerTextLine(height = 11.dp, fraction = 0.34f)
        }
        if (showActionButton) {
            Spacer(modifier = Modifier.width(10.dp))
            ShimmerBox(height = 34.dp, width = 86.dp, cornerRadius = 17.dp)
        }
    }
}

@Composable
fun UserRowSkeletonList(
    rowCount: Int = 5,
    showActionButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(rowCount) {
            UserRowSkeleton(showActionButton = showActionButton)
        }
    }
}

/**
 * Minimal, lightweight conversation placeholders shown only when initial chat state is genuinely loading
 * and no cached conversation messages are available yet.
 */
@Composable
fun ChatMessagesSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ShimmerBox(height = 20.dp, width = 64.dp, cornerRadius = 10.dp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            ShimmerBox(height = 36.dp, width = 168.dp, cornerRadius = 16.dp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            ShimmerBox(height = 34.dp, width = 116.dp, cornerRadius = 16.dp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ShimmerBox(height = 36.dp, width = 184.dp, cornerRadius = 16.dp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            ShimmerBox(height = 46.dp, width = 212.dp, cornerRadius = 16.dp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            ShimmerBox(height = 34.dp, width = 136.dp, cornerRadius = 16.dp)
        }
    }
}

/**
 * Reusable WorkDaily error view with explicit Retry action.
 */
@Composable
fun WorkDailyErrorRetryState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.CloudOff
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (message.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Retry",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
