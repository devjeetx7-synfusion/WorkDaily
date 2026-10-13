package com.synfusion.attendance.rewards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.Achievement
import com.synfusion.attendance.rewards.data.AchievementCategory
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectAchievement: (Achievement) -> Unit
) {
    val achievements by viewModel.achievements.collectAsState()
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Attendance, 2: Work, 3: Milestones

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size

    val filteredAchievements = remember(selectedFilterTab, achievements) {
        when (selectedFilterTab) {
            1 -> achievements.filter { it.category == AchievementCategory.ATTENDANCE }
            2 -> achievements.filter { it.category == AchievementCategory.WORK }
            3 -> achievements.filter { it.category == AchievementCategory.MILESTONES }
            else -> achievements
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements & Badges", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Unlocked summary banner
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
                            text = "$unlockedCount / $totalCount Unlocked",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1F1047)
                        )
                        Text(
                            text = "${(unlockedCount * 100) / totalCount}%",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = RewardsPurple
                        )
                    }
                    LinearProgressIndicator(
                        progress = { unlockedCount.toFloat() / totalCount.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = RewardsPurple,
                        trackColor = RewardsPurple.copy(alpha = 0.2f)
                    )
                }
            }

            // Filter Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RewardsPurple
            ) {
                Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                    Text("All", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                    Text("Attendance", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                    Text("Work", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
                Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                    Text("Milestones", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredAchievements) { achievement ->
                    AchievementBadgeItem(
                        achievement = achievement,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedAchievement by viewModel.selectedAchievement.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Badge Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        val ach = selectedAchievement
        if (ach == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No achievement selected")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(if (ach.isUnlocked) RewardsGold.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (ach.isUnlocked) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RewardsGold, modifier = Modifier.size(48.dp))
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(ach.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(ach.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(16.dp))

                        if (ach.isUnlocked) {
                            RewardStatusChip(statusText = "UNLOCKED ${ach.unlockedDate ?: ""}", statusColor = StatusGreen)
                        } else {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Progress: ${ach.currentProgress} / ${ach.maxProgress}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { ach.currentProgress.toFloat() / ach.maxProgress.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                    color = RewardsPurple,
                                    trackColor = RewardsPurple.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                RewardStatusChip(statusText = "LOCKED", statusColor = StatusOrange)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadgeItem(
    achievement: Achievement,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (achievement.isUnlocked) RewardsGold.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (achievement.isUnlocked) RewardsGold.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = RewardsGold, modifier = Modifier.size(28.dp))
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = achievement.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (achievement.isUnlocked) {
                Text("Unlocked", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
            } else {
                Text("${achievement.currentProgress}/${achievement.maxProgress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
