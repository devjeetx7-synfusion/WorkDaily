package com.synfusion.attendance.rewards.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R
import com.synfusion.attendance.rewards.data.RewardTask
import com.synfusion.attendance.rewards.data.TaskCategory
import com.synfusion.attendance.rewards.data.TaskStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectTask: (RewardTask) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val tabs = remember { listOf("Daily", "Weekly", "Special", "Completed") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Tasks & Challenges",
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
                val filteredTasks = remember(pageIndex, tasks) {
                    when (pageIndex) {
                        0 -> tasks.filter { it.category == TaskCategory.DAILY && it.status != TaskStatus.CLAIMED }
                        1 -> tasks.filter { it.category == TaskCategory.WEEKLY && it.status != TaskStatus.CLAIMED }
                        2 -> tasks.filter { it.category == TaskCategory.SPECIAL && it.status != TaskStatus.CLAIMED }
                        3 -> tasks.filter { it.status == TaskStatus.CLAIMED }
                        else -> tasks
                    }
                }

                if (filteredTasks.isEmpty()) {
                    RewardEmptyState(
                        title = "No ${tabs[pageIndex]} Tasks",
                        description = "All tasks in this category have been completed or none are currently available."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTasks, key = { it.id }) { task ->
                            TaskCardRow(
                                task = task,
                                onClick = {
                                    viewModel.selectTask(task)
                                    onSelectTask(task)
                                },
                                onClaim = {
                                    viewModel.claimTask(task.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedTask by viewModel.selectedTask.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Task Details",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val task = selectedTask
        if (task == null) {
            RewardEmptyState(
                title = "No Task Selected",
                description = "Please select a task from the list to view its details."
            )
        } else {
            val drawableRes = when (task.category) {
                TaskCategory.DAILY -> R.drawable.task_daily
                TaskCategory.WEEKLY -> R.drawable.task_weekly
                TaskCategory.SPECIAL -> R.drawable.task_special
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = drawableRes),
                            contentDescription = task.title,
                            modifier = Modifier.size(80.dp)
                        )

                        Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(task.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        RewardCoinBalance(amount = task.coinReward, fontSize = 22, textColor = RewardsPurple, coinSize = 24.dp)

                        Spacer(modifier = Modifier.height(4.dp))

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Progress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${task.currentProgress} / ${task.maxProgress}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            RewardProgressBar(progress = task.currentProgress.toFloat() / task.maxProgress.toFloat())
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Requirements:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            task.requirements.forEach { req ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(req, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (task.status) {
                            TaskStatus.CLAIMABLE -> {
                                RewardPrimaryButton(
                                    text = "Claim Reward (+🪙${task.coinReward})",
                                    onClick = {
                                        viewModel.claimTask(task.id)
                                        Toast.makeText(context, "Task claimed! +🪙${task.coinReward}", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                            TaskStatus.CLAIMED -> {
                                RewardSecondaryButton(
                                    text = "Task Completed & Claimed",
                                    onClick = { },
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                            TaskStatus.IN_PROGRESS -> {
                                RewardPrimaryButton(
                                    text = "Task In Progress",
                                    onClick = {
                                        Toast.makeText(context, "Task in progress! Complete required actions.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCardRow(
    task: RewardTask,
    onClick: () -> Unit,
    onClaim: () -> Unit
) {
    val drawableRes = when (task.category) {
        TaskCategory.DAILY -> R.drawable.task_daily
        TaskCategory.WEEKLY -> R.drawable.task_weekly
        TaskCategory.SPECIAL -> R.drawable.task_special
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = task.title,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    RewardCoinBalance(amount = task.coinReward, fontSize = 12, textColor = RewardsPurple, coinSize = 13.dp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${task.currentProgress}/${task.maxProgress}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    RewardProgressBar(
                        progress = task.currentProgress.toFloat() / task.maxProgress.toFloat(),
                        modifier = Modifier.width(80.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            when (task.status) {
                TaskStatus.CLAIMABLE -> {
                    RewardPrimaryButton(
                        text = "Claim",
                        onClick = onClaim,
                        modifier = Modifier.height(36.dp)
                    )
                }
                TaskStatus.CLAIMED -> {
                    RewardStatusChip(statusText = "Completed", statusColor = StatusGreen)
                }
                TaskStatus.IN_PROGRESS -> {
                    RewardStatusChip(statusText = "In Progress", statusColor = StatusOrange)
                }
            }
        }
    }
}
