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
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import java.io.InputStream
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

    // Load bitmap in background
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
        val cropSizeDp = with(LocalDensity.current) { cropSizePx.toDp() }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val bitmap = originalBitmap
            if (bitmap == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Failed to load image", color = Color.White)
                        Button(onClick = onCancel) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                val W = bitmap.width.toFloat()
                val H = bitmap.height.toFloat()

                // Calculate baseScale so the image fills the 1:1 box initially
                val baseScale = maxOf(cropSizePx / W, cropSizePx / H)

                // Clamp visual translation so the image always covers the 1:1 box perfectly
                val maxOffsetX = ((W * baseScale * scale) - cropSizePx) / 2f
                val maxOffsetY = ((H * baseScale * scale) - cropSizePx) / 2f

                val clampedOffsetX = if (maxOffsetX > 0f) offset.x.coerceIn(-maxOffsetX, maxOffsetX) else 0f
                val clampedOffsetY = if (maxOffsetY > 0f) offset.y.coerceIn(-maxOffsetY, maxOffsetY) else 0f

                // Main cropping area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 8f)
                                offset = offset + pan
                            }
                        }
                ) {
                    // Render image centered in the crop box size
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(cropSizeDp)
                            .clipToBounds()
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = clampedOffsetX
                                    translationY = clampedOffsetY
                                },
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }

                    // Clear cutout overlay
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(alpha = 0.99f)
                    ) {
                        // Dim screen
                        drawRect(color = Color.Black.copy(alpha = 0.7f))

                        // Clear hole for crop area
                        val left = (size.width - cropSizePx) / 2f
                        val top = (size.height - cropSizePx) / 2f
                        drawRect(
                            color = Color.Transparent,
                            topLeft = Offset(left, top),
                            size = Size(cropSizePx, cropSizePx),
                            blendMode = BlendMode.Clear
                        )

                        // Draw subtle white border
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
                            text = "Crop Photo",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Actions Bar at the bottom
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
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel", fontWeight = FontWeight.Bold)
                        }

                        // Done / Confirm Button
                        Button(
                            onClick = {
                                if (!isProcessing) {
                                    isProcessing = true
                                    coroutineScope.launch {
                                        try {
                                            val finalScale = baseScale * scale
                                            val cropLeft = ((W * finalScale - cropSizePx) / 2f - clampedOffsetX) / finalScale
                                            val cropTop = ((H * finalScale - cropSizePx) / 2f - clampedOffsetY) / finalScale
                                            val cropWidth = cropSizePx / finalScale
                                            val cropHeight = cropSizePx / finalScale

                                            val x = cropLeft.toInt().coerceIn(0, bitmap.width - 1)
                                            val y = cropTop.toInt().coerceIn(0, bitmap.height - 1)
                                            val w = cropWidth.toInt().coerceAtMost(bitmap.width - x).coerceAtLeast(1)
                                            val h = cropHeight.toInt().coerceAtMost(bitmap.height - y).coerceAtLeast(1)

                                            val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                                            val targetSize = 500
                                            val scaled = Bitmap.createScaledBitmap(cropped, targetSize, targetSize, true)

                                            val tempFile = File(context.cacheDir, "temp_cropped_profile_${System.currentTimeMillis()}.jpg")
                                            withContext(Dispatchers.IO) {
                                                FileOutputStream(tempFile).use { out ->
                                                    scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                }
                                            }

                                            onCropSuccess(Uri.fromFile(tempFile))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Failed to crop image", Toast.LENGTH_SHORT).show()
                                            isProcessing = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(50.dp).weight(1f).padding(start = 8.dp)
                        ) {
                            Icon(Icons.Default.Done, contentDescription = "Confirm")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Done", fontWeight = FontWeight.Bold)
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
                            text = "Processing...",
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
 * Loads a bitmap from the Uri scaling it down to a sensible max dimension (1500px) and maintaining rotation.
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

        val maxDim = 1500
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
