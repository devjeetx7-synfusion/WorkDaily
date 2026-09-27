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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.collectAsStateWithLifecycle
import com.synfusion.attendance.utils.pdf.PdfFileManager
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    pdfUri: Uri,
    title: String? = null,
    exportType: String = "",
    monthDate: Long = 0L,
    onNavigateBack: () -> Unit,
    viewModel: PdfViewerViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by viewModel.uiState.collectAsStateWithLifecycle(initialValue = PdfViewerState())

    LaunchedEffect(pdfUri, exportType, monthDate) {
        viewModel.loadPdf(pdfUri, title, exportType, monthDate)
    }

    var showShareSheet by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    // SAF Save Launcher
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { destUri ->
        if (destUri != null && state.pdfUri != null) {
            scope.launch {
                val success = PdfFileManager.savePdfToUri(context, state.pdfUri!!, destUri)
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
        val targetUri = state.pdfUri ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.pdf_open_with)))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, context.getString(R.string.pdf_no_share_app), Toast.LENGTH_SHORT).show()
        }
    }

    fun printPdf() {
        val targetUri = state.pdfUri ?: return
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
                            input = context.contentResolver.openInputStream(targetUri)
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
                                text = stringResource(R.string.pdf_export_preparing),
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
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = onNavigateBack) {
                                    Text(stringResource(R.string.pdf_go_back))
                                }
                                Button(onClick = { viewModel.loadPdf(pdfUri, title, exportType, monthDate) }) {
                                    Text(stringResource(R.string.pdf_try_again))
                                }
                            }
                        }
                    }
                }

                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val density = LocalDensity.current
                        val viewportWidthPx = with(density) { maxWidth.toPx() }
                        val viewportHeightPx = with(density) { maxHeight.toPx() }

                        // Zoom & Pan State
                        var zoomScale by remember { mutableFloatStateOf(1.0f) }
                        var panOffset by remember { mutableStateOf(Offset.Zero) }

                        // Transient Floating Page Indicator Auto-Fade Effect
                        var showPageIndicator by remember { mutableStateOf(false) }
                        LaunchedEffect(state.currentPage) {
                            if (state.totalPages > 0) {
                                showPageIndicator = true
                                kotlinx.coroutines.delay(1200L)
                                showPageIndicator = false
                            }
                        }

                        // Recalculate and clamp zoom bounds on rotation
                        LaunchedEffect(viewportWidthPx, viewportHeightPx) {
                            if (zoomScale > 1.05f) {
                                val maxX = (viewportWidthPx * (zoomScale - 1f)) / 2f
                                val maxY = (viewportHeightPx * (zoomScale - 1f)) / 2f
                                panOffset = Offset(
                                    x = panOffset.x.coerceIn(-maxX, maxX),
                                    y = panOffset.y.coerceIn(-maxY, maxY)
                                )
                            } else {
                                zoomScale = 1.0f
                                panOffset = Offset.Zero
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { centroid, pan, zoom, _ ->
                                        val oldScale = zoomScale
                                        val newScale = (oldScale * zoom).coerceIn(1.0f, 4.0f)
                                        val scaleFactor = if (oldScale > 0.001f) newScale / oldScale else 1.0f

                                        var newOffset = centroid - (centroid - panOffset) * scaleFactor + pan

                                        if (newScale > 1.05f) {
                                            val maxX = (viewportWidthPx * (newScale - 1f)) / 2f
                                            val maxY = (viewportHeightPx * (newScale - 1f)) / 2f
                                            newOffset = Offset(
                                                x = newOffset.x.coerceIn(-maxX, maxX),
                                                y = newOffset.y.coerceIn(-maxY, maxY)
                                            )
                                        } else {
                                            newOffset = Offset.Zero
                                        }

                                        zoomScale = if (newScale <= 1.02f) 1.0f else newScale
                                        panOffset = newOffset
                                    }
                                }
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            viewModel.toggleControlsVisibility()
                                        },
                                        onDoubleTap = { tapOffset ->
                                            val targetScale = if (zoomScale < 1.2f) 2.5f else 1.0f
                                            val targetOffset = if (targetScale > 1.05f) {
                                                val center = Offset(viewportWidthPx / 2f, viewportHeightPx / 2f)
                                                val rawOffset = (center - tapOffset) * (targetScale - 1f)
                                                val maxX = (viewportWidthPx * (targetScale - 1f)) / 2f
                                                val maxY = (viewportHeightPx * (targetScale - 1f)) / 2f
                                                Offset(
                                                    x = rawOffset.x.coerceIn(-maxX, maxX),
                                                    y = rawOffset.y.coerceIn(-maxY, maxY)
                                                )
                                            } else {
                                                Offset.Zero
                                            }

                                            scope.launch {
                                                val startScale = zoomScale
                                                val startOffset = panOffset
                                                val anim = Animatable(0f)
                                                anim.animateTo(1f, tween(220)) {
                                                    zoomScale = startScale + (targetScale - startScale) * value
                                                    panOffset = Offset(
                                                        startOffset.x + (targetOffset.x - startOffset.x) * value,
                                                        startOffset.y + (targetOffset.y - startOffset.y) * value
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale,
                                        translationX = panOffset.x,
                                        translationY = panOffset.y
                                    )
                            ) {
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
                                        val delta = if (currIdx != previousIndex) (currIdx - previousIndex) * 500 + (currOff - previousOffset) else (currOff - previousOffset)
                                        if (delta > 20 && state.isControlsVisible) {
                                            viewModel.setControlsVisible(false)
                                        } else if (delta < -20 && !state.isControlsVisible) {
                                            viewModel.setControlsVisible(true)
                                        }
                                    }

                                    previousIndex = currIdx
                                    previousOffset = currOff
                                }

                                LaunchedEffect(listState.firstVisibleItemIndex) {
                                    val visibleIdx = (listState.firstVisibleItemIndex + 1).coerceIn(1, state.totalPages)
                                    if (visibleIdx != state.currentPage) {
                                        viewModel.updateCurrentPage(visibleIdx)
                                    }
                                }

                                LaunchedEffect(state.currentPage) {
                                    if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != state.currentPage - 1) {
                                        listState.scrollToItem((state.currentPage - 1).coerceAtLeast(0))
                                    }
                                }

                                LazyColumn(
                                    state = listState,
                                    userScrollEnabled = zoomScale <= 1.05f,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp,
                                        bottom = 80.dp,
                                        start = 0.dp,
                                        end = 0.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(
                                        items = List(state.totalPages) { it },
                                        key = { index, _ -> "pdf_page_$index" }
                                    ) { pageIndex, _ ->
                                        PdfPageView(
                                            pageIndex = pageIndex,
                                            targetWidth = viewportWidthPx.toInt(),
                                            renderScale = zoomScale,
                                            viewModel = viewModel
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Page Indicator Overlay (Bottom-Center)
                        AnimatedVisibility(
                            visible = showPageIndicator && state.totalPages > 0,
                            enter = fadeIn(animationSpec = tween(180)),
                            exit = fadeOut(animationSpec = tween(300)),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = String.format("%02d / %02d", state.currentPage, state.totalPages),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Overlay Top Bar
                        AnimatedVisibility(
                            visible = state.isControlsVisible,
                            enter = fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(180)) { -it },
                            exit = fadeOut(animationSpec = tween(180)) + slideOutVertically(animationSpec = tween(180)) { -it },
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Surface(
                                shadowElevation = 2.dp,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PdfViewerTopBar(
                                    title = state.fileName,
                                    onNavigateBack = onNavigateBack,
                                    onOpenMore = { showMoreSheet = true }
                                )
                            }
                        }

                        // Overlay FAB
                        AnimatedVisibility(
                            visible = state.isControlsVisible && !state.isLoading && state.errorMessage == null && state.totalPages > 0,
                            enter = fadeIn(animationSpec = tween(180)) + scaleIn(animationSpec = tween(180)),
                            exit = fadeOut(animationSpec = tween(180)) + scaleOut(animationSpec = tween(180)),
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
    }

    if (showShareSheet && state.pdfUri != null) {
        PdfViewerShareSheet(
            pdfUri = state.pdfUri!!,
            onDismiss = { showShareSheet = false }
        )
    }

    if (showMoreSheet) {
        PdfViewerSettingsSheet(
            fileName = state.fileName,
            totalPages = state.totalPages,
            fileSizeStr = state.fileSizeStr,
            onOpenGoToPage = { viewModel.setShowPageJumpDialog(true) },
            onOpenThumbnails = { viewModel.setShowThumbnailsSheet(true) },
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
        var selectedThumbnailPage by remember { mutableIntStateOf(-1) }

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
                    modifier = Modifier.fillMaxHeight(0.55f)
                ) {
                    items(state.totalPages) { index ->
                        val pageNum = index + 1
                        val isSelected = pageNum == selectedThumbnailPage

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
                                    selectedThumbnailPage = pageNum
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

                if (selectedThumbnailPage in 1..state.totalPages) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.jumpToPage(selectedThumbnailPage)
                            viewModel.setShowThumbnailsSheet(false)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "View",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
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
    renderScale: Float = 1.0f,
    viewModel: PdfViewerViewModel
) {
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    val discreteScale = when {
        renderScale > 3.2f -> 4.0f
        renderScale > 2.2f -> 3.0f
        renderScale > 1.4f -> 2.0f
        renderScale > 1.1f -> 1.5f
        else -> 1.0f
    }

    LaunchedEffect(pageIndex, targetWidth, discreteScale) {
        val newBitmap = viewModel.renderPageBitmap(
            pageIndex = pageIndex,
            targetWidth = targetWidth,
            renderScale = discreteScale
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
        shape = RectangleShape,
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
