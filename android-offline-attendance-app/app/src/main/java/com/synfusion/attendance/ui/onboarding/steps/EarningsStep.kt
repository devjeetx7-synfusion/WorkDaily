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
fun EarningsStep(onNext: () -> Unit) {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode
    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_earnings_title),
        description = stringResource(R.string.onboarding_earnings_desc),
        ctaText = stringResource(R.string.onboarding_next),
        onCtaClick = onNext
    ) {
        Box(contentAlignment = Alignment.Center) {
            FloatingWorker(size = 50.dp, xOffset = (-90).dp, yOffset = 80.dp, animationDelay = 100)
            FloatingWorker(size = 60.dp, xOffset = 100.dp, yOffset = (-50).dp, animationDelay = 500)

            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .height(150.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_mock_monthly_earnings), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        AmountFormatter.formatWithSymbol(18200.0, defaultCurrency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            stringResource(R.string.onboarding_mock_daily_earnings, AmountFormatter.formatWithSymbol(700.0, defaultCurrency)),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
