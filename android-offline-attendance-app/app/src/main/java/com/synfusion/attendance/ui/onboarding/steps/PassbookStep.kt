package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.utils.AmountFormatter

@Composable
fun PassbookStep() {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val depth by animateFloatAsState(
        targetValue = if (entered) 1f else 0.90f,
        animationSpec = tween(560),
        label = "passbookDepth"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_passbook_title),
        description = stringResource(R.string.onboarding_passbook_desc)
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
                yOffset = (-82).dp,
                animationDelay = 200,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                size = 56.dp,
                xOffset = 130.dp,
                yOffset = 92.dp,
                animationDelay = 620,
                floatRange = 6.dp,
                horizontalRange = 4.dp
            )

            Surface(
                modifier = Modifier
                    .offset(x = (-28).dp, y = (-28).dp)
                    .widthIn(max = 246.dp)
                    .fillMaxWidth(0.70f)
                    .height(146.dp)
                    .graphicsLayer {
                        rotationZ = -7f
                        scaleX = depth
                        scaleY = depth
                    },
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
            ) {}

            Surface(
                modifier = Modifier
                    .offset(x = 24.dp, y = (-8).dp)
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.75f)
                    .height(154.dp)
                    .graphicsLayer {
                        rotationZ = 5f
                        scaleX = depth
                        scaleY = depth
                    },
                shape = RoundedCornerShape(27.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.58f)
            ) {}

            Surface(
                modifier = Modifier
                    .offset(y = 28.dp)
                    .widthIn(max = 292.dp)
                    .fillMaxWidth(0.84f)
                    .height(194.dp)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .graphicsLayer {
                        scaleX = 0.95f + ((depth - 0.90f) * 0.5f)
                        scaleY = 0.95f + ((depth - 0.90f) * 0.5f)
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
                                    imageVector = Icons.Default.ListAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.onboarding_mock_passbook),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    PassbookVisualRow(
                        label = stringResource(R.string.onboarding_mock_attendance),
                        value = stringResource(R.string.onboarding_mock_26_days)
                    )
                    PassbookVisualRow(
                        label = stringResource(R.string.onboarding_mock_advances),
                        value = AmountFormatter.formatWithSymbol(2000.0, defaultCurrency)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    PassbookVisualRow(
                        label = stringResource(R.string.onboarding_mock_balance),
                        value = AmountFormatter.formatWithSymbol(16200.0, defaultCurrency),
                        highlighted = true
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 112.dp, y = (-94).dp)
                    .size(50.dp)
                    .shadow(8.dp, RoundedCornerShape(17.dp))
                    .graphicsLayer { rotationZ = 8f },
                shape = RoundedCornerShape(17.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PassbookVisualRow(
    label: String,
    value: String,
    highlighted: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.62f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                fontWeight = if (highlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
