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
fun PassbookStep(
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_passbook_title),
        description = stringResource(R.string.onboarding_passbook_desc),
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
            FloatingWorker(size = 42.dp, xOffset = (-122).dp, yOffset = (-86).dp, animationDelay = 220)
            FloatingWorker(size = 54.dp, xOffset = 116.dp, yOffset = 86.dp, animationDelay = 560)
            FloatingWorker(size = 38.dp, xOffset = 116.dp, yOffset = (-98).dp, animationDelay = 780)

            Surface(
                modifier = Modifier
                    .offset(x = (-18).dp, y = (-16).dp)
                    .widthIn(max = 250.dp)
                    .fillMaxWidth(0.72f)
                    .height(144.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(24.dp),
                        ambientColor = Color.Black.copy(alpha = 0.03f),
                        spotColor = Color.Black.copy(alpha = 0.05f)
                    ),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f)
            ) {}

            Surface(
                modifier = Modifier
                    .offset(x = 14.dp, y = 18.dp)
                    .widthIn(max = 284.dp)
                    .fillMaxWidth(0.82f)
                    .height(184.dp)
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ListAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(21.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.onboarding_mock_passbook),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(99.dp))
                        )
                    }

                    PassbookMockRow(
                        label = stringResource(R.string.onboarding_mock_attendance),
                        value = stringResource(R.string.onboarding_mock_26_days)
                    )
                    PassbookMockRow(
                        label = stringResource(R.string.onboarding_mock_advances),
                        value = AmountFormatter.formatWithSymbol(2000.0, defaultCurrency)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    PassbookMockRow(
                        label = stringResource(R.string.onboarding_mock_balance),
                        value = AmountFormatter.formatWithSymbol(16200.0, defaultCurrency),
                        isBold = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PassbookMockRow(
    label: String,
    value: String,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isBold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
