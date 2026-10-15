package com.synfusion.attendance.rewards.ui.screens

import kotlinx.coroutines.launch
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.Achievement
import com.synfusion.attendance.rewards.data.AchievementCategory
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

fun getBadgeDrawableRes(iconName: String): Int {
    return when (iconName) {
        "flag" -> R.drawable.badge_first_step
        "fire" -> R.drawable.badge_streak
        "calendar" -> R.drawable.badge_month_master
        "description" -> R.drawable.badge_first_report
        "badge" -> R.drawable.badge_work_pro
        "crown" -> R.drawable.badge_legend
        else -> R.drawable.badge_first_step
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectAchievement: (Achievement) -> Unit
) {
    val achievements by viewModel.achievements.collectAsState()
    val tabs = remember { listOf("All", "Attendance", "Work", "Milestones") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size
    val completionPercent = if (totalCount > 0) (unlockedCount * 100) / totalCount else 0

    var selectedAchievementForSheet by remember { mutableStateOf<Achievement?>(null) }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Achievements & Badges",
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
            // Achievement Progress Hero Banner with Canvas Graphics
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RewardsHeroBackground(
                        modifier = Modifier.matchParentSize(),
                        baseGradient = RewardsPrimaryGradient,
                        showGlow = true,
                        glowColor = GoldBright,
                        showSparkles = true
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "BADGE PROGRESS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = HighlightLavender,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$unlockedCount / $totalCount Unlocked ($completionPercent%)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            RewardProgressBar(
                                progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f,
                                color = GoldPrimary,
                                trackColor = Color.White.copy(alpha = 0.25f),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Keep completing daily tasks to earn more badges",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(125.dp)
                        ) {
                            RewardsRadialGlow(glowColor = GoldBright, alpha = 0.45f)
                            Image(
                                painter = painterResource(id = R.drawable.badge_legend),
                                contentDescription = "Badge Trophy",
                                modifier = Modifier.size(120.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(tabs.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val coroutineScope = rememberCoroutineScope()
                    Surface(
                        modifier = Modifier.clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        shape = CircleShape,
                        color = if (isSelected) RoyalPurple else RewardsTheme.elevatedSurface,
                        contentColor = if (isSelected) Color.White else RewardsTheme.textPrimary
                    ) {
                        Text(
                            text = tabs[index],
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val filteredAchievements = remember(pageIndex, achievements) {
                    when (pageIndex) {
                        1 -> achievements.filter { it.category == AchievementCategory.ATTENDANCE }
                        2 -> achievements.filter { it.category == AchievementCategory.WORK }
                        3 -> achievements.filter { it.category == AchievementCategory.MILESTONES }
                        else -> achievements
                    }
                }

                if (filteredAchievements.isEmpty()) {
                    RewardEmptyState(
                        title = "No ${tabs[pageIndex]} Badges",
                        description = "Achievements in this category will appear as you progress."
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filteredAchievements, key = { it.id }) { achievement ->
                            RewardBadgeCard(
                                achievement = achievement,
                                drawableRes = getBadgeDrawableRes(achievement.iconName),
                                onClick = {
                                    viewModel.selectAchievement(achievement)
                                    selectedAchievementForSheet = achievement
                                    onSelectAchievement(achievement)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Custom Modal Bottom Sheet directly over achievements grid without extra scaffold or duplicate header
        val currentSheetAchievement = selectedAchievementForSheet
        if (currentSheetAchievement != null) {
            val drawableRes = getBadgeDrawableRes(currentSheetAchievement.iconName)
            val isUnlocked = currentSheetAchievement.isUnlocked

            ModalBottomSheet(
                onDismissRequest = { selectedAchievementForSheet = null },
                containerColor = RewardsTheme.cardBackground,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                windowInsets = WindowInsets(0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(if (isUnlocked) RewardsTheme.goldBadgeSurface else RewardsTheme.lockedBadgeSurface)
                            .border(
                                2.dp,
                                if (isUnlocked) GoldPrimary else HighlightLavender,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUnlocked) {
                            RewardsRadialGlow(glowColor = GoldBright, alpha = 0.5f)
                            RewardsSparkleLayer(starColor = GoldBright)
                        }

                        val colorFilter = if (!isUnlocked) {
                            ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                        } else null

                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = currentSheetAchievement.title,
                            modifier = Modifier.size(95.dp),
                            colorFilter = colorFilter,
                            contentScale = ContentScale.Fit
                        )

                        if (!isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = DeepIndigo,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = currentSheetAchievement.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RewardsTheme.textPrimary
                    )

                    Text(
                        text = currentSheetAchievement.description,
                        fontSize = 14.sp,
                        color = RewardsTheme.textSecondary,
                        textAlign = TextAlign.Center
                    )

                    if (isUnlocked) {
                        RewardStatusChip(
                            statusText = "UNLOCKED ${currentSheetAchievement.unlockedDate ?: ""}",
                            statusColor = RewardsSuccessGreen
                        )
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Progress",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RewardsTheme.textPrimary
                                )
                                Text(
                                    text = "${currentSheetAchievement.currentProgress} / ${currentSheetAchievement.maxProgress}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RoyalPurple
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            RewardProgressBar(
                                progress = currentSheetAchievement.currentProgress.toFloat() / currentSheetAchievement.maxProgress.toFloat(),
                                color = RoyalPurple,
                                trackColor = HighlightLavender.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            RewardStatusChip(statusText = "LOCKED", statusColor = StatusOrange)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RewardPrimaryButton(
                        text = "Close",
                        onClick = { selectedAchievementForSheet = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()

    val ach = selectedAchievement
    if (ach == null) {
        RewardEmptyState(
            title = "No Badge Selected",
            description = "Select a badge from achievements to view details."
        )
    } else {
        val drawableRes = getBadgeDrawableRes(ach.iconName)
        val isUnlocked = ach.isUnlocked

        ModalBottomSheet(
            onDismissRequest = onNavigateBack,
            containerColor = RewardsTheme.cardBackground,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            windowInsets = WindowInsets(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) RewardsTheme.goldBadgeSurface else RewardsTheme.lockedBadgeSurface)
                        .border(
                            2.dp,
                            if (isUnlocked) GoldPrimary else HighlightLavender,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isUnlocked) {
                        RewardsRadialGlow(glowColor = GoldBright, alpha = 0.5f)
                        RewardsSparkleLayer(starColor = GoldBright)
                    }

                    val colorFilter = if (!isUnlocked) {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                    } else null

                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = ach.title,
                        modifier = Modifier.size(85.dp),
                        colorFilter = colorFilter,
                        contentScale = ContentScale.Fit
                    )

                    if (!isUnlocked) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked",
                                    tint = DeepIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = ach.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = RewardsTheme.textPrimary
                )

                Text(
                    text = ach.description,
                    fontSize = 14.sp,
                    color = RewardsTheme.textSecondary,
                    textAlign = TextAlign.Center
                )

                if (isUnlocked) {
                    RewardStatusChip(
                        statusText = "UNLOCKED ${ach.unlockedDate ?: ""}",
                        statusColor = RewardsSuccessGreen
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Progress",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RewardsTheme.textPrimary
                            )
                            Text(
                                text = "${ach.currentProgress} / ${ach.maxProgress}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = RoyalPurple
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        RewardProgressBar(
                            progress = ach.currentProgress.toFloat() / ach.maxProgress.toFloat(),
                            color = RoyalPurple,
                            trackColor = HighlightLavender.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RewardStatusChip(statusText = "LOCKED", statusColor = StatusOrange)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                RewardPrimaryButton(
                    text = "Close",
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                )
            }
        }
    }
}
