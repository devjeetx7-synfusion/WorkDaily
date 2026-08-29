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
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Load bitmap in background preserving EXIF orientation
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
        val containerWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        val cropSizePx = min(containerWidthPx, containerHeightPx) * 0.8f

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.failed_to_load_image),
                            color = Color.White
                        )
                        Button(onClick = onCancel) {
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.go_back))
                        }
                    }
                }
            } else {
                val W = bitmap.width.toFloat()
                val H = bitmap.height.toFloat()

                // Calculate base scale so the photo initially fits in the view container while preserving aspect ratio
                val fitScale = minOf(containerWidthPx / W, containerHeightPx / H)
                val totalScale = fitScale * scale

                // Main cropping interactive viewport
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 10f)
                                offset += pan
                            }
                        }
                ) {
                    // Full original photo rendered uncropped in natural aspect ratio
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            },
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )

                    // Clear cutout overlay showing 1:1 crop frame over the image
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.99f)
                    ) {
                        // Dim background around crop frame
                        drawRect(color = Color.Black.copy(alpha = 0.7f))

                        // Clear 1:1 square hole for crop area
                        val left = (size.width - cropSizePx) / 2f
                        val top = (size.height - cropSizePx) / 2f
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(cropSizePx, cropSizePx),
                            blendMode = BlendMode.Clear
                        )

                        // Subtle white border around crop frame
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(left, top),
                            size = Size(cropSizePx, cropSizePx),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    // Title Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopCenter)
                    ) {
                        Text(
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.crop_photo),
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Actions Bar at bottom
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button
                        Button(
                            onClick = onCancel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.cancel), fontWeight = FontWeight.Bold)
                        }

                        // Done / Confirm Button
                        Button(
                            onClick = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        try {
                                            val bitmapCropLeft = ((W * totalScale - cropSizePx) / 2f - offset.x) / totalScale
                                            val bitmapCropTop = ((H * totalScale - cropSizePx) / 2f - offset.y) / totalScale
                                            val bitmapCropSize = cropSizePx / totalScale

                                            val targetSize = 500
                                            val resultBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
                                            val canvas = android.graphics.Canvas(resultBitmap)

                                            val srcLeft = bitmapCropLeft.toInt().coerceIn(0, bitmap.width)
                                            val srcTop = bitmapCropTop.toInt().coerceIn(0, bitmap.height)
                                            val srcRight = (bitmapCropLeft + bitmapCropSize).toInt().coerceIn(0, bitmap.width)
                                            val srcBottom = (bitmapCropTop + bitmapCropSize).toInt().coerceIn(0, bitmap.height)

                                            val srcRect = android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom)

                                            if (srcRect.width() > 0 && srcRect.height() > 0) {
                                                val dstLeft = ((srcLeft - bitmapCropLeft) / bitmapCropSize * targetSize).toFloat()
                                                val dstTop = ((srcTop - bitmapCropTop) / bitmapCropSize * targetSize).toFloat()
                                                val dstRight = ((srcRight - bitmapCropLeft) / bitmapCropSize * targetSize).toFloat()
                                                val dstBottom = ((srcBottom - bitmapCropTop) / bitmapCropSize * targetSize).toFloat()

                                                val dstRect = android.graphics.RectF(dstLeft, dstTop, dstRight, dstBottom)
                                                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
                                                canvas.drawBitmap(bitmap, srcRect, dstRect, paint)
                                            }

                                            // Clean up old temp crop files
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
                                                context.getString(com.synfusion.attendance.R.string.failed_to_crop_image),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.confirm))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.done), fontWeight = FontWeight.Bold)
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
                    .background(Color.Black.copy(alpha = 0.5f)),
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
                            text = androidx.compose.ui.res.stringResource(com.synfusion.attendance.R.string.processing_dots),
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
 * Loads a bitmap from the Uri scaling it down to a sensible max dimension (2000px) and preserving EXIF rotation.
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
        } catch (e: Exception) {
            // Ignore exif issues
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
    } catch (e: Exception) {
        null
    }
}
