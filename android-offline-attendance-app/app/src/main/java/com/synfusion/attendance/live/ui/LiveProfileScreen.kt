package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveProfileScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateWallet: () -> Unit,
    onNavigateEarnings: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigateDemoControls: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsState()
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val followedIds by viewModel.followedCreatorIds.collectAsState()

    val followedCreators = remember(creators, followedIds) {
        creators.filter { followedIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Profile & Channels", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateDemoControls) {
                        Icon(Icons.Default.BugReport, contentDescription = "Demo Controls", tint = LivePrimaryMint)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PulsingLiveAvatar(
                            name = "You (Demo Streamer)",
                            size = 64.dp,
                            isLive = false
                        )

                        Text("You (Live Creator)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("WorkDaily Live Site Member", fontSize = 12.sp, color = Color.Gray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${wallet.diamondBalance}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LiveDiamondColor)
                                Text("Diamonds", fontSize = 11.sp, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${creatorEarning.totalDiamondsReceived}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LiveGold)
                                Text("Received 💎", fontSize = 11.sp, color = Color.Gray)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${followedCreators.size}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = LivePrimaryMint)
                                Text("Following", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Quick Actions Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onNavigateWallet,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LiveDiamondColor.copy(alpha = 0.2f), contentColor = LiveDiamondColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Diamond, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wallet", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onNavigateEarnings,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Earnings", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Followed Creators List
            item {
                Text("Followed Stream Creators", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            if (followedCreators.isEmpty()) {
                item {
                    Text("You aren't following any creators yet.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                items(followedCreators) { creator ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                PulsingLiveAvatar(name = creator.name, size = 36.dp, isLive = false)
                                Column {
                                    Text(creator.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${creator.followerCount} followers", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            FollowButton(
                                isFollowing = true,
                                onToggle = { viewModel.toggleFollowCreator(creator.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
