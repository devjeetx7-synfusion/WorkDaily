package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Achievements & Badges",
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
            // Unlocked Summary Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = RewardsPurpleContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, RewardsPurple.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$unlockedCount / $totalCount Badges Unlocked",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1047)
                        )
                        Text(
                            text = "${if (totalCount > 0) (unlockedCount * 100) / totalCount else 0}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RewardsPurple
                        )
                    }
                    RewardProgressBar(
                        progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f,
                        color = RewardsPurple
                    )
                }
            }

            // Custom Synchronized Tab Row
            RewardTabRow(
                tabs = tabs,
                pagerState = pagerState
            )

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
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val ach = selectedAchievement
        if (ach == null) {
            RewardEmptyState(
                title = "No Badge Selected",
                description = "Select a badge from achievements to view details."
            )
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
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(if (isUnlocked) RewardsGoldContainer else Color.LightGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val colorFilter = if (!isUnlocked) {
                                ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })
                            } else null

                            Image(
                                painter = painterResource(id = drawableRes),
                                contentDescription = ach.title,
                                modifier = Modifier
                                    .size(80.dp)
                                    .scale(badgeScale),
                                colorFilter = colorFilter
                            )

                            if (!isUnlocked) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Locked",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }

                        Text(ach.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(ach.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        if (isUnlocked) {
                            RewardStatusChip(statusText = "UNLOCKED ${ach.unlockedDate ?: ""}", statusColor = StatusGreen)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("${ach.currentProgress} / ${ach.maxProgress}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                RewardProgressBar(progress = ach.currentProgress.toFloat() / ach.maxProgress.toFloat())
                                Spacer(modifier = Modifier.height(10.dp))
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
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        )
                    }
                }
            }
        }
    }
}
