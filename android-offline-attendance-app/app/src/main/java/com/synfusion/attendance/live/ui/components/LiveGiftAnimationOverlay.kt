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
import androidx.compose.ui.platform.LocalContext
import com.synfusion.attendance.live.model.LiveGiftOverlayEvent
import com.synfusion.attendance.rewards.audio.RewardsSoundEvent
import com.synfusion.attendance.rewards.audio.RewardsSoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
 * Real-time particle instance supporting continuous frame-driven velocity, trajectory and lifetime.
 */
private data class MovingParticle(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val size: Float,
    val maxLifetimeMs: Float,
    val color: Color
)

/**
 * Live Gift Animation Host V4:
 * Coordinates the deterministic gift queue, aggregates rapid same-sender combo events,
 * and renders transparent non-blocking construction gift celebration overlays.
 */
@Composable
fun LiveGiftAnimationHost(
    events: List<LiveGiftOverlayEvent>,
    modifier: Modifier = Modifier,
    currentUserId: String = "current_user",
    soundEnabled: Boolean = true,
    onEventFinished: (String) -> Unit = {}
) {
    var currentDisplayEvent by remember { mutableStateOf<LiveGiftOverlayEvent?>(null) }
    var comboCount by remember { mutableIntStateOf(1) }
    val pendingQueue = remember { mutableStateListOf<LiveGiftOverlayEvent>() }
    val processedEventIds = remember { mutableSetOf<String>() }

    // Process incoming events in chronological order with unique ID tracking
    LaunchedEffect(events) {
        for (event in events) {
            if (processedEventIds.contains(event.id)) continue

            val active = currentDisplayEvent
            if (active != null &&
                active.roomId == event.roomId &&
                active.giftName == event.giftName &&
                active.senderName == event.senderName
            ) {
                // Same room + same sender + same gift = Aggregate combo streak!
                comboCount += event.quantity
                processedEventIds.add(event.id)
                onEventFinished(event.id)
            } else {
                if (pendingQueue.none { it.id == event.id } && active?.id != event.id) {
                    if (pendingQueue.size < 20) { // Bounded queue capacity safety
                        pendingQueue.add(event)
                    }
                    processedEventIds.add(event.id)
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
                    soundEnabled = soundEnabled,
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
 * Premium Construction Gift Overlay V4:
 * 4-Phase Lifecycle (Entrance -> Reveal & Construction Motion -> Hold -> Exit) with real-time moving particles and sound.
 */
@Composable
fun LiveGiftOverlayV2(
    event: LiveGiftOverlayEvent,
    comboCount: Int,
    currentUserId: String = "current_user",
    soundEnabled: Boolean = true,
    onAnimationComplete: () -> Unit
) {
    val context = LocalContext.current
    val unitCost = if (event.unitDiamondCost > 0) event.unitDiamondCost else (event.diamondCost / event.quantity.coerceAtLeast(1)).coerceAtLeast(1)
    val aggregateTotalCost = unitCost * comboCount

    val (entranceMs, revealMs, holdMs, exitMs) = remember(aggregateTotalCost) {
        when {
            aggregateTotalCost <= 15 -> Tuple4(600, 1100, 2800, 1000)   // ~5.5s Standard
            aggregateTotalCost <= 50 -> Tuple4(600, 1400, 3500, 1000)   // ~6.5s Medium
            aggregateTotalCost <= 200 -> Tuple4(600, 1800, 4600, 1000)  // ~8.0s Premium
            else -> Tuple4(600, 2200, 6200, 1000)              // ~10.0s Special
        }
    }

    var animationStage by remember { mutableIntStateOf(0) } // 0: Entrance, 1: Reveal, 2: Hold, 3: Exit

    val scale = remember { Animatable(0.55f) }
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(30f) }
    val comboBounce = remember { Animatable(1.0f) }

    // Floating animation during Hold stage
    val infiniteTransition = rememberInfiniteTransition(label = "giftFloatTransition")
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatY"
    )

    // Stage Lifecycle Controller with Sound Triggers
    LaunchedEffect(event.id) {
        val soundManager = if (soundEnabled) RewardsSoundManager.getInstance(context) else null

        // Stage 0: Entrance
        animationStage = 0
        soundManager?.playSound(RewardsSoundEvent.SPIN_START)
        launch { alpha.animateTo(1f, tween(350)) }
        launch { offsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        scale.animateTo(1.1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        scale.animateTo(1.0f, tween(150))
        delay(entranceMs.toLong() - 500)

        // Stage 1: Reveal & Gift-Specific Construction Motion
        animationStage = 1
        when {
            aggregateTotalCost >= 200 -> soundManager?.playSound(RewardsSoundEvent.CONGRATULATIONS)
            aggregateTotalCost >= 50 -> soundManager?.playSound(RewardsSoundEvent.SEVEN_DAY_STREAK)
            else -> soundManager?.playSound(RewardsSoundEvent.COIN_REWARD)
        }
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
            if (soundEnabled) {
                RewardsSoundManager.getInstance(context).playSound(RewardsSoundEvent.SPIN_TICK)
            }
            comboBounce.animateTo(1.0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
    }

    // Time-driven particle animation
    var elapsedParticleTimeMs by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(event.id) {
        val startTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                elapsedParticleTimeMs = (frameTime - startTime) / 1_000_000f
            }
        }
    }

    val particlePool = remember(event.giftName) {
        List(28) { i ->
            val angleRad = (i * (360f / 28f) + Random.nextFloat() * 15f) * (Math.PI / 180f)
            val speed = Random.nextFloat() * 0.12f + 0.05f
            MovingParticle(
                startX = 0f,
                startY = 0f,
                vx = (cos(angleRad) * speed).toFloat(),
                vy = (sin(angleRad) * speed).toFloat(),
                size = Random.nextFloat() * 6f + 3f,
                maxLifetimeMs = Random.nextFloat() * 1800f + 1200f,
                color = when (i % 4) {
                    0 -> LiveGold
                    1 -> LivePrimaryMint
                    2 -> LiveDiamondColor
                    else -> Color(0xFFF59E0B)
                }
            )
        }
    }

    val giftIcon = remember(event.giftName) { getGiftIcon(event.giftName) }
    val isSender = event.senderName.equals("You", ignoreCase = true) ||
            event.senderName.equals(currentUserId, ignoreCase = true)

    val senderText = if (isSender) "You sent a gift" else "${event.senderName} sent a gift"
    val shadowStyle = Shadow(color = Color.Black.copy(alpha = 0.9f), blurRadius = 8f)

    // Animated gift icon motion values
    val (iconScale, iconRotation, iconOffsetX, iconOffsetY) = remember(event.giftName, animationStage) {
        when {
            event.giftName.contains("Tape", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.15f, 0f, 0f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Drill", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.08f, 6f, 0f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Hammer", ignoreCase = true) || event.giftName.contains("Trowel", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.10f, -14f, 0f, 8f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Excavator", ignoreCase = true) || event.giftName.contains("Bulldozer", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.08f, 0f, 10f, 0f) else Tuple4(1.0f, 0f, 0f, 0f)
            }
            event.giftName.contains("Crane", ignoreCase = true) -> {
                if (animationStage == 1) Tuple4(1.10f, 0f, 0f, -10f) else Tuple4(1.0f, 0f, 0f, 0f)
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

                // Continuous Moving Particle Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (animationStage == 3) 0.2f else 0.85f)
                ) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    particlePool.forEach { p ->
                        val ageMs = elapsedParticleTimeMs % p.maxLifetimeMs
                        val lifecycleProgress = ageMs / p.maxLifetimeMs
                        val x = center.x + p.startX + p.vx * ageMs
                        val y = center.y + p.startY + p.vy * ageMs
                        val particleAlpha = (1f - lifecycleProgress).coerceIn(0f, 1f)
                        drawCircle(
                            color = p.color.copy(alpha = particleAlpha),
                            radius = p.size * (1f - lifecycleProgress * 0.3f),
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
                            text = "$aggregateTotalCost",
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
