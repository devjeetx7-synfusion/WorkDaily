package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.theme.MintPrimary
import kotlinx.coroutines.launch

/**
 * Custom WorkDaily Swipe-To-Refresh Box container.
 * Replaces generic Material 3 PullToRefreshContainer with a reliable, custom state-driven refresh system.
 * Handles complete lifecycle: IDLE -> PULLING -> READY -> REFRESHING -> COMPLETING -> IDLE.
 */
@Composable
fun WorkDailyPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    refreshThreshold: Dp = 80.dp,
    refreshingOffset: Dp = 56.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val thresholdPx = with(density) { refreshThreshold.toPx() }
    val refreshingOffsetPx = with(density) { refreshingOffset.toPx() }
    val maxPullPx = thresholdPx * 1.5f

    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    val animatedIndicatorOffset = remember { Animatable(0f) }

    // Synchronize animatedIndicatorOffset with pullOffsetPx when dragging
    LaunchedEffect(pullOffsetPx) {
        if (!isRefreshing) {
            animatedIndicatorOffset.snapTo(pullOffsetPx)
        }
    }

    // Handle isRefreshing state changes smoothly
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            animatedIndicatorOffset.animateTo(
                targetValue = refreshingOffsetPx,
                animationSpec = spring(stiffness = 300f, dampingRatio = 0.75f)
            )
        } else {
            // Smoothly animate out and reset offset completely
            animatedIndicatorOffset.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
            pullOffsetPx = 0f
        }
    }

    val nestedScrollConnection = remember(enabled, isRefreshing) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || isRefreshing) return Offset.Zero

                // If user scrolls up while pullOffsetPx > 0, consume scroll to decrease offset
                if (pullOffsetPx > 0f && available.y < 0f) {
                    val consumedY = available.y
                    pullOffsetPx = (pullOffsetPx + consumedY).coerceAtLeast(0f)
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || isRefreshing) return Offset.Zero

                // If user pulls down and scrollable content cannot scroll further up (available.y > 0)
                if (available.y > 0f) {
                    val newOffset = pullOffsetPx + available.y * 0.45f // Damping factor
                    pullOffsetPx = newOffset.coerceIn(0f, maxPullPx)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabled || isRefreshing) return Velocity.Zero

                if (pullOffsetPx >= thresholdPx) {
                    onRefresh()
                } else if (pullOffsetPx > 0f) {
                    scope.launch {
                        animatedIndicatorOffset.animateTo(0f, tween(200))
                        pullOffsetPx = 0f
                    }
                }
                return Velocity.Zero
            }
        }
    }

    val pullProgress by remember {
        derivedStateOf {
            (pullOffsetPx / thresholdPx).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        content()

        // Render WorkDaily Custom Refresh Indicator at top center
        val currentOffsetPx = animatedIndicatorOffset.value
        val isVisible = currentOffsetPx > 0.5f || isRefreshing

        if (isVisible) {
            val alpha by animateFloatAsState(
                targetValue = if (currentOffsetPx > 2f) 1f else 0f,
                animationSpec = tween(150),
                label = "indicator_alpha"
            )

            val indicatorScale by animateFloatAsState(
                targetValue = if (isRefreshing) 1f else (0.5f + pullProgress * 0.5f),
                animationSpec = spring(stiffness = 400f),
                label = "indicator_scale"
            )

            WorkDailyRefreshIndicator(
                isRefreshing = isRefreshing,
                pullProgress = pullProgress,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = currentOffsetPx
                        this.alpha = alpha
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                    }
            )
        }
    }
}

@Composable
fun WorkDailyRefreshIndicator(
    isRefreshing: Boolean,
    pullProgress: Float,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "workdaily_refresh_transition")

    val continuousRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_rotation"
    )

    val pulseScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val currentRotation = if (isRefreshing) continuousRotation else (pullProgress * 360f)
    val currentPulse = if (isRefreshing) pulseScale else 1f

    Surface(
        modifier = modifier
            .padding(top = 8.dp)
            .scale(currentPulse),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = MintPrimary,
                    strokeWidth = 3.dp,
                    trackColor = MintPrimary.copy(alpha = 0.15f)
                )
            } else {
                CircularProgressIndicator(
                    progress = { pullProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MintPrimary,
                    strokeWidth = 3.dp,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                )
            }

            Image(
                painter = painterResource(id = R.drawable.logo_transparent),
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(currentRotation)
            )
        }
    }
}
