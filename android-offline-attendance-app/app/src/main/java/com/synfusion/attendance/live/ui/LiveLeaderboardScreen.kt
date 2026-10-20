package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLeaderboardScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProfile: (String) -> Unit = {}
) {
    val creators by viewModel.creators.collectAsState()
    val sortedCreators = remember(creators) {
        creators.sortedByDescending { it.totalEarnings }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Leaderboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiveDarkBg, titleContentColor = Color.White)
            )
        },
        containerColor = LiveDarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Top Stream Creators & Gifters", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            itemsIndexed(sortedCreators) { index, creator ->
                val rank = index + 1
                val rankColor = when (rank) {
                    1 -> LiveGold
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateProfile(creator.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (rank <= 3) rankColor.copy(alpha = 0.5f) else LivePrimaryEmerald.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(rankColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (rank <= 3) rankColor else Color.White
                                )
                            }

                            WorkDailyLiveAvatar(
                                name = creator.name,
                                avatarUrl = creator.avatar,
                                size = 40.dp,
                                isLive = false,
                                onClick = { onNavigateProfile(creator.id) }
                            )

                            Column {
                                Text(creator.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(creator.bio, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(16.dp))
                            Text("${creator.totalEarnings}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = LiveDiamondColor)
                        }
                    }
                }
            }
        }
    }
}
