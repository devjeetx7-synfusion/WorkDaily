package com.synfusion.attendance.ui.pdf

import android.net.Uri

data class PdfViewerState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val zoom: Float = 1.0f,
    val isControlsVisible: Boolean = true,
    val fabExpanded: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fileSizeStr: String = "",
    val showPageJumpDialog: Boolean = false,
    val showThumbnailsSheet: Boolean = false
)
