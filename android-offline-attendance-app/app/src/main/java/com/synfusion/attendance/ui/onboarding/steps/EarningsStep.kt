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
import com.synfusion.attendance.ui.onboarding.components.WorkerRole
import com.synfusion.attendance.ui.theme.MintPrimary
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

    val chipMotion = rememberInfiniteTransition(label = "earningsChipMotion")
    val chipY by chipMotion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "earningsChipY"
    )

    OnboardingSlideContent(
        title = stringResource(R.string.onboarding_earnings_title),
        description = stringResource(R.string.onboarding_earnings_desc)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 270.dp, max = 360.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingWorker(
                role = WorkerRole.PLUMBER,
                size = 50.dp,
                xOffset = (-136).dp,
                yOffset = 104.dp,
                animationDelay = 160,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )
            FloatingWorker(
                role = WorkerRole.ELECTRICIAN,
                size = 54.dp,
                xOffset = 134.dp,
                yOffset = (-96).dp,
                animationDelay = 560,
                floatRange = 5.dp,
                horizontalRange = 3.dp
            )

            Surface(
                modifier = Modifier
                    .offset(x = (-32).dp, y = (-72 + chipY).dp)
                    .widthIn(max = 210.dp)
                    .fillMaxWidth(0.62f)
                    .height(58.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp))
                    .graphicsLayer {
                        rotationZ = -3f
                        alpha = 0.76f + (reveal * 0.24f)
                    },
                shape = RoundedCornerShape(20.dp),
                color = MintPrimary.copy(alpha = 0.085f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.78f), RoundedCornerShape(13.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Text(
                        text = stringResource(
                            R.string.onboarding_mock_daily_earnings,
                            AmountFormatter.formatWithSymbol(700.0, defaultCurrency)
                        ),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .offset(y = 30.dp)
                    .widthIn(max = 298.dp)
                    .fillMaxWidth(0.86f)
                    .height(182.dp)
                    .shadow(
                        elevation = 19.dp,
                        shape = RoundedCornerShape(30.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.07f)
                    )
                    .graphicsLayer {
                        scaleX = 0.95f + (reveal * 0.05f)
                        scaleY = 0.95f + (reveal * 0.05f)
                    },
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 19.dp, vertical = 18.dp),
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
                        Box(Modifier.size(9.dp).background(MintPrimary, CircleShape))
                    }

                    Text(
                        text = AmountFormatter.formatWithSymbol(18200.0, defaultCurrency),
                        fontSize = 29.sp,
                        lineHeight = 34.sp,
                        maxLines = 1,
                        softWrap = false,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
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
                                        MintPrimary.copy(alpha = 0.18f + (index * 0.055f)),
                                        RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp)
                                    )
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .offset(x = 118.dp, y = (104 + chipY).dp)
                    .size(48.dp)
                    .shadow(7.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = MintPrimary.copy(alpha = 0.10f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountBalanceWallet, null, tint = MintPrimary, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
