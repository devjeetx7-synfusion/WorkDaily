package com.synfusion.attendance.utils

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropMathTest {

    @Test
    fun testMinimumCoverScalePortraitImage() {
        val imageW = 1000f
        val imageH = 2000f
        val cropDiameter = 800f

        val scale = CropMath.calculateMinimumCoverScale(imageW, imageH, cropDiameter)
        // Need max(800/1000, 800/2000) = max(0.8, 0.4) = 0.8
        assertEquals(0.8f, scale, 0.0001f)

        // Verifying rendered dimensions:
        val renderedW = imageW * scale // 800f
        val renderedH = imageH * scale // 1600f
        assertTrue(renderedW >= cropDiameter)
        assertTrue(renderedH >= cropDiameter)
    }

    @Test
    fun testMinimumCoverScaleLandscapeImage() {
        val imageW = 2000f
        val imageH = 1000f
        val cropDiameter = 800f

        val scale = CropMath.calculateMinimumCoverScale(imageW, imageH, cropDiameter)
        // Need max(800/2000, 800/1000) = max(0.4, 0.8) = 0.8
        assertEquals(0.8f, scale, 0.0001f)

        val renderedW = imageW * scale // 1600f
        val renderedH = imageH * scale // 800f
        assertTrue(renderedW >= cropDiameter)
        assertTrue(renderedH >= cropDiameter)
    }

    @Test
    fun testMinimumCoverScaleSquareImage() {
        val imageW = 1200f
        val imageH = 1200f
        val cropDiameter = 600f

        val scale = CropMath.calculateMinimumCoverScale(imageW, imageH, cropDiameter)
        assertEquals(0.5f, scale, 0.0001f)
    }

    @Test
    fun testClampOffsetWithinBounds() {
        val imageW = 1000f
        val imageH = 2000f
        val cropDiameter = 800f
        val scale = 0.8f // Rendered size: 800 x 1600

        // maxOffsetX = (800 - 800) / 2 = 0
        // maxOffsetY = (1600 - 800) / 2 = 400
        val inputOffset = Offset(0f, 200f)
        val clamped = CropMath.clampOffset(inputOffset, scale, imageW, imageH, cropDiameter)

        assertEquals(0f, clamped.x, 0.0001f)
        assertEquals(200f, clamped.y, 0.0001f)
    }

    @Test
    fun testClampOffsetExceedingBounds() {
        val imageW = 1000f
        val imageH = 2000f
        val cropDiameter = 800f
        val scale = 0.8f // Rendered size: 800 x 1600

        // maxOffsetX = 0, maxOffsetY = 400
        val inputOffset = Offset(150f, -600f)
        val clamped = CropMath.clampOffset(inputOffset, scale, imageW, imageH, cropDiameter)

        assertEquals(0f, clamped.x, 0.0001f)
        assertEquals(-400f, clamped.y, 0.0001f)
    }

    @Test
    fun testCalculateCropRectCentered() {
        val imageW = 1000
        val imageH = 2000
        val cropDiameter = 800f
        val scale = 0.8f // Rendered size: 800 x 1600, cover scale
        val offset = Offset.Zero

        val cropRect = CropMath.calculateCropRect(imageW, imageH, scale, offset, cropDiameter)

        // Rendered top-left of crop: ( (800 - 800)/2 - 0, (1600 - 800)/2 - 0 ) = (0, 400)
        // Divide by 0.8 scale: bitmap crop left = 0, top = 400 / 0.8 = 500
        // Size in bitmap: 800 / 0.8 = 1000
        assertEquals(0, cropRect.left)
        assertEquals(500, cropRect.top)
        assertEquals(1000, cropRect.right)
        assertEquals(1500, cropRect.bottom)
    }

    @Test
    fun testCalculateCropRectWithPanOffset() {
        val imageW = 2000
        val imageH = 2000
        val cropDiameter = 800f
        val scale = 0.5f // Rendered size: 1000 x 1000
        // maxOffset = (1000 - 800) / 2 = 100
        val offset = Offset(100f, -100f)

        val cropRect = CropMath.calculateCropRect(imageW, imageH, scale, offset, cropDiameter)

        // Rendered top-left of crop: ( (1000 - 800)/2 - 100, (1000 - 800)/2 - (-100) ) = (0, 200)
        // Divide by 0.5 scale: bitmap crop left = 0, top = 400
        // Size in bitmap: 800 / 0.5 = 1600
        assertEquals(0, cropRect.left)
        assertEquals(400, cropRect.top)
        assertEquals(1600, cropRect.right)
        assertEquals(2000, cropRect.bottom)
    }
}
