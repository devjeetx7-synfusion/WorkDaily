package com.synfusion.attendance.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.max
import kotlin.math.roundToInt

data class CropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top

    fun toAndroidRect(): android.graphics.Rect = android.graphics.Rect(left, top, right, bottom)
}

object CropMath {
    const val TARGET_CROP_SIZE = 512
    const val DEFAULT_MAX_ZOOM_FACTOR = 6f

    /**
     * Calculates the minimum scale required so that the image completely covers
     * a crop square/circle of size [cropDiameter] on both axes without leaving empty space.
     */
    fun calculateMinimumCoverScale(
        imageWidth: Float,
        imageHeight: Float,
        cropDiameter: Float
    ): Float {
        if (imageWidth <= 0f || imageHeight <= 0f || cropDiameter <= 0f) return 1f
        return max(cropDiameter / imageWidth, cropDiameter / imageHeight)
    }

    /**
     * Clamps the pan [offset] so that the rendered image (scaled by [totalScale])
     * completely covers the crop square/circle of size [cropDiameter] centered in the viewport.
     */
    fun clampOffset(
        offset: Offset,
        totalScale: Float,
        imageWidth: Float,
        imageHeight: Float,
        cropDiameter: Float
    ): Offset {
        if (imageWidth <= 0f || imageHeight <= 0f || cropDiameter <= 0f || totalScale <= 0f) {
            return Offset.Zero
        }

        val renderedW = imageWidth * totalScale
        val renderedH = imageHeight * totalScale

        val maxOffsetX = max(0f, (renderedW - cropDiameter) / 2f)
        val maxOffsetY = max(0f, (renderedH - cropDiameter) / 2f)

        val clampedX = offset.x.coerceIn(-maxOffsetX, maxOffsetX)
        val clampedY = offset.y.coerceIn(-maxOffsetY, maxOffsetY)

        return Offset(clampedX, clampedY)
    }

    /**
     * Computes the source [CropRect] on the original bitmap to crop, matching what is visible inside
     * the crop circle/square given [totalScale] and clamped [offset].
     */
    fun calculateCropRect(
        imageWidth: Int,
        imageHeight: Int,
        totalScale: Float,
        offset: Offset,
        cropDiameter: Float
    ): CropRect {
        if (imageWidth <= 0 || imageHeight <= 0 || totalScale <= 0f || cropDiameter <= 0f) {
            return CropRect(0, 0, max(1, imageWidth), max(1, imageHeight))
        }

        val W = imageWidth.toFloat()
        val H = imageHeight.toFloat()

        val renderedW = W * totalScale
        val renderedH = H * totalScale

        val cropLeftInRendered = (renderedW - cropDiameter) / 2f - offset.x
        val cropTopInRendered = (renderedH - cropDiameter) / 2f - offset.y

        val srcLeftFloat = cropLeftInRendered / totalScale
        val srcTopFloat = cropTopInRendered / totalScale
        val srcSizeFloat = cropDiameter / totalScale

        val srcLeft = srcLeftFloat.roundToInt().coerceIn(0, imageWidth)
        val srcTop = srcTopFloat.roundToInt().coerceIn(0, imageHeight)
        val srcRight = (srcLeftFloat + srcSizeFloat).roundToInt().coerceIn(srcLeft, imageWidth)
        val srcBottom = (srcTopFloat + srcSizeFloat).roundToInt().coerceIn(srcTop, imageHeight)

        val rect = CropRect(srcLeft, srcTop, srcRight, srcBottom)
        if (rect.width <= 0 || rect.height <= 0) {
            return CropRect(0, 0, imageWidth, imageHeight)
        }
        return rect
    }
}
