package com.synfusion.attendance.ui.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R

data class ShareTargetItem(
    val id: String,
    val name: String,
    val isInstalled: Boolean,
    val packageName: String?,
    val iconResId: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerShareSheet(
    isWhatsAppInstalled: Boolean,
    isTelegramInstalled: Boolean,
    isGmailInstalled: Boolean,
    onShareToApp: (String) -> Unit,
    onShareMore: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.pdf_share),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWhatsAppInstalled) {
                    ShareTargetButton(
                        label = "WhatsApp",
                        bgColor = Color(0xFF25D366),
                        onClick = {
                            onShareToApp("com.whatsapp")
                            onDismiss()
                        }
                    )
                }

                if (isTelegramInstalled) {
                    ShareTargetButton(
                        label = "Telegram",
                        bgColor = Color(0xFF0088CC),
                        onClick = {
                            onShareToApp("org.telegram.messenger")
                            onDismiss()
                        }
                    )
                }

                if (isGmailInstalled) {
                    ShareTargetButton(
                        label = "Gmail",
                        bgColor = Color(0xFFEA4335),
                        onClick = {
                            onShareToApp("com.google.android.gm")
                            onDismiss()
                        }
                    )
                }

                ShareTargetButton(
                    label = stringResource(R.string.pdf_more),
                    bgColor = MaterialTheme.colorScheme.primary,
                    onClick = {
                        onShareMore()
                        onDismiss()
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareTargetButton(
    label: String,
    bgColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
