package com.synfusion.attendance.rewards.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.*
import kotlinx.coroutines.launch
import java.util.Locale

// Centralized Rewards UI Design Tokens (Section 3)

// Primary Purple Palette
val DeepIndigo = Color(0xFF4F35E8)
val RoyalPurple = Color(0xFF6737F5)
val VibrantViolet = Color(0xFF873FFF)
val ElectricPurple = Color(0xFFA64CFF)
val HighlightLavender = Color(0xFFCA7BFF)

// Primary Hero Gradients
val RewardsPrimaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF4F35E8), Color(0xFF873FFF), Color(0xFFBD5CFA))
)
val RewardsSecondaryGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF5135E9), Color(0xFF8744F6), Color(0xFFBC65FC))
)
val RewardsReferralGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFF8DFFF), Color(0xFFFFF4FD), Color(0xFFF0E8FF))
)

// Gold Palette
val GoldBright = Color(0xFFFFD54A)
val GoldPrimary = Color(0xFFFFBE0B)
val GoldRich = Color(0xFFF59E0B)
val GoldDeep = Color(0xFFE88A08)

val RewardsGoldGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFD54A), Color(0xFFFFBE0B), Color(0xFFF59E0B))
)
val RewardsSilverGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFF1F5F9), Color(0xFFCBD5E1), Color(0xFF94A3B8))
)
val RewardsBronzeGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFED7AA), Color(0xFFF97316), Color(0xFFC2410C))
)

// Surfaces
val RewardsScreenBg = Color(0xFFF8F9FD)
val RewardsCardBg = Color(0xFFFFFFFF)
val RewardsLavenderSurface = Color(0xFFF4EBFF)
val RewardsSoftPurple = Color(0xFFEEE5FF)
val RewardsSoftPink = Color(0xFFFFEAFB)
val RewardsGoldSurface = Color(0xFFFFF8DE)

// Text Colors
val RewardsTextPrimary = Color(0xFF11152F)
val RewardsTextSecondary = Color(0xFF647086)
val RewardsPurpleAccent = Color(0xFF8745F5)

// Status & Indicators
val RewardsSuccessGreen = Color(0xFF07994C)
val RewardsSuccessBg = Color(0xFFE2F8EA)
val StatusOrange = Color(0xFFE88A08)
val StatusRed = Color(0xFFDC2626)

// Legacy alias compatibility
val RewardsPurple = Color(0xFF873FFF)
val RewardsPurpleDark = Color(0xFF4F35E8)
val RewardsPurpleContainer = Color(0xFFF4EBFF)
val RewardsGold = Color(0xFFFFBE0B)
val RewardsGoldDark = Color(0xFFE88A08)
val RewardsGoldContainer = Color(0xFFFFF8DE)
val StatusGreen = RewardsSuccessGreen
val NeutralSlate = RewardsTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsTopAppBar(
    title: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = modifier
    )
}

@Composable
fun RewardCoinBalance(
    amount: Int,
    fontSize: Int = 18,
    textColor: Color = MaterialTheme.colorScheme.onBackground,
    showSign: Boolean = false,
    coinSize: Dp = (fontSize * 1.1).dp,
    modifier: Modifier = Modifier
) {
    val displayStr = if (showSign && amount > 0) "+${String.format(Locale.getDefault(), "%,d", amount)}"
    else String.format(Locale.getDefault(), "%,d", amount)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        Image(
            painter = painterResource(id = R.drawable.workdaily_coin),
            contentDescription = "WorkDaily Coin",
            modifier = Modifier.size(coinSize),
            contentScale = ContentScale.Fit
        )
        Text(
            text = displayStr,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}

@Composable
fun RewardStatusChip(
    statusText: String,
    statusColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(statusColor.copy(alpha = 0.12f))
            .border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = statusText.uppercase(Locale.getDefault()),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor
        )
    }
}

@Composable
fun RewardsSectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = RewardsPurpleAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onActionClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun RewardPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = DeepIndigo,
    contentColor: Color = Color.White,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        interactionSource = interactionSource
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun RewardSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = RoyalPurple,
    contentColor: Color = RoyalPurple,
    containerColor: Color = Color.White
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        interactionSource = interactionSource
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun RewardProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = GoldPrimary,
    trackColor: Color = Color.White.copy(alpha = 0.25f)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "progressBar"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
        color = color,
        trackColor = trackColor
    )
}

