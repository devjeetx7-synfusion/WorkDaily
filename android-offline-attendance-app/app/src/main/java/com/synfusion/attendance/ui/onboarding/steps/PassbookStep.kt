package com.synfusion.attendance.ui.onboarding.steps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.data.model.CountryRepository
import com.synfusion.attendance.ui.onboarding.components.FloatingWorker
import com.synfusion.attendance.ui.onboarding.components.OnboardingSlideContent
import com.synfusion.attendance.ui.onboarding.components.WorkerRole
import com.synfusion.attendance.ui.theme.MintPrimary
import com.synfusion.attendance.utils.AmountFormatter

@Composable
fun PassbookStep() {
    val defaultCurrency = CountryRepository.DEFAULT_COUNTRY.currencyCode
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val depth by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = tween(560),
        label = "passbookDepth"
    )

    val chipMotion = rememberInfiniteTransition(label = "passbookChipMotion")
    val chipY by chipMotion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "passbookChipY"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_passbook_title),
        description = stringResource(R.string.onboarding_passbook_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 282.dp, max = 370.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                role = WorkerRole.ELECTRICIAN,
                size = 52.dp,
                xOffset = (-136).dp,
                yOffset = (-88).dp,
                animationDelay = 200,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.LABOUR,
                size = 58.dp,
                xOffset = 136.dp,
                yOffset = 108.dp,
                animationDelay = 620,
                floatRange = 6.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .offset(x = (-26).dp, y = (-34).dp)
                    .widthIn(max = 252.dp)
                    .fillMaxWidth(0.72f)
                    .height(154.dp)
                    .graphicsLayer {
                        rotationZ = -6f
                        scaleX = depth
                        scaleY = depth
                    },
                shape = RoundedCornerShape(26.dp),
                color = MintPrimary.copy(alpha = 0.055f)
            ) {}

            Surface(
                modifier = Modifier
                    .offset(x = 22.dp, y = (-15).dp)
                    .widthIn(max = 266.dp)
                    .fillMaxWidth(0.77f)
                    .height(162.dp)
                    .graphicsLayer {
                        rotationZ = 4f
                        scaleX = depth
                        scaleY = depth
                    },
                shape = RoundedCornerShape(27.dp),
                color = MintPrimary.copy(alpha = 0.09f)
            ) {}

            Surface(
                modifier = Modifier
                    .offset(y = 33.dp)
                    .widthIn(max = 304.dp)
                    .fillMaxWidth(0.88f)
                    .height(224.dp)
                    .shadow(
                        elevation = 19.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    )
                    .graphicsLayer {
                        scaleX = 0.96f + ((depth - 0.92f) * 0.5f)
                        scaleY = 0.96f + ((depth - 0.92f) * 0.5f)
                    },
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MintPrimary.copy(alpha = 0.11f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ListAlt, null, tint = MintPrimary, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.onboarding_mock_passbook),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    PassbookVisualRow(
                        label = stringResource(R.string.onboarding_mock_attendance),
                        value = stringResource(R.string.onboarding_mock_26_days)
                    )
                    PassbookVisualRow(
                        label = stringResource(R.string.onboarding_mock_advances),
                        value = AmountFormatter.formatWithSymbol(2000.0, defaultCurrency)
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
                    .offset(x = 118.dp, y = (-100 + chipY).dp)
                    .size(48.dp)
                    .shadow(7.dp, RoundedCornerShape(16.dp))
                    .graphicsLayer { rotationZ = 6f },
                shape = RoundedCornerShape(16.dp),
                color = MintPrimary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.ListAlt, null, tint = MintPrimary, modifier = Modifier.size(22.dp))
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (highlighted) MintPrimary.copy(alpha = 0.095f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = if (highlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (highlighted) MintPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
