package com.synfusion.attendance.ui.onboarding.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synfusion.attendance.ui.theme.MintPrimary

/** Visual-only worker identities used in onboarding illustrations. */
enum class WorkerRole {
    CONTRACTOR,
    PLUMBER,
    ELECTRICIAN,
    LABOUR,
    MASON
}

@Composable
fun FloatingWorker(
    role: WorkerRole = WorkerRole.LABOUR,
    size: Dp = 60.dp,
    xOffset: Dp = 0.dp,
    yOffset: Dp = 0.dp,
    animationDelay: Int = 0,
    floatRange: Dp = 6.dp,
    horizontalRange: Dp = 4.dp,
    depthRange: Float = 0.035f
) {
    val transition = rememberInfiniteTransition(label = "workerAvatarMotion")

    val animatedY by transition.animateFloat(
        initialValue = -floatRange.value,
        targetValue = floatRange.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 3600 + animationDelay,
                delayMillis = animationDelay,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workerAvatarY"
    )

    val animatedX by transition.animateFloat(
        initialValue = -horizontalRange.value,
        targetValue = horizontalRange.value,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4400 + animationDelay,
                delayMillis = animationDelay / 2,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workerAvatarX"
    )

    val depth by transition.animateFloat(
        initialValue = 1f - depthRange,
        targetValue = 1f + depthRange,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4800 + animationDelay,
                delayMillis = animationDelay / 3,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workerAvatarDepth"
    )

    Box(
        modifier = Modifier
            .offset(x = xOffset + animatedX.dp, y = yOffset + animatedY.dp)
            .graphicsLayer {
                scaleX = depth
                scaleY = depth
                rotationZ = (depth - 1f) * 4f
            }
            .size(size)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = 0.07f),
                spotColor = Color.Black.copy(alpha = 0.09f)
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(2.dp, MintPrimary.copy(alpha = 0.20f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MintPrimary.copy(alpha = 0.08f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            WorkerHalfPortrait(role = role, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun WorkerHalfPortrait(
    role: WorkerRole,
    modifier: Modifier = Modifier
) {
    val skin = when (role) {
        WorkerRole.CONTRACTOR -> Color(0xFFD79A72)
        WorkerRole.PLUMBER -> Color(0xFFB97855)
        WorkerRole.ELECTRICIAN -> Color(0xFFE0A47A)
        WorkerRole.LABOUR -> Color(0xFF9A6044)
        WorkerRole.MASON -> Color(0xFFC98762)
    }
    val shirt = when (role) {
        WorkerRole.CONTRACTOR -> MintPrimary
        WorkerRole.PLUMBER -> Color(0xFF137E68)
        WorkerRole.ELECTRICIAN -> Color(0xFF0B6E5B)
        WorkerRole.LABOUR -> Color(0xFF18A982)
        WorkerRole.MASON -> Color(0xFF0D8C6D)
    }
    val helmet = when (role) {
        WorkerRole.CONTRACTOR -> Color(0xFFF8FAFC)
        WorkerRole.PLUMBER -> Color(0xFFE2E8F0)
        WorkerRole.ELECTRICIAN -> Color(0xFFFFFFFF)
        WorkerRole.LABOUR -> Color(0xFFDCEFE9)
        WorkerRole.MASON -> Color(0xFFF1F5F9)
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w / 2f

        // Shoulders / half-profile body.
        drawRoundRect(
            color = shirt,
            topLeft = Offset(w * 0.18f, h * 0.67f),
            size = Size(w * 0.64f, h * 0.42f),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )

        // Neck.
        drawRoundRect(
            color = skin,
            topLeft = Offset(w * 0.43f, h * 0.55f),
            size = Size(w * 0.14f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )

        // Head.
        drawCircle(
            color = skin,
            radius = w * 0.205f,
            center = Offset(centerX, h * 0.43f)
        )

        // Hair / side profile framing.
        drawArc(
            color = Color(0xFF263238),
            startAngle = 190f,
            sweepAngle = 160f,
            useCenter = false,
            topLeft = Offset(w * 0.295f, h * 0.225f),
            size = Size(w * 0.41f, w * 0.41f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.07f)
        )

        // Eyes.
        drawCircle(Color(0xFF1E293B), w * 0.018f, Offset(w * 0.43f, h * 0.43f))
        drawCircle(Color(0xFF1E293B), w * 0.018f, Offset(w * 0.57f, h * 0.43f))

        // Simple smile.
        drawArc(
            color = Color(0xFF7C3F2D),
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.43f, h * 0.45f),
            size = Size(w * 0.14f, h * 0.10f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.018f)
        )

        // Work helmet / cap. Small shape variations make each profession recognizable.
        when (role) {
            WorkerRole.PLUMBER -> {
                drawArc(
                    color = helmet,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.30f, h * 0.19f),
                    size = Size(w * 0.40f, h * 0.25f)
                )
                drawRoundRect(
                    color = MintPrimary,
                    topLeft = Offset(w * 0.29f, h * 0.30f),
                    size = Size(w * 0.42f, h * 0.055f),
                    cornerRadius = CornerRadius(w * 0.02f)
                )
            }
            else -> {
                drawArc(
                    color = helmet,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.28f, h * 0.17f),
                    size = Size(w * 0.44f, h * 0.29f)
                )
                drawRoundRect(
                    color = MintPrimary,
                    topLeft = Offset(w * 0.27f, h * 0.31f),
                    size = Size(w * 0.46f, h * 0.052f),
                    cornerRadius = CornerRadius(w * 0.02f)
                )
            }
        }

        // Role marker on shirt: minimal, brand-safe and readable at small sizes.
        when (role) {
            WorkerRole.CONTRACTOR -> drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = Offset(w * 0.57f, h * 0.74f),
                size = Size(w * 0.11f, h * 0.08f),
                cornerRadius = CornerRadius(w * 0.018f)
            )
            WorkerRole.ELECTRICIAN -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.61f, h * 0.71f)
                    lineTo(w * 0.55f, h * 0.82f)
                    lineTo(w * 0.61f, h * 0.80f)
                    lineTo(w * 0.57f, h * 0.89f)
                    lineTo(w * 0.69f, h * 0.76f)
                    lineTo(w * 0.63f, h * 0.78f)
                    close()
                }
                drawPath(path, Color.White.copy(alpha = 0.95f))
            }
            WorkerRole.MASON -> {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.90f),
                    topLeft = Offset(w * 0.55f, h * 0.74f),
                    size = Size(w * 0.16f, h * 0.045f),
                    cornerRadius = CornerRadius(w * 0.015f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.75f),
                    topLeft = Offset(w * 0.59f, h * 0.81f),
                    size = Size(w * 0.12f, h * 0.04f),
                    cornerRadius = CornerRadius(w * 0.015f)
                )
            }
            WorkerRole.PLUMBER, WorkerRole.LABOUR -> Unit
        }
    }
}
