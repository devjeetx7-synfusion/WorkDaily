package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_welcome_title),
        description = stringResource(R.string.onboarding_welcome_desc),
        ctaText = stringResource(R.string.onboarding_next),
        onCtaClick = onNext
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Background workers
            FloatingWorker(size = 50.dp, xOffset = (-100).dp, yOffset = (-80).dp, animationDelay = 100)
            FloatingWorker(size = 65.dp, xOffset = 90.dp, yOffset = (-60).dp, animationDelay = 300)
            FloatingWorker(size = 45.dp, xOffset = (-80).dp, yOffset = 90.dp, animationDelay = 500)
            FloatingWorker(size = 55.dp, xOffset = 110.dp, yOffset = 70.dp, animationDelay = 700)

            // Central Branding
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "WorkDaily",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
