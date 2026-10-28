package com.synfusion.attendance.live.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.synfusion.attendance.live.model.WatchPartyPlaylistItem
import com.synfusion.attendance.live.model.WatchPartyState
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LivePrimaryMint
import com.synfusion.attendance.live.watchparty.YouTubeLinkParser
import com.synfusion.attendance.live.watchparty.YouTubeParseResult

/**
 * Watch Party playlist sheet.
 *
 * - Only the host can add/select/remove items or turn the Watch Party off; participants get a
 *   read-only queue plus a *local* mute toggle (their audio preference is not published).
 * - Video titles/thumbnails always come from the persisted playlist documents. Nothing is
 *   hard-coded; when metadata is missing the row shows the real video id.
 * - The URL field and Add button stay visible when the keyboard opens (`imePadding`) while the list
 *   itself scrolls, so long playlists never push the composer around.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchPartyPlaylistSheet(
    watchPartyState: WatchPartyState?,
    playlist: List<WatchPartyPlaylistItem>,
    isHost: Boolean,
    isSessionBacked: Boolean,
    isLoading: Boolean,
    locallyMuted: Boolean,
    onToggleLocalMute: () -> Unit,
    onAddVideo: (String) -> Unit,
    onSelectItem: (WatchPartyPlaylistItem) -> Unit,
    onRemoveItem: (WatchPartyPlaylistItem) -> Unit,
    onTurnOff: () -> Unit,
    onTurnOn: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputUrl by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    val activeItemId = watchPartyState?.activePlaylistItemId
    val sharedMuted = watchPartyState?.isMuted == true
    val isTurnedOff = watchPartyState?.isTurnedOff == true
    val canEdit = isHost && isSessionBacked

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        windowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Fixed header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Watch Party Playlist",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleLocalMute, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (sharedMuted || locallyMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (locallyMuted) "Unmute locally" else "Mute locally",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (isHost && isSessionBacked) {
                        TextButton(
                            onClick = { if (isTurnedOff) onTurnOn() else onTurnOff() },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isTurnedOff) LivePrimaryEmerald else Color(0xFFEF4444)
                            )
                        ) {
                            Text(
                                text = if (isTurnedOff) "Turn On" else "Turn Off",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Host-only add field (validated locally, then by the backend)
            if (canEdit) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Add YouTube video",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = {
                                    inputUrl = it
                                    inputError = null
                                },
                                placeholder = { Text("Paste a YouTube link or 11-char ID", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                isError = inputError != null
                            )
                            Button(
                                onClick = {
                                    when (val parsed = YouTubeLinkParser.parse(inputUrl)) {
                                        is YouTubeParseResult.Success -> {
                                            if (!parsed.selection.isVideoPlayable) {
                                                inputError = "Add a single video link (playlists are set on the room)"
                                            } else if (playlist.any {
                                                    it.videoId.equals(parsed.selection.videoId, ignoreCase = true)
                                                }
                                            ) {
                                                inputError = "Already in the playlist"
                                            } else {
                                                onAddVideo(inputUrl)
                                                inputUrl = ""
                                                inputError = null
                                            }
                                        }

                                        is YouTubeParseResult.Failure -> inputError = parsed.message
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        inputError?.let { message ->
                            Text(
                                message,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            "YouTube and YouTube Music video links are supported.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Fixed list header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlist (${playlist.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isTurnedOff) {
                    Text(
                        "Playback off",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF87171)
                    )
                }
            }

            when {
                isLoading && playlist.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = LivePrimaryMint, strokeWidth = 2.dp)
                    }
                }

                playlist.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(38.dp)
                            )
                            Text(
                                if (canEdit) "No videos yet. Add a YouTube link above." else "The host has not added videos yet.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 12.dp)
                    ) {
                        items(items = playlist, key = { it.id }) { item ->
                            PlaylistRow(
                                item = item,
                                isActive = item.id == activeItemId,
                                canEdit = canEdit,
                                onSelect = { onSelectItem(item) },
                                onRemove = { onRemoveItem(item) }
                            )
                        }
                    }
                }
            }

            Text(
                text = when {
                    !isSessionBacked -> "Local demo room — playlist changes are not persisted."
                    isHost -> "Changes are saved and synced to everyone in this room."
                    else -> "Only the host can change the queue."
                },
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    item: WatchPartyPlaylistItem,
    isActive: Boolean,
    canEdit: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) {
            LivePrimaryEmerald.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        },
        border = BorderStroke(
            1.dp,
            if (isActive) LivePrimaryEmerald.copy(alpha = 0.6f) else Color.Transparent
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = item.thumbnailUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title.ifBlank { item.videoId },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Added by ${item.addedByUserName.ifBlank { "host" }}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isActive) {
                    Text(
                        "▶ PLAYING NOW",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LivePrimaryEmerald
                    )
                }
            }

            if (canEdit) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isActive && item.videoId.isNotBlank()) {
                        IconButton(onClick = onSelect, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play now",
                                tint = LivePrimaryMint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove from playlist",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
