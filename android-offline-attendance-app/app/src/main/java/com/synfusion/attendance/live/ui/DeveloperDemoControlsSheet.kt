package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDemoControlsSheet(
    viewModel: LiveViewModel,
    onDismiss: () -> Unit
) {
    val wallet by viewModel.wallet.collectAsState()
    val creatorEarning by viewModel.creatorEarning.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LiveDarkBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = LivePrimaryMint, modifier = Modifier.size(24.dp))
                Text("Developer Demo Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Text(
                "Modify Live feature demo state, balances, and simulate transactions locally for testing.",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))

            // Action 1: Add Free Diamonds
            Button(
                onClick = {
                    viewModel.addFreeDiamonds(1000)
                    Toast.makeText(context, "+1000 Demo Diamonds Added! 💎", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add +1000 Diamonds (Current: ${wallet.diamondBalance} 💎)", fontWeight = FontWeight.Bold)
            }

            // Action 2: Reset Demo Data
            OutlinedButton(
                onClick = {
                    viewModel.resetDemoData()
                    Toast.makeText(context, "All Live Demo Data Reset to Default!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Live Demo Data", fontWeight = FontWeight.Bold)
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
