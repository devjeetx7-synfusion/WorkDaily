package com.synfusion.attendance.rewards.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.ReferralItem
import com.synfusion.attendance.rewards.data.ReferralStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@Composable
fun ReferAndEarnScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateReferralHistory: () -> Unit
) {
    val stats by viewModel.referralStats.collectAsState()
    val referrals by viewModel.referrals.collectAsState()
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Refer & Earn",
                onNavigateBack = onNavigateBack
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
                        Image(
                            painter = painterResource(id = R.drawable.refer_workers),
                            contentDescription = "Refer Workers",
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Invite Friends. Earn Together.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1F1047)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Share your referral code with fellow workers and contractors to earn bonus WorkDaily Coins.",
                            fontSize = 13.sp,
                            color = RewardsPurple,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Coupon Ticket Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .border(1.5.dp, RewardsPurple.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("YOUR REFERRAL CODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeutralSlate, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(stats.referralCode, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = RewardsPurple)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Referral Code", stats.referralCode)
                                            clipboard.setPrimaryClip(clip)
                                            isCopied = true
                                            Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                            contentDescription = "Copy Code",
                                            tint = if (isCopied) StatusGreen else RewardsPurple
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Join WorkDaily using my referral code: ${stats.referralCode}")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Referral Code"))
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RewardCoinBalance(amount = stats.totalCoinsEarned, fontSize = 15, textColor = RewardsPurple, coinSize = 16.dp)
                            Text("Earned", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // How It Works - Vertical Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("How It Works", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                        TimelineStep(1, "Share your code", "Send code to contractor or worker peers", isLast = false)
                        TimelineStep(2, "Friend joins WorkDaily", "They register their workspace using your code", isLast = false)
                        TimelineStep(3, "Friend qualifies", "They complete initial attendance and work logs", isLast = false)
                        TimelineStep(4, "Reward confirmed", "Both receive +🪙100 confirmed reward coins!", isLast = true)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReferralHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val referrals by viewModel.referrals.collectAsState()
    val tabs = remember { listOf("All", "Pending", "Rewarded", "Not Qualified") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Referral History",
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
                val filteredReferrals = remember(pageIndex, referrals) {
                    when (pageIndex) {
                        1 -> referrals.filter { it.status == ReferralStatus.PENDING }
                        2 -> referrals.filter { it.status == ReferralStatus.REWARDED }
                        3 -> referrals.filter { it.status == ReferralStatus.NOT_QUALIFIED }
                        else -> referrals
                    }
                }

                if (filteredReferrals.isEmpty()) {
                    RewardEmptyState(
                        title = "No ${tabs[pageIndex]} Referrals",
                        description = "Referral records matching this status will be listed here."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredReferrals, key = { it.id }) { referral ->
                            ReferralRowItem(referral = referral)
                        }
                    }
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
private fun TimelineStep(stepNumber: Int, title: String, description: String, isLast: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(RewardsPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("$stepNumber", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(30.dp)
                        .background(RewardsPurple.copy(alpha = 0.25f))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
            RewardCoinBalance(
                amount = referral.rewardCoins,
                fontSize = 14,
                textColor = statusColor,
                showSign = true,
                coinSize = 16.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            RewardStatusChip(statusText = referral.status.name, statusColor = statusColor)
        }
    }
}
