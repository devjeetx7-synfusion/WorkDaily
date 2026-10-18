package com.synfusion.attendance.live.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.synfusion.attendance.live.model.DemoPrivateMessage
import com.synfusion.attendance.live.ui.components.*
import com.synfusion.attendance.live.viewmodel.LiveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateChatScreen(
    friendId: String,
    viewModel: LiveViewModel,
    onNavigateBack: () -> Unit,
    onNavigateProfile: (String) -> Unit
) {
    val context = LocalContext.current
    val publicUsers by viewModel.publicUsers.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val privateMessagesMap by viewModel.privateMessagesMap.collectAsState()

    val friend = remember(publicUsers, friendId) {
        publicUsers.find { it.id == friendId }
    }

    val conversation = remember(conversations, friendId) {
        conversations.find { it.friendId == friendId }
    }

    val messages = remember(privateMessagesMap, conversation) {
        if (conversation != null) {
            privateMessagesMap[conversation.id] ?: emptyList()
        } else emptyList()
    }

    var messageInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val isFriend = friend?.isFriend == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.clickable { friend?.let { onNavigateProfile(it.id) } }
                    ) {
                        PulsingLiveAvatar(
                            name = friend?.name ?: "Friend",
                            size = 32.dp,
                            isLive = friend?.isLiveNow == true
                        )
                        Column {
                            Text(
                                text = friend?.name ?: "Worker Friend",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isFriend) "Connected Friend" else "Not Friends",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View Profile") },
                                onClick = {
                                    showMenu = false
                                    friend?.let { onNavigateProfile(it.id) }
                                }
                            )
                            if (conversation != null) {
                                DropdownMenuItem(
                                    text = { Text("Clear Chat") },
                                    onClick = {
                                        showMenu = false
                                        viewModel.clearConversation(conversation.id)
                                        Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            if (friend != null) {
                                DropdownMenuItem(
                                    text = { Text("Unfriend", color = LiveRed) },
                                    onClick = {
                                        showMenu = false
                                        viewModel.unfriendUser(friend.id)
                                        Toast.makeText(context, "Unfriended user", Toast.LENGTH_SHORT).show()
                                        onNavigateBack()
                                    }
                                )
                            }
                        }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Messages List
            if (!isFriend) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Text(
                            text = "Only accepted friends can exchange private messages.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet. Say hello!", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { msg ->
                        PrivateMessageBubble(message = msg)
                    }
                }
            }

            // Input Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        enabled = isFriend,
                        placeholder = { Text(if (isFriend) "Type a message..." else "Only friends can chat", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp)),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (messageInput.isNotBlank()) {
                                val sent = viewModel.sendPrivateMessage(friendId, messageInput)
                                if (sent) {
                                    messageInput = ""
                                } else {
                                    Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = isFriend && messageInput.isNotBlank(),
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isFriend && messageInput.isNotBlank()) LivePrimaryEmerald else Color.Gray)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PrivateMessageBubble(message: DemoPrivateMessage) {
    val isMe = message.senderId == "current_user"

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) LivePrimaryEmerald else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = message.message,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 13.sp,
                color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
