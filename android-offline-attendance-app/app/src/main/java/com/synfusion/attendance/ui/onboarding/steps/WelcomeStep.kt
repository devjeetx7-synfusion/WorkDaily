package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun WelcomeStep() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(500),
        label = "welcomeStageScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(380),
        label = "welcomeStageAlpha"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 350.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(286.dp, 216.dp)
                    .border(1.dp, MintPrimary.copy(alpha = 0.10f), CircleShape)
            )

            FloatingWorker(
                role = WorkerRole.CONTRACTOR,
                size = 54.dp,
                xOffset = (-128).dp,
                yOffset = (-76).dp,
                animationDelay = 120,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.PLUMBER,
                size = 58.dp,
                xOffset = 126.dp,
                yOffset = 82.dp,
                animationDelay = 520,
                floatRange = 6.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 294.dp)
                    .fillMaxWidth(0.84f)
                    .height(198.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    ),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
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
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(23.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WelcomeFeatureTile(Modifier.weight(1f)) { Icon(Icons.Default.DateRange, null) }
                        WelcomeFeatureTile(Modifier.weight(1f)) { Icon(Icons.Default.AccountBalanceWallet, null) }
                        WelcomeFeatureTile(Modifier.weight(1f)) { Icon(Icons.Default.ListAlt, null) }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MintPrimary.copy(alpha = 0.065f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(9.dp).background(MintPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier.width(92.dp).height(6.dp)
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.13f), RoundedCornerShape(99.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    Modifier.fillMaxWidth(0.66f).height(5.dp)
                                        .background(MintPrimary.copy(alpha = 0.30f), RoundedCornerShape(99.dp))
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 111.dp, y = (-91).dp)
                    .size(50.dp)
                    .shadow(7.dp, RoundedCornerShape(17.dp))
                    .graphicsLayer { rotationZ = 5f },
                shape = RoundedCornerShape(17.dp),
                color = MintPrimary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DateRange, null, tint = MintPrimary, modifier = Modifier.size(23.dp))
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureTile(
    modifier: Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(17.dp),
        color = MintPrimary.copy(alpha = 0.09f),
        contentColor = MintPrimary
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) { icon() }
    }
}
