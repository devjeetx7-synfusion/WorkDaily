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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveGiftOverlayEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Transient Particle representation for Canvas construction sparkles.
 */
private data class ConstructionParticle(
    val initialX: Float,
    val initialY: Float,
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)

/**
 * Live Gift Animation Host V2:
 * Coordinates the bounded gift queue, aggregates rapid same-sender combo events,
 * and renders transparent non-blocking construction gift celebration overlays.
 */
@Composable
fun LiveGiftAnimationHost(
    events: List<LiveGiftOverlayEvent>,
    modifier: Modifier = Modifier,
    currentUserId: String = "current_user",
    onEventFinished: (String) -> Unit = {}
) {
    var currentDisplayEvent by remember { mutableStateOf<LiveGiftOverlayEvent?>(null) }
    var comboCount by remember { mutableIntStateOf(1) }
    val pendingQueue = remember { mutableStateListOf<LiveGiftOverlayEvent>() }

    // Sync incoming events into queue and handle combos
    LaunchedEffect(events) {
        if (events.isNotEmpty()) {
            val latest = events.last()
            val active = currentDisplayEvent

            if (active != null &&
                active.roomId == latest.roomId &&
                active.giftName == latest.giftName &&
                active.senderName == latest.senderName
            ) {
                // Same sender + same gift = Aggregate combo streak!
                comboCount += latest.quantity
            } else {
                // Add to queue if not already present or active
                if (pendingQueue.none { it.id == latest.id } && active?.id != latest.id) {
                    if (pendingQueue.size < 12) { // Bounded queue safety
                        pendingQueue.add(latest)
                    }
                }
            }
        }
    }

    // Queue Processor Loop
    LaunchedEffect(currentDisplayEvent, pendingQueue.size) {
        if (currentDisplayEvent == null && pendingQueue.isNotEmpty()) {
            val nextEvent = pendingQueue.removeAt(0)
            comboCount = if (nextEvent.comboCount > 1) nextEvent.comboCount else nextEvent.quantity.coerceAtLeast(1)
            currentDisplayEvent = nextEvent
        }
    }

    // Active Gift Celebration Overlay
    val activeEvent = currentDisplayEvent
    if (activeEvent != null) {
        key(activeEvent.id) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                LiveGiftOverlayV2(
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
 * Premium Construction Gift Overlay V2:
 * 4-Phase Lifecycle (Entrance -> Reveal & Construction Motion -> Hold -> Exit).
 */
@Composable
fun LiveGiftOverlayV2(
    event: LiveGiftOverlayEvent,
    comboCount: Int,
    currentUserId: String = "current_user",
    onAnimationComplete: () -> Unit
) {
    // Determine target durations based on gift diamond cost tier
    val totalCost = event.diamondCost * comboCount
    val (entranceMs, revealMs, holdMs, exitMs) = remember(totalCost) {
        when {
            totalCost <= 15 -> Tuple4(600, 1100, 2800, 1000)   // ~5.5s Standard
            totalCost <= 50 -> Tuple4(600, 1400, 3500, 1000)   // ~6.5s Medium
            totalCost <= 200 -> Tuple4(600, 1800, 4600, 1000)  // ~8.0s Premium
            else -> Tuple4(600, 2200, 6200, 1000)              // ~10.0s Special
        }
    }

    var animationStage by remember { mutableIntStateOf(0) } // 0: Entrance, 1: Reveal, 2: Hold, 3: Exit

    // Scale, Alpha, Offset Animatables
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }
    val comboBounce = remember { Animatable(1.0f) }

    // Floating animation during Hold stage
    val infiniteTransition = rememberInfiniteTransition(label = "giftFloatTransition")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Stage Lifecycle Controller
    LaunchedEffect(event.id) {
        // Stage 0: Entrance
        animationStage = 0
        launch { alpha.animateTo(1f, tween(350)) }
        launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        scale.animateTo(1.1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        scale.animateTo(1.0f, tween(150))
        delay(entranceMs.toLong() - 500)

        // Stage 1: Reveal & Gift-Specific Construction Motion
        animationStage = 1
        delay(revealMs.toLong())

        // Stage 2: Hold
        animationStage = 2
        delay(holdMs.toLong())

        // Stage 3: Exit
        animationStage = 3
        launch { offsetY.animateTo(-30f, tween(exitMs, easing = FastOutLinearInEasing)) }
        alpha.animateTo(0f, tween(exitMs))

        onAnimationComplete()
    }

    // Bounce combo counter whenever combo increments
    LaunchedEffect(comboCount) {
        if (comboCount > 1) {
            comboBounce.snapTo(1.35f)
            comboBounce.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Dynamic Sparkle Particles
    val particles = remember(event.giftName) {
        List(22) {
            ConstructionParticle(
                initialX = Random.nextFloat() * 160f - 80f,
                initialY = Random.nextFloat() * 160f - 80f,
                angle = Random.nextFloat() * 360f,
                speed = Random.nextFloat() * 2.5f + 1f,
                size = Random.nextFloat() * 7f + 4f,
                color = when (it % 3) {
                    0 -> LiveGold
                    1 -> LivePrimaryMint
                    else -> LiveDiamondColor
                }
            )
        }
    }

    val giftIcon = remember(event.giftName) { getGiftIcon(event.giftName) }
    val isSender = event.senderName.equals("You", ignoreCase = true) ||
            event.senderName.equals(currentUserId, ignoreCase = true)

    val senderText = if (isSender) "You sent a gift" else "${event.senderName} sent a gift"
    val shadowStyle = Shadow(color = Color.Black.copy(alpha = 0.9f), blurRadius = 8f)

    // Gift-specific motion parameters
    val (iconScale, iconRotation, iconOffsetX, iconOffsetY) = remember(event.giftName, animationStage) {
        when {
            event.giftName.contains("Tape", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.12f, 0f, 0f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Drill", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.05f, 4f, 0f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Hammer", ignoreCase = true) || event.giftName.contains("Trowel", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.08f, -12f, 0f, 6f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Excavator", ignoreCase = true) || event.giftName.contains("Bulldozer", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.06f, 0f, 8f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Crane", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.08f, 0f, 0f, -8f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            else -> Tuple4(1.0f, 0f, 0f, 0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
                translationY = offsetY.value + (if (animationStage == 2) floatY else 0f)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PROMINENT CONSTRUCTION ARTWORK CONTAINER WITH RADIAL GLOW & CANVAS PARTICLES
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                // Soft Radial Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    LiveGold.copy(alpha = if (animationStage == 1) 0.55f else 0.35f),
                                    LivePrimaryMint.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Particle Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (animationStage == 3) 0.2f else 0.85f)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val progress = if (animationStage == 1) 0.6f else 0.3f
                    particles.forEach { p ->
                        val rad = Math.toRadians((p.angle + progress * 180f).toDouble())
                        val x = center.x + p.initialX + (cos(rad) * progress * 35f).toFloat()
                        val y = center.y + p.initialY + (sin(rad) * progress * 35f).toFloat()
                        drawCircle(
                            color = p.color.copy(alpha = (1f - progress * 0.5f).coerceIn(0f, 1f)),
                            radius = p.size,
                            center = Offset(x, y)
                        )
                    }
                }

                // Main Construction Gift Icon Artwork (118dp Circle Box with 68dp Vector Icon)
                Box(
                    modifier = Modifier
                        .size(118.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.65f),
                                    Color(0xFF161C28).copy(alpha = 0.85f)
                                )
                            )
                        )
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(LiveGold, LivePrimaryMint, LiveDiamondColor, LiveGold)
                            ),
                            CircleShape
                        )
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            rotationZ = iconRotation
                            translationX = iconOffsetX
                            translationY = iconOffsetY
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = giftIcon,
                        contentDescription = event.giftName,
                        tint = LiveGold,
                        modifier = Modifier.size(68.dp)
                    )
                }
            }

            // CLEAN GIFT NAME & IDENTITY TEXT HIERARCHY
            Text(
                text = event.giftName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(shadow = shadowStyle)
            )

            Text(
                text = senderText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(shadow = shadowStyle)
            )

            // QUANTITY & CONFIRMED DIAMOND CHIP WITH COMBO MULTIPLIER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LiveGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "×$comboCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = LiveGold,
                            modifier = Modifier.graphicsLayer {
                                scaleX = comboBounce.value
                                scaleY = comboBounce.value
                            }
                        )

                        Text("·", color = Color.Gray, fontSize = 12.sp)

                        Icon(
                            imageVector = Icons.Default.Diamond,
                            contentDescription = null,
                            tint = LiveDiamondColor,
                            modifier = Modifier.size(15.dp)
                        )

                        Text(
                            text = "${event.diamondCost * comboCount}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = LiveDiamondColor
                        )
                    }
                }
            }
        }
    }
}

private data class Tuple4<A, B, C, D>(
    val val1: A,
    val val2: B,
    val val3: C,
    val val4: D
)
