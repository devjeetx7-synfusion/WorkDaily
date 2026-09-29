package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent

@Composable
fun FinalIntroStep(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_final_title),
        description = stringResource(R.string.onboarding_final_desc),
        ctaText = stringResource(R.string.onboarding_get_started),
        backText = stringResource(R.string.onboarding_back),
        showBack = true,
        onBackClick = onBack,
        onCtaClick = onComplete
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(size = 42.dp, xOffset = (-126).dp, yOffset = (-88).dp, animationDelay = 120)
            FloatingWorker(size = 54.dp, xOffset = 120.dp, yOffset = (-76).dp, animationDelay = 340)
            FloatingWorker(size = 46.dp, xOffset = (-112).dp, yOffset = 98.dp, animationDelay = 580)
            FloatingWorker(size = 58.dp, xOffset = 122.dp, yOffset = 92.dp, animationDelay = 820)

            Surface(
                modifier = Modifier
                    .widthIn(max = 286.dp)
                    .fillMaxWidth(0.82f)
                    .height(202.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.09f)
                    ),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WorkDaily",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FinalFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        FinalFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                        FinalFeatureTile(
                            modifier = Modifier.weight(1f),
                            icon = { Icon(Icons.Default.ListAlt, null, tint = MaterialTheme.colorScheme.primary) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        repeat(3) { index ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .size(if (index == 1) 8.dp else 6.dp)
                                    .background(
                                        if (index == 1) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                        RoundedCornerShape(99.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinalFeatureTile(
    modifier: Modifier,
    icon: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.height(66.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            icon()
        }
    }
}
