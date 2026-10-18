package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveEndSummary
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveEndSummaryScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateStudio: () -> Unit,
    onNavigateSetup: () -> Unit
) {
    val lastEndSummary by viewModel.lastEndSummary.collectAsState()
    val summary = lastEndSummary ?: LiveEndSummary(
        roomId = "room_demo",
        title = "Civil Site Inspection & Safety Briefing",
        creatorName = "You (Host)",
        durationSeconds = 1840,
        peakViewers = 420,
        totalViewers = 1250,
        heartsReceived = 3400,
        newFollowersCount = 48,
        giftsReceivedCount = 18,
        diamondsEarned = 1850,
        rupeesEarned = 925.0
    )

    val formattedDuration = remember(summary.durationSeconds) {
        val mins = summary.durationSeconds / 60
        val secs = summary.durationSeconds % 60
        "${mins}m ${secs}s"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Live Session Summary", fontWeight = FontWeight.Bold) },
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Hero Banner
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(LivePrimaryEmerald.copy(alpha = 0.2f))
                    .border(2.dp, LivePrimaryMint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = LivePrimaryMint, modifier = Modifier.size(48.dp))
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Live Stream Ended",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = summary.title,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Stats Grid 1: Earnings & Viewers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Diamond, contentDescription = null, tint = LiveDiamondColor, modifier = Modifier.size(16.dp))
                            Text("EARNED", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveDiamondColor)
                        }
                        Text("${summary.diamondsEarned} 💎", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("≈ ₹${summary.rupeesEarned.toInt()} Demo Earnings", fontSize = 11.sp, color = LivePrimaryMint)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = LiveGold, modifier = Modifier.size(16.dp))
                            Text("VIEWERS", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = LiveGold)
                        }
                        Text("${summary.totalViewers}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Peak Viewers: ${summary.peakViewers}", fontSize = 11.sp, color = Color.LightGray)
                    }
                }
            }

            // Stats Grid 2: Duration, Hearts, Followers, Gifts
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = LivePrimaryEmerald)
                            Text("Stream Duration", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text(formattedDuration, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = LiveRed)
                            Text("Hearts Received", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("${summary.heartsReceived} ❤️", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = LivePurple)
                            Text("New Followers", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("+${summary.newFollowersCount}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LivePrimaryMint)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = LiveGold)
                            Text("Gifts Received", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Text("${summary.giftsReceivedCount} gifts", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action CTAs
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onNavigateSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Another Live", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateStudio,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = LivePrimaryEmerald),
                    border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("View Creator Studio", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Back to Discover")
                }
            }
        }
    }
}
