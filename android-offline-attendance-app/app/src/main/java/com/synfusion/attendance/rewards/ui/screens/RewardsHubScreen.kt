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
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WorkDailyBrandPrimary)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "WORKDAILY COINS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 36,
                                textColor = Color.White,
                                coinSize = 32.dp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Available", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    RewardCoinBalance(amount = walletSummary.availableCoins, fontSize = 14, textColor = Color.White, coinSize = 12.dp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pending", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    RewardCoinBalance(amount = walletSummary.pendingCoins, fontSize = 14, textColor = Color.White, coinSize = 12.dp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onNavigateWallet,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Wallet", fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = onNavigateRedeem,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkDailyBrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Redeem", fontWeight = FontWeight.Bold)
                                }
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
                            .height(100.dp)
                            .clickable { onNavigateDailyCheckIn() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
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
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(24.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
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
                                    color = RewardsTheme.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Builder's Spin Wheel Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clickable { onNavigateSpin() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
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
                                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(24.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
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
                                    color = RewardsTheme.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. LIVE DIAMONDS & CREATOR EARNINGS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateLiveWallet() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Live Diamonds", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                    Text("Gifts Wallet 💎", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RewardsTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateLiveEarnings() }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Creator Earnings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                    Text("Cashout Demo 💵", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RewardsTheme.textSecondary, modifier = Modifier.size(16.dp))
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
