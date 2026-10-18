package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun CreatorStudioScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit,
    onNavigateSetup: () -> Unit = {}
) {
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val creators by viewModel.creators.collectAsState()
    val myCreator = creators.find { it.id == "creator_me" } ?: creators.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Profile & Go Live Header Hero Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PulsingLiveAvatar(name = myCreator.name, size = 44.dp, isLive = myCreator.isLiveNow)
                            Column {
                                Text(myCreator.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(myCreator.profession, fontSize = 12.sp, color = Color.LightGray)
                            }
                        }

                        LiveBadge(text = if (myCreator.isLiveNow) "LIVE NOW" else "OFFLINE", bgColor = if (myCreator.isLiveNow) LiveRed else LivePrimaryEmerald)
                    }

                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))

                    Button(
                        onClick = onNavigateSetup,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Go Live Now", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Overview Analytics Grid
            Text("Stream Analytics Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("FOLLOWERS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LivePrimaryEmerald)
                        Text("${myCreator.followerCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("+12 this week", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("DIAMONDS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveDiamondColor)
                        Text("${creatorEarning.totalDiamondsReceived} 💎", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("≈ ₹${creatorEarning.totalRupeesEarned.toInt()}", fontSize = 11.sp, color = LivePrimaryMint)
                    }
                }
            }

            // Earnings Quick Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("AVAILABLE DEMO EARNINGS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveGold)
                        Text("${creatorEarning.availableDiamonds} 💎 (₹${(creatorEarning.availableDiamonds * 0.50).toInt()})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Pending: ${creatorEarning.pendingDiamonds} 💎", fontSize = 11.sp, color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            viewModel.cashoutEarnings(100)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LiveGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cashout", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
