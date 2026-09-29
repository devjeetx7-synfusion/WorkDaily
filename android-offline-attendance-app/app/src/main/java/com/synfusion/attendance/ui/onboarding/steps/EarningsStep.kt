package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
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
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.utils.AmountFormatter

@Composable
fun EarningsStep(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_earnings_title),
        description = stringResource(R.string.onboarding_earnings_desc),
        ctaText = stringResource(R.string.onboarding_next),
        backText = stringResource(R.string.onboarding_back),
        showBack = true,
        onBackClick = onBack,
        onCtaClick = onNext
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(size = 46.dp, xOffset = (-122).dp, yOffset = 92.dp, animationDelay = 160)
            FloatingWorker(size = 58.dp, xOffset = 116.dp, yOffset = (-72).dp, animationDelay = 520)
            FloatingWorker(size = 38.dp, xOffset = (-112).dp, yOffset = (-88).dp, animationDelay = 760)

            Surface(
                modifier = Modifier
                    .offset(y = (-22).dp)
                    .widthIn(max = 248.dp)
                    .fillMaxWidth(0.72f)
                    .height(86.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.06f)
                    ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.onboarding_mock_daily_earnings, AmountFormatter.formatWithSymbol(700.0, defaultCurrency)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .width(74.dp)
                                .height(6.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(y = 38.dp)
                    .widthIn(max = 284.dp)
                    .fillMaxWidth(0.82f)
                    .height(152.dp)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    ),
                shape = RoundedCornerShape(28.dp),
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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                            )
                        }
                    }

                    Text(
                        text = AmountFormatter.formatWithSymbol(18200.0, defaultCurrency),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        listOf(0.34f, 0.52f, 0.45f, 0.72f, 0.64f, 0.88f, 0.76f).forEach { fraction ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((34 * fraction).dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.28f + (fraction * 0.35f)),
                                        RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}
