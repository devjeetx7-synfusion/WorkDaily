package com.synfusion.attendance.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synfusion.attendance.R
import com.synfusion.attendance.utils.CropMath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

@Composable
fun CropScreenDialog(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            CropScreenContent(
                imageUri = imageUri,
                onCropSuccess = onCropSuccess,
                onCancel = onCancel
            )
        }
    }
}

@Composable
fun CropScreenContent(
    imageUri: Uri,
    onCropSuccess: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }

    // User zoom factor (1f = minimum cover scale)
    var userZoom by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Load scaled bitmap in background with EXIF correction
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            originalBitmap = loadScaledBitmap(context, imageUri)
            isLoading = false
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }

        // Reserved vertical height for top bar (~64dp) and bottom controls (~110dp)
        val topBarHeightPx = with(density) { 64.dp.toPx() }
        val bottomControlsHeightPx = with(density) { 110.dp.toPx() }
        val availableHeightPx = (containerHeightPx - topBarHeightPx - bottomControlsHeightPx).coerceAtLeast(100f)

        // Diameter of circular crop area
        val cropDiameter = min(containerWidthPx * 0.88f, availableHeightPx * 0.88f)

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val bitmap = originalBitmap
            if (bitmap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.failed_to_load_image),
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Button(onClick = onCancel) {
                            Text(stringResource(R.string.go_back))
                        }
                    }
                }
            } else {
                val imageW = bitmap.width.toFloat()
                val imageH = bitmap.height.toFloat()

                val minCoverScale = remember(imageW, imageH, cropDiameter) {
                    CropMath.calculateMinimumCoverScale(imageW, imageH, cropDiameter)
                }

                val totalScale = minCoverScale * userZoom

                // Ensure initial offset is clamped
                LaunchedEffect(userZoom, cropDiameter) {
                    offset = CropMath.clampOffset(offset, totalScale, imageW, imageH, cropDiameter)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(minCoverScale, cropDiameter) {
                            detectTransformGestures { centroid, pan, zoom, _ ->
                                val oldScale = minCoverScale * userZoom
                                val newZoom = (userZoom * zoom).coerceIn(1f, CropMath.DEFAULT_MAX_ZOOM_FACTOR)
                                val newScale = minCoverScale * newZoom

                                // Center relative calculation for zoom anchoring
                                val center = Offset(containerWidthPx / 2f, containerHeightPx / 2f)
                                val centroidShift = (centroid - center) * (1f - newScale / oldScale)

                                val unclampedOffset = offset + centroidShift + pan
                                offset = CropMath.clampOffset(
                                    unclampedOffset,
                                    newScale,
                                    imageW,
                                    imageH,
                                    cropDiameter
                                )
                                userZoom = newZoom
                            }
                        }
                ) {
                    // Render uncropped bitmap at 1:1 raw size centered in container, then transformed with graphicsLayer
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = totalScale
                                scaleY = totalScale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = ContentScale.None,
                        alignment = Alignment.Center
                    )

                    // Overlay: Scrim, Circular Hole, White Border, 3x3 Grid
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.99f)
                    ) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val radius = cropDiameter / 2f
                        val left = centerX - radius
                        val top = centerY - radius

                        // 1. Dark translucent scrim outside circle
                        drawRect(color = Color.Black.copy(alpha = 0.72f))

                        // 2. Clear circular hole inside crop region
                        drawCircle(
                            color = Color.Transparent,
                            radius = radius,
                            center = Offset(centerX, centerY),
                            blendMode = BlendMode.Clear
                        )

                        // 3. Clean circular border
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = radius,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // 4. Subtle 3x3 Grid clipped inside the circle
                        val circlePath = Path().apply {
                            addOval(Rect(left, top, left + cropDiameter, top + cropDiameter))
                        }

                        clipPath(circlePath) {
                            val gridColor = Color.White.copy(alpha = 0.35f)
                            val gridStrokeWidth = 1.dp.toPx()

                            // Vertical grid lines
                            drawLine(
                                color = gridColor,
                                start = Offset(left + cropDiameter / 3f, top),
                                end = Offset(left + cropDiameter / 3f, top + cropDiameter),
                                strokeWidth = gridStrokeWidth
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(left + cropDiameter * 2f / 3f, top),
                                end = Offset(left + cropDiameter * 2f / 3f, top + cropDiameter),
                                strokeWidth = gridStrokeWidth
                            )

                            // Horizontal grid lines
                            drawLine(
                                color = gridColor,
                                start = Offset(left, top + cropDiameter / 3f),
                                end = Offset(left + cropDiameter, top + cropDiameter / 3f),
                                strokeWidth = gridStrokeWidth
                            )
                            drawLine(
                                color = gridColor,
                                start = Offset(left, top + cropDiameter * 2f / 3f),
                                end = Offset(left + cropDiameter, top + cropDiameter * 2f / 3f),
                                strokeWidth = gridStrokeWidth
                            )
                        }
                    }

                    // Top Bar Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .height(64.dp)
                            .padding(horizontal = 16.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancel,
                            enabled = !isProcessing
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = Color.White
                            )
                        }

                        Text(
                            text = stringResource(R.string.crop_photo),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        try {
                                            val cropRect = CropMath.calculateCropRect(
                                                imageWidth = bitmap.width,
                                                imageHeight = bitmap.height,
                                                totalScale = totalScale,
                                                offset = offset,
                                                cropDiameter = cropDiameter
                                            )

                                            val targetSize = CropMath.TARGET_CROP_SIZE
                                            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(resultBitmap)

                                            val srcRect = cropRect.toAndroidRect()
                                            val dstRect = android.graphics.Rect(0, 0, targetSize, targetSize)
                                            val paint = android.graphics.Paint(
                                                android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG
                                            )

                                            canvas.drawBitmap(bitmap, srcRect, dstRect, paint)

                                            // Cleanup old temporary crop files
                                            val cacheDir = context.cacheDir
                                            cacheDir.listFiles { _, name -> name.startsWith("temp_cropped_profile_") }?.forEach { oldFile ->
                                                try { oldFile.delete() } catch (_: Exception) {}
                                            }

                                            val tempFile = File(cacheDir, "temp_cropped_profile_${System.currentTimeMillis()}.jpg")
                                            withContext(Dispatchers.IO) {
                                                FileOutputStream(tempFile).use { out ->
                                                    resultBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                                }
                                            }

                                            onCropSuccess(Uri.fromFile(tempFile))
                                        } catch (e: Exception) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.failed_to_crop_image),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
                            enabled = !isProcessing
                        ) {
                            Text(
                                text = stringResource(R.string.done),
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Bottom Instruction & Reset Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.drag_and_zoom_hint),
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )

                        TextButton(
                            onClick = {
                                userZoom = 1f
                                offset = Offset.Zero
                            },
                            enabled = !isProcessing
                        ) {
                            Text(
                                text = stringResource(R.string.reset),
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // Processing Overlay
        AnimatedVisibility(
            visible = isProcessing,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = stringResource(R.string.processing_dots),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

/**
 * Loads a bitmap from [Uri] scaling it down to a sensible max dimension (2000px) and preserving EXIF rotation.
 */
private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val resolver = context.contentResolver
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var rotation = 0
        try {
            resolver.openInputStream(uri)?.use { stream ->
                val exifInterface = ExifInterface(stream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        } catch (_: Exception) {
            // Ignore EXIF parsing errors
        }

        val maxDim = 2000
        var sampleSize = 1
        while (srcWidth / sampleSize > maxDim || srcHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        var loadedBitmap = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }

        if (loadedBitmap != null && rotation != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(
                loadedBitmap, 0, 0, loadedBitmap.width, loadedBitmap.height, matrix, true
            )
            if (rotated != loadedBitmap) {
                loadedBitmap.recycle()
            }
            loadedBitmap = rotated
        }
        loadedBitmap
    } catch (_: Exception) {
        null
    }
}
