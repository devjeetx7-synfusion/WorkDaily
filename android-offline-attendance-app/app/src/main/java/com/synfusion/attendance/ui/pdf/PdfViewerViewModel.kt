package com.synfusion.attendance.ui.pdf

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Rect
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

    // Cache up to 15 page bitmaps in memory for smooth rendering
    private val bitmapCache = object : LruCache<String, Bitmap>(15) {
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

    suspend fun renderPageBitmap(pageIndex: Int, targetWidth: Int, scale: Float = 1.0f): Bitmap? = withContext(Dispatchers.IO) {
        val renderer = pdfRenderer ?: return@withContext null
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null

        val cacheKey = "${pageIndex}_${targetWidth}_$scale"
        bitmapCache.get(cacheKey)?.let { cachedBitmap ->
            if (!cachedBitmap.isRecycled) return@withContext cachedBitmap
        }

        try {
            val page = renderer.openPage(pageIndex)
            val pdfWidth = page.width
            val pdfHeight = page.height

            val screenDensityScale = targetWidth.toFloat() / pdfWidth.toFloat()
            val effectiveScale = screenDensityScale * scale.coerceIn(0.5f, 5.0f)

            val renderWidth = (pdfWidth * effectiveScale).toInt().coerceAtLeast(1)
            val renderHeight = (pdfHeight * effectiveScale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            // White background fill for clean PDF rendering
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

    fun toggleViewMode() {
        val nextMode = if (_uiState.value.viewMode == PdfViewMode.VERTICAL) PdfViewMode.HORIZONTAL else PdfViewMode.VERTICAL
        _uiState.update { it.copy(viewMode = nextMode) }
    }

    fun setViewMode(mode: PdfViewMode) {
        _uiState.update { it.copy(viewMode = mode) }
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

    fun setTouchIndicators(show: Boolean) {
        _uiState.update { it.copy(showTouchIndicators = show) }
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
