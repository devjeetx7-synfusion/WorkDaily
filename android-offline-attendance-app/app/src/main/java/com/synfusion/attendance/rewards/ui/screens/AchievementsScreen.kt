package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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

@OptIn(ExperimentalFoundationApi::class)
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

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Achievements & Badges",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = RewardsScreenBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Achievement Progress Hero Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RewardsPrimaryGradient)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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

                        Image(
                            painter = painterResource(id = R.drawable.badge_legend),
                            contentDescription = "Badge Trophy",
                            modifier = Modifier.size(90.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // Category Tabs
            Box(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                RewardTabRow(
                    tabs = tabs,
                    pagerState = pagerState
                )
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
                                    onSelectAchievement(achievement)
                                }
                            )
                        }
                    }
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
    var showModalSheet by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Badge Details",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = RewardsScreenBg
    ) { paddingValues ->
        val ach = selectedAchievement
        if (ach == null) {
            Box(modifier = Modifier.padding(paddingValues)) {
                RewardEmptyState(
                    title = "No Badge Selected",
                    description = "Select a badge from achievements to view details."
                )
            }
        } else {
            val drawableRes = getBadgeDrawableRes(ach.iconName)
            val isUnlocked = ach.isUnlocked

            val badgeScale by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "badgeScale"
            )

            if (showModalSheet) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showModalSheet = false
                        onNavigateBack()
                    },
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
                                .background(if (isUnlocked) RewardsGoldSurface else RewardsLavenderSurface)
                                .border(
                                    2.dp,
                                    if (isUnlocked) GoldPrimary else HighlightLavender,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val colorFilter = if (!isUnlocked) {
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            } else null

                            Image(
                                painter = painterResource(id = drawableRes),
                                contentDescription = ach.title,
                                modifier = Modifier
                                    .size(85.dp)
                                    .scale(badgeScale),
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
                            color = RewardsTextPrimary
                        )

                        Text(
                            text = ach.description,
                            fontSize = 14.sp,
                            color = RewardsTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                                        color = RewardsTextPrimary
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
                            onClick = {
                                showModalSheet = false
                                onNavigateBack()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }
            }
        }
    }
}
