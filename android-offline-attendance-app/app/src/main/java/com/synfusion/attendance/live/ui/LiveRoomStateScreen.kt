package com.synfusion.attendance.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.live.model.LiveRoomMember
import com.synfusion.attendance.live.ui.components.LiveDarkBg
import com.synfusion.attendance.live.ui.components.LivePrimaryEmerald
import com.synfusion.attendance.live.ui.components.LiveSurfaceDark
import com.synfusion.attendance.live.ui.components.WorkDailyLiveAvatar

/**
 * Explicit room states: loading, unavailable (deleted/wrong link), ended and error.
 * A shared link to an inactive room resolves here instead of showing an empty player.
 */
@Composable
fun LiveRoomStateScreen(
    title: String,
    message: String,
    isLoading: Boolean = false,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LiveDarkBg)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .statusBarsPadding()
                .padding(8.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = LivePrimaryEmerald, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                title,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                color = Color.LightGray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            if (onRetry != null && !isLoading) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = LivePrimaryEmerald)
                ) {
                    Text("Retry", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onBack) {
                Text("Back to Live", color = LivePrimaryEmerald, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Profile sheet for a real room participant: uses the persisted membership document plus the
 * existing social graph follow state. Falls back to the full WorkDaily profile screen for details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMemberProfileSheet(
    member: LiveRoomMember,
    isSelf: Boolean,
    isFollowing: Boolean,
    followBusy: Boolean,
    onToggleFollow: () -> Unit,
    onOpenFullProfile: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        windowInsets = WindowInsets(0.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkDailyLiveAvatar(
                name = member.displayName,
                avatarUrl = member.photoUrl,
                size = 72.dp,
                isLive = false,
                showBadge = false
            )
            Text(
                member.displayName,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (member.username.isNotBlank()) {
                Text(
                    "@${member.username}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = buildString {
                        if (member.isHost) append("Host")
                        if (member.profession.isNotBlank()) {
                            if (isNotEmpty()) append(" · ")
                            append(member.profession)
                        }
                        if (isEmpty()) append("WorkDaily member")
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }

            if (!isSelf) {
                Button(
                    onClick = onToggleFollow,
                    enabled = !followBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isFollowing) LiveSurfaceDark else LivePrimaryEmerald,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isFollowing) "Following" else "Follow",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenFullProfile()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View full profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Text(
                "Follows, blocks and privacy are enforced by WorkDaily's existing social rules.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
