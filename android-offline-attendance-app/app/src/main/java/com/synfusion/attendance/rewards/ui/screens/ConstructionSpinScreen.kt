package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
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
    val segments = viewModel.spinSegments

    val coroutineScope = rememberCoroutineScope()

    var isSpinning by remember { mutableStateOf(false) }
    var selectedWinningSegment by remember { mutableStateOf<SpinWheelSegment?>(null) }
    var showResultSheet by remember { mutableStateOf(false) }

    // Animation state for wheel rotation angle
    val rotationAnim = remember { Animatable(0f) }

    fun startSpin() {
        if (spinTokens.availableSpins <= 0 || isSpinning) return

        isSpinning = true
        // 1. Pick winning segment from repository first
        val winningSegment = viewModel.spinWheel() ?: run {
            isSpinning = false
            return
        }
        selectedWinningSegment = winningSegment

        // 2. Calculate target rotation angle
        val segmentCount = segments.size
        val degreesPerSegment = 360f / segmentCount
        val targetSegmentIndex = segments.indexOfFirst { it.id == winningSegment.id }

        // Top pointer is at 270 degrees (or -90 degrees) in standard math canvas coordinate
        // Segment 0 center starts at 0 degree offset.
        // Stopping angle formula: Target angle at top pointer = 360 * 5 full rotations + (360 - (targetSegmentIndex * degreesPerSegment + degreesPerSegment / 2) - 90)
        val segmentCenterAngle = targetSegmentIndex * degreesPerSegment + (degreesPerSegment / 2f)
        val pointerAngleOffset = 270f
        val finalTargetAngle = 360f * 5f + (pointerAngleOffset - segmentCenterAngle)

        coroutineScope.launch {
            rotationAnim.snapTo(rotationAnim.value % 360f)
            rotationAnim.animateTo(
                targetValue = finalTargetAngle,
                animationSpec = tween(
                    durationMillis = 4500,
                    easing = FastOutSlowInEasing
                )
            )
            isSpinning = false
            showResultSheet = true
        }
    }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Builder's Spin Wheel",
                onNavigateBack = onNavigateBack,
                actions = {
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "Builder's Spin Wheel",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Spin and collect WorkDaily Demo Coins!",
                    fontSize = 13.sp,
                    color = RewardsTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Available Spins Badge
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

            // Canvas Spin Wheel Container
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(310.dp)
                    .padding(8.dp)
            ) {
                // Background Radial Glow
                RewardsRadialGlow(
                    glowColor = GoldPrimary,
                    alpha = 0.35f,
                    modifier = Modifier.size(280.dp)
                )

                // The Rotating Wheel Canvas
                Canvas(
                    modifier = Modifier
                        .size(280.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val radius = canvasWidth / 2f

                    // Rotate entire canvas according to animation angle
                    rotate(rotationAnim.value, pivot = center) {
                        val segmentCount = segments.size
                        val sweepAngle = 360f / segmentCount

                        val colors = listOf(
                            Color(0xFF6737F5), // Royal Purple
                            Color(0xFFFFBE0B), // Rich Gold
                            Color(0xFF873FFF), // Vibrant Violet
                            Color(0xFFF59E0B)  // Warm Yellow
                        )

                        // Draw 8 Sectors
                        for (i in 0 until segmentCount) {
                            val startAngle = i * sweepAngle
                            val segmentColor = colors[i % colors.size]

                            drawArc(
                                color = segmentColor,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset.Zero,
                                size = Size(canvasWidth, canvasHeight)
                            )

                            // Sector Border Line
                            val lineRad = (startAngle * PI / 180f).toFloat()
                            val lineEnd = Offset(
                                x = center.x + radius * cos(lineRad),
                                y = center.y + radius * sin(lineRad)
                            )
                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = center,
                                end = lineEnd,
                                strokeWidth = 2.dp.toPx()
                            )

                            // Draw Sector Content (Reward text & icon indicator)
                            val midAngleRad = ((startAngle + sweepAngle / 2f) * PI / 180f).toFloat()
                            val textRadius = radius * 0.68f
                            val textX = center.x + textRadius * cos(midAngleRad)
                            val textY = center.y + textRadius * sin(midAngleRad)

                            val seg = segments[i]
                            val textPaint = android.graphics.Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = 13.dp.toPx()
                                isFakeBoldText = true
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }

                            drawContext.canvas.nativeCanvas.drawText(
                                "+${seg.coinReward} 🪙",
                                textX,
                                textY + 5.dp.toPx(),
                                textPaint
                            )
                        }

                        // Outer Metallic Frame
                        drawCircle(
                            brush = Brush.sweepGradient(
                                listOf(GoldPrimary, GoldSecondary, Color.White, GoldPrimary)
                            ),
                            radius = radius,
                            center = center,
                            style = Stroke(width = 8.dp.toPx())
                        )
                    }

                    // Center Hub (Stationary overlay or spinning with hub)
                    drawCircle(
                        color = GoldPrimary,
                        radius = 28.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 24.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = RoyalPurple,
                        radius = 20.dp.toPx(),
                        center = center
                    )

                    val hubPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 16.dp.toPx()
                        isFakeBoldText = true
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText("W", center.x, center.y + 6.dp.toPx(), hubPaint)
                }

                // Top Pointer Arrow (Fixed at Top Center pointing down)
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

            // Bottom Action Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    modifier = Modifier.size(80.dp)
                ) {
                    RewardsRadialGlow(glowColor = GoldPrimary, alpha = 0.5f)
                    Image(
                        painter = painterResource(id = R.drawable.workdaily_coin),
                        contentDescription = "Winning Coin",
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Congratulations! 🎉",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "You Won ",
                        fontSize = 15.sp,
                        color = RewardsTheme.textSecondary
                    )
                    RewardCoinBalance(
                        amount = seg.coinReward,
                        fontSize = 18,
                        textColor = RewardsSuccessGreen,
                        coinSize = 20.dp
                    )
                    Text(
                        text = " on ${seg.name}!",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTheme.textPrimary
                    )
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
