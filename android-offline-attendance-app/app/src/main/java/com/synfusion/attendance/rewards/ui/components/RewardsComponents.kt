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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

// Rewards Design Tokens
val RewardsPurple = Color(0xFF7C4DFF)
val RewardsPurpleDark = Color(0xFF512DA8)
val RewardsPurpleContainer = Color(0xFFF3E8FF)
val RewardsGold = Color(0xFFFFB300)
val RewardsGoldDark = Color(0xFFE68C00)
val RewardsGoldContainer = Color(0xFFFFF8E1)
val StatusGreen = Color(0xFF2E7D32)
val StatusOrange = Color(0xFFEF6C00)
val StatusRed = Color(0xFFC62828)
val NeutralSlate = Color(0xFF64748B)

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
            .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
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
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = RewardsPurple,
                modifier = Modifier.clickable { onActionClick() }
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
    containerColor: Color = RewardsPurple,
    contentColor: Color = Color.White,
    icon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f)
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
    borderColor: Color = RewardsPurple,
    contentColor: Color = RewardsPurple
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "buttonScale"
    )

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = contentColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun RewardProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = RewardsPurple,
    trackColor: Color = color.copy(alpha = 0.15f)
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

// Custom Synchronized Swipeable Tab Row with continuous indicator motion
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
        contentColor = RewardsPurple,
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
                        .background(color = RewardsPurple, shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
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
                        color = if (isSelected) RewardsPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
fun RewardTransactionRow(
    transaction: RewardTransaction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (transaction.status) {
        RewardTransactionStatus.CONFIRMED, RewardTransactionStatus.COMPLETED -> StatusGreen
        RewardTransactionStatus.PENDING -> StatusOrange
        RewardTransactionStatus.REVERSED, RewardTransactionStatus.REJECTED -> StatusRed
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
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
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = if (transaction.amount >= 0) Icons.Default.AddCircleOutline else Icons.Default.RemoveCircleOutline
                Icon(imageVector = icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = transaction.timestamp,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Spacer(modifier = Modifier.height(2.dp))
            RewardStatusChip(
                statusText = transaction.status.name,
                statusColor = statusColor
            )
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
    val cardBg = if (isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    val border = if (isUnlocked) {
        androidx.compose.foundation.BorderStroke(1.dp, RewardsGold.copy(alpha = 0.6f))
    } else {
        androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = border
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) RewardsGoldContainer else Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                val colorFilter = if (!isUnlocked) {
                    ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                } else null

                Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = achievement.title,
                    modifier = Modifier.size(52.dp),
                    colorFilter = colorFilter
                )

                if (!isUnlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (isUnlocked) {
                RewardStatusChip(statusText = "Unlocked", statusColor = StatusGreen)
            } else {
                Text(
                    text = "${achievement.currentProgress} / ${achievement.maxProgress}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                RewardProgressBar(
                    progress = achievement.currentProgress.toFloat() / achievement.maxProgress.toFloat(),
                    color = RewardsPurple,
                    modifier = Modifier.width(60.dp)
                )
            }
        }
    }
}
