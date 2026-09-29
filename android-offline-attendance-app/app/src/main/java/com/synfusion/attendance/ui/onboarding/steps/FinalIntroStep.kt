package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.ui.onboarding.components.WorkerRole
import com.synfusion.attendance.ui.theme.MintPrimary

@Composable
fun FinalIntroStep() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val settle by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(620),
        label = "finalSettle"
    )

    val accentMotion = rememberInfiniteTransition(label = "finalAccentMotion")
    val accentY by accentMotion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "finalAccentY"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_final_title),
        description = stringResource(R.string.onboarding_final_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 280.dp, max = 370.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                role = WorkerRole.CONTRACTOR,
                size = 52.dp,
                xOffset = (-136).dp,
                yOffset = (-92).dp,
                animationDelay = 120,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.PLUMBER,
                size = 58.dp,
                xOffset = 138.dp,
                yOffset = (-82).dp,
                animationDelay = 360,
                floatRange = 6.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.MASON,
                size = 54.dp,
                xOffset = (-132).dp,
                yOffset = 112.dp,
                animationDelay = 640,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 292.dp)
                    .fillMaxWidth(0.84f)
                    .height(230.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(34.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .graphicsLayer {
                        scaleX = 0.94f + (settle * 0.06f)
                        scaleY = 0.94f + (settle * 0.06f)
                    },
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MintPrimary.copy(alpha = 0.11f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = MintPrimary, modifier = Modifier.size(23.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 21.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        FinalFeatureRow(Icons.Default.DateRange, 0.66f)
                        FinalFeatureRow(Icons.Default.AccountBalanceWallet, 0.79f)
                        FinalFeatureRow(Icons.Default.ListAlt, 0.58f)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == 1) 8.dp else 6.dp)
                                    .background(
                                        if (index == 1) MintPrimary else MintPrimary.copy(alpha = 0.24f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 118.dp, y = (104 + accentY).dp)
                    .size(50.dp)
                    .shadow(7.dp, RoundedCornerShape(17.dp))
                    .graphicsLayer { rotationZ = -4f },
                shape = RoundedCornerShape(17.dp),
                color = MintPrimary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CheckCircle, null, tint = MintPrimary, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun FinalFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    widthFraction: Float
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        color = MintPrimary.copy(alpha = 0.055f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MintPrimary.copy(alpha = 0.11f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f), RoundedCornerShape(99.dp))
            )
        }
    }
}
