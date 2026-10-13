package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.LeaderboardTimeframe
import com.synfusion.attendance.rewards.data.LeaderboardUser
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeaderboardScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val tabs = remember { listOf("Weekly", "Monthly") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val weeklyLeaderboard by viewModel.getLeaderboard().collectAsState()

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Leaderboard",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RewardTabRow(
                tabs = tabs,
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                LaunchedEffect(pageIndex) {
                    viewModel.setLeaderboardTimeframe(if (pageIndex == 0) LeaderboardTimeframe.WEEKLY else LeaderboardTimeframe.MONTHLY)
                }

                val top1 = weeklyLeaderboard.find { it.rank == 1 }
                val top2 = weeklyLeaderboard.find { it.rank == 2 }
                val top3 = weeklyLeaderboard.find { it.rank == 3 }
                val restUsers = weeklyLeaderboard.filter { it.rank > 3 }
                val currentUser = weeklyLeaderboard.find { it.isCurrentUser }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Podium Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("XP RANKINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RewardsPurple, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    PodiumSlot(user = top2, drawableRes = R.drawable.leaderboard_second, rankNum = 2, heightDp = 80)
                                    PodiumSlot(user = top1, drawableRes = R.drawable.leaderboard_first, rankNum = 1, heightDp = 110)
                                    PodiumSlot(user = top3, drawableRes = R.drawable.leaderboard_third, rankNum = 3, heightDp = 65)
                                }
                            }
                        }
                    }

                    // Sticky/Highlighted Current User Rank Card
                    if (currentUser != null && currentUser.rank > 3) {
                        item {
                            LeaderboardRowItem(user = currentUser)
                        }
                    }

                    // List of remaining users
                    items(restUsers, key = { it.id }) { user ->
                        if (!user.isCurrentUser) {
                            LeaderboardRowItem(user = user)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PodiumSlot(user: LeaderboardUser?, drawableRes: Int, rankNum: Int, heightDp: Int) {
    if (user == null) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Rank #$rankNum",
            modifier = Modifier.size(if (rankNum == 1) 64.dp else 52.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1047))
        Text("${user.xp} XP", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = RewardsPurple)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(heightDp.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(if (rankNum == 1) RewardsGold else RewardsPurple.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text("#$rankNum", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
    }
}

@Composable
private fun LeaderboardRowItem(user: LeaderboardUser) {
    val isMe = user.isCurrentUser
    val cardBg = if (isMe) RewardsPurpleContainer else MaterialTheme.colorScheme.surface
    val borderStroke = if (isMe) androidx.compose.foundation.BorderStroke(1.5.dp, RewardsPurple) else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = borderStroke
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isMe) RewardsPurple else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${user.rank}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = user.name,
                    fontSize = 14.sp,
                    fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "${user.xp} XP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RewardsPurple
            )
        }
    }
}
