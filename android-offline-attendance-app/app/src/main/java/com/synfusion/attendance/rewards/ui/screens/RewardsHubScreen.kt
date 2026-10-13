package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    onSelectTransaction: (RewardTransaction) -> Unit
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = stringResource(id = R.string.rewards_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateInfo) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = "Rewards Info", tint = RewardsPurple)
                    }
                }
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
            // Hero Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(RewardsPurpleContainer, Color.White)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(id = R.string.my_workdaily_coins),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = RewardsPurple
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    RewardCoinBalance(
                                        amount = walletSummary.totalBalance,
                                        fontSize = 30,
                                        textColor = Color(0xFF1F1047),
                                        coinSize = 32.dp
                                    )
                                }
                                Image(
                                    painter = painterResource(id = R.drawable.rewards_hub_worker),
                                    contentDescription = "Rewards Worker",
                                    modifier = Modifier.size(90.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.85f))
                                    .border(1.dp, RewardsPurple.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stringResource(id = R.string.available), fontSize = 12.sp, color = NeutralSlate, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RewardCoinBalance(amount = walletSummary.availableCoins, fontSize = 13, textColor = StatusGreen, coinSize = 14.dp)
                                }
                                Box(
                                    modifier = Modifier
                                        .height(16.dp)
                                        .width(1.dp)
                                        .background(Color.LightGray)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = stringResource(id = R.string.pending), fontSize = 12.sp, color = NeutralSlate, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    RewardCoinBalance(amount = walletSummary.pendingCoins, fontSize = 13, textColor = StatusOrange, coinSize = 14.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RewardSecondaryButton(
                                    text = stringResource(id = R.string.view_wallet),
                                    onClick = onNavigateWallet,
                                    modifier = Modifier.weight(1f).height(44.dp)
                                )
                                RewardPrimaryButton(
                                    text = stringResource(id = R.string.redeem),
                                    onClick = onNavigateRedeem,
                                    modifier = Modifier.weight(1f).height(44.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Refer & Earn Card
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
                        Image(
                            painter = painterResource(id = R.drawable.refer_workers),
                            contentDescription = "Refer Workers",
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.refer_and_earn),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RewardsPurple,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = stringResource(id = R.string.refer_subtitle),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Earn up to ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = StatusGreen
                                )
                                RewardCoinBalance(amount = 500, fontSize = 12, textColor = StatusGreen, coinSize = 13.dp)
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Feature Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RewardFeatureCard(
                            title = stringResource(id = R.string.tasks),
                            subtitle = "3 available",
                            icon = Icons.Default.TaskAlt,
                            iconTint = Color(0xFF1976D2),
                            badgeText = "Tasks",
                            onClick = onNavigateTasks,
                            modifier = Modifier.weight(1f)
                        )
                        RewardFeatureCard(
                            title = stringResource(id = R.string.achievements),
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
                            title = stringResource(id = R.string.leaderboard),
                            subtitle = "Rank #128",
                            icon = Icons.Default.EmojiEvents,
                            iconTint = RewardsPurple,
                            badgeText = "Rank",
                            onClick = onNavigateLeaderboard,
                            modifier = Modifier.weight(1f)
                        )
                        RewardFeatureCard(
                            title = stringResource(id = R.string.coin_history),
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
                    title = stringResource(id = R.string.recent_activity),
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

@Composable
private fun RewardFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(RewardsPurple.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}
