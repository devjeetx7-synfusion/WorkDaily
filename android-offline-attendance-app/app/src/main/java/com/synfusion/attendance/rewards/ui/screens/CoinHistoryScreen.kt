package com.synfusion.attendance.rewards.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.rewards.data.RewardTransaction
import com.synfusion.attendance.rewards.data.RewardTransactionType
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectTransaction: (RewardTransaction) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    var selectedFilterTab by remember { mutableStateOf(0) } // 0: All, 1: Earned, 2: Pending, 3: Spent

    val filteredTransactions = remember(selectedFilterTab, transactions) {
        when (selectedFilterTab) {
            1 -> transactions.filter { it.type == RewardTransactionType.EARNED }
            2 -> transactions.filter { it.type == RewardTransactionType.PENDING }
            3 -> transactions.filter { it.type == RewardTransactionType.SPENT }
            else -> transactions
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coin History", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            // Filter Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedFilterTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = RewardsPurple
            ) {
                Tab(selected = selectedFilterTab == 0, onClick = { selectedFilterTab = 0 }) {
                    Text("All", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 1, onClick = { selectedFilterTab = 1 }) {
                    Text("Earned", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 2, onClick = { selectedFilterTab = 2 }) {
                    Text("Pending", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Tab(selected = selectedFilterTab == 3, onClick = { selectedFilterTab = 3 }) {
                    Text("Spent", modifier = Modifier.padding(vertical = 12.dp), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredTransactions) { tx ->
                    RewardTransactionRow(
                        transaction = tx,
                        onClick = {
                            viewModel.selectTransaction(tx)
                            onSelectTransaction(tx)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val transaction by viewModel.selectedTransaction.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        val tx = transaction
        if (tx == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No transaction selected")
            }
        } else {
            val statusColor = when (tx.status) {
                com.synfusion.attendance.rewards.data.RewardTransactionStatus.CONFIRMED,
                com.synfusion.attendance.rewards.data.RewardTransactionStatus.COMPLETED -> StatusGreen
                com.synfusion.attendance.rewards.data.RewardTransactionStatus.PENDING -> StatusOrange
                com.synfusion.attendance.rewards.data.RewardTransactionStatus.REVERSED,
                com.synfusion.attendance.rewards.data.RewardTransactionStatus.REJECTED -> StatusRed
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
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = tx.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CoinAmount(
                            amount = tx.amount,
                            fontSize = 28,
                            showSign = true,
                            textColor = if (tx.amount < 0) StatusRed else statusColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RewardStatusChip(statusText = tx.status.name, statusColor = statusColor)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailRow("Description", tx.description)
                        DetailRow("Date & Time", tx.timestamp)
                        DetailRow("Source", tx.source)
                        DetailRow("Transaction ID", tx.id)

                        if (tx.referenceCode != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Reference Code", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(tx.referenceCode, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Reference Code", tx.referenceCode)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied reference code!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = RewardsPurple)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
