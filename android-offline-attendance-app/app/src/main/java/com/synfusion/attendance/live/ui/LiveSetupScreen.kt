package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveSetupScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember { mutableStateOf(1) } // 1 = Choose Format, 2 = Setup Details
    var selectedType by remember { mutableStateOf(LiveRoomType.VIDEO) }

    var titleInput by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Site Safety") }
    var descriptionInput by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    var isChatEnabled by remember { mutableStateOf(true) }
    var allowGuestRequests by remember { mutableStateOf(true) }
    var showGifts by remember { mutableStateOf(true) }
    var micEnabled by remember { mutableStateOf(true) }
    var cameraEnabled by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    val categories = listOf(
        "Construction",
        "Site Safety",
        "Masonry",
        "Plumbing",
        "Electrical",
        "Painting",
        "Carpentry",
        "Machinery",
        "Worker Discussions",
        "General Chat"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentStep == 1) "Go Live — Choose Format" else "Go Live — Stream Setup", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 2) currentStep = 1 else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentStep == 1) {
                // STEP 1: Choose Format (Audio vs Video)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Select Live Experience", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                        Text("Choose how you want to interact with site workers and followers.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Format Option 1: VIDEO LIVE
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedType = LiveRoomType.VIDEO
                                currentStep = 2
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LivePrimaryEmerald.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Videocam, contentDescription = null, tint = LiveRed, modifier = Modifier.size(32.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("VIDEO LIVE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    LiveBadge(text = "CAMERA", bgColor = LiveRed)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Full live camera broadcast with live comment feed, reactions, and construction gifts.", fontSize = 12.sp, color = Color.LightGray)
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LivePrimaryMint)
                        }
                    }

                    // Format Option 2: AUDIO LIVE
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedType = LiveRoomType.AUDIO
                                currentStep = 2
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = LiveSurfaceDark),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LivePurple.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(LivePurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = LivePurple, modifier = Modifier.size(32.dp))
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("AUDIO LIVE", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    LiveBadge(text = "VOICE STAGE", bgColor = LivePurple)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Voice stage panel with 6-8 speaker seats for worker Q&A and site discussions.", fontSize = 12.sp, color = Color.LightGray)
                            }

                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = LivePurple)
                        }
                    }
                }
            } else {
                // STEP 2: Live Setup Form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Stream Title
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Live Stream Title *") },
                        placeholder = { Text("e.g. Site Safety Inspection & Concrete Pouring") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp)),
                        singleLine = true
                    )

                    // Category Selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Category *", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            categories.forEach { cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = LivePrimaryEmerald,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // Optional Description
                    OutlinedTextField(
                        value = descriptionInput,
                        onValueChange = { descriptionInput = it },
                        label = { Text("Description (Optional)") },
                        placeholder = { Text("Provide site location details or topic agenda...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )

                    // Toggles Group
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Public Access", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Enable Live Chat", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = isChatEnabled, onCheckedChange = { isChatEnabled = it })
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Allow Guest Requests", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = allowGuestRequests, onCheckedChange = { allowGuestRequests = it })
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show Gifts in Room", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Switch(checked = showGifts, onCheckedChange = { showGifts = it })
                            }
                        }
                    }

                    // Demo Mic/Camera State Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = micEnabled,
                            onClick = { micEnabled = !micEnabled },
                            leadingIcon = { Icon(if (micEnabled) Icons.Default.Mic else Icons.Default.MicOff, contentDescription = null) },
                            label = { Text(if (micEnabled) "Mic On" else "Mic Muted") },
                            modifier = Modifier.weight(1f)
                        )

                        if (selectedType == LiveRoomType.VIDEO) {
                            FilterChip(
                                selected = cameraEnabled,
                                onClick = { cameraEnabled = !cameraEnabled },
                                leadingIcon = { Icon(if (cameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff, contentDescription = null) },
                                label = { Text(if (cameraEnabled) "Camera On" else "Camera Off") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Start Demo Live CTA
                    Button(
                        onClick = {
                            if (titleInput.isBlank()) {
                                Toast.makeText(context, "Please enter a stream title!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (isSubmitting) return@Button
                            isSubmitting = true

                            val createdRoom = viewModel.startLiveStream(
                                title = titleInput.trim(),
                                category = selectedCategory,
                                type = selectedType,
                                tags = listOf(selectedCategory, "SiteWork"),
                                description = descriptionInput,
                                isPublic = isPublic,
                                allowGuestRequests = allowGuestRequests,
                                showGifts = showGifts
                            )

                            Toast.makeText(context, "You are LIVE! 🔴", Toast.LENGTH_SHORT).show()
                            onNavigateLiveRoom(createdRoom.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Podcasts, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Demo Live", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
