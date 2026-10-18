package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val hapticsEnabled by viewModel.hapticsEnabled.collectAsState()

    val context = LocalContext.current

    var diamondsInput by remember { mutableStateOf(wallet.diamondBalance.toString()) }
    var creatorDiamondsInput by remember { mutableStateOf(creatorEarning.totalDiamondsReceived.toString()) }
    var followersInput by remember { mutableStateOf("1420") }
    var viewerCountInput by remember { mutableStateOf("350") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.BugReport, contentDescription = null, tint = LivePrimaryEmerald)
                    Text("Developer Demo Controls", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                text = "Modify Live feature demo state, balances, sound effects, and simulate transactions locally for developer testing.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Sound & Haptics Toggles
            Text("Sound & Feedback Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Live Audio Effects", fontSize = 13.sp)
                Switch(checked = soundEnabled, onCheckedChange = { viewModel.toggleSoundEnabled() })
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Haptic Feedback", fontSize = 13.sp)
                Switch(checked = hapticsEnabled, onCheckedChange = { viewModel.toggleHapticsEnabled() })
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Editable Demo Values
            Text("Editable Balances & Stats", fontSize = 14.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = diamondsInput,
                    onValueChange = { diamondsInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Diamonds") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = creatorDiamondsInput,
                    onValueChange = { creatorDiamondsInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Creator Earned") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = followersInput,
                    onValueChange = { followersInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Followers") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = viewerCountInput,
                    onValueChange = { viewerCountInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Simulated Viewers") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Button(
                onClick = {
                    viewModel.updateDemoDataOverrides(
                        diamonds = diamondsInput.toIntOrNull(),
                        creatorDiamonds = creatorDiamondsInput.toIntOrNull(),
                        followersCount = followersInput.toIntOrNull(),
                        viewerCount = viewerCountInput.toIntOrNull()
                    )
                    Toast.makeText(context, "Demo balances & stats updated!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply Demo Overrides", fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Quick Actions
            Button(
                onClick = {
                    viewModel.addFreeDiamonds(1000)
                    diamondsInput = (wallet.diamondBalance + 1000).toString()
                    Toast.makeText(context, "+1000 Demo Diamonds Added! 💎", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add +1000 Diamonds", fontWeight = FontWeight.Bold)
            }

            // Reset Demo Data
            OutlinedButton(
                onClick = {
                    viewModel.resetDemoData()
                    Toast.makeText(context, "All Live Demo Data Reset to Default!", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LiveRed),
                border = androidx.compose.foundation.BorderStroke(1.dp, LiveRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset All Live Demo Data", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
