package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveRoomType
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateLiveRoom: (String) -> Unit
) {
    val context = LocalContext.current

    var streamTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Site Safety") }
    var selectedType by remember { mutableStateOf(LiveRoomType.VIDEO) }
    var tagsInput by remember { mutableStateOf("Construction, LiveWork") }

    val categories = listOf("Site Safety", "Machinery", "Masonry", "Engineering", "Contractor", "Plumbing", "Electrical")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Creator Studio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LiveDarkBg, titleContentColor = Color.White)
            )
        },
        containerColor = LiveDarkBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Setup Your Live Site Stream", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)

            // Stream Title Field
            OutlinedTextField(
                value = streamTitle,
                onValueChange = { streamTitle = it },
                label = { Text("Stream Title", color = Color.Gray) },
                placeholder = { Text("e.g., Concrete Pouring & Safety Briefing") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LivePrimaryMint,
                    unfocusedBorderColor = LivePrimaryEmerald.copy(alpha = 0.3f),
                    focusedContainerColor = LiveSurfaceDark,
                    unfocusedContainerColor = LiveSurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            // Stream Type Selection: Video Stream vs Audio Stage
            Text("Stream Format", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedType = LiveRoomType.VIDEO },
                    color = if (selectedType == LiveRoomType.VIDEO) LivePrimaryEmerald.copy(alpha = 0.3f) else LiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedType == LiveRoomType.VIDEO) LivePrimaryMint else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null, tint = LiveRed)
                        Column {
                            Text("Video Stream", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Live site camera feed", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedType = LiveRoomType.AUDIO },
                    color = if (selectedType == LiveRoomType.AUDIO) LivePurple.copy(alpha = 0.3f) else LiveSurfaceDark,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedType == LiveRoomType.AUDIO) LivePurple else Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = LivePurple)
                        Column {
                            Text("Audio Stage", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Voice panel discussion", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Category Selection
            Text("Category", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(3).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LivePrimaryEmerald,
                                    selectedLabelColor = Color.White,
                                    containerColor = LiveSurfaceDark,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            // Tags Input
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (comma separated)", color = Color.Gray) },
                placeholder = { Text("Safety, Civil, Machinery") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LivePrimaryMint,
                    unfocusedBorderColor = LivePrimaryEmerald.copy(alpha = 0.3f),
                    focusedContainerColor = LiveSurfaceDark,
                    unfocusedContainerColor = LiveSurfaceDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Start Live Stream CTA
            Button(
                onClick = {
                    val title = streamTitle.ifBlank { "Live Site Workday Discussion" }
                    val tags = tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val createdRoom = viewModel.startLiveStream(title, selectedCategory, selectedType, tags)
                    Toast.makeText(context, "You are LIVE! 🔴", Toast.LENGTH_SHORT).show()
                    onNavigateLiveRoom(createdRoom.id)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Podcasts, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Stream Now", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
