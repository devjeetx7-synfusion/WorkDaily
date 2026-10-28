package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.synfusion.attendance.R
import com.synfusion.attendance.live.model.LiveRoom
import com.synfusion.attendance.live.ui.components.ViewerCountChip
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar
import com.synfusion.attendance.live.viewmodel.LiveViewModel
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardsHubScreen(
    viewModel: RewardsViewModel,
    liveViewModel: LiveViewModel? = null,
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
    onNavigateLiveRoom: (String) -> Unit = {},
    onNavigateLiveDiscover: () -> Unit = {},
    onNavigateLiveWallet: () -> Unit = {},
    onNavigateLiveEarnings: () -> Unit = {}
) {
    val walletSummary by viewModel.walletSummary.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val checkInState by viewModel.dailyCheckInState.collectAsState()
    val streakState by viewModel.streakState.collectAsState()
    val spinTokens by viewModel.spinTokens.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val leaderboardState = remember(viewModel) { viewModel.getLeaderboard() }
    val leaderboard by leaderboardState.collectAsState()

    val liveRoomsState = liveViewModel?.liveRooms?.collectAsState()
    val liveRooms = liveRoomsState?.value ?: emptyList()

    val availableTasksCount = remember(tasks) { tasks.count { it.status == TaskStatus.CLAIMABLE || it.status == TaskStatus.IN_PROGRESS } }
    val unlockedBadgesCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val userRankText = remember(leaderboard) {
        val userRank = leaderboard.find { it.isCurrentUser }?.rank
        if (userRank != null) "Rank #$userRank this week" else "Weekly Leaderboard"
    }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = stringResource(id = R.string.rewards_title),
                onNavigateBack = onNavigateBack,
                actions = {
                    IconButton(onClick = onNavigateCoinHistory) {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Coin History", tint = RoyalPurple)
                    }
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
            // Section 2 — Compact Coin Wallet
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "AVAILABLE COINS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f)
                            )

                            RewardCoinBalance(
                                amount = walletSummary.availableCoins,
                                fontSize = 34,
                                textColor = Color.White,
                                coinSize = 30.dp
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.15f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Pending", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                                    RewardCoinBalance(amount = walletSummary.pendingCoins, fontSize = 12, textColor = Color.White, coinSize = 12.dp)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.White.copy(alpha = 0.2f)))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Total Earned", color = Color.White.copy(alpha = 0.75f), fontSize = 10.sp)
                                    RewardCoinBalance(amount = walletSummary.totalBalance, fontSize = 12, textColor = Color.White, coinSize = 12.dp)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onNavigateWallet,
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Wallet", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Button(
                                    onClick = onNavigateRedeem,
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = WorkDailyBrandPrimary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Redeem", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 3 — Live Now (NEW FEATURE)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RewardsSectionHeader(
                        title = "Live Now",
                        actionText = "See All",
                        onActionClick = onNavigateLiveDiscover
                    )

                    val activeStreams = remember(liveRooms) { liveRooms.filter { it.isLive } }

                    if (activeStreams.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            items(activeStreams) { room ->
                                LiveNowCard(
                                    room = room,
                                    onClick = { onNavigateLiveRoom(room.id) }
                                )
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(RoyalPurple.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = RoyalPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "No Live Streams Right Now",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RewardsTheme.textPrimary
                                        )
                                        Text(
                                            text = "Check back later or go live yourself!",
                                            fontSize = 11.sp,
                                            color = RewardsTheme.textSecondary
                                        )
                                    }
                                }

                                TextButton(onClick = onNavigateLiveDiscover) {
                                    Text("Explore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RoyalPurple)
                                }
                            }
                        }
                    }
                }
            }

            // Section 4 — Daily Rewards (Check-in & Spin)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Daily Check-in Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 92.dp)
                            .clickable { onNavigateDailyCheckIn() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = RoyalPurple, modifier = Modifier.size(22.dp))
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
                            .heightIn(min = 92.dp)
                            .clickable { onNavigateSpin() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(22.dp))
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

            // Section 5 — Explore Rewards
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
                        CompactActionRow(
                            iconRes = R.drawable.refer_workers,
                            title = stringResource(id = R.string.refer_and_earn),
                            subtitle = "Invite & earn coins together",
                            onClick = onNavigateRefer
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        CompactActionRow(
                            iconRes = R.drawable.task_daily,
                            title = stringResource(id = R.string.tasks),
                            subtitle = if (availableTasksCount > 0) "$availableTasksCount tasks available" else "All tasks completed!",
                            onClick = onNavigateTasks
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        CompactActionRow(
                            iconRes = R.drawable.badge_work_pro,
                            title = stringResource(id = R.string.achievements),
                            subtitle = "$unlockedBadgesCount badges unlocked",
                            onClick = onNavigateAchievements
                        )

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        CompactActionRow(
                            iconRes = R.drawable.leaderboard_first,
                            title = stringResource(id = R.string.leaderboard),
                            subtitle = userRankText,
                            onClick = onNavigateLeaderboard
                        )
                    }
                }
            }

            // Section 6 — Consolidated Live Wallet
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
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.Diamond, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Live Diamonds Wallet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                    Text("Send gifts & manage diamonds 💎", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RewardsTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }

                        HorizontalDivider(color = RewardsTheme.softBorder.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateLiveEarnings() }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Creator Earnings & Cashout", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
                                    Text("View earnings & cashout rules 💵", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = RewardsTheme.textSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Section 7 — Recent Activity Preview
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
private fun LiveNowCard(
    room: LiveRoom,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val previewImageUrl = remember(room.thumbnailUrl, room.creatorAvatar, room.youtubeVideoId, room.watchPartyState, room.type) {
        val ytThumb = room.youtubeVideoId?.takeIf { it.isNotBlank() }?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
            ?: room.watchPartyState?.playlist?.firstOrNull()?.thumbnailUrl?.takeIf { it.isNotBlank() }

        when {
            room.type == com.synfusion.attendance.live.model.LiveRoomType.YOUTUBE_WATCH_PARTY && !ytThumb.isNullOrBlank() -> ytThumb
            !room.thumbnailUrl.isNullOrEmpty() -> room.thumbnailUrl
            !ytThumb.isNullOrBlank() -> ytThumb
            !room.creatorAvatar.isNullOrEmpty() -> room.creatorAvatar
            else -> null
        }
    }

    Card(
        modifier = Modifier
            .width(150.dp)
            .height(180.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full-Card Image Preview (Host Avatar / YouTube Thumbnail / Custom Thumbnail)
            if (!previewImageUrl.isNullOrBlank()) {
                val request = remember(previewImageUrl) {
                    ImageRequest.Builder(context)
                        .data(previewImageUrl)
                        .crossfade(true)
                        .build()
                }
                SubcomposeAsyncImage(
                    model = request,
                    contentDescription = room.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = {
                        DefaultCardGradient()
                    },
                    loading = {
                        DefaultCardGradient()
                    }
                )
            } else {
                DefaultCardGradient()
            }

            // Top Header: Viewer Count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                ViewerCountChip(count = room.viewerCount)
            }

            // Bottom Content overlay with subtle scrim
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
                    .padding(10.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WorkDailyLiveAvatar(
                            name = room.creatorName,
                            avatarUrl = room.creatorAvatar,
                            size = 28.dp,
                            isLive = room.isLive,
                            showBadge = false
                        )

                        Text(
                            text = room.creatorName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = room.title,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun DefaultCardGradient() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
    )
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