@Composable
fun RewardSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) RewardsSecondaryGradient else Brush.linearGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            )
                        )
                        .clickable { onOptionSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else RewardsTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RewardPillTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) RoyalPurple else RewardsLavenderSurface)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else RewardsTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RewardTabRow(
    tabs: List<String>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    TabRow(
        selectedTabIndex = pagerState.currentPage,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = RoyalPurple,
        divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)) },
        indicator = { tabPositions ->
            if (tabPositions.isNotEmpty()) {
                val currentTab = pagerState.currentPage.coerceIn(0, tabPositions.size - 1)
                val fractionalOffset = pagerState.currentPageOffsetFraction
                val targetTab = if (fractionalOffset > 0) {
                    (currentTab + 1).coerceAtMost(tabPositions.size - 1)
                } else {
                    (currentTab - 1).coerceAtLeast(0)
                }

                val currentPos = tabPositions[currentTab]
                val targetPos = tabPositions[targetTab]

                val left = androidx.compose.ui.unit.lerp(
                    currentPos.left,
                    targetPos.left,
                    fractionalOffset.coerceIn(-1f, 1f).let { if (it < 0) -it else it }
                )
                val width = androidx.compose.ui.unit.lerp(
                    currentPos.width,
                    targetPos.width,
                    fractionalOffset.coerceIn(-1f, 1f).let { if (it < 0) -it else it }
                )

                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.BottomStart)
                        .offset(x = left)
                        .width(width)
                        .height(3.dp)
                        .background(color = RoyalPurple, shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                )
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = pagerState.currentPage == index
            Tab(
                selected = isSelected,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                },
                text = {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) RoyalPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun RewardIllustration(
    drawableRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit
) {
    Image(
        painter = painterResource(id = drawableRes),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}

@Composable
fun RewardEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RewardIllustration(
            drawableRes = R.drawable.empty_rewards,
            contentDescription = "Empty Rewards",
            modifier = Modifier.size(160.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun RewardTransactionRow(
    transaction: RewardTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (transaction.status) {
        RewardTransactionStatus.CONFIRMED, RewardTransactionStatus.COMPLETED -> RewardsSuccessGreen
        RewardTransactionStatus.PENDING -> StatusOrange
        RewardTransactionStatus.REVERSED, RewardTransactionStatus.REJECTED -> StatusRed
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    val icon = if (transaction.amount >= 0) Icons.Default.AddCircleOutline else Icons.Default.RemoveCircleOutline
                    Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.timestamp,
                        fontSize = 12.sp,
                        color = RewardsTextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                horizontalAlignment = Alignment.End
            ) {
                RewardCoinBalance(
                    amount = transaction.amount,
                    fontSize = 14,
                    textColor = if (transaction.amount < 0) StatusRed else statusColor,
                    showSign = true,
                    coinSize = 16.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                RewardStatusChip(
                    statusText = transaction.status.name,
                    statusColor = statusColor
                )
            }
        }
    }
}

@Composable
fun RewardBadgeCard(
    achievement: Achievement,
    drawableRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnlocked = achievement.isUnlocked
    val cardBg = if (isUnlocked) RewardsGoldSurface else RewardsLavenderSurface
    val border = if (isUnlocked) {
        androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.5f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, RewardsTextSecondary.copy(alpha = 0.15f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = border,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnlocked) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                val colorFilter = if (!isUnlocked) {
                    ColorFilter.colorMatrix(ColorMatrix().apply {
                        setToSaturation(0f)
                    })
                } else null

                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = achievement.title,
                    modifier = Modifier.size(115.dp),
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Fit
                )

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF647086),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = achievement.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = RewardsTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (isUnlocked) {
                RewardStatusChip(statusText = "UNLOCKED", statusColor = RewardsSuccessGreen)
            } else {
                Text(
                    text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                RewardProgressBar(
                    progress = achievement.currentProgress.toFloat() / achievement.maxProgress.toFloat(),
                    color = RoyalPurple,
                    trackColor = RewardsTextSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
            }
        }
    }
}
