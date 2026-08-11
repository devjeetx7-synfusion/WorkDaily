import re

file_path = './android-offline-attendance-app/app/src/main/java/com/synfusion/attendance/ui/SettingsScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# I will replace the DeveloperContactBottomSheet implementation with the new one.
# It currently starts with `@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun DeveloperContactBottomSheet(onDismiss: () -> Unit) {`
# and ends right before `@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nfun OfflineProfileEditBottomSheet` or at the end of the file.

new_developer_sheet = """
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperContactBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                coil.compose.AsyncImage(
                    model = coil.request.ImageRequest.Builder(context)
                        .data("https://i.postimg.cc/tRFX32BV/IMG-20260724-202445.png")
                        .crossfade(true)
                        .build(),
                    contentDescription = "Dharmjeet Singh Profile",
                    modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = coil.compose.rememberAsyncImagePainter(model = Icons.Default.Person),
                    placeholder = coil.compose.rememberAsyncImagePainter(model = Icons.Default.Person)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dharmjeet Singh", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("@devjeetd7 | SynFusion", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("workdaily.synfusion@gmail.com", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ContactIconItem(
                    icon = Icons.Default.Code,
                    contentDescription = "GitHub",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devjeetx7-synfusion")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle GitHub link", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.CameraAlt,
                    contentDescription = "Instagram",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/devjeetx7")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle Instagram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.Send,
                    contentDescription = "Telegram",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/getsynfusion")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle Telegram", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "YouTube",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/@getsynfusiom")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No app found to handle YouTube", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                ContactIconItem(
                    icon = Icons.Default.Email,
                    contentDescription = "Email",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:workdaily.synfusion@gmail.com")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
"""

# Regex to match the DeveloperContactBottomSheet function and ContactIconItem exactly.
pattern = r'@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun DeveloperContactBottomSheet\(.*?\)\s*\{.*?(?=@OptIn\(ExperimentalMaterial3Api::class\)\s*@Composable\s*fun OfflineProfileEditBottomSheet)'

if re.search(pattern, content, re.DOTALL):
    new_content = re.sub(pattern, new_developer_sheet + "\n", content, flags=re.DOTALL)

    # We must also retain ContactIconItem if it was removed by the greedy regex.
    # Let's ensure it's in the new string.
    if 'fun ContactIconItem' not in new_content:
        contact_icon_item = """
@Composable
fun ContactIconItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
    }
}
"""
        new_content = new_content.replace(new_developer_sheet, new_developer_sheet + contact_icon_item)

    with open(file_path, 'w') as f:
        f.write(new_content)
    print("Replaced DeveloperContactBottomSheet")
else:
    print("Could not find DeveloperContactBottomSheet to replace")
