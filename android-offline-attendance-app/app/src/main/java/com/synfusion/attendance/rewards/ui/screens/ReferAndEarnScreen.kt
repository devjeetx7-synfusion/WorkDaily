package com.synfusion.attendance.rewards.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.ReferralItem
import com.synfusion.attendance.rewards.data.ReferralStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferAndEarnScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateReferralHistory: () -> Unit
) {
    val stats by viewModel.referralStats.collectAsState()
    val referrals by viewModel.referrals.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Refer & Earn", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero card
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
                        Text(text = "🎁", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Invite Friends & Earn Together",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1047)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share your referral code and earn rewards when they qualify.",
                            fontSize = 13.sp,
                            color = RewardsPurple,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Demo Code Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .border(1.dp, RewardsPurple.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("YOUR REFERRAL CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    Text(stats.referralCode, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = RewardsPurple)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Referral Code", stats.referralCode)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = RewardsPurple)
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Join WorkDaily using my referral code: ${stats.referralCode}")
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share Referral Code")
                                            context.startActivity(shareIntent)
                                        }
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Share", tint = RewardsPurple)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stats row
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn("${stats.invitedCount}", "Invited")
                        StatColumn("${stats.rewardedCount}", "Rewarded", StatusGreen)
                        StatColumn("${stats.pendingCount}", "Pending", StatusOrange)
                        StatColumn("🪙${stats.totalCoinsEarned}", "Earned", RewardsPurple)
                    }
                }
            }

            // How It Works
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("How It Works", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        StepRow(1, "Share code with your contractor/worker friends")
                        StepRow(2, "Friend downloads WorkDaily and registers")
                        StepRow(3, "Friend completes initial attendance entries")
                        StepRow(4, "Both receive 🪙100 confirmed reward coins!")
                    }
                }
            }

            // Recent Referrals Header
            item {
                RewardsSectionHeader(
                    title = "Recent Referrals",
                    actionText = "View All",
                    onActionClick = onNavigateReferralHistory
                )
            }

            items(referrals.take(3)) { referral ->
                ReferralRowItem(referral = referral)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferralHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val referrals by viewModel.referrals.collectAsState()
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Pending, 2: Rewarded, 3: Not Qualified

    val filteredReferrals = remember(selectedFilterTab, referrals) {
        when (selectedFilterTab) {
            1 -> referrals.filter { it.status == ReferralStatus.PENDING }
            2 -> referrals.filter { it.status == ReferralStatus.REWARDED }
            3 -> referrals.filter { it.status == ReferralStatus.NOT_QUALIFIED }
            else -> referrals
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Referral History", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            PrimaryTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RewardsPurple
            ) {
                Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                    Text("All", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                    Text("Pending", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                    Text("Rewarded", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                    Text("Not Qualified", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredReferrals) { referral ->
                    ReferralRowItem(referral = referral)
                }
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StepRow(stepNumber: Int, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(RewardsPurple.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$stepNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ReferralRowItem(referral: ReferralItem) {
    val statusColor = when (referral.status) {
        ReferralStatus.REWARDED -> StatusGreen
        ReferralStatus.PENDING -> StatusOrange
        ReferralStatus.NOT_QUALIFIED -> StatusRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(referral.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(referral.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (referral.rewardCoins > 0) {
                CoinAmount(amount = referral.rewardCoins, fontSize = 14, textColor = statusColor, showSign = true)
            } else {
                Text("🪙0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(2.dp))
            RewardStatusChip(statusText = referral.status.name, statusColor = statusColor)
        }
    }
}
