package com.synfusion.attendance.rewards.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.RewardTask
import com.synfusion.attendance.rewards.data.TaskCategory
import com.synfusion.attendance.rewards.data.TaskStatus
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectTask: (RewardTask) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: Daily, 1: Weekly, 2: Special, 3: Completed

    val filteredTasks = remember(selectedFilterTab, tasks) {
        when (selectedFilterTab) {
            0 -> tasks.filter { it.category == TaskCategory.DAILY && it.status != TaskStatus.CLAIMED }
            1 -> tasks.filter { it.category == TaskCategory.WEEKLY && it.status != TaskStatus.CLAIMED }
            2 -> tasks.filter { it.category == TaskCategory.SPECIAL && it.status != TaskStatus.CLAIMED }
            3 -> tasks.filter { it.status == TaskStatus.CLAIMED }
            else -> tasks
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks & Challenges", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            PrimaryTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RewardsPurple
            ) {
                Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                    Text("Daily", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                    Text("Weekly", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                    Text("Special", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                    Text("Completed", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTasks) { task ->
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedTask by viewModel.selectedTask.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Task Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        val task = selectedTask
        if (task == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No task selected")
            }
        } else {
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(task.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            CoinAmount(amount = task.coinReward, fontSize = 18, textColor = RewardsPurple)
                        }
                        Text(task.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Progress (${task.currentProgress}/${task.maxProgress})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        LinearProgressIndicator(
                            progress = { task.currentProgress.toFloat() / task.maxProgress.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = RewardsPurple,
                            trackColor = RewardsPurple.copy(alpha = 0.15f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Requirements:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        task.requirements.forEach { req ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(req, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (task.status == TaskStatus.CLAIMABLE) {
                            Button(
                                onClick = {
                                    viewModel.claimTask(task.id)
                                    Toast.makeText(context, "Task claimed! +🪙${task.coinReward}", Toast.LENGTH_SHORT).show()
                                    onNavigateBack()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                            ) {
                                Text("Claim Reward (🪙${task.coinReward})", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else if (task.status == TaskStatus.CLAIMED) {
                            OutlinedButton(
                                onClick = { },
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Task Completed & Claimed", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    Toast.makeText(context, "Task in progress! Complete required actions.", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Start Task", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(task.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(8.dp))
                    CoinAmount(amount = task.coinReward, fontSize = 13, textColor = RewardsPurple)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(task.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${task.currentProgress}/${task.maxProgress}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RewardsPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { task.currentProgress.toFloat() / task.maxProgress.toFloat() },
                        modifier = Modifier.width(80.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = RewardsPurple,
                        trackColor = RewardsPurple.copy(alpha = 0.15f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            when (task.status) {
                TaskStatus.CLAIMABLE -> {
                    Button(
                        onClick = onClaim,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RewardsPurple)
                    ) {
                        Text("Claim", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
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
