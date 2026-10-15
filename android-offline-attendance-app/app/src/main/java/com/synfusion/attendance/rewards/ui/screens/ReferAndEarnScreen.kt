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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Referral Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RewardsPrimaryGradient)
                            .padding(22.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.refer_workers),
                                contentDescription = "Refer Workers",
                                modifier = Modifier.size(130.dp),
                                contentScale = ContentScale.Fit
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Invite Friends. Earn Together.",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Share your referral code with fellow workers and contractors to earn bonus WorkDaily Coins.",
                                fontSize = 13.sp,
                                color = HighlightLavender,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Coupon Ticket Box for Referral Code
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "YOUR REFERRAL CODE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RewardsTheme.textSecondary,
                                            letterSpacing = 1.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = stats.referralCode,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DeepIndigo,
                                            letterSpacing = 1.sp
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Referral Code", stats.referralCode)
                                                clipboard.setPrimaryClip(clip)
                                                isCopied = true
                                                Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(RewardsTheme.elevatedSurface)
                                        ) {
                                            Icon(
                                                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                contentDescription = "Copy Code",
                                                tint = if (isCopied) RewardsSuccessGreen else RoyalPurple,
                                                modifier = Modifier.size(20.dp)
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
                                            },
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(RoyalPurple)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stats Row Summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatColumn("${stats.invitedCount}", "Invited")
                        StatColumn("${stats.rewardedCount}", "Rewarded", RewardsSuccessGreen)
                        StatColumn("${stats.pendingCount}", "Pending", StatusOrange)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RewardCoinBalance(
                                amount = stats.totalCoinsEarned,
                                fontSize = 16,
                                textColor = RoyalPurple,
                                coinSize = 18.dp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Total Earned", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                        }
                    }
                }
            }

            // How It Works - Vertical Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "How It Works",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsTheme.textPrimary
                        )

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
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                RewardTabRow(
                    tabs = tabs,
                    pagerState = pagerState
                )
            }

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
private fun StatColumn(value: String, label: String, color: Color = RewardsTheme.textPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = RewardsTheme.textSecondary)
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
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(RewardsTheme.elevatedSurface),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$stepNumber",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RoyalPurple
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(HighlightLavender.copy(alpha = 0.4f))
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = RewardsTheme.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = RewardsTheme.textSecondary
            )
        }
    }
}

@Composable
private fun ReferralRowItem(referral: ReferralItem) {
    val statusColor = when (referral.status) {
        ReferralStatus.REWARDED -> RewardsSuccessGreen
        ReferralStatus.PENDING -> StatusOrange
        ReferralStatus.NOT_QUALIFIED -> StatusRed
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = referral.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = RewardsTheme.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = referral.date,
                    fontSize = 12.sp,
                    color = RewardsTheme.textSecondary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                RewardCoinBalance(
                    amount = referral.rewardCoins,
                    fontSize = 14,
                    textColor = statusColor,
                    showSign = true,
                    coinSize = 16.dp
                )
                Spacer(modifier = Modifier.height(4.dp))
                RewardStatusChip(statusText = referral.status.name, statusColor = statusColor)
            }
        }
    }
}
