package com.synfusion.attendance.ui.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.utils.pdf.PdfFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun loadPdf(uri: Uri, title: String? = null) {
        if (_uiState.value.pdfUri == uri && !_uiState.value.isLoading && _uiState.value.errorMessage == null) {
            return
        }

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
                    val descriptor = PdfFileManager.openParcelFileDescriptor(context, uri)
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
                    val sizeBytes = PdfFileManager.getFileSize(context, uri)
                    val formattedSize = PdfFileManager.formatFileSize(sizeBytes)

                    var name = title ?: uri.lastPathSegment ?: "WorkDaily_Passbook.pdf"
                    if (name.contains("/")) {
                        name = name.substringAfterLast("/")
                    }
                    if (!name.endsWith(".pdf", ignoreCase = true)) {
                        name = "$name.pdf"
                    }

                    _uiState.update {
                        it.copy(
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
        scale: Float = 1.0f,
        renderScale: Float = 1.0f
    ): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        val clampedRenderScale = renderScale.coerceIn(1.0f, 5.0f)
        val cacheKey = "${pageIndex}_${targetWidth}_${scale}_${clampedRenderScale}"
        bitmapCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
        }

        try {
            val page = renderer.openPage(pageIndex)
            val origWidth = page.width
            val origHeight = page.height

            val screenDensityScale = targetWidth.toFloat() / origWidth.toFloat()
            val effectiveScale = screenDensityScale * scale.coerceIn(0.5f, 5.0f) * clampedRenderScale

            // Bound maximum dimension to 4096px to prevent OOM and Canvas overflow while ensuring crispness
            val renderWidth = (origWidth * effectiveScale).toInt().coerceIn(1, 4096)
            val renderHeight = (origHeight * effectiveScale).toInt().coerceIn(1, 4096)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

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
            val page = renderer.openPage(pageIndex)
            val thumbWidth = 180
            val scale = thumbWidth.toFloat() / page.width.toFloat()
            val thumbHeight = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

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

    fun toggleViewMode() {
        val nextMode = if (_uiState.value.viewMode == PdfViewMode.VERTICAL) PdfViewMode.HORIZONTAL else PdfViewMode.VERTICAL
        _uiState.update { it.copy(viewMode = nextMode) }
    }

    fun setViewMode(mode: PdfViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
    }

    fun setFitMode(fitMode: PdfFitMode) {
        _uiState.update { it.copy(fitMode = fitMode) }
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

    fun toggleTouchIndicators() {
        _uiState.update { it.copy(showTouchIndicators = !it.showTouchIndicators) }
    }

    fun setShowPageJumpDialog(show: Boolean) {
        _uiState.update { it.copy(showPageJumpDialog = show) }
    }

    fun setShowThumbnailsSheet(show: Boolean) {
        _uiState.update { it.copy(showThumbnailsSheet = show) }
    }


    fun toggleAutoSlide() {
        _uiState.update {
            val nextState = !it.isAutoSliding
            it.copy(
                isAutoSliding = nextState,
                viewMode = if (nextState) PdfViewMode.HORIZONTAL else it.viewMode
            )
        }
    }

    fun setAutoSlide(enabled: Boolean) {
        _uiState.update { it.copy(isAutoSliding = enabled) }
    }

    fun updateZoom(zoom: Float) {
        _uiState.update { it.copy(zoom = zoom.coerceIn(1.0f, 5.0f)) }
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
