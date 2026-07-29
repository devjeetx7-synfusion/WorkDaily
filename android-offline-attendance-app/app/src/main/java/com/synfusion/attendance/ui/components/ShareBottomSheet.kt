package com.synfusion.attendance.ui.components

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap

data class ShareAppInfo(
    val packageName: String,
    val activityName: String,
    val appName: String,
    val appIcon: Drawable
)

private const val APP_DESCRIPTION = "WorkDaily - Manage your daily labor, attendance, earnings, and payments efficiently."
private const val APP_LINK = "https://play.google.com/store/apps/details?id=com.synfusion.attendance"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareBottomSheet(
    onDismiss: () -> Unit,
    imageUri: Uri? = null
) {
    val context = LocalContext.current
    val apps = remember { getShareableApps(context) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share WorkDaily",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(apps) { appInfo ->
                    AppItem(appInfo) {
                        shareToApp(context, appInfo, imageUri)
                        onDismiss()
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(appInfo: ShareAppInfo, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            bitmap = appInfo.appIcon.toBitmap().asImageBitmap(),
            contentDescription = appInfo.appName,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = appInfo.appName,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun getShareableApps(context: Context): List<ShareAppInfo> {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
    }
    val pm = context.packageManager
    val resolveInfos = pm.queryIntentActivities(intent, 0)

    return resolveInfos.map { resolveInfo ->
        ShareAppInfo(
            packageName = resolveInfo.activityInfo.packageName,
            activityName = resolveInfo.activityInfo.name,
            appName = resolveInfo.loadLabel(pm).toString(),
            appIcon = resolveInfo.loadIcon(pm)
        )
    }.distinctBy { it.packageName }
}

private fun shareToApp(context: Context, appInfo: ShareAppInfo, imageUri: Uri?) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        setClassName(appInfo.packageName, appInfo.activityName)

        val shareText = "$APP_DESCRIPTION\n\nDownload Now: $APP_LINK"

        if (imageUri != null) {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, shareText)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
    }
    context.startActivity(shareIntent)
}
