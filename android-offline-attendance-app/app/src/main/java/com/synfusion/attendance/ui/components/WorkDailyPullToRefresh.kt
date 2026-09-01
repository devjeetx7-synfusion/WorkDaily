package com.synfusion.attendance.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.pow

/**
 * Custom WorkDaily Stretch-Only Swipe-To-Refresh Box container.
 * Provides a highly polished physical elastic stretch interaction without any visual spinners, progress bars, or icons.
 */
@Composable
fun WorkDailyPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    refreshThreshold: Dp = 90.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val thresholdPx = with(density) { refreshThreshold.toPx() }
    val maxStretchPx = thresholdPx * 1.8f

    var pullOffsetPx by remember { mutableFloatStateOf(0f) }
    val animatedContentOffset = remember { Animatable(0f) }
    var hasCrossedThreshold by remember { mutableStateOf(false) }

    // Synchronize animatedContentOffset with pullOffsetPx while dragging
    LaunchedEffect(pullOffsetPx) {
        if (!animatedContentOffset.isRunning) {
            animatedContentOffset.snapTo(pullOffsetPx)
        }
    }

    // Handle isRefreshing state cleanup when refresh completes
    LaunchedEffect(isRefreshing) {
        if (!isRefreshing && animatedContentOffset.value > 0f) {
            animatedContentOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioLowBouncy
                )
            )
            pullOffsetPx = 0f
            hasCrossedThreshold = false
        }
    }

    val updatedIsRefreshing by rememberUpdatedState(isRefreshing)
    val updatedOnRefresh by rememberUpdatedState(onRefresh)

    val nestedScrollConnection = remember(enabled, thresholdPx, maxStretchPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!enabled || updatedIsRefreshing) return Offset.Zero

                // If user scrolls up while pullOffsetPx > 0, consume scroll to decrease offset
                if (pullOffsetPx > 0f && available.y < 0f) {
                    val consumedY = available.y
                    pullOffsetPx = (pullOffsetPx + consumedY).coerceAtLeast(0f)
                    scope.launch {
                        animatedContentOffset.snapTo(pullOffsetPx)
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || updatedIsRefreshing) return Offset.Zero

                // If user pulls down and scrollable content cannot scroll further up (available.y > 0)
                if (available.y > 0f && source == NestedScrollSource.Drag) {
                    // Rubber-band resistance: resistance gets progressively stronger
                    val currentRatio = (pullOffsetPx / maxStretchPx).coerceIn(0f, 1f)
                    val resistance = (1f - currentRatio).pow(2.0f).coerceAtLeast(0.06f)

                    val newOffset = (pullOffsetPx + available.y * resistance).coerceIn(0f, maxStretchPx)
                    pullOffsetPx = newOffset

                    scope.launch {
                        animatedContentOffset.snapTo(pullOffsetPx)
                    }

                    if (pullOffsetPx >= thresholdPx && !hasCrossedThreshold) {
                        hasCrossedThreshold = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } else if (pullOffsetPx < thresholdPx && hasCrossedThreshold) {
                        hasCrossedThreshold = false
                    }

                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabled || updatedIsRefreshing) return Velocity.Zero

                val currentPull = pullOffsetPx
                val crossed = hasCrossedThreshold || currentPull >= thresholdPx

                if (currentPull > 0f) {
                    if (crossed) {
                        updatedOnRefresh()
                    }

                    // Rubber band spring release animation: fast contraction + tiny overshoot + smooth settle
                    scope.launch {
                        animatedContentOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                stiffness = 380f,
                                dampingRatio = 0.62f
                            )
                        )
                        pullOffsetPx = 0f
                        hasCrossedThreshold = false
                    }
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        val currentOffsetPx = animatedContentOffset.value

        // Elastic Translation: Vertically translate UI content based on elastic pull offset
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = currentOffsetPx
                }
        ) {
            content()
        }
    }
}
