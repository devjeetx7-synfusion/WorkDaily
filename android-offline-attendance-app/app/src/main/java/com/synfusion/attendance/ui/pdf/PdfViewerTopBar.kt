package com.synfusion.attendance.ui.pdf

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerTopBar(
    title: String,
    currentPage: Int,
    totalPages: Int,
    isAutoSliding: Boolean = false,
    onNavigateBack: () -> Unit,
    onToggleAutoSlide: () -> Unit = {},
    onOpenMore: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title.ifEmpty { stringResource(R.string.pdf_viewer_title) },
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.pdf_go_back),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = {
            if (totalPages > 1) {
                IconButton(onClick = onToggleAutoSlide) {
                    Icon(
                        imageVector = if (isAutoSliding) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isAutoSliding) "Pause" else "Play",
                        tint = if (isAutoSliding) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            IconButton(onClick = onOpenMore) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.pdf_more),
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
        ),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}
