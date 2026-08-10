package com.synfusion.attendance.utils

import java.io.File

sealed class PdfExportState {
    object Idle : PdfExportState()
    object Preparing : PdfExportState()
    object CollectingRecords : PdfExportState()
    object GeneratingPdf : PdfExportState()
    object Finalizing : PdfExportState()
    data class Success(val file: File) : PdfExportState()
    data class Error(val message: String) : PdfExportState()
}
