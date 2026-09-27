package com.synfusion.attendance.ui.pdf

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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.pdf.PdfFileManager
import com.synfusion.attendance.utils.pdf.PdfShareManager
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUri: Uri,
    title: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle(initialValue = PdfViewerState())

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

    fun openWithExternalApp() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_open_with)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.pdf_no_share_app), Toast.LENGTH_SHORT).show()
        }
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

                    val fitScaleMultiplier = when (state.fitMode) {
                        PdfFitMode.FIT_WIDTH -> 1.0f
                        PdfFitMode.FIT_PAGE -> 0.85f
                        PdfFitMode.ACTUAL_SIZE -> 1.25f
                    }

                    val renderScale = when {
                        scale < 1.3f -> 1.0f
                        scale < 2.3f -> 2.0f
                        scale < 3.3f -> 3.0f
                        else -> 4.0f
                    }

                    val screenHeightPx = with(LocalDensity.current) { configuration.screenHeightDp.dp.toPx() }

                    LaunchedEffect(state.isAutoSliding, state.currentPage, state.totalPages) {
                        if (state.isAutoSliding && state.totalPages > 1) {
                            kotlinx.coroutines.delay(3000L)
                            val nextPage = if (state.currentPage >= state.totalPages) 1 else state.currentPage + 1
                            viewModel.jumpToPage(nextPage)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTransformGestures { centroid, pan, zoom, _ ->
                                    if (state.isAutoSliding) {
                                        viewModel.setAutoSlide(false)
                                    }
                                    val oldScale = scale
                                    val newScale = (scale * zoom).coerceIn(1.0f, 5.0f)
                                    val scaleFactor = if (oldScale > 0.001f) newScale / oldScale else 1.0f

                                    // Scale around touch centroid focal point
                                    var newOffset = centroid - (centroid - offset) * scaleFactor + pan

                                    // Clamp pan offsets within PDF page bounds
                                    if (newScale > 1.0f) {
                                        val maxX = (screenWidthPx * (newScale - 1f)) / 2f
                                        val maxY = (screenHeightPx * (newScale - 1f)) / 2f
                                        newOffset = Offset(
                                            x = newOffset.x.coerceIn(-maxX, maxX),
                                            y = newOffset.y.coerceIn(-maxY, maxY)
                                        )
                                    } else {
                                        newOffset = Offset.Zero
                                    }

                                    scale = newScale
                                    offset = newOffset

                                    if (state.showTouchIndicators) {
                                        touchPoint1 = centroid
                                        touchPoint2 = centroid + Offset(30f, 30f)
                                    }
                                }
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        viewModel.toggleControlsVisibility()
                                    },
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1.2f) {
                                            scale = 1.0f
                                            offset = Offset.Zero
                                        } else {
                                            val targetScale = 2.5f
                                            val center = Offset(screenWidthPx / 2f, screenHeightPx / 2f)
                                            var targetOffset = (center - tapOffset) * targetScale
                                            val maxX = (screenWidthPx * (targetScale - 1f)) / 2f
                                            val maxY = (screenHeightPx * (targetScale - 1f)) / 2f
                                            targetOffset = Offset(
                                                x = targetOffset.x.coerceIn(-maxX, maxX),
                                                y = targetOffset.y.coerceIn(-maxY, maxY)
                                            )
                                            scale = targetScale
                                            offset = targetOffset
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
                                val listState = rememberLazyListState(
                                    initialFirstVisibleItemIndex = (state.currentPage - 1).coerceAtLeast(0)
                                )

                                var previousIndex by remember { mutableIntStateOf(0) }
                                var previousOffset by remember { mutableIntStateOf(0) }

                                LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                                    val currIdx = listState.firstVisibleItemIndex
                                    val currOff = listState.firstVisibleItemScrollOffset

                                    if (currIdx == 0 && currOff == 0) {
                                        viewModel.setControlsVisible(true)
                                    } else {
                                        val isScrollingDown = if (currIdx != previousIndex) currIdx > previousIndex else (currOff - previousOffset) > 15
                                        val isScrollingUp = if (currIdx != previousIndex) currIdx < previousIndex else (previousOffset - currOff) > 15

                                        if (isScrollingDown && state.isControlsVisible) {
                                            viewModel.setControlsVisible(false)
                                        } else if (isScrollingUp && !state.isControlsVisible) {
                                            viewModel.setControlsVisible(true)
                                        }
                                    }

                                    previousIndex = currIdx
                                    previousOffset = currOff
                                }

                                val visiblePage by remember {
                                    derivedStateOf {
                                        val firstVisible = listState.firstVisibleItemIndex
                                        (firstVisible + 1).coerceIn(1, state.totalPages)
                                    }
                                }

                                LaunchedEffect(visiblePage) {
                                    viewModel.updateCurrentPage(visiblePage)
                                }

                                LaunchedEffect(state.currentPage) {
                                    if (listState.firstVisibleItemIndex != state.currentPage - 1) {
                                        listState.scrollToItem((state.currentPage - 1).coerceAtLeast(0))
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                                        bottom = 80.dp,
                                        start = 8.dp,
                                        end = 8.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(
                                        items = List(state.totalPages) { it },
                                        key = { index, _ -> "pdf_page_$index" }
                                    ) { pageIndex, _ ->
                                        PdfPageView(
                                            pageIndex = pageIndex,
                                            targetWidth = screenWidthPx,
                                            fitScaleMultiplier = fitScaleMultiplier,
                                            renderScale = renderScale,
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

                                LaunchedEffect(state.currentPage) {
                                    if (pagerState.currentPage != state.currentPage - 1) {
                                        pagerState.scrollToPage((state.currentPage - 1).coerceAtLeast(0))
                                    }
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
                                        fitScaleMultiplier = fitScaleMultiplier,
                                        renderScale = renderScale,
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

                    // Overlay Top Bar
                    AnimatedVisibility(
                        visible = state.isControlsVisible,
                        enter = fadeIn() + slideInVertically { -it },
                        exit = fadeOut() + slideOutVertically { -it },
                        modifier = Modifier.align(Alignment.TopCenter)
                    ) {
                        Surface(
                            shadowElevation = 4.dp,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PdfViewerTopBar(
                                title = state.fileName,
                                currentPage = state.currentPage,
                                totalPages = state.totalPages,
                                viewMode = state.viewMode,
                                isAutoSliding = state.isAutoSliding,
                                onNavigateBack = onNavigateBack,
                                onToggleViewMode = { viewModel.toggleViewMode() },
                                onToggleAutoSlide = { viewModel.toggleAutoSlide() },
                                onOpenMore = { showMoreSheet = true }
                            )
                        }
                    }

                    // Overlay FAB
                    AnimatedVisibility(
                        visible = state.isControlsVisible && !state.isLoading && state.errorMessage == null && state.totalPages > 0,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut(),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 24.dp, end = 24.dp)
                    ) {
                        PdfViewerFab(
                            expanded = state.fabExpanded,
                            onToggleExpanded = { viewModel.toggleFabExpanded() },
                            onShare = { showShareSheet = true },
                            onSave = { initiateSave() },
                            onMore = { showMoreSheet = true }
                        )
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
            viewMode = state.viewMode,
            fitMode = state.fitMode,
            showTouchIndicators = state.showTouchIndicators,
            onToggleTouchIndicators = { viewModel.toggleTouchIndicators() },
            onOpenGoToPage = { viewModel.setShowPageJumpDialog(true) },
            onOpenThumbnails = { viewModel.setShowThumbnailsSheet(true) },
            onToggleViewMode = { viewModel.toggleViewMode() },
            onSelectFitMode = { viewModel.setFitMode(it) },
            onPrint = { printPdf() },
            onOpenWith = { openWithExternalApp() },
            onDismiss = { showMoreSheet = false },
            onCloseViewer = onNavigateBack
        )
    }

    // Go to Page Dialog
    if (state.showPageJumpDialog) {
        var pageInput by remember { mutableStateOf(state.currentPage.toString()) }
        var isError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { viewModel.setShowPageJumpDialog(false) },
            title = {
                Text(
                    text = stringResource(R.string.pdf_go_to_page),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { input ->
                            pageInput = input.filter { it.isDigit() }
                            isError = false
                        },
                        label = { Text(stringResource(R.string.pdf_enter_page_number)) },
                        placeholder = { Text("1 - ${state.totalPages}") },
                        isError = isError,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isError) {
                        Text(
                            text = stringResource(R.string.pdf_invalid_page),
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = pageInput.toIntOrNull()
                        if (parsed != null && parsed in 1..state.totalPages) {
                            viewModel.jumpToPage(parsed)
                        } else {
                            isError = true
                        }
                    }
                ) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowPageJumpDialog(false) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Page Thumbnails Bottom Sheet
    if (state.showThumbnailsSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowThumbnailsSheet(false) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            windowInsets = WindowInsets(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.pdf_thumbnails),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxHeight(0.6f)
                ) {
                    items(state.totalPages) { index ->
                        val pageNum = index + 1
                        val isSelected = pageNum == state.currentPage

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    viewModel.jumpToPage(pageNum)
                                    viewModel.setShowThumbnailsSheet(false)
                                }
                                .padding(6.dp)
                        ) {
                            PdfThumbnailView(
                                pageIndex = index,
                                viewModel = viewModel
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$pageNum",
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

}

@Composable
private fun PdfPageView(
    pageIndex: Int,
    targetWidth: Int,
    fitScaleMultiplier: Float,
    renderScale: Float = 1.0f,
    viewModel: PdfViewerViewModel
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    LaunchedEffect(pageIndex, targetWidth, fitScaleMultiplier, renderScale) {
        isLoadingPage = (bitmap == null)
        val newBitmap = viewModel.renderPageBitmap(
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            scale = fitScaleMultiplier,
            renderScale = renderScale
        )
        if (newBitmap != null) {
            bitmap = newBitmap
        }
        isLoadingPage = false
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = androidx.compose.ui.graphics.RectangleShape,
        color = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
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

@Composable
private fun PdfThumbnailView(
    pageIndex: Int,
    viewModel: PdfViewerViewModel
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(pageIndex) {
        bitmap = viewModel.renderThumbnailBitmap(pageIndex)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        contentAlignment = Alignment.Center
    ) {
        val bmp = bitmap
        if (bmp != null && !bmp.isRecycled) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Thumbnail ${pageIndex + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }
}
