package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.LeaderboardTimeframe
import com.synfusion.attendance.rewards.data.LeaderboardUser
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val timeframe by viewModel.leaderboardTimeframe.collectAsState()
    val leaderboardUsers by viewModel.getLeaderboard().collectAsState()

    val top1 = leaderboardUsers.find { it.rank == 1 }
    val top2 = leaderboardUsers.find { it.rank == 2 }
    val top3 = leaderboardUsers.find { it.rank == 3 }
    val restUsers = leaderboardUsers.filter { it.rank > 3 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Leaderboard", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Weekly / Monthly Toggle Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { viewModel.setLeaderboardTimeframe(LeaderboardTimeframe.WEEKLY) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timeframe == LeaderboardTimeframe.WEEKLY) RewardsPurple else Color.Transparent,
                        contentColor = if (timeframe == LeaderboardTimeframe.WEEKLY) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Weekly", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { viewModel.setLeaderboardTimeframe(LeaderboardTimeframe.MONTHLY) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (timeframe == LeaderboardTimeframe.MONTHLY) RewardsPurple else Color.Transparent,
                        contentColor = if (timeframe == LeaderboardTimeframe.MONTHLY) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Monthly", fontWeight = FontWeight.Bold)
                }
            }

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
                                PodiumSlot(user = top2, medal = "🥈", rankNum = 2, heightDp = 90)
                                PodiumSlot(user = top1, medal = "🥇", rankNum = 1, heightDp = 120)
                                PodiumSlot(user = top3, medal = "🥉", rankNum = 3, heightDp = 75)
                            }
                        }
                    }
                }

                // List of remaining users
                items(restUsers) { user ->
                    LeaderboardRowItem(user = user)
                }
            }
        }
    }
}

@Composable
private fun PodiumSlot(user: LeaderboardUser?, medal: String, rankNum: Int, heightDp: Int) {
    if (user == null) return
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(medal, fontSize = 24.sp)
        Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1F1047))
        Text("${user.xp} XP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = RewardsPurple)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(70.dp)
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
                Text(
                    text = "#${user.rank}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isMe) RewardsPurple else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
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
