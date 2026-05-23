package com.dailywork.attedance.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dailywork.attedance.R
import com.dailywork.attedance.viewmodel.AuthViewModel
import com.dailywork.attedance.viewmodel.LoginState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation

fun Modifier.pressScaleEffect() = composed {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "pressScale")

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    selectedLanguage: String,
    onLoginSuccess: () -> Unit
) {
    val loginState by authViewModel.loginState.collectAsState()
    val isProcessing = loginState is LoginState.Loading
    val context = LocalContext.current

    val googleText = when (selectedLanguage) {
        "hi" -> "Google के साथ जारी रखें"
        "bn" -> "Google এর সাথে চালিয়ে যান"
        "mr" -> "Google सह सुरू ठेवा"
        "ta" -> "Google உடன் தொடரவும்"
        "te" -> "Googleతో కొనసాగించండి"
        else -> "Continue with Google"
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { token ->
                authViewModel.loginWithGoogleCredential(token)
            } ?: run {
                authViewModel.setLoginError("Google Sign-In failed: No ID Token found")
            }
        } catch (e: ApiException) {
            val errorMessage = when (e.statusCode) {
                12501 -> "Sign-in cancelled"
                7 -> "Network Error"
                else -> "Google Sign-In failed: ${e.message}"
            }
            authViewModel.setLoginError(errorMessage)
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium Background Decor
        PremiumBackgroundEffect()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP: Branding
            Spacer(modifier = Modifier.height(32.dp))
            BrandingSection()

            // MIDDLE: Premium Feature Showcase
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                PremiumFeatureShowcase()
            }

            // BOTTOM: Google Sign-In
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loginState is LoginState.Error) {
                    Text(
                        text = (loginState as LoginState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(vertical = 10.dp, horizontal = 16.dp)
                    )
                }

                GoogleSignInButtonPremium(
                    text = googleText,
                    isLoading = isProcessing,
                    onClick = {
                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(context.getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        launcher.launch(googleSignInClient.signInIntent)
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Trusted by 10,000+ Contractors",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PremiumBackgroundEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_particles")
    val animValue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgAnim"
    )

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize().alpha(0.4f).blur(80.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF13B28A).copy(alpha = 0.15f), Color.Transparent),
                center = center.copy(
                    x = center.x * (0.3f + animValue * 0.7f),
                    y = center.y * (0.2f + animValue * 0.4f)
                ),
                radius = size.minDimension * 0.8f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.1f), Color.Transparent),
                center = center.copy(
                    x = center.x * (1.2f - animValue * 0.5f),
                    y = center.y * (1.1f + animValue * 0.3f)
                ),
                radius = size.minDimension
            )
        )
    }
}

@Composable
fun BrandingSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BusinessCenter,
                contentDescription = "Logo",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "DailyWork",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun PremiumFeatureShowcase() {
    var currentState by remember { mutableIntStateOf(0) }
    val totalStates = 6

    LaunchedEffect(Unit) {
        while (true) {
            delay(3500)
            currentState = (currentState + 1) % totalStates
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600, delayMillis = 100)) + scaleIn(initialScale = 0.92f, animationSpec = tween(600, delayMillis = 100)))
                    .togetherWith(fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 1.05f, animationSpec = tween(500)))
            },
            label = "feature_transition"
        ) { state ->
            when (state) {
                0 -> AttendanceVisual()
                1 -> PaymentVisual()
                2 -> ReportsVisual()
                3 -> AdvanceVisual()
                4 -> HistoryVisual()
                5 -> AnalyticsVisual()
            }
        }

        // Bottom Text for current state
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val title = when (currentState) {
                0 -> "Attendance Tracking"
                1 -> "Worker Payments"
                2 -> "PDF Reports"
                3 -> "Advance Tracking"
                4 -> "Worker History"
                else -> "Analytics Dashboard"
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manage your business like a pro",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun FloatingWidget(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 3000,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis, delayMillis = delayMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.translationY = translationY
        }
    ) {
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.6f))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = Color.Black.copy(alpha = 0.05f))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun AttendanceVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        FloatingWidget(durationMillis = 2500) {
            GlassCard(modifier = Modifier.size(200.dp, 240.dp)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        repeat(3) {
                            Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)))
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    repeat(3) { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            repeat(3) { col ->
                                val isChecked = (row + col) % 2 == 0
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp).size(40.dp).clip(RoundedCornerShape(10.dp))
                                        .background(if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isChecked) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingWidget(modifier = Modifier.align(Alignment.BottomEnd).offset(x = 10.dp, y = (-40).dp), delayMillis = 500) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

@Composable
fun PaymentVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        FloatingWidget {
            GlassCard(modifier = Modifier.width(260.dp)) {
                Column {
                    Text("Total Payouts", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("₹ 45,800", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF13B28A))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), CircleShape)) {
                        Box(modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight().background(Color(0xFF13B28A), CircleShape))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Mistry: ₹ 28k", style = MaterialTheme.typography.labelSmall)
                        Text("Helpers: ₹ 17k", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        FloatingWidget(modifier = Modifier.align(Alignment.TopEnd).offset(x = 20.dp, y = 20.dp), delayMillis = 800) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFF13B28A)).padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Payments, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun ReportsVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        FloatingWidget(durationMillis = 3500) {
            GlassCard(modifier = Modifier.size(180.dp, 240.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    repeat(5) {
                        Box(modifier = Modifier.fillMaxWidth().height(6.dp).padding(vertical = 4.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEF4444).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text("EXPORT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                    }
                }
            }
        }

        FloatingWidget(modifier = Modifier.align(Alignment.BottomStart).offset(x = (-20).dp, y = (-60).dp), delayMillis = 1000) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun AdvanceVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        FloatingWidget {
            GlassCard(modifier = Modifier.width(220.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF59E0B).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Advance Paid", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("₹ 1,200", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
                    Text("Pending to Rahul S.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun HistoryVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val names = listOf("Amit Kumar", "Suresh Singh", "Priya Das")
            names.forEachIndexed { i, name ->
                FloatingWidget(delayMillis = i * 400) {
                    GlassCard(modifier = Modifier.width(240.dp).padding(vertical = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                Text(name.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                Text("Last seen: 2h ago", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsVisual() {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        FloatingWidget {
            GlassCard(modifier = Modifier.size(260.dp, 200.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Productivity", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceEvenly) {
                        val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f)
                        heights.forEach { h ->
                            Box(modifier = Modifier.width(24.dp).fillMaxHeight(h).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoogleSignInButtonPremium(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val glowColor = Color(0xFF13B28A).copy(alpha = 0.15f)

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .pressScaleEffect()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(18.dp),
                spotColor = glowColor,
                ambientColor = glowColor
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
