package com.synfusion.workdailyadmin.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.synfusion.workdailyadmin.ui.theme.AdminBrandPrimary

@Composable
fun KpiCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = AdminBrandPrimary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .padding(4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1
        )
    }
}

@Composable
fun AdminSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = "Search...",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholderText, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Truncates long identifier strings (e.g. UIDs, room IDs) cleanly with a one-tap copy button.
 */
@Composable
fun TruncatedIdChip(
    id: String,
    prefix: String = "ID: ",
    maxLength: Int = 12,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val displayText = if (id.length > maxLength) {
        val half = (maxLength - 3) / 2
        "${id.take(half)}...${id.takeLast(half)}"
    } else {
        id
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable {
                clipboardManager.setText(AnnotatedString(id))
                Toast.makeText(context, "Copied ID to clipboard", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$prefix$displayText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Default.ContentCopy,
            contentDescription = "Copy ID",
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Responsive user avatar with Coil image loading and deterministic fallback initials.
 */
@Composable
fun UserAvatar(
    photoUrl: String?,
    name: String,
    size: Int = 44,
    modifier: Modifier = Modifier
) {
    val initials = name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .take(2)
        .joinToString("")
        .ifBlank { "W" }
        .uppercase()

    if (!photoUrl.isNullOrBlank()) {
        SubcomposeAsyncImage(
            model = photoUrl,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size((size / 2).dp), strokeWidth = 2.dp)
                }
            },
            error = {
                InitialAvatar(initials = initials, size = size)
            },
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape)
        )
    } else {
        InitialAvatar(initials = initials, size = size, modifier = modifier)
    }
}

@Composable
private fun InitialAvatar(
    initials: String,
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(AdminBrandPrimary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size * 0.4).sp
        )
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
