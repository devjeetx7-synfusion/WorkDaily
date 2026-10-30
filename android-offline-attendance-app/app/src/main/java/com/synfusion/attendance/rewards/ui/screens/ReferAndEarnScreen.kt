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
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.audio.RewardsSoundEvent
import com.synfusion.attendance.rewards.data.ReferralItem
import com.synfusion.attendance.rewards.data.ReferralStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import com.synfusion.attendance.ui.components.InstagramIcon
import com.synfusion.attendance.ui.components.TelegramIcon
import com.synfusion.attendance.ui.components.WhatsAppIcon

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
    var isCopied by remember { mutableStateOf(false) }

    var inputReferralCode by remember { mutableStateOf("") }
    var isSubmittingCode by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var showCodeSuccessSheet by remember { mutableStateOf(false) }

    fun shareMessage(targetPackage: String? = null) {
        val shareText = "Join me on WorkDaily! Track attendance, wages, overtime, and earnings easily. Use my referral code: ${stats.referralCode}. Download WorkDaily: https://play.google.com/store/apps/details?id=com.synfusion.attendance"
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }
        viewModel.playSound(RewardsSoundEvent.BUTTON_TAP)
        try {
            if (targetPackage != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent.createChooser(intent, "Share Invite Link"))
            }
        } catch (e: Exception) {
            val fallbackChooser = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(fallbackChooser, "Share Invite Link"))
        }
    }

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
            // 1. Premium Referral Hero
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
                                text = "Invite Friends, Earn Together",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Share WorkDaily and earn rewards when your friends qualify.",
                                fontSize = 13.sp,
                                color = HighlightLavender,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 2. Referral Statistics Cards
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
                        StatColumn("${stats.invitedCount}", "Total Invited")
                        StatColumn("${stats.rewardedCount}", "Successful", RewardsSuccessGreen)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RewardCoinBalance(
                                amount = stats.totalCoinsEarned,
                                fontSize = 16,
                                textColor = RoyalPurple,
                                coinSize = 18.dp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Coins Earned", fontSize = 11.sp, color = RewardsTheme.textSecondary)
                        }
                    }
                }
            }

            // 3. Your Referral Code
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Your Referral Code",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsTheme.textPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(RewardsTheme.elevatedSurface)
                                .border(1.dp, RewardsTheme.softBorder, RoundedCornerShape(14.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stats.referralCode,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = DeepIndigo,
                                letterSpacing = 1.2.sp
                            )

                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Referral Code", stats.referralCode)
                                    clipboard.setPrimaryClip(clip)
                                    isCopied = true
                                    viewModel.playSound(RewardsSoundEvent.REFERRAL_COPY)
                                    Toast.makeText(context, "Referral code copied!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCopied) RewardsSuccessGreen else RoyalPurple
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCopied) "COPIED" else "COPY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Primary Invite Button
                        RewardPrimaryButton(
                            text = "Invite Friends",
                            onClick = { shareMessage(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            containerColor = RoyalPurple,
                            contentColor = Color.White,
                            icon = Icons.Default.Share
                        )

                        // Dedicated Social Sharing Buttons Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SocialShareButton(
                                name = "WhatsApp",
                                icon = WhatsAppIcon,
                                iconTint = Color(0xFF25D366),
                                onClick = { shareMessage("com.whatsapp") },
                                modifier = Modifier.weight(1f)
                            )
                            SocialShareButton(
                                name = "Instagram",
                                icon = InstagramIcon,
                                iconTint = Color(0xFFE4405F),
                                onClick = { shareMessage("com.instagram.android") },
                                modifier = Modifier.weight(1f)
                            )
                            SocialShareButton(
                                name = "Telegram",
                                icon = TelegramIcon,
                                iconTint = Color(0xFF229ED9),
                                onClick = { shareMessage("org.telegram.messenger") },
                                modifier = Modifier.weight(1f)
                            )
                            SocialShareButton(
                                name = "More",
                                icon = Icons.Default.Share,
                                iconTint = RoyalPurple,
                                onClick = { shareMessage(null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 4. Enter Referral Code Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Have a Referral Code?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsTheme.textPrimary
                        )
                        Text(
                            text = "Enter your friend's code to connect your referral.",
                            fontSize = 12.sp,
                            color = RewardsTheme.textSecondary
                        )

                        OutlinedTextField(
                            value = inputReferralCode,
                            onValueChange = {
                                inputReferralCode = it.uppercase()
                                codeError = null
                            },
                            placeholder = { Text("e.g. WD7X29") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            isError = codeError != null
                        )

                        if (codeError != null) {
                            Text(
                                text = codeError!!,
                                fontSize = 12.sp,
                                color = StatusRed,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                val trimmed = inputReferralCode.trim()
                                if (trimmed.isEmpty()) {
                                    codeError = "Please enter a referral code."
                                } else if (trimmed.equals(stats.referralCode, ignoreCase = true)) {
                                    codeError = "You cannot enter your own referral code."
                                } else {
                                    isSubmittingCode = true
                                    viewModel.playSound(RewardsSoundEvent.BUTTON_TAP)
                                    viewModel.submitReferralCode(trimmed) { result ->
                                        if (result.isSuccess) {
                                            showCodeSuccessSheet = true
                                            codeError = null
                                        } else {
                                            codeError = result.exceptionOrNull()?.message ?: "Failed to apply referral code."
                                        }
                                        isSubmittingCode = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigo),
                            enabled = !isSubmittingCode
                        ) {
                            if (isSubmittingCode) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text(text = "Apply Code", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 5. How It Works - Step-by-Step Modern Timeline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
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

                        TimelineStep(1, "Share Your Code", "Invite a friend using your referral link or code.", isLast = false)
                        TimelineStep(2, "Your Friend Joins", "Your friend joins WorkDaily and enters your valid referral code.", isLast = false)
                        TimelineStep(3, "Complete Requirements", "Your friend completes initial attendance logs.", isLast = false)
                        TimelineStep(4, "Earn Your Coins", "Approved rewards are credited according to referral rules.", isLast = true)
                    }
                }
            }

            // 6. Recent Referrals Header
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

    // Success Dialog Sheet for Referral Code Entry
    if (showCodeSuccessSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCodeSuccessSheet = false },
            containerColor = RewardsTheme.cardBackground,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            windowInsets = WindowInsets(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(RewardsSuccessGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = RewardsSuccessGreen, modifier = Modifier.size(36.dp))
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Referral Code Applied!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your referral link has been recorded. Rewards will be credited upon qualification.",
                    fontSize = 13.sp,
                    color = RewardsTheme.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                RewardPrimaryButton(
                    text = "DONE",
                    onClick = {
                        showCodeSuccessSheet = false
                        inputReferralCode = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
    }
}

@Composable
private fun SocialShareButton(
    name: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = RewardsTheme.elevatedSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = name, tint = iconTint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RewardsTheme.textPrimary)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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

    val initials = if (referral.name.contains(" ")) {
        val parts = referral.name.split(" ")
        "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
    } else {
        referral.name.take(2).uppercase()
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(RoyalPurple.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RoyalPurple
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = referral.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RewardsTheme.textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Joined: ${referral.date}",
                        fontSize = 12.sp,
                        color = RewardsTheme.textSecondary
                    )
                }
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
