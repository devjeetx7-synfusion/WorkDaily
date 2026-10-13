package com.synfusion.attendance.rewards.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.synfusion.attendance.rewards.data.RewardTransactionStatus
import com.synfusion.attendance.rewards.data.RewardTransactionType
import com.synfusion.attendance.rewards.ui.components.*
import com.synfusion.attendance.rewards.viewmodel.RewardsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoinHistoryScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit,
    onSelectTransaction: (RewardTransaction) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState()
    val tabs = remember { listOf("All", "Earned", "Pending", "Spent") }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Coin History",
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
            // Custom Synchronized Swipeable Tab Bar
            RewardTabRow(
                tabs = tabs,
                pagerState = pagerState
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val filteredTransactions = remember(pageIndex, transactions) {
                    when (pageIndex) {
                        1 -> transactions.filter { it.type == RewardTransactionType.EARNED }
                        2 -> transactions.filter { it.type == RewardTransactionType.PENDING }
                        3 -> transactions.filter { it.type == RewardTransactionType.SPENT }
                        else -> transactions
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    RewardEmptyState(
                        title = "No ${tabs[pageIndex]} Transactions",
                        description = "Your coin activities for this category will appear here once available."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
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
    }
}

@Composable
fun TransactionDetailScreen(
    viewModel: RewardsViewModel,
    onNavigateBack: () -> Unit
) {
    val transaction by viewModel.selectedTransaction.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            RewardsTopAppBar(
                title = "Transaction Details",
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val tx = transaction
        if (tx == null) {
            RewardEmptyState(
                title = "No Transaction Selected",
                description = "Please select a transaction from history to view details."
            )
        } else {
            val statusColor = when (tx.status) {
                RewardTransactionStatus.CONFIRMED, RewardTransactionStatus.COMPLETED -> StatusGreen
                RewardTransactionStatus.PENDING -> StatusOrange
                RewardTransactionStatus.REVERSED, RewardTransactionStatus.REJECTED -> StatusRed
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
                        RewardCoinBalance(
                            amount = tx.amount,
                            fontSize = 28,
                            showSign = true,
                            textColor = if (tx.amount < 0) StatusRed else statusColor,
                            coinSize = 32.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RewardStatusChip(statusText = tx.status.name, statusColor = statusColor)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
