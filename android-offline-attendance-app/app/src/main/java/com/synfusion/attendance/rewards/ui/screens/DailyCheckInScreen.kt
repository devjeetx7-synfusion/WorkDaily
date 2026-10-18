package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.*
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
                        color = WorkDailyBrandPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RewardCoinBalance(
                                amount = walletSummary.totalBalance,
                                fontSize = 13,
                                textColor = WorkDailyBrandPrimary,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Compact Hero Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = WorkDailyBrandPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "My Daily Check-in Streak",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("${streakState.currentStreak} Days", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text("Current Streak", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${streakState.longestStreak} Days", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Longest Streak", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }

            // B. Today's Reward Card (Compact, Responsive, No letter wrapping)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = RewardsTheme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, RewardsTheme.softBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Header Title & Subtitle + Reward Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Reward",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RewardsTheme.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (checkInState.isTodayClaimed) "Already claimed today" else "Keep your streak going!",
                                    fontSize = 12.sp,
                                    color = RewardsTheme.textSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // FlowRow for chips so each chip wraps intact as a whole unit
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = GoldPrimary.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RewardCoinBalance(
                                            amount = 5,
                                            fontSize = 12,
                                            textColor = GoldSecondary,
                                            coinSize = 14.dp
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WorkDailyBrandPrimary.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, WorkDailyBrandPrimary.copy(alpha = 0.25f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Casino,
                                            contentDescription = "Bonus Spin",
                                            tint = WorkDailyBrandPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = "+1 Spin",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WorkDailyBrandPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Compact CTA Button
                        if (checkInState.isTodayClaimed) {
                            RewardSecondaryButton(
                                text = "Claimed Today ✓",
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                enabled = false,
                                containerColor = RewardsTheme.inputBackground,
                                contentColor = RewardsTheme.textSecondary,
                                borderColor = RewardsTheme.softBorder
                            )
                        } else {
                            RewardPrimaryButton(
                                text = "Claim Reward",
                                onClick = {
                                    if (viewModel.claimDailyCheckIn()) {
                                        showSuccessDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                containerColor = WorkDailyBrandPrimary,
                                contentColor = Color.White,
                                icon = Icons.Default.CheckCircle
                            )
                        }
                    }
                }
            }

            // C. Seven-Day Check-in Strip
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
                                val isPast = dateCell.isBefore(todayDate)
                                val isMissed = isPast && !isClaimed
                                val dayName = dateCell.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dayName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isToday) WorkDailyBrandPrimary else RewardsTheme.textSecondary
                                    )

                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isClaimed -> RewardsSuccessGreen
                                                    isMissed -> StatusRed.copy(alpha = 0.1f)
                                                    isToday -> WorkDailyBrandPrimary.copy(alpha = 0.15f)
                                                    else -> RewardsTheme.inputBackground
                                                }
                                            )
                                            .border(
                                                width = if (isToday) 2.dp else 1.dp,
                                                color = when {
                                                    isClaimed -> RewardsSuccessGreen
                                                    isMissed -> StatusRed.copy(alpha = 0.3f)
                                                    isToday -> WorkDailyBrandPrimary
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
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else if (isMissed) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Missed",
                                                tint = StatusRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Text(
                                                text = "${dateCell.dayOfMonth}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isToday) WorkDailyBrandPrimary else RewardsTheme.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
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
                                    color = WorkDailyBrandPrimary
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
                                                            isTodayCell -> WorkDailyBrandPrimary.copy(alpha = 0.15f)
                                                            else -> Color.Transparent
                                                        }
                                                    )
                                                    .border(
                                                        width = if (isTodayCell) 1.5.dp else 0.dp,
                                                        color = if (isTodayCell) WorkDailyBrandPrimary else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                            ) {
                                                Text(
                                                    text = "$dayNum",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isTodayCell || isClaimed) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isClaimed -> RewardsSuccessGreen
                                                        isTodayCell -> WorkDailyBrandPrimary
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
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(WorkDailyBrandPrimary.copy(alpha = 0.12f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = milestone.title,
                                        tint = WorkDailyBrandPrimary,
                                        modifier = Modifier.size(22.dp)
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
                                        color = WorkDailyBrandPrimary,
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
                                                    color = WorkDailyBrandPrimary
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
                                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = WorkDailyBrandPrimaryDark),
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
                    text = "You earned +5 Coins and 1 Free Spin Token!",
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
                        text = "SPIN NOW",
                        onClick = {
                            showSuccessDialog = false
                            onNavigateSpin()
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = WorkDailyBrandPrimary,
                        contentColor = Color.White
                    )
                }
            }
        }
    }
}
