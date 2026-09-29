package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlinx.coroutines.delay

@Composable
fun AttendanceStep(onNext: () -> Unit) {
    var checkVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while(true) {
            checkVisible = false
            delay(800)
            checkVisible = true
            delay(2000)
        }
    }

    val checkScale by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_attendance_title),
        description = stringResource(R.string.onboarding_attendance_desc),
        ctaText = stringResource(R.string.onboarding_next),
        onCtaClick = onNext
    ) {
        Box(contentAlignment = Alignment.Center) {
            FloatingWorker(size = 55.dp, xOffset = (-110).dp, yOffset = (-30).dp, animationDelay = 200)
            FloatingWorker(size = 45.dp, xOffset = 100.dp, yOffset = 60.dp, animationDelay = 400)

            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .height(140.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_mock_today), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    if (checkVisible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (checkVisible) stringResource(R.string.onboarding_mock_present) else stringResource(R.string.onboarding_mock_pending),
                            fontWeight = FontWeight.Medium,
                            color = if (checkVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
