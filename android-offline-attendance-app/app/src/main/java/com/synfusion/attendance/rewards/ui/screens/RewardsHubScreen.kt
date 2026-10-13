package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    onSelectTransaction: (RewardTransaction) -> Unit
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Rewards Hub",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateInfo) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Rewards Info", tint = RewardsPurple)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top premium balance card
            item {
                RewardsBalanceCard(
                    availableCoins = walletSummary.availableCoins,
                    pendingCoins = walletSummary.pendingCoins,
                    onViewWallet = onNavigateWallet,
                    onRedeem = onNavigateRedeem
                )
            }

            // Refer & Earn Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateRefer() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(RewardsPurple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎁", fontSize = 24.sp)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "REFER & EARN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RewardsPurple,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Invite friends, earn together",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Earn up to 🪙500",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = StatusGreen
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2-column grid for Features
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RewardFeatureCard(
                            title = "Tasks",
                            subtitle = "3 available",
                            icon = Icons.Default.TaskAlt,
                            iconTint = Color(0xFF1976D2),
                            badgeText = "Tasks",
                            onClick = onNavigateTasks,
                            modifier = Modifier.weight(1f)
                        )
                        RewardFeatureCard(
                            title = "Achievements",
                            subtitle = "8 unlocked",
                            icon = Icons.Default.MilitaryTech,
                            iconTint = RewardsGold,
                            badgeText = "Badges",
                            onClick = onNavigateAchievements,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RewardFeatureCard(
                            title = "Leaderboard",
                            subtitle = "Rank #128",
                            icon = Icons.Default.EmojiEvents,
                            iconTint = RewardsPurple,
                            badgeText = "Rank",
                            onClick = onNavigateLeaderboard,
                            modifier = Modifier.weight(1f)
                        )
                        RewardFeatureCard(
                            title = "Coin History",
                            subtitle = "24 activities",
                            icon = Icons.Default.History,
                            iconTint = Color(0xFF388E3C),
                            badgeText = "History",
                            onClick = onNavigateCoinHistory,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Recent Activity
            item {
                RewardsSectionHeader(
                    title = "Recent Activity",
                    actionText = "View All",
                    onActionClick = onNavigateCoinHistory
                )
            }

            items(transactions.take(3)) { tx ->
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
