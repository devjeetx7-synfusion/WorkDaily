package com.synfusion.attendance.ui.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synfusion.attendance.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerSettingsSheet(
    fileName: String,
    totalPages: Int,
    fileSizeStr: String,
    viewMode: PdfViewMode,
    fitMode: PdfFitMode,
    showTouchIndicators: Boolean,
    onToggleTouchIndicators: () -> Unit,
    onOpenGoToPage: () -> Unit,
    onOpenThumbnails: () -> Unit,
    onToggleViewMode: () -> Unit,
    onSelectFitMode: (PdfFitMode) -> Unit,
    onPrint: () -> Unit,
    onOpenWith: () -> Unit,
    onDismiss: () -> Unit,
    onCloseViewer: () -> Unit
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.pdf_more),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            // Page Jump Action
            MenuSheetRow(
                icon = Icons.Default.Pin,
                label = stringResource(R.string.pdf_go_to_page),
                onClick = {
                    onDismiss()
                    onOpenGoToPage()
                }
            )

            // Thumbnails Action
            MenuSheetRow(
                icon = Icons.Default.GridView,
                label = stringResource(R.string.pdf_thumbnails),
                onClick = {
                    onDismiss()
                    onOpenThumbnails()
                }
            )

            // View Mode Toggle
            MenuSheetRow(
                icon = if (viewMode == PdfViewMode.VERTICAL) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                label = stringResource(R.string.pdf_view_mode) + ": " + if (viewMode == PdfViewMode.VERTICAL) stringResource(R.string.pdf_vertical) else stringResource(R.string.pdf_horizontal),
                onClick = {
                    onToggleViewMode()
                }
            )

            // Fit Mode Option Cards
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.pdf_fit_mode),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FitModeChip(
                        label = stringResource(R.string.pdf_fit_width),
                        selected = fitMode == PdfFitMode.FIT_WIDTH,
                        onClick = { onSelectFitMode(PdfFitMode.FIT_WIDTH) },
                        modifier = Modifier.weight(1f)
                    )
                    FitModeChip(
                        label = stringResource(R.string.pdf_fit_page),
                        selected = fitMode == PdfFitMode.FIT_PAGE,
                        onClick = { onSelectFitMode(PdfFitMode.FIT_PAGE) },
                        modifier = Modifier.weight(1f)
                    )
                    FitModeChip(
                        label = stringResource(R.string.pdf_actual_size),
                        selected = fitMode == PdfFitMode.ACTUAL_SIZE,
                        onClick = { onSelectFitMode(PdfFitMode.ACTUAL_SIZE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Print Action
            MenuSheetRow(
                icon = Icons.Default.Print,
                label = stringResource(R.string.pdf_print),
                onClick = {
                    onDismiss()
                    onPrint()
                }
            )

            // Open With Action
            MenuSheetRow(
                icon = Icons.Default.OpenInNew,
                label = stringResource(R.string.pdf_open_with),
                onClick = {
                    onDismiss()
                    onOpenWith()
                }
            )

            // PDF Information Action
            MenuSheetRow(
                icon = Icons.Default.Info,
                label = stringResource(R.string.pdf_info),
                onClick = { showInfoDialog = true }
            )

            // Touch Indicators Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleTouchIndicators() }
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = stringResource(R.string.pdf_touch_indicators),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.pdf_touch_indicators),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Switch(
                    checked = showTouchIndicators,
                    onCheckedChange = { onToggleTouchIndicators() }
                )
            }

            // Close Viewer Action
            MenuSheetRow(
                icon = Icons.Default.Close,
                label = stringResource(R.string.pdf_close),
                textColor = MaterialTheme.colorScheme.error,
                iconTint = MaterialTheme.colorScheme.error,
                onClick = {
                    onDismiss()
                    onCloseViewer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.pdf_info),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pdf_file_name),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.pdf_total_pages),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = totalPages.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (fileSizeStr.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.pdf_file_size),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = fileSizeStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
}

@Composable
private fun MenuSheetRow(
    icon: ImageVector,
    label: String,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    iconTint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun FitModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        },
        modifier = modifier
    )
}
