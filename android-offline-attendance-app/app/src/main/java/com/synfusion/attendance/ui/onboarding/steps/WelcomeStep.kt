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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent

@Composable
fun WelcomeStep() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val scale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.93f,
        animationSpec = tween(520),
        label = "welcomeStageScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(420),
        label = "welcomeStageAlpha"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp, max = 360.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(285.dp, 215.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        shape = CircleShape
                    )
            )

            FloatingWorker(
                size = 48.dp,
                xOffset = (-128).dp,
                yOffset = (-76).dp,
                animationDelay = 120,
                floatRange = 5.dp,
                horizontalRange = 4.dp
            )
            FloatingWorker(
                size = 58.dp,
                xOffset = 126.dp,
                yOffset = 72.dp,
                animationDelay = 520,
                floatRange = 6.dp,
                horizontalRange = 4.dp
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 286.dp)
                    .fillMaxWidth(0.82f)
                    .height(198.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .graphicsLayer { rotationZ = -1.2f },
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        RoundedCornerShape(13.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WelcomeFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.DateRange, null) }
                        )
                        WelcomeFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.AccountBalanceWallet, null) }
                        )
                        WelcomeFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.ListAlt, null) }
                        )
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(9.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .width(92.dp)
                                        .height(6.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f),
                                            RoundedCornerShape(99.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.66f)
                                        .height(5.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                            RoundedCornerShape(99.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 104.dp, y = (-91).dp)
                    .size(54.dp)
                    .shadow(8.dp, RoundedCornerShape(18.dp))
                    .graphicsLayer { rotationZ = 7f },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
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
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            icon()
        }
    }
}
