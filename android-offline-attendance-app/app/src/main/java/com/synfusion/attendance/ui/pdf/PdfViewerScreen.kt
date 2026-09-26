package com.synfusion.attendance.ui.pdf

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.pdf.PdfFileManager
import com.synfusion.attendance.utils.pdf.PdfShareManager
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PdfViewerScreen(
    pdfUri: Uri,
    title: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(pdfUri) {
        viewModel.loadPdf(pdfUri, title)
    }

    var showShareSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    // Touch Indicator positions
    var touchPoint1 by remember { mutableStateOf<Offset?>(null) }
    var touchPoint2 by remember { mutableStateOf<Offset?>(null) }

    // SAF Save Launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null) {
            scope.launch {
                val success = PdfFileManager.savePdfToUri(context, pdfUri, destUri)
                if (success) {
                    Toast.makeText(context, context.getString(R.string.pdf_saved_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.pdf_could_not_save), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun initiateSave() {
        val defaultName = state.fileName.ifEmpty { "WorkDaily_Passbook.pdf" }
        saveLauncher.launch(defaultName)
    }

    fun printPdf() {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val adapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(state.fileName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(state.totalPages)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        var input: InputStream? = null
                        var output: OutputStream? = null
                        try {
                            input = context.contentResolver.openInputStream(pdfUri)
                            if (destination != null && input != null) {
                                output = FileOutputStream(destination.fileDescriptor)
                                val buf = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buf).also { bytesRead = it } != -1) {
                                    if (cancellationSignal?.isCanceled == true) {
                                        callback?.onWriteCancelled()
                                        return
                                    }
                                    output.write(buf, 0, bytesRead)
                                }
                                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                            }
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.localizedMessage)
                        } finally {
                            try { input?.close() } catch (_: Exception) {}
                            try { output?.close() } catch (_: Exception) {}
                        }
                    }
                }
                printManager.print(state.fileName, adapter, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Print failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            PdfViewerTopBar(
                title = state.fileName,
                currentPage = state.currentPage,
                totalPages = state.totalPages,
                viewMode = state.viewMode,
                onNavigateBack = onNavigateBack,
                onToggleViewMode = { viewModel.toggleViewMode() },
                onOpenMore = { showMoreSheet = true }
            )
        },
        floatingActionButton = {
            if (!state.isLoading && state.errorMessage == null && state.totalPages > 0) {
                PdfViewerFab(
                    expanded = state.fabExpanded,
                    onToggleExpanded = { viewModel.toggleFabExpanded() },
                    onShare = { showShareSheet = true },
                    onSave = { initiateSave() },
                    onMore = { showMoreSheet = true }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = stringResource(R.string.pdf_loading),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                state.errorMessage != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = stringResource(R.string.pdf_unable_to_open),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = state.errorMessage ?: stringResource(R.string.pdf_error_desc),
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onNavigateBack) {
                                    Text(stringResource(R.string.pdf_go_back))
                                }
                                Button(onClick = { viewModel.loadPdf(pdfUri, title) }) {
                                    Text(stringResource(R.string.pdf_try_again))
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Zoom and Touch state
                    var scale by remember { mutableStateOf(1.0f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }

                    val configuration = LocalConfiguration.current
                    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }.toInt()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    val newScale = (scale * zoom).coerceIn(1.0f, 5.0f)
                                    scale = newScale
                                    if (scale > 1.0f) {
                                        offset += pan
                                    } else {
                                        offset = Offset.Zero
                                    }
                                    if (state.showTouchIndicators) {
                                        touchPoint1 = centroid
                                        touchPoint2 = centroid + Offset(30f, 30f)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1.2f) {
                                            scale = 1.0f
                                            offset = Offset.Zero
                                        } else {
                                            scale = 2.5f
                                            offset = tapOffset
                                        }
                                    },
                                    onPress = { tapOffset ->
                                        if (state.showTouchIndicators) {
                                            touchPoint1 = tapOffset
                                        }
                                        tryAwaitRelease()
                                        touchPoint1 = null
                                        touchPoint2 = null
                                    }
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        ) {
                            if (state.viewMode == PdfViewMode.VERTICAL) {
                                val listState = rememberLazyListState()

                                val visiblePage by remember {
                                    derivedStateOf {
                                        val firstVisible = listState.firstVisibleItemIndex
                                        (firstVisible + 1).coerceIn(1, state.totalPages)
                                    }
                                }

                                LaunchedEffect(visiblePage) {
                                    viewModel.updateCurrentPage(visiblePage)
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    itemsIndexed(
                                        items = List(state.totalPages) { it },
                                        key = { index, _ -> "pdf_page_$index" }
                                    ) { pageIndex, _ ->
                                        PdfPageView(
                                            pageIndex = pageIndex,
                                            targetWidth = screenWidthPx,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            } else {
                                val pagerState = rememberPagerState(
                                    initialPage = (state.currentPage - 1).coerceAtLeast(0),
                                    pageCount = { state.totalPages }
                                )

                                LaunchedEffect(pagerState.currentPage) {
                                    viewModel.updateCurrentPage(pagerState.currentPage + 1)
                                }

                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 16.dp),
                                    pageSpacing = 12.dp
                                ) { pageIndex ->
                                    PdfPageView(
                                        pageIndex = pageIndex,
                                        targetWidth = screenWidthPx,
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }

                        // Touch Indicators Overlay
                        if (state.showTouchIndicators && (touchPoint1 != null || touchPoint2 != null)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                touchPoint1?.let { p1 ->
                                    drawCircle(
                                        color = Color(0x662196F3),
                                        radius = 28.dp.toPx(),
                                        center = p1
                                    )
                                    drawCircle(
                                        color = Color(0xAA2196F3),
                                        radius = 8.dp.toPx(),
                                        center = p1
                                    )
                                }
                                touchPoint2?.let { p2 ->
                                    drawCircle(
                                        color = Color(0x662196F3),
                                        radius = 28.dp.toPx(),
                                        center = p2
                                    )
                                    drawCircle(
                                        color = Color(0xAA2196F3),
                                        radius = 8.dp.toPx(),
                                        center = p2
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showShareSheet) {
        val isWa = PdfShareManager.isWhatsAppInstalled(context)
        val isTg = PdfShareManager.isTelegramInstalled(context)
        val isGm = PdfShareManager.isGmailInstalled(context)

        PdfViewerShareSheet(
            isWhatsAppInstalled = isWa,
            isTelegramInstalled = isTg,
            isGmailInstalled = isGm,
            onShareToApp = { pkg ->
                val shared = PdfShareManager.shareToApp(context, pdfUri, pkg)
                if (!shared) {
                    Toast.makeText(context, context.getString(R.string.pdf_could_not_share), Toast.LENGTH_SHORT).show()
                }
            },
            onShareMore = {
                val shared = PdfShareManager.shareToSystemChooser(context, pdfUri, context.getString(R.string.pdf_share))
                if (!shared) {
                    Toast.makeText(context, context.getString(R.string.pdf_no_share_app), Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = { showShareSheet = false }
        )
    }

    if (showMoreSheet) {
        PdfViewerSettingsSheet(
            fileName = state.fileName,
            totalPages = state.totalPages,
            fileSizeStr = state.fileSizeStr,
            showTouchIndicators = state.showTouchIndicators,
            onToggleTouchIndicators = { viewModel.toggleTouchIndicators() },
            onPrint = { printPdf() },
            onDismiss = { showMoreSheet = false },
            onCloseViewer = onNavigateBack
        )
    }
}

@Composable
private fun PdfPageView(
    pageIndex: Int,
    targetWidth: Int,
    viewModel: PdfViewerViewModel
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    LaunchedEffect(pageIndex, targetWidth) {
        isLoadingPage = true
        bitmap = viewModel.renderPageBitmap(pageIndex, targetWidth)
        isLoadingPage = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null && !bmp.isRecycled) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Page ${pageIndex + 1}",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth
                )
            } else if (isLoadingPage) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}
