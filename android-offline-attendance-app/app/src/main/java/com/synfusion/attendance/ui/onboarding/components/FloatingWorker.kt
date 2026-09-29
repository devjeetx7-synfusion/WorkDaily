package com.synfusion.attendance.ui.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun FloatingWorker(
    size: Dp = 60.dp,
    xOffset: Dp = 0.dp,
    yOffset: Dp = 0.dp,
    animationDelay: Int = 0,
    floatRange: Dp = 9.dp,
    horizontalRange: Dp = 6.dp,
    depthRange: Float = 0.05f
) {
    val transition = rememberInfiniteTransition(label = "floatingWorkerTransition")

    val animatedY by transition.animateFloat(
        initialValue = -floatRange.value,
        targetValue = floatRange.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2600 + animationDelay,
                delayMillis = animationDelay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingWorkerY"
    )

    val animatedX by transition.animateFloat(
        initialValue = -horizontalRange.value,
        targetValue = horizontalRange.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3200 + animationDelay,
                delayMillis = animationDelay / 2,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingWorkerX"
    )

    val depth by transition.animateFloat(
        initialValue = 1f - depthRange,
        targetValue = 1f + depthRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600 + animationDelay,
                delayMillis = animationDelay / 3,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingWorkerDepth"
    )

    Box(
        modifier = Modifier
            .offset(x = xOffset + animatedX.dp, y = yOffset + animatedY.dp)
            .graphicsLayer {
                scaleX = depth
                scaleY = depth
                rotationZ = (depth - 1f) * 12f
            }
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.12f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.58f)
        )
    }
}
