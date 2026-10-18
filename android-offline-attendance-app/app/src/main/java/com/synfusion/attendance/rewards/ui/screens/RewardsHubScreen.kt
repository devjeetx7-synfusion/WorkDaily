package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@Composable
fun RewardsHubScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateRedeem: () -> Unit,
    onNavigateRefer: () -> Unit,
    onNavigateTasks: () -> Unit,
    onNavigateAchievements: () -> Unit,
    onNavigateLeaderboard: () -> Unit,
    onNavigateCoinHistory: () -> Unit,
    onNavigateInfo: () -> Unit,
    onNavigateDailyCheckIn: () -> Unit,
    onNavigateSpin: () -> Unit,
    onSelectTransaction: (RewardTransaction) -> Unit,
    onNavigateLiveWallet: () -> Unit = {},
    onNavigateLiveEarnings: () -> Unit = {}
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val checkInState by viewModel.dailyCheckInState.collectAsState()
    val streakState by viewModel.streakState.collectAsState()
    val spinTokens by viewModel.spinTokens.collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = stringResource(id = R.string.rewards_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateInfo) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Rewards Info", tint = RoyalPurple)
                    }
                }
            )
        },
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HERO BANNER: Main Reward Coin Wallet Hero
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RewardsHeroBackground(
                            modifier = Modifier.matchParentSize(),
                            baseGradient = RewardsPrimaryGradient,
                            showGlow = true,
                            glowColor = GoldPrimary,
                            showSparkles = true
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.my_workdaily_coins).uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp,
                                        color = HighlightLavender
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    RewardCoinBalance(
                                        amount = walletSummary.totalBalance,
                                        fontSize = 30,
                                        textColor = Color.White,
                                        coinSize = 32.dp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Keep earning, keep growing!",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(110.dp)
                                ) {
                                    RewardsRadialGlow(
                                        glowColor = GoldPrimary,
                                        alpha = 0.35f
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.rewards_hub_worker),
                                        contentDescription = "Rewards Worker",
                                        modifier = Modifier.size(105.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Available & Pending Inset Summary Panel
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White.copy(alpha = 0.95f))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.available),
                                        fontSize = 11.sp,
                                        color = RewardsTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RewardCoinBalance(
                                        amount = walletSummary.availableCoins,
                                        fontSize = 13,
                                        textColor = RewardsSuccessGreen,
                                        coinSize = 14.dp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .width(1.dp)
                                        .background(Color(0xFFCBD5E1))
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.pending),
                                        fontSize = 11.sp,
                                        color = RewardsTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RewardCoinBalance(
                                        amount = walletSummary.pendingCoins,
                                        fontSize = 13,
                                        textColor = StatusOrange,
                                        coinSize = 14.dp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RewardSecondaryButton(
                                    text = stringResource(id = R.string.view_wallet),
                                    onClick = onNavigateWallet,
                                    modifier = Modifier.weight(1f),
                                    borderColor = Color.White,
                                    contentColor = DeepIndigo,
                                    containerColor = Color.White
                                )
                                RewardPrimaryButton(
                                    text = stringResource(id = R.string.redeem),
                                    onClick = onNavigateRedeem,
                                    modifier = Modifier.weight(1f),
                                    containerColor = RoyalPurple,
                                    contentColor = Color.White,
                                    icon = Icons.Default.CardGiftcard
                                )
                            }
                        }
                    }
                }
            }

            // 2. DAILY CHECK-IN & BUILDER'S SPIN WHEEL
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Check-in Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onNavigateDailyCheckIn() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoyalPurple.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_wd_daily_checkin),
                                    contentDescription = "Daily Check-in",
                                    modifier = Modifier.size(34.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (checkInState.isTodayClaimed) RewardsSuccessGreen.copy(alpha = 0.15f) else GoldPrimary.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (checkInState.isTodayClaimed) "CLAIMED" else "CLAIM",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (checkInState.isTodayClaimed) RewardsSuccessGreen else DeepIndigo
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Daily Check-in",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RewardsTheme.textPrimary
                                )
                                Text(
                                    text = "${streakState.currentStreak} Day Streak 🔥",
                                    fontSize = 11.sp,
                                    color = RoyalPurple,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Builder's Spin Wheel Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clickable { onNavigateSpin() },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_wd_builder_spin),
                                    contentDescription = "Builder's Spin",
                                    modifier = Modifier.size(34.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RoyalPurple.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${spinTokens.availableSpins} SPINS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = RoyalPurple
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Builder's Spin",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RewardsTheme.textPrimary
                                )
                                Text(
                                    text = "Spin & Win Coins 🪙",
                                    fontSize = 11.sp,
                                    color = GoldSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. LIVE DIAMONDS & CREATOR EARNINGS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onNavigateLiveWallet() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Live Diamonds", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Gifts Wallet 💎", fontSize = 10.sp, color = Color(0xFF00E5FF))
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onNavigateLiveEarnings() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F9F7A).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Creator Earnings", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Cashout Demo 💵", fontSize = 10.sp, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // 4. COMPACT ACTION ROWS: REFER & EARN, TASKS, ACHIEVEMENTS, LEADERBOARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Compact Action Row 1: Refer & Earn
                        CompactActionRow(
                            iconRes = R.drawable.refer_workers,
                            title = stringResource(id = R.string.refer_and_earn),
                            subtitle = "Earn up to 500 🪙 per friend",
                            onClick = onNavigateRefer
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        // Compact Action Row 2: Tasks
                        CompactActionRow(
                            iconRes = R.drawable.task_daily,
                            title = stringResource(id = R.string.tasks),
                            subtitle = "3 daily tasks available",
                            onClick = onNavigateTasks
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        // Compact Action Row 3: Achievements
                        CompactActionRow(
                            iconRes = R.drawable.badge_work_pro,
                            title = stringResource(id = R.string.achievements),
                            subtitle = "8 badges unlocked",
                            onClick = onNavigateAchievements
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        // Compact Action Row 4: Leaderboard
                        CompactActionRow(
                            iconRes = R.drawable.leaderboard_first,
                            title = stringResource(id = R.string.leaderboard),
                            subtitle = "Rank #128 this week",
                            onClick = onNavigateLeaderboard
                        )
                    }
                }
            }

            // 5. RECENT ACTIVITY COMPACT PREVIEW
            item {
                RewardsSectionHeader(
                    title = stringResource(id = R.string.recent_activity),
                    actionText = "View All",
                    onActionClick = onNavigateCoinHistory
                )
            }

            items(transactions.take(2)) { tx ->
                RewardTransactionRow(
                    transaction = tx,
                    onClick = {
                        viewModel.selectTransaction(tx)
                        onSelectTransaction(tx)
                    }
                )
            }
        }
    }
}

@Composable
private fun CompactActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = RewardsTheme.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Open $title",
            tint = RoyalPurple,
            modifier = Modifier.size(16.dp)
        )
    }
}
