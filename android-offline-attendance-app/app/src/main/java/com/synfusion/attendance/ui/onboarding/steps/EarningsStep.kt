package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.utils.AmountFormatter

@Composable
fun EarningsStep() {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 620),
        label = "earningsReveal"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_earnings_title),
        description = stringResource(R.string.onboarding_earnings_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 250.dp, max = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                size = 46.dp,
                xOffset = (-132).dp,
                yOffset = 84.dp,
                animationDelay = 160,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                size = 58.dp,
                xOffset = 128.dp,
                yOffset = (-82).dp,
                animationDelay = 560,
                floatRange = 6.dp,
                horizontalRange = 4.dp
            )

            Surface(
                modifier = Modifier
                    .offset(y = (-58).dp)
                    .widthIn(max = 238.dp)
                    .fillMaxWidth(0.70f)
                    .height(82.dp)
                    .shadow(9.dp, RoundedCornerShape(22.dp))
                    .graphicsLayer {
                        rotationZ = -4f
                        alpha = 0.72f + (reveal * 0.28f)
                        scaleX = 0.96f + (reveal * 0.04f)
                        scaleY = 0.96f + (reveal * 0.04f)
                    },
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                                RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column {
                        Text(
                            text = stringResource(
                                R.string.onboarding_mock_daily_earnings,
                                AmountFormatter.formatWithSymbol(700.0, defaultCurrency)
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Box(
                            modifier = Modifier
                                .width(82.dp)
                                .height(5.dp)
                                .background(
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.16f),
                                    RoundedCornerShape(99.dp)
                                )
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(y = 31.dp)
                    .widthIn(max = 292.dp)
                    .fillMaxWidth(0.84f)
                    .height(176.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .graphicsLayer {
                        rotationZ = 1.1f
                        scaleX = 0.94f + (reveal * 0.06f)
                        scaleY = 0.94f + (reveal * 0.06f)
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
                        Text(
                            text = stringResource(R.string.onboarding_mock_monthly_earnings),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    Text(
                        text = AmountFormatter.formatWithSymbol(18200.0, defaultCurrency),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(0.30f, 0.48f, 0.40f, 0.67f, 0.58f, 0.86f, 0.73f).forEachIndexed { index, fraction ->
                            val barHeight = 42f * fraction * reveal.coerceAtLeast(0.12f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(barHeight.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.20f + (index * 0.055f)
                                        ),
                                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 112.dp, y = 95.dp)
                    .size(50.dp)
                    .shadow(8.dp, RoundedCornerShape(17.dp)),
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}
