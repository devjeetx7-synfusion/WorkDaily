package com.dailywork.attedance.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    val rotationAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit"
    )

    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = SineOverShootEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(2800)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle Background Particles
        SplashParticlesEffect()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp),
                contentAlignment = Alignment.Center
            ) {
                // Orbit Lines
                Canvas(modifier = Modifier.size(240.dp).alpha(0.1f)) {
                    drawCircle(color = Color(0xFF13B28A), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                }

                // Center Card (Fintech/SaaS Dashboard style)
                Surface(
                    modifier = Modifier
                        .size(160.dp, 200.dp)
                        .offset(y = floatAnim.dp)
                        .scale(0.95f),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    shadowElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.White)
                            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Payments, null, tint = Color(0xFF13B28A), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.width(60.dp).height(8.dp).clip(CircleShape).background(Color(0xFFF1F5F9)))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(Color(0xFFF1F5F9)))
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).clip(CircleShape).background(Color(0xFFF1F5F9)))

                            Spacer(modifier = Modifier.weight(1f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                repeat(4) { i ->
                                    Box(modifier = Modifier.width(14.dp).height((30 + i * 15).dp).clip(RoundedCornerShape(4.dp)).background(if(i==2) Color(0xFF13B28A) else Color(0xFFF1F5F9)))
                                }
                            }
                        }
                    }
                }

                // Orbiting Icons
                val icons = listOf(Icons.Default.AccountBalanceWallet, Icons.Default.Description, Icons.Default.CheckCircle, Icons.Default.TrendingUp)
                val radius = 120f
                icons.forEachIndexed { index, icon ->
                    val angle = rotationAnim + (index * (360f / icons.size))
                    val angleRad = Math.toRadians(angle.toDouble())
                    val x = (radius * cos(angleRad)).toFloat()
                    val y = (radius * sin(angleRad)).toFloat()

                    Box(
                        modifier = Modifier
                            .offset(x = x.dp, y = y.dp)
                            .size(40.dp)
                            .scale(pulseAnim)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape)
                            .shadow(8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.05f), spotColor = Color.Black.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = if(index%2==0) Color(0xFF13B28A) else Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Bottom Loading Section
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                modifier = Modifier
                    .width(140.dp)
                    .height(3.dp)
                    .clip(CircleShape),
                color = Color(0xFF13B28A),
                trackColor = Color(0xFFF1F5F9)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Preparing Workspace...",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun SplashParticlesEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAnim"
    )

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.3f).blur(60.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF13B28A).copy(alpha = 0.08f), Color.Transparent),
                center = center.copy(x = center.x * (0.2f + animValue * 0.8f), y = center.y * (0.1f + animValue * 0.5f)),
                radius = size.minDimension * 0.6f
            )
        )
    }
}

private val SineOverShootEasing = Easing { f ->
    val t = f - 1.0f
    t * t * ((2.0f + 1.0f) * t + 2.0f) + 1.0f
}
