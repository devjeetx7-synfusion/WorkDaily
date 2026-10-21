package com.synfusion.attendance.live.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveGiftOverlayEvent
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Data model for transient gift particles.
 */
private data class SparkleParticle(
    val initialX: Float,
    val initialY: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

/**
 * Live Gift Animation Host:
 * Coordinates the bounded gift queue, aggregates rapid combo streaks,
 * and renders transparent non-blocking animated gift overlays over the video stage.
 */
@Composable
fun LiveGiftAnimationHost(
    events: List<LiveGiftOverlayEvent>,
    modifier: Modifier = Modifier,
    currentUserId: String = "current_user",
    onEventFinished: (String) -> Unit = {}
) {
    // Current active display event and queued events
    var currentDisplayEvent by remember { mutableStateOf<LiveGiftOverlayEvent?>(null) }
    var comboCount by remember { mutableIntStateOf(1) }
    val pendingQueue = remember { mutableStateListOf<LiveGiftOverlayEvent>() }

    // Sync incoming events into local queue/combo engine
    LaunchedEffect(events) {
        if (events.isNotEmpty()) {
            val latest = events.last()
            val active = currentDisplayEvent

            if (active != null &&
                active.giftName == latest.giftName &&
                active.senderName == latest.senderName
            ) {
                // Combo Streak! Increase combo multiplier on active overlay
                comboCount += latest.quantity
            } else {
                // Add to queue if not already present
                if (pendingQueue.none { it.id == latest.id } && active?.id != latest.id) {
                    if (pendingQueue.size < 10) { // Bounded queue limit
                        pendingQueue.add(latest)
                    }
                }
            }
        }
    }

    // Queue processor loop
    LaunchedEffect(currentDisplayEvent, pendingQueue.size) {
        if (currentDisplayEvent == null && pendingQueue.isNotEmpty()) {
            val nextEvent = pendingQueue.removeAt(0)
            comboCount = nextEvent.comboCount.coerceAtLeast(1)
            currentDisplayEvent = nextEvent
        }
    }

    // Animation lifecycle effect for current active gift overlay
    val activeEvent = currentDisplayEvent
    if (activeEvent != null) {
        key(activeEvent.id) {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                LiveGiftOverlay(
                    event = activeEvent,
                    comboCount = comboCount,
                    currentUserId = currentUserId,
                    onAnimationComplete = {
                        onEventFinished(activeEvent.id)
                        currentDisplayEvent = null
                    }
                )
            }
        }
    }
}

/**
 * Transparent Animated Gift Overlay Composable:
 * 3-stage lifecycle (Entrance -> Celebration Glow & Particles -> Float & Fade Exit).
 */
@Composable
fun LiveGiftOverlay(
    event: LiveGiftOverlayEvent,
    comboCount: Int,
    currentUserId: String = "current_user",
    onAnimationComplete: () -> Unit
) {
    var animationStage by remember { mutableIntStateOf(0) } // 0: Entrance, 1: Celebration, 2: Exit

    // Smooth stage transitions
    val transitionState = remember { MutableTransitionState(0) }
    transitionState.targetState = animationStage

    val animProgress by animateFloatAsState(
        targetValue = when (animationStage) {
            0 -> 0f
            1 -> 0.5f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = when (animationStage) {
                0 -> 350
                1 -> 1400
                else -> 800
            },
            easing = LinearOutSlowInEasing
        ),
        label = "animProgress"
    )

    // Stage Driver
    LaunchedEffect(event.id) {
        animationStage = 0
        delay(350)
        animationStage = 1
        delay(1400)
        animationStage = 2
        delay(800)
        onAnimationComplete()
    }

    // Scale and opacity animatables for fluid bounce and float exit
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(40f) }

    LaunchedEffect(animationStage) {
        when (animationStage) {
            0 -> {
                alpha.animateTo(1f, tween(250))
                offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scale.animateTo(1.15f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                scale.animateTo(1.0f, tween(100))
            }
            1 -> {
                scale.animateTo(1.05f, infiniteRepeatable(tween(700), RepeatMode.Reverse))
            }
            2 -> {
                offsetY.animateTo(-60f, tween(700, easing = FastOutLinearInEasing))
                alpha.animateTo(0f, tween(700))
            }
        }
    }

    // Is the current viewer the sender?
    val isSender = event.senderName.equals("You", ignoreCase = true) ||
            event.senderName.equals(currentUserId, ignoreCase = true)

    val senderText = if (isSender) "You sent" else "${event.senderName} sent"
    val giftIcon = getGiftIcon(event.giftName)

    // Dynamic Sparkle Particle generator
    val particles = remember {
        List(18) {
            SparkleParticle(
                initialX = Random.nextFloat() * 180f - 90f,
                initialY = Random.nextFloat() * 180f - 90f,
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 2f + 1f,
                size = Random.nextFloat() * 8f + 4f,
                color = if (it % 2 == 0) LiveGold else LivePrimaryMint
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                translationY = offsetY.value
            },
        contentAlignment = Alignment.Center
    ) {
        // Particle Canvas Background
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .alpha(if (animationStage == 1) 0.85f else 0.4f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            particles.forEach { p ->
                val rad = Math.toRadians((p.angle + animProgress * 180f).toDouble())
                val x = center.x + p.initialX + (cos(rad) * animProgress * 40f).toFloat()
                val y = center.y + p.initialY + (sin(rad) * animProgress * 40f).toFloat()
                drawCircle(
                    color = p.color.copy(alpha = (1f - animProgress).coerceIn(0f, 1f)),
                    radius = p.size,
                    center = Offset(x, y)
                )
            }
        }

        // Transparent Frosted Gift Pill Container (Non-blocking, subtle gradient outline)
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color.Black.copy(alpha = 0.55f),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        LiveGold.copy(alpha = 0.8f),
                        LivePrimaryMint.copy(alpha = 0.8f),
                        LiveGold.copy(alpha = 0.8f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gift Icon Box with Soft Glow
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    LiveGold.copy(alpha = 0.4f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, LiveGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = giftIcon,
                        contentDescription = event.giftName,
                        tint = LiveGold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Gift Details Text Column
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = senderText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.LightGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = event.giftName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = Shadow(color = Color.Black, blurRadius = 4f)
                            )
                        )

                        if (event.diamondCost > 0) {
                            Icon(
                                imageVector = Icons.Default.Diamond,
                                contentDescription = null,
                                tint = LiveDiamondColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${event.diamondCost * comboCount}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = LiveDiamondColor
                            )
                        }
                    }
                }

                // Combo Multiplier Animated Chip (e.g. x5, x10)
                if (comboCount > 1) {
                    Surface(
                        shape = CircleShape,
                        color = LiveGold,
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                    ) {
                        Text(
                            text = "x$comboCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
