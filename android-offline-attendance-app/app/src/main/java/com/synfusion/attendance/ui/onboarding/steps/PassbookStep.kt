package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.utils.AmountFormatter

@Composable
fun PassbookStep(onNext: () -> Unit) {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode
    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_passbook_title),
        description = stringResource(R.string.onboarding_passbook_desc),
        ctaText = stringResource(R.string.onboarding_next),
        onCtaClick = onNext
    ) {
        Box(contentAlignment = Alignment.Center) {
            FloatingWorker(size = 45.dp, xOffset = (-100).dp, yOffset = (-60).dp, animationDelay = 300)
            FloatingWorker(size = 55.dp, xOffset = 90.dp, yOffset = 70.dp, animationDelay = 600)

            Surface(
                modifier = Modifier
                    .width(220.dp)
                    .height(160.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color.Black.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ListAlt, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.onboarding_mock_passbook), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Mock rows
                    PassbookMockRow(stringResource(R.string.onboarding_mock_attendance), stringResource(R.string.onboarding_mock_26_days))
                    Spacer(modifier = Modifier.height(8.dp))
                    PassbookMockRow(stringResource(R.string.onboarding_mock_advances), AmountFormatter.formatWithSymbol(2000.0, defaultCurrency))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    Spacer(modifier = Modifier.height(8.dp))
                    PassbookMockRow(stringResource(R.string.onboarding_mock_balance), AmountFormatter.formatWithSymbol(16200.0, defaultCurrency), isBold = true)
                }
            }
        }
    }
}

@Composable
private fun PassbookMockRow(label: String, value: String, isBold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium, color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
