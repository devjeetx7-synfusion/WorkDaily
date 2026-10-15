package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.audio.RewardsSoundEvent
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConstructionSpinScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val spinTokens by viewModel.spinTokens.collectAsState()
    val walletSummary by viewModel.walletSummary.collectAsState()
    val audioSettings by viewModel.audioSettings.collectAsState()
    val segments = viewModel.spinSegments

    val coroutineScope = rememberCoroutineScope()

    var isSpinning by remember { mutableStateOf(false) }
    var selectedWinningSegment by remember { mutableStateOf<SpinWheelSegment?>(null) }
    var showResultSheet by remember { mutableStateOf(false) }

    val rotationAnim = remember { Animatable(0f) }

    fun startSpin() {
        if (spinTokens.availableSpins <= 0 || isSpinning) return

        isSpinning = true
        viewModel.playSound(RewardsSoundEvent.SPIN_START)

        val winningSegment = viewModel.spinWheel() ?: run {
            isSpinning = false
            return
        }
        selectedWinningSegment = winningSegment

        val segmentCount = segments.size
        val degreesPerSegment = 360f / segmentCount
        val targetSegmentIndex = segments.indexOfFirst { it.id == winningSegment.id }

        val segmentCenterAngle = targetSegmentIndex * degreesPerSegment + (degreesPerSegment / 2f)
        val pointerAngleOffset = 270f
        val finalTargetAngle = 360f * 5f + (pointerAngleOffset - segmentCenterAngle)

        coroutineScope.launch {
            val startAngle = rotationAnim.value % 360f
            rotationAnim.snapTo(startAngle)

            var lastSectorPassed = -1
            rotationAnim.animateTo(
                targetValue = finalTargetAngle,
                animationSpec = tween(
                    durationMillis = 4500,
                    easing = FastOutSlowInEasing
                )
            ) {
                // Calculate current sector under top pointer (270 deg)
                val currentDegree = (value % 360f)
                val effectivePointerAngle = (360f + 270f - currentDegree) % 360f
                val currentSectorIndex = (effectivePointerAngle / degreesPerSegment).toInt() % segmentCount
                if (currentSectorIndex != lastSectorPassed) {
                    lastSectorPassed = currentSectorIndex
                    viewModel.playSound(RewardsSoundEvent.SPIN_TICK)
                }
            }

            viewModel.playSound(RewardsSoundEvent.SPIN_STOP)
            isSpinning = false
            showResultSheet = true

            when (winningSegment.rewardType) {
                is SpinRewardType.Coins -> viewModel.playSound(RewardsSoundEvent.COIN_REWARD)
                is SpinRewardType.BonusSpin -> viewModel.playSound(RewardsSoundEvent.BONUS_SPIN)
            }
        }
    }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Builder's Spin Wheel",
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = {
                        viewModel.updateAudioSettings(audioSettings.copy(isSoundEnabled = !audioSettings.isSoundEnabled))
                    }) {
                        Icon(
                            imageVector = if (audioSettings.isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                            contentDescription = "Mute Sound",
                            tint = RoyalPurple
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RoyalPurple.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 13,
                                textColor = RoyalPurple,
                                coinSize = 16.dp
                            )
                        }
                    }
                }
            )
        },
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Title & Subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = "Builder's Spin Wheel",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spin and collect WorkDaily Demo Rewards!",
                    fontSize = 13.sp,
                    color = RewardsTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Available Spins Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = RoyalPurple,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Available Spins: ${spinTokens.availableSpins}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = "🎯", fontSize = 14.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Canvas Spin Wheel Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(310.dp)
                    .padding(8.dp)
            ) {
                RewardsRadialGlow(
                    glowColor = GoldPrimary,
                    alpha = 0.4f,
                    modifier = Modifier.size(290.dp)
                )

                Canvas(
                    modifier = Modifier.size(280.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val radius = canvasWidth / 2f

                    rotate(rotationAnim.value, pivot = center) {
                        val segmentCount = segments.size
                        val sweepAngle = 360f / segmentCount

                        val sectorColors = listOf(
                            Color(0xFF6737F5), // Royal Purple
                            Color(0xFFFFBE0B), // Rich Gold
                            Color(0xFF873FFF), // Vibrant Violet
                            Color(0xFFF59E0B)  // Deep Yellow
                        )

                        for (i in 0 until segmentCount) {
                            val startAngle = i * sweepAngle
                            val segmentColor = sectorColors[i % sectorColors.size]

                            drawArc(
                                color = segmentColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = Size(canvasWidth, canvasHeight)
                            )

                            // Sector separator line
                            val lineRad = (startAngle * PI / 180f).toFloat()
                            val lineEnd = Offset(
                                x = center.x + radius * cos(lineRad),
                                y = center.y + radius * sin(lineRad)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.65f),
                                start = center,
                                end = lineEnd,
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw Sector Illustration near outer-middle
                            val midAngleRad = ((startAngle + sweepAngle / 2f) * PI / 180f).toFloat()
                            val iconRadius = radius * 0.68f
                            val iconX = center.x + iconRadius * cos(midAngleRad)
                            val iconY = center.y + iconRadius * sin(midAngleRad)
                            val itemSize = 28.dp.toPx()

                            when (segments[i].id) {
                                1 -> drawSafetyHelmet(iconX, iconY, itemSize)
                                2 -> drawHammer(iconX, iconY, itemSize)
                                3 -> drawBricks(iconX, iconY, itemSize)
                                4 -> drawToolbox(iconX, iconY, itemSize)
                                5 -> drawCrane(iconX, iconY, itemSize)
                                6 -> drawCementTruck(iconX, iconY, itemSize)
                                7 -> drawGoldenBlueprint(iconX, iconY, itemSize)
                                8 -> drawForemansPass(iconX, iconY, itemSize)
                            }

                            // Draw Sector Reward Label near inner portion
                            val labelRadius = radius * 0.38f
                            val labelX = center.x + labelRadius * cos(midAngleRad)
                            val labelY = center.y + labelRadius * sin(midAngleRad)

                            val seg = segments[i]
                            val labelText = when (val r = seg.rewardType) {
                                is SpinRewardType.Coins -> "+${r.amount}"
                                is SpinRewardType.BonusSpin -> "+1 Spin"
                            }

                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 11.dp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                labelText,
                                labelX,
                                labelY + 4.dp.toPx(),
                                textPaint
                            )
                        }

                        // Outer Golden Rim Frame
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(GoldPrimary, GoldSecondary, Color.White, GoldPrimary)
                            ),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }

                    // Center Hub
                    drawCircle(color = GoldPrimary, radius = 28.dp.toPx(), center = center)
                    drawCircle(color = Color.White, radius = 24.dp.toPx(), center = center)
                    drawCircle(color = RoyalPurple, radius = 20.dp.toPx(), center = center)

                    val hubPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 16.dp.toPx()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                    drawContext.canvas.nativeCanvas.drawText("W", center.x, center.y + 6.dp.toPx(), hubPaint)
                }

                // Top Pointer Arrow (Fixed)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-6).dp)
                ) {
                    Canvas(modifier = Modifier.size(28.dp, 32.dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path, color = GoldPrimary)
                        drawPath(path, color = Color.White, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            if (spinTokens.availableSpins > 0) {
                RewardPrimaryButton(
                    text = if (isSpinning) "SPINNING..." else "SPIN NOW 🎯",
                    onClick = { startSpin() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSpinning,
                    containerColor = RoyalPurple,
                    contentColor = Color.White
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Spins Available",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsTheme.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Claim tomorrow's Daily Check-in to receive another free spin!",
                            fontSize = 12.sp,
                            color = RewardsTheme.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Possible Rewards Section (8 Prize Showcase Cards)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Possible Rewards",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val rowItems = segments.chunked(2)
                rowItems.forEach { pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { seg ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                                border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val cx = size.width / 2f
                                            val cy = size.height / 2f
                                            val itemSize = 24.dp.toPx()
                                            when (seg.id) {
                                                1 -> drawSafetyHelmet(cx, cy, itemSize)
                                                2 -> drawHammer(cx, cy, itemSize)
                                                3 -> drawBricks(cx, cy, itemSize)
                                                4 -> drawToolbox(cx, cy, itemSize)
                                                5 -> drawCrane(cx, cy, itemSize)
                                                6 -> drawCementTruck(cx, cy, itemSize)
                                                7 -> drawGoldenBlueprint(cx, cy, itemSize)
                                                8 -> drawForemansPass(cx, cy, itemSize)
                                            }
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = seg.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RewardsTheme.textPrimary
                                        )
                                        Text(
                                            text = when (val r = seg.rewardType) {
                                                is SpinRewardType.Coins -> "+${r.amount} Demo Coins"
                                                is SpinRewardType.BonusSpin -> "+1 Bonus Spin Token"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = RoyalPurple
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Spin Result Bottom Sheet Overlay
    if (showResultSheet && selectedWinningSegment != null) {
        val seg = selectedWinningSegment!!
        ModalBottomSheet(
            onDismissRequest = { showResultSheet = false },
            containerColor = RewardsTheme.cardBackground,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(90.dp)
                ) {
                    RewardsRadialGlow(glowColor = GoldPrimary, alpha = 0.6f)
                    RewardsSparkleLayer(modifier = Modifier.fillMaxSize())
                    Canvas(modifier = Modifier.size(60.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val itemSize = 50.dp.toPx()
                        when (seg.id) {
                            1 -> drawSafetyHelmet(cx, cy, itemSize)
                            2 -> drawHammer(cx, cy, itemSize)
                            3 -> drawBricks(cx, cy, itemSize)
                            4 -> drawToolbox(cx, cy, itemSize)
                            5 -> drawCrane(cx, cy, itemSize)
                            6 -> drawCementTruck(cx, cy, itemSize)
                            7 -> drawGoldenBlueprint(cx, cy, itemSize)
                            8 -> drawForemansPass(cx, cy, itemSize)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "CONGRATULATIONS! 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                when (val r = seg.rewardType) {
                    is SpinRewardType.Coins -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "YOU WON ", fontSize = 15.sp, color = RewardsTheme.textSecondary)
                            RewardCoinBalance(
                                amount = r.amount,
                                fontSize = 18,
                                textColor = RewardsSuccessGreen,
                                coinSize = 20.dp
                            )
                            Text(text = " DEMO COINS!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                        }
                    }
                    is SpinRewardType.BonusSpin -> {
                        Text(
                            text = "YOU WON +1 BONUS SPIN TOKEN! 🎯",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RewardSecondaryButton(
                        text = "DONE",
                        onClick = { showResultSheet = false },
                        modifier = Modifier.weight(1f)
                    )

                    if (spinTokens.availableSpins > 0) {
                        RewardPrimaryButton(
                            text = "SPIN AGAIN 🎯",
                            onClick = {
                                showResultSheet = false
                                startSpin()
                            },
                            modifier = Modifier.weight(1f),
                            containerColor = RoyalPurple,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }
    }
}
