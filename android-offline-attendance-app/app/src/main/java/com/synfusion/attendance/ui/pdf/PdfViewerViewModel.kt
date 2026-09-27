package com.synfusion.attendance.ui.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.utils.PdfExportManager
import com.synfusion.attendance.utils.PdfExportState
import com.synfusion.attendance.utils.pdf.PdfFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfViewerState())
    val uiState: StateFlow<PdfViewerState> = _uiState.asStateFlow()

    private var pfd: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null

    // Memory-aware LRU cache using 1/8th of available memory for smooth, crisp rendering
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = (maxMemoryKb / 8).coerceIn(10240, 65536) // 10MB to 64MB
    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }

        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            if (evicted && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    fun loadPdf(uri: Uri, title: String? = null, exportType: String = "", monthDate: Long = 0L) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    pdfUri = uri,
                    isLoading = true,
                    errorMessage = null
                )
            }

            try {
                withContext(Dispatchers.IO) {
                    closeRenderer()

                    val context = getApplication<Application>()
                    var targetUri = uri

                    if (exportType == "monthly" || exportType == "all_time") {
                        val localRepo = LocalAttendanceRepository(context)
                        val exportManager = PdfExportManager(context, localRepo)
                        var generatedFile: File? = null
                        var exportError: String? = null

                        if (exportType == "monthly") {
                            val selectedDate = if (monthDate > 0L) Date(monthDate) else Date()
                            exportManager.exportMonthlyPdf(selectedDate) { file ->
                                generatedFile = file
                            }
                        } else {
                            exportManager.exportAllTimePdf { file ->
                                generatedFile = file
                            }
                        }

                        val lastState = exportManager.exportState.value
                        if (lastState is PdfExportState.Error) {
                            exportError = lastState.message
                        }

                        val file = generatedFile
                        if (file != null && file.exists() && file.length() > 0L) {
                            targetUri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = exportError ?: "Failed to generate PDF document"
                                )
                            }
                            return@withContext
                        }
                    }

                    val descriptor = PdfFileManager.openParcelFileDescriptor(context, targetUri)
                    if (descriptor == null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "Unable to access PDF file descriptor"
                            )
                        }
                        return@withContext
                    }

                    pfd = descriptor
                    val renderer = PdfRenderer(descriptor)
                    pdfRenderer = renderer

                    val pages = renderer.pageCount
                    val sizeBytes = PdfFileManager.getFileSize(context, targetUri)
                    val formattedSize = PdfFileManager.formatFileSize(sizeBytes)

                    var name = title ?: targetUri.lastPathSegment ?: "WorkDaily_Passbook.pdf"
                    if (name.contains("/")) {
                        name = name.substringAfterLast("/")
                    }
                    if (!name.endsWith(".pdf", ignoreCase = true)) {
                        name = "$name.pdf"
                    }

                    _uiState.update {
                        it.copy(
                            pdfUri = targetUri,
                            fileName = name,
                            totalPages = pages,
                            currentPage = 1,
                            fileSizeStr = formattedSize,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Failed to open PDF document"
                    )
                }
            }
        }
    }

    suspend fun renderPageBitmap(
        pageIndex: Int,
        targetWidth: Int,
        renderScale: Float = 1.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        val clampedRenderScale = renderScale.coerceIn(1.0f, 4.0f)
        // Quantize scale to 0.5f steps for memory efficiency and bitmap reuse
        val roundedScale = (clampedRenderScale * 2f).toInt() / 2f
        val cacheKey = "${pageIndex}_${targetWidth}_${roundedScale}"
        bitmapCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
        }

        try {
            val bitmap: Bitmap
            synchronized(this@PdfViewerViewModel) {
                val page = renderer.openPage(pageIndex)
                val origWidth = page.width
                val origHeight = page.height

                val baseScale = targetWidth.toFloat() / origWidth.toFloat()
                val effectiveScale = baseScale * roundedScale

                // Bound max dimensions safely to prevent OOM while keeping text vector-crisp
                val renderWidth = (origWidth * effectiveScale).toInt().coerceIn(1, 4096)
                val renderHeight = (origHeight * effectiveScale).toInt().coerceIn(1, 4096)

                val bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)

                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = bmp
            }

            bitmapCache.put(cacheKey, bitmap)
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    suspend fun renderThumbnailBitmap(pageIndex: Int): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        val cacheKey = "thumb_$pageIndex"
        bitmapCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
        }

        try {
            val bitmap: Bitmap
            synchronized(this@PdfViewerViewModel) {
                val page = renderer.openPage(pageIndex)
                val thumbWidth = 180
                val scale = thumbWidth.toFloat() / page.width.toFloat()
                val thumbHeight = (page.height * scale).toInt().coerceAtLeast(1)

                val bmp = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                bmp.eraseColor(android.graphics.Color.WHITE)

                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap = bmp
            }

            bitmapCache.put(cacheKey, bitmap)
            return@withContext bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }

    fun updateCurrentPage(page: Int) {
        if (page in 1.._uiState.value.totalPages) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }

    fun jumpToPage(page: Int) {
        val validPage = page.coerceIn(1, _uiState.value.totalPages)
        _uiState.update {
            it.copy(
                currentPage = validPage,
                showPageJumpDialog = false
            )
        }
    }

    fun toggleControlsVisibility() {
        _uiState.update { it.copy(isControlsVisible = !it.isControlsVisible) }
    }

    fun setControlsVisible(visible: Boolean) {
        _uiState.update { it.copy(isControlsVisible = visible) }
    }

    fun toggleFabExpanded() {
        _uiState.update { it.copy(fabExpanded = !it.fabExpanded) }
    }

    fun setFabExpanded(expanded: Boolean) {
        _uiState.update { it.copy(fabExpanded = expanded) }
    }

    fun setShowPageJumpDialog(show: Boolean) {
        _uiState.update { it.copy(showPageJumpDialog = show) }
    }

    fun setShowThumbnailsSheet(show: Boolean) {
        _uiState.update { it.copy(showThumbnailsSheet = show) }
    }

    private fun closeRenderer() {
        bitmapCache.evictAll()
        try {
            pdfRenderer?.close()
        } catch (_: Exception) {}
        pdfRenderer = null
        try {
            pfd?.close()
        } catch (_: Exception) {}
        pfd = null
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}
