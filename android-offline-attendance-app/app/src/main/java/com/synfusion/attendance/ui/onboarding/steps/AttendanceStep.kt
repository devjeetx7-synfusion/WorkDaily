package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
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
import com.synfusion.attendance.ui.onboarding.components.WorkerRole
import com.synfusion.attendance.ui.theme.MintPrimary

@Composable
fun AttendanceStep() {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val checkScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.35f,
        animationSpec = tween(durationMillis = 520, delayMillis = 180),
        label = "attendanceCheckScale"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.94f,
        animationSpec = tween(480),
        label = "attendanceCardScale"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_attendance_title),
        description = stringResource(R.string.onboarding_attendance_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 350.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                role = WorkerRole.LABOUR,
                size = 52.dp,
                xOffset = (-134).dp,
                yOffset = 96.dp,
                animationDelay = 180,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.CONTRACTOR,
                size = 54.dp,
                xOffset = 132.dp,
                yOffset = (-92).dp,
                animationDelay = 620,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .widthIn(max = 292.dp)
                    .fillMaxWidth(0.84f)
                    .height(210.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    )
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                    },
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MintPrimary.copy(alpha = 0.11f), RoundedCornerShape(13.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DateRange, null, tint = MintPrimary, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.onboarding_mock_today),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MintPrimary.copy(alpha = 0.09f)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_mock_26_days),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        repeat(7) { index ->
                            Surface(
                                modifier = Modifier.weight(1f).aspectRatio(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    index == 4 -> MintPrimary
                                    index < 4 -> MintPrimary.copy(alpha = 0.11f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (index == 4) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    } else {
                                        Box(
                                            Modifier.size(5.dp).background(
                                                if (index < 4) MintPrimary else MaterialTheme.colorScheme.outline,
                                                CircleShape
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MintPrimary.copy(alpha = 0.06f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .graphicsLayer {
                                        scaleX = checkScale
                                        scaleY = checkScale
                                    }
                                    .background(MintPrimary, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = stringResource(R.string.onboarding_mock_present),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = (-118).dp, y = (-96).dp)
                    .shadow(7.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MintPrimary.copy(alpha = 0.10f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.padding(11.dp).size(20.dp)
                )
            }
        }
    }
}
