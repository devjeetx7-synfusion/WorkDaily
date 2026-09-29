package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent

@Composable
fun FinalIntroStep() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val settle by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(620),
        label = "finalSettle"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_final_title),
        description = stringResource(R.string.onboarding_final_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp, max = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                size = 45.dp,
                xOffset = (-132).dp,
                yOffset = (-82).dp,
                animationDelay = 120,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                size = 58.dp,
                xOffset = 132.dp,
                yOffset = (-70).dp,
                animationDelay = 360,
                floatRange = 6.dp,
                horizontalRange = 4.dp
            )
            FloatingWorker(
                size = 50.dp,
                xOffset = (-122).dp,
                yOffset = 98.dp,
                animationDelay = 640,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 246.dp)
                    .fillMaxWidth(0.70f)
                    .height(226.dp)
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(34.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.09f)
                    )
                    .graphicsLayer {
                        scaleX = 0.92f + (settle * 0.08f)
                        scaleY = 0.92f + (settle * 0.08f)
                    },
                shape = RoundedCornerShape(34.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
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
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        FinalFeatureRow(Icons.Default.DateRange, 0.74f)
                        FinalFeatureRow(Icons.Default.AccountBalanceWallet, 0.88f)
                        FinalFeatureRow(Icons.Default.ListAlt, 0.66f)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == 1) 8.dp else 6.dp)
                                    .background(
                                        if (index == 1) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 110.dp, y = 92.dp)
                    .size(54.dp)
                    .shadow(9.dp, RoundedCornerShape(18.dp))
                    .graphicsLayer {
                        rotationZ = -8f + (settle * 4f)
                    },
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(25.dp)
                    )
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
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                        RoundedCornerShape(99.dp)
                    )
            )
        }
    }
}
