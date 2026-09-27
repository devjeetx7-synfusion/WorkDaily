package com.synfusion.attendance.ui.pdf

import android.net.Uri

enum class PdfViewMode {
    VERTICAL,
    HORIZONTAL
}

enum class PdfFitMode {
    FIT_WIDTH,
    FIT_PAGE,
    ACTUAL_SIZE
}

data class PdfViewerState(
    val pdfUri: Uri? = null,
    val fileName: String = "",
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val zoom: Float = 1.0f,
    val viewMode: PdfViewMode = PdfViewMode.VERTICAL,
    val fitMode: PdfFitMode = PdfFitMode.FIT_WIDTH,
    val isControlsVisible: Boolean = true,
    val fabExpanded: Boolean = false,
    val showTouchIndicators: Boolean = true,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val fileSizeStr: String = "",
    val showPageJumpDialog: Boolean = false,
    val showThumbnailsSheet: Boolean = false,
    val isAutoSliding: Boolean = false
)
