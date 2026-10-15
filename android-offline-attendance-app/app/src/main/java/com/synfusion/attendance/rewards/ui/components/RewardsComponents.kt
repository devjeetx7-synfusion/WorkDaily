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

// Centralized Rewards UI Design Tokens & Theme Accessors (Section 3)

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

// Static Fallback Surfaces
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

/**
 * Theme-aware Rewards Design System Accessor.
 * Dynamically resolves surface, background, text, border, and badge colors based on effective dark/light state.
 */
object RewardsTheme {
    val screenBackground: Color
        @Composable get() = MaterialTheme.colorScheme.background

    val cardBackground: Color
        @Composable get() = MaterialTheme.colorScheme.surface

    val elevatedSurface: Color
        @Composable get() = if (isDark) Color(0xFF202640) else Color(0xFFF4F0FF)

    val textPrimary: Color
        @Composable get() = MaterialTheme.colorScheme.onBackground

    val textSecondary: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    val softBorder: Color
        @Composable get() = MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.25f else 0.4f)

    val goldBadgeSurface: Color
        @Composable get() = if (isDark) Color(0xFF2C2210) else Color(0xFFFFF8DE)

    val lockedBadgeSurface: Color
        @Composable get() = if (isDark) Color(0xFF1F1D32) else Color(0xFFF4EBFF)

    val referralBannerGradient: Brush
        @Composable get() = if (isDark) {
            Brush.linearGradient(listOf(Color(0xFF2A1C44), Color(0xFF201833), Color(0xFF331B47)))
        } else {
            Brush.linearGradient(listOf(Color(0xFFF8DFFF), Color(0xFFFFF4FD), Color(0xFFF0E8FF)))
        }

    val isDark: Boolean
        @Composable get() = MaterialTheme.colorScheme.background.red < 0.2f
}

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
                color = RewardsTheme.textPrimary
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = RewardsTheme.textPrimary
                )
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = RewardsTheme.screenBackground),
        modifier = modifier
    )
}

@Composable
fun RewardCoinBalance(
    amount: Int,
    fontSize: Int = 18,
    textColor: Color = RewardsTheme.textPrimary,
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
            color = RewardsTheme.textPrimary
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
    containerColor: Color = RewardsTheme.cardBackground
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
            .background(RewardsTheme.cardBackground)
            .border(1.dp, RewardsTheme.softBorder, RoundedCornerShape(16.dp))
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
                        color = if (isSelected) Color.White else RewardsTheme.textSecondary
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
                    .background(if (isSelected) RoyalPurple else RewardsTheme.elevatedSurface)
                    .clickable { onTabSelected(index) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else RewardsTheme.textSecondary
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
        containerColor = RewardsTheme.cardBackground,
        contentColor = RoyalPurple,
        divider = { HorizontalDivider(color = RewardsTheme.softBorder) },
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
                        color = if (isSelected) RoyalPurple else RewardsTheme.textSecondary,
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
            color = RewardsTheme.textPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            fontSize = 13.sp,
            color = RewardsTheme.textSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

/**
 * Structured Transaction Row with meaningful category icons based on transaction type.
 */
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

    val titleLower = transaction.title.lowercase(Locale.getDefault())
    val (iconVector: ImageVector, iconTint: Color) = when {
        transaction.status == RewardTransactionStatus.PENDING -> Icons.Default.Schedule to StatusOrange
        transaction.type == RewardTransactionType.SPENT || transaction.amount < 0 -> Icons.Default.CardGiftcard to StatusOrange
        titleLower.contains("referral") -> Icons.Default.CardGiftcard to RoyalPurple
        titleLower.contains("task") || titleLower.contains("challenge") -> Icons.Default.TaskAlt to Color(0xFF1976D2)
        titleLower.contains("achievement") || titleLower.contains("badge") -> Icons.Default.MilitaryTech to GoldPrimary
        titleLower.contains("streak") || titleLower.contains("login") || titleLower.contains("attendance") -> Icons.Default.EventAvailable to RewardsSuccessGreen
        titleLower.contains("leaderboard") || titleLower.contains("rank") -> Icons.Default.EmojiEvents to GoldPrimary
        else -> Icons.Default.MonetizationOn to GoldPrimary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
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
                        .background(iconTint.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = transaction.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTheme.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.timestamp,
                        fontSize = 12.sp,
                        color = RewardsTheme.textSecondary
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
    val cardBg = if (isUnlocked) RewardsTheme.goldBadgeSurface else RewardsTheme.lockedBadgeSurface
    val border = if (isUnlocked) {
        androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary.copy(alpha = 0.6f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
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
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(105.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    RewardsRadialGlow(
                        glowColor = GoldBright,
                        alpha = 0.45f,
                        radiusFraction = 0.85f,
                        modifier = Modifier.fillMaxSize()
                    )
                    RewardsSparkleLayer(
                        starColor = GoldBright,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                val colorFilter = if (!isUnlocked) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else null

                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = achievement.title,
                    modifier = Modifier.size(98.dp),
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Fit
                )

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF343653)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RewardsTheme.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 17.sp,
                modifier = Modifier.heightIn(min = 34.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (isUnlocked) {
                RewardStatusChip(statusText = "UNLOCKED", statusColor = RewardsSuccessGreen)
            } else {
                Text(
                    text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                RewardProgressBar(
                    progress = achievement.currentProgress.toFloat() / achievement.maxProgress.toFloat(),
                    color = RoyalPurple,
                    trackColor = RewardsTheme.textSecondary.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
            }
        }
    }
}
