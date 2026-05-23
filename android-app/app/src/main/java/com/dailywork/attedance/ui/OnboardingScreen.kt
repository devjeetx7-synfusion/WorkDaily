package com.dailywork.attedance.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.data.UserPreferencesRepository
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val content: @Composable (Float) -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    repository: UserPreferencesRepository,
    onComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val pages = remember { getOnboardingPages() }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle background motion
        AnimatedBackground()

        // Skip Button at top right
        if (pagerState.currentPage < pages.size - 1) {
            TextButton(
                onClick = {
                    scope.launch {
                        repository.saveOnboardingCompleted(true)
                        onComplete()
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .statusBarsPadding()
            ) {
                Text(
                    "Skip",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val pageOffset = (
                        (pagerState.currentPage - page) + pagerState
                            .currentPageOffsetFraction
                        ).absoluteValue

                OnboardingPageContent(
                    page = pages[page],
                    offset = pageOffset
                )
            }

            // Bottom Navigation Area
            OnboardingBottomBar(
                pagerState = pagerState,
                pageCount = pages.size,
                onNext = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        scope.launch {
                            repository.saveOnboardingCompleted(true)
                            onComplete()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAnim"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f).blur(100.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF13B28A).copy(alpha = 0.2f), Color.Transparent),
                center = center.copy(x = center.x * (0.5f + animValue * 0.5f), y = center.y * (0.5f - animValue * 0.2f)),
                radius = size.minDimension
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFEF4444).copy(alpha = 0.15f), Color.Transparent),
                center = center.copy(x = center.x * (1.5f - animValue * 0.5f), y = center.y * (1.5f + animValue * 0.2f)),
                radius = size.minDimension
            )
        )
    }
}

@Composable
fun OnboardingPageContent(
    page: OnboardingPage,
    offset: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            page.content(offset)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 38.sp
            ),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - (offset * 2.5f).coerceIn(0f, 1f)
                translationY = offset * 80f
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer {
                alpha = 1f - (offset * 3.5f).coerceIn(0f, 1f)
                translationY = offset * 120f
            }
        )

        Spacer(modifier = Modifier.weight(0.2f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingBottomBar(
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageCount: Int,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Indicators
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(pageCount) { index ->
                val isSelected = pagerState.currentPage == index
                val width by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "indicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                )
            }
        }

        // Primary Action Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (pagerState.currentPage < pageCount - 1) "Continue" else "Get Started",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

fun getOnboardingPages(): List<OnboardingPage> {
    return listOf(
        OnboardingPage(
            title = "Manage Attendance Easily",
            subtitle = "Track daily work, attendance, and payments in one place.",
            content = { offset -> OnboardingAttendanceVisual(offset) }
        ),
        OnboardingPage(
            title = "For Personal & Contractors",
            subtitle = "Manage your own work or handle complete worker teams.",
            content = { offset -> OnboardingRoleVisual(offset) }
        ),
        OnboardingPage(
            title = "Complete Worker Records",
            subtitle = "Track helpers, mistry, overtime, dues, and advances easily.",
            content = { offset -> OnboardingWorkerRecordsVisual(offset) }
        ),
        OnboardingPage(
            title = "Smart Reports & Passbooks",
            subtitle = "Generate attendance reports and share PDF passbooks anytime.",
            content = { offset -> OnboardingReportsVisual(offset) }
        ),
        OnboardingPage(
            title = "Track Every Payment",
            subtitle = "Manage advance, half payment, full payment, and balances clearly.",
            content = { offset -> OnboardingPaymentVisual(offset) }
        ),
        OnboardingPage(
            title = "Work Smarter Daily",
            subtitle = "Stay organized with simple and powerful work management tools.",
            content = { offset -> OnboardingProductivityVisual(offset) }
        ),
        OnboardingPage(
            title = "Unlock Premium Experience",
            subtitle = "Get advanced analytics, premium contractor tools, exclusive features, and a faster experience.",
            content = { offset -> OnboardingPremiumVisual(offset) }
        )
    )
}

@Composable
fun OnboardingAttendanceVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "attendance")
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        // Calendar Grid Background
        Card(
            modifier = Modifier
                .size(240.dp)
                .graphicsLayer {
                    rotationX = 15f + offset * 20f
                    rotationZ = -5f
                    translationY = floatAnim
                    alpha = 1f - offset
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(4) {
                        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                repeat(3) { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(4) { col ->
                            val isChecked = (row + col) % 3 == 0
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .size(40.dp)
                                    .background(
                                        if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                        RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isChecked) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Worker Card
        Card(
            modifier = Modifier
                .offset(x = 60.dp, y = 80.dp)
                .width(160.dp)
                .height(60.dp)
                .graphicsLayer {
                    rotationZ = 5f - offset * 10f
                    scaleX = 1f + floatAnim / 100f
                    scaleY = 1f + floatAnim / 100f
                    alpha = 1f - offset
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Box(modifier = Modifier.width(60.dp).height(8.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f), CircleShape))
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(40.dp).height(6.dp).background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f), CircleShape))
                }
            }
        }
    }
}

