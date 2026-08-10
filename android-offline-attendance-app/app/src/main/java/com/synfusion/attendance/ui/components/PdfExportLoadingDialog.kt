package com.synfusion.attendance.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.PdfExportState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle

@Composable
fun PdfExportLoadingDialog(
    state: PdfExportState,
    onCancel: () -> Unit,
    onDismissError: () -> Unit
) {
    if (state is PdfExportState.Idle) return

    Dialog(
        onDismissRequest = {
            if (state is PdfExportState.Error) {
                onDismissError()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = state is PdfExportState.Error,
            dismissOnClickOutside = state is PdfExportState.Error
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (state) {
                    is PdfExportState.Preparing -> {
                        Text(
                            text = stringResource(R.string.pdf_export_preparing),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.skip)) // Or Cancel
                        }
                    }
                    is PdfExportState.CollectingRecords -> {
                        Text(
                            text = stringResource(R.string.pdf_export_collecting),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.skip))
                        }
                    }
                    is PdfExportState.GeneratingPdf -> {
                        Text(
                            text = stringResource(R.string.pdf_export_generating),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.skip))
                        }
                    }
                    is PdfExportState.Finalizing -> {
                        Text(
                            text = stringResource(R.string.pdf_export_finalizing),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onCancel) {
                            Text(stringResource(R.string.skip))
                        }
                    }
                    is PdfExportState.Success -> {
                        Text(
                            text = stringResource(R.string.pdf_export_success),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent()
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = com.synfusion.attendance.ui.theme.AttendanceStatusColors.getPresent(),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    is PdfExportState.Error -> {
                        Text(
                            text = stringResource(R.string.restore_failed),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = stringResource(R.string.pdf_export_error, state.message),
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = onDismissError,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("OK")
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
