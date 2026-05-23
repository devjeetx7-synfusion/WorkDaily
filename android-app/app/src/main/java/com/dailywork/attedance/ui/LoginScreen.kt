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
import androidx.compose.foundation.interaction.MutableInteractionSource
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
            .background(Color(0xFFF8FAFB)) // SaaS clean white/grey background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TOP AREA: Clean Empty Spacing
            Spacer(modifier = Modifier.height(48.dp))

            // CENTER AREA: SaaS Feature Showcase
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                SaaSFeatureShowcase()
            }

            // BOTTOM AREA: Google Sign-In
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .padding(bottom = 48.dp),
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
                            .padding(bottom = 20.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f))
                            .padding(vertical = 12.dp, horizontal = 16.dp)
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

                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Professional Workforce Management",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF94A3B8), // Slate 400
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SaaSFeatureShowcase() {
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
            .height(450.dp),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow (very subtle mint)
        Box(
            modifier = Modifier
                .size(300.dp)
                .alpha(0.3f)
                .blur(100.dp)
                .background(Color(0xFF13B28A).copy(alpha = 0.1f), CircleShape)
        )

        AnimatedContent(
            targetState = currentState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(700, easing = LinearOutSlowInEasing)) + scaleIn(initialScale = 0.95f, animationSpec = tween(700)))
                    .togetherWith(fadeOut(animationSpec = tween(500)) + scaleOut(targetScale = 1.02f, animationSpec = tween(500)))
            },
            label = "saas_feature_transition"
        ) { state ->
            when (state) {
                0 -> PayoutsWidget()
                1 -> AttendanceStatsWidget()
                2 -> WorkerAnalyticsWidget()
                3 -> PdfReportWidget()
                4 -> AdvanceTrackWidget()
                5 -> ActiveWorkersWidget()
            }
        }
    }
}

@Composable
fun SaaSWidget(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y"
    )

    Surface(
        modifier = modifier
            .graphicsLayer { this.translationY = translationY }
            .width(280.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        shadowElevation = 0.dp // Using custom soft shadow below
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(28.dp)) // Slate 100 border
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(28.dp),
                    clip = false,
                    spotColor = Color(0xFF000000).copy(alpha = 0.03f),
                    ambientColor = Color(0xFF000000).copy(alpha = 0.03f)
                )
                .padding(24.dp)
        ) {
            content()
        }
    }
}

@Composable
fun PayoutsWidget() {
    SaaSWidget {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF0FDF4)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Payments, null, tint = Color(0xFF13B28A), modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("Total Payouts", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(24.dp))
            CountingText("₹ 1,28,450", MaterialTheme.typography.headlineMedium, Color(0xFF1E293B))
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color(0xFF13B28A).copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.TrendingUp, null, tint = Color(0xFF13B28A), modifier = Modifier.size(10.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("12% increase from last month", style = MaterialTheme.typography.labelSmall, color = Color(0xFF13B28A), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AttendanceStatsWidget() {
    SaaSWidget {
        Column {
            Text("Attendance Stats", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.6f, 0.8f, 0.75f)
                heights.forEachIndexed { i, h ->
                    val animatedHeight = animateFloatAsState(targetValue = h, animationSpec = tween(1000, delayMillis = i * 100), label = "bar").value
                    Box(modifier = Modifier.width(18.dp).fillMaxHeight(animatedHeight * 0.5f).clip(RoundedCornerShape(6.dp)).background(if (i == 3) Color(0xFF13B28A) else Color(0xFFF1F5F9)))
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Present", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text("94%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFF1E293B))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Absent", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                    Text("6%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
fun WorkerAnalyticsWidget() {
    SaaSWidget {
        Column {
            Text("Worker Analytics", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            repeat(3) { i ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(listOf("Mistry", "Helper", "Carpenter")[i], style = MaterialTheme.typography.labelMedium, color = Color(0xFF475569), fontWeight = FontWeight.Bold)
                        Text(listOf("85%", "92%", "78%")[i], style = MaterialTheme.typography.labelMedium, color = Color(0xFF13B28A), fontWeight = FontWeight.Black)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFFF1F5F9))) {
                        Box(modifier = Modifier.fillMaxWidth(listOf(0.85f, 0.92f, 0.78f)[i]).fillMaxHeight().clip(CircleShape).background(Color(0xFF13B28A)))
                    }
                }
            }
        }
    }
}

@Composable
fun PdfReportWidget() {
    SaaSWidget {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFEF2F2)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PictureAsPdf, null, tint = Color(0xFFEF4444), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Monthly Passbook", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
            Text("Ready to export", style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AdvanceTrackWidget() {
    SaaSWidget {
        Column {
            Text("Advance Tracking", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFFBEB)).padding(12.dp)) {
                Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFFF59E0B)), contentAlignment = Alignment.Center) {
                    Text("R", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rahul Kumar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    Text("Pending Due", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309))
                }
                Text("₹ 2,400", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black, color = Color(0xFFF59E0B))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Manage credits with ease", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun ActiveWorkersWidget() {
    SaaSWidget {
        Column {
            Text("Active Workers", style = MaterialTheme.typography.titleSmall, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                repeat(5) { i ->
                    val color = listOf(Color(0xFF3B82F6), Color(0xFF10B981), Color(0xFFF59E0B), Color(0xFFEF4444), Color(0xFF8B5CF6))[i]
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape).background(color.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text(listOf("A", "B", "S", "M", "K")[i], color = color, fontWeight = FontWeight.Bold)
                    }
                }
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape).background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                    Text("+12", color = Color(0xFF64748B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("Currently on site", style = MaterialTheme.typography.labelMedium, color = Color(0xFF13B28A), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CountingText(text: String, style: androidx.compose.ui.text.TextStyle, color: Color) {
    // Simple static text for now as animation requires complex state management
    Text(text = text, style = style, color = color, fontWeight = FontWeight.Black)
}

@Composable
fun GoogleSignInButtonPremium(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .pressScaleEffect(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(20.dp),
                    clip = false,
                    spotColor = Color(0xFF000000).copy(alpha = 0.05f),
                    ambientColor = Color(0xFF000000).copy(alpha = 0.05f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = Color(0xFF13B28A)
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
                        color = Color(0xFF1E293B),
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }
    }
}