@Composable
fun OnboardingRoleVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "roles")
    val toggle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "toggle"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        // Personal Role Card
        Card(
            modifier = Modifier
                .offset(x = (-40 + toggle * 20).dp, y = (-30 + toggle * 10).dp)
                .size(160.dp)
                .graphicsLayer {
                    scaleX = 0.9f + (1f - toggle) * 0.2f
                    scaleY = 0.9f + (1f - toggle) * 0.2f
                    alpha = 0.7f + (1f - toggle) * 0.3f - offset
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = (4 + (1f - toggle) * 8).dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("👤", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Personal", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }

        // Contractor Role Card
        Card(
            modifier = Modifier
                .offset(x = (40 - toggle * 20).dp, y = (30 - toggle * 10).dp)
                .size(160.dp)
                .graphicsLayer {
                    scaleX = 0.9f + toggle * 0.2f
                    scaleY = 0.9f + toggle * 0.2f
                    alpha = 0.7f + toggle * 0.3f - offset
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = (4 + toggle * 8).dp)
        ) {
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("🏗️", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Contractor", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun OnboardingWorkerRecordsVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "workers")
    val listAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "list"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val workers = listOf("Rahul Kumar" to "Mistry", "Amit Singh" to "Helper", "Suresh" to "Helper")
            workers.forEachIndexed { index, (name, role) ->
                val itemOffset = (index * 0.2f)
                val entryAnim = ((listAnim - itemOffset).coerceIn(0f, 0.5f) * 2f)

                Card(
                    modifier = Modifier
                        .width(260.dp)
                        .graphicsLayer {
                            translationX = (1f - entryAnim) * 100f
                            alpha = entryAnim - offset
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                            Text(name.first().toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(role, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.background(Color(0xFF10B981).copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("OT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                            if (index == 0) {
                                Box(modifier = Modifier.background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                    Text("Due", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingReportsVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "reports")
    val shareScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        // Document Card
        Card(
            modifier = Modifier
                .size(200.dp, 260.dp)
                .graphicsLayer {
                    rotationZ = -3f + offset * 10f
                    alpha = 1f - offset
                },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.width(80.dp).height(8.dp).background(Color.LightGray.copy(alpha = 0.3f), CircleShape))
                }
                Spacer(modifier = Modifier.height(20.dp))
                repeat(6) {
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp).background(Color.LightGray.copy(alpha = 0.2f), CircleShape))
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("PDF Passbook", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                }
            }
        }

        // Floating Share Icon
        Box(
            modifier = Modifier
                .offset(x = 80.dp, y = -100.dp)
                .size(56.dp)
                .scale(shareScale)
                .graphicsLayer { alpha = 1f - offset }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun OnboardingPaymentVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "payment")
    val rupeeY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rupee"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        // Balance Card
        Card(
            modifier = Modifier
                .width(240.dp)
                .graphicsLayer {
                    rotationX = offset * 30f
                    alpha = 1f - offset
                },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Current Balance", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("₹ 12,450", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Paid", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹ 8,200", fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Due", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("₹ 4,250", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        // Floating Rupee Indicators
        repeat(3) { i ->
            val xPos = (i - 1) * 80
            val delay = i * 600
            val anim by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rupee$i"
            )

            Text(
                "₹",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 1f - anim),
                modifier = Modifier
                    .offset(x = xPos.dp, y = (40 - anim * 100).dp)
                    .graphicsLayer { alpha = (1f - anim) * (1f - offset) }
            )
        }
    }
}

@Composable
fun OnboardingProductivityVisual(offset: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "productivity")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progress"
    )

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = 1f - offset * 0.1f
                    scaleY = 1f - offset * 0.1f
                    alpha = 1f - offset
                },
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TrendingUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Daily Stats", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.height(24.dp))
                // Simple Bar Chart Visual
                Row(modifier = Modifier.height(100.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f)
                    heights.forEachIndexed { index, h ->
                        val animatedHeight = h * progress
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(animatedHeight)
                                .background(
                                    if (index == 3) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("85% Productivity Reached", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun OnboardingPremiumVisual(offset: Float) {
    var isUnlocked by remember { mutableStateOf(false) }

    LaunchedEffect(offset) {
        if (offset < 0.1f) {
            kotlinx.coroutines.delay(500)
            isUnlocked = true
        } else {
            isUnlocked = false
        }
    }

    val lockRotation by animateFloatAsState(if (isUnlocked) 0f else -15f, label = "rot")
    val lockScale by animateFloatAsState(if (isUnlocked) 1.2f else 1f, label = "scale")

    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
        // Glowing background
        Box(
            modifier = Modifier
                .size(160.dp)
                .blur(40.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer {
                        rotationZ = lockRotation + offset * 45f
                        scaleX = lockScale - offset
                        scaleY = lockScale - offset
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(if (isUnlocked) "🔓" else "🔒", fontSize = 60.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text("PREMIUM", color = Color(0xFFB8860B), fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}
