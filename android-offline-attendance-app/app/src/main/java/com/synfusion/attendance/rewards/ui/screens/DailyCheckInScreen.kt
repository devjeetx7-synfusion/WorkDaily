package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyCheckInScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateSpin: () -> Unit
) {
    val checkInState by viewModel.dailyCheckInState.collectAsState()
    val streakState by viewModel.streakState.collectAsState()
    val milestones by viewModel.milestones.collectAsState()
    val walletSummary by viewModel.walletSummary.collectAsState()

    var showSuccessDialog by remember { mutableStateOf(false) }
    var selectedCalendarMonth by remember { mutableStateOf(YearMonth.now()) }

    val todayDate = remember { LocalDate.now() }
    val todayDateStr = remember(todayDate) { todayDate.format(DateTimeFormatter.ISO_LOCAL_DATE) }

    LaunchedEffect(todayDateStr) {
        viewModel.updateDateState(todayDateStr)
    }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Daily Check-in",
                onNavigateBack = onNavigateBack,
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = RoyalPurple.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 13,
                                textColor = RoyalPurple,
                                coinSize = 16.dp
                            )
                        }
                    }
                }
            )
        },
        containerColor = RewardsTheme.screenBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // A. Streak Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        RewardsHeroBackground(
                            modifier = Modifier.matchParentSize(),
                            baseGradient = RewardsPrimaryGradient,
                            showGlow = true,
                            glowColor = GoldPrimary,
                            showSparkles = true
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "DAILY STREAK",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.2.sp,
                                        color = HighlightLavender
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = "${streakState.currentStreak}",
                                            fontSize = 38.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Days 🔥",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldPrimary,
                                            modifier = Modifier.padding(bottom = 6.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (checkInState.isTodayClaimed) "Today's check-in complete!" else "Check in today to keep your streak!",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                Image(
                                    painter = painterResource(id = R.drawable.rewards_hub_worker),
                                    contentDescription = "Construction Worker",
                                    modifier = Modifier.size(100.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Streak Stats Inset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.95f))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(text = "Longest Streak", fontSize = 11.sp, color = RewardsTextSecondary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${streakState.longestStreak} Days", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = DeepIndigo)
                                }

                                Box(modifier = Modifier.height(24.dp).width(1.dp).background(Color(0xFFCBD5E1)))

                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(text = "Total Days", fontSize = 11.sp, color = RewardsTextSecondary, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = "${streakState.totalCheckInDays} Days", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = RoyalPurple)
                                }
                            }
                        }
                    }
                }
            }

            // B. Seven-Day Check-in Strip
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "This Week's Progress",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsTheme.textPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val startOfWeek = todayDate.minusDays(todayDate.dayOfWeek.value.toLong() - 1)
                            for (i in 0..6) {
                                val dateCell = startOfWeek.plusDays(i.toLong())
                                val dateStr = dateCell.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                val isClaimed = checkInState.claimedDates.contains(dateStr)
                                val isToday = dateCell.isEqual(todayDate)
                                val dayName = dateCell.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isToday) RoyalPurple else RewardsTheme.textSecondary
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isClaimed -> RewardsSuccessGreen
                                                    isToday -> RoyalPurple.copy(alpha = 0.15f)
                                                    else -> RewardsTheme.inputBackground
                                                }
                                            )
                                            .border(
                                                width = if (isToday) 2.dp else 1.dp,
                                                color = when {
                                                    isClaimed -> RewardsSuccessGreen
                                                    isToday -> RoyalPurple
                                                    else -> RewardsTheme.softBorder
                                                },
                                                shape = CircleShape
                                            )
                                    ) {
                                        if (isClaimed) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Claimed",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${dateCell.dayOfMonth}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isToday) RoyalPurple else RewardsTheme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // C. Today's Reward Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Reward",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RewardsTheme.textPrimary
                                )
                                Text(
                                    text = if (checkInState.isTodayClaimed) "Already claimed today" else "Claim now to get bonus coins & free spin",
                                    fontSize = 12.sp,
                                    color = RewardsTheme.textSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = GoldPrimary.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RewardCoinBalance(amount = 5, fontSize = 12, textColor = GoldSecondary, coinSize = 14.dp)
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = RoyalPurple.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "+1 Spin 🎯", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalPurple)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (checkInState.isTodayClaimed) {
                            RewardSecondaryButton(
                                text = "CLAIMED TODAY ✓",
                                onClick = { },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = false,
                                containerColor = RewardsTheme.inputBackground,
                                contentColor = RewardsTheme.textSecondary,
                                borderColor = RewardsTheme.softBorder
                            )
                        } else {
                            RewardPrimaryButton(
                                text = "CLAIM DAILY REWARD (+5 COINS & 1 SPIN)",
                                onClick = {
                                    if (viewModel.claimDailyCheckIn()) {
                                        showSuccessDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                containerColor = RoyalPurple,
                                contentColor = Color.White,
                                icon = Icons.Default.CheckCircle
                            )
                        }
                    }
                }
            }

            // D. Calendar Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Check-in History",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = RewardsTheme.textPrimary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { selectedCalendarMonth = selectedCalendarMonth.minusMonths(1) }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous Month", tint = RewardsTheme.textPrimary)
                                }
                                Text(
                                    text = selectedCalendarMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RoyalPurple
                                )
                                IconButton(onClick = { selectedCalendarMonth = selectedCalendarMonth.plusMonths(1) }) {
                                    Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Month", tint = RewardsTheme.textPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Weekday headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RewardsTheme.textSecondary,
                                    modifier = Modifier.width(36.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days grid
                        val firstOfMonth = selectedCalendarMonth.atDay(1)
                        val daysInMonth = selectedCalendarMonth.lengthOfMonth()
                        val startOffset = firstOfMonth.dayOfWeek.value - 1 // Mon=1 -> offset 0

                        val totalCells = ((startOffset + daysInMonth + 6) / 7) * 7

                        Column {
                            var cellIndex = 0
                            while (cellIndex < totalCells) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    for (col in 0..6) {
                                        val dayNum = cellIndex - startOffset + 1
                                        if (dayNum in 1..daysInMonth) {
                                            val cellDate = selectedCalendarMonth.atDay(dayNum)
                                            val cellDateStr = cellDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                            val isClaimed = checkInState.claimedDates.contains(cellDateStr)
                                            val isTodayCell = cellDate.isEqual(todayDate)

                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            isClaimed -> RewardsSuccessGreen.copy(alpha = 0.2f)
                                                            isTodayCell -> RoyalPurple.copy(alpha = 0.15f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isTodayCell) 1.5.dp else 0.dp,
                                                        color = if (isTodayCell) RoyalPurple else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isTodayCell || isClaimed) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isClaimed -> RewardsSuccessGreen
                                                        isTodayCell -> RoyalPurple
                                                        else -> RewardsTheme.textPrimary
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(36.dp))
                                        }
                                        cellIndex++
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // E. Milestone List
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Milestone Bonuses",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RewardsTheme.textPrimary
                    )

                    milestones.forEach { milestone ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(RoyalPurple.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = milestone.title,
                                        tint = RoyalPurple,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = milestone.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RewardsTheme.textPrimary
                                    )
                                    Text(
                                        text = milestone.description,
                                        fontSize = 11.sp,
                                        color = RewardsTheme.textSecondary
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    LinearProgressIndicator(
                                        progress = { milestone.currentProgress.toFloat() / milestone.requiredProgress.toFloat() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = RoyalPurple,
                                        trackColor = RewardsTheme.inputBackground
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${milestone.currentProgress}/${milestone.requiredProgress}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RewardsTheme.textSecondary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RewardCoinBalance(
                                                amount = milestone.coinReward,
                                                fontSize = 11,
                                                textColor = RewardsSuccessGreen,
                                                coinSize = 12.dp
                                            )
                                            if (milestone.bonusSpins > 0) {
                                                Text(
                                                    text = " +${milestone.bonusSpins} Spin",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = RoyalPurple
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                when (milestone.status) {
                                    MilestoneStatus.CLAIMABLE -> {
                                        Button(
                                            onClick = { viewModel.claimMilestone(milestone.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = DeepIndigo),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = "CLAIM", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    MilestoneStatus.CLAIMED -> {
                                        Text(
                                            text = "CLAIMED ✓",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RewardsSuccessGreen
                                        )
                                    }
                                    MilestoneStatus.LOCKED -> {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = RewardsTheme.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Success Celebration Bottom Sheet Overlay
    if (showSuccessDialog) {
        ModalBottomSheet(
            onDismissRequest = { showSuccessDialog = false },
            containerColor = RewardsTheme.cardBackground,
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(80.dp)
                ) {
                    RewardsRadialGlow(glowColor = GoldPrimary, alpha = 0.5f)
                    Image(
                        painter = painterResource(id = R.drawable.workdaily_coin),
                        contentDescription = "Gold Coin",
                        modifier = Modifier.size(70.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Daily Reward Claimed!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = RewardsTheme.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You earned +5 Demo Coins and 1 Free Spin Token!",
                    fontSize = 13.sp,
                    color = RewardsTheme.textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RewardSecondaryButton(
                        text = "DONE",
                        onClick = { showSuccessDialog = false },
                        modifier = Modifier.weight(1f)
                    )
                    RewardPrimaryButton(
                        text = "SPIN NOW 🎯",
                        onClick = {
                            showSuccessDialog = false
                            onNavigateSpin()
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = RoyalPurple,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}
