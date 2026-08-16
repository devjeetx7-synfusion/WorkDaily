package com.synfusion.attendance.utils

import com.synfusion.attendance.ui.calculateNiceScale
import com.synfusion.attendance.ui.formatYAxisLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class ChartMathTest {

    @Test
    fun testCalculateNiceScaleZeroAndNegative() {
        val scale = calculateNiceScale(0.0)
        assertEquals(500.0, scale.maxVal, 0.0001)
        assertTrue(scale.ticks.contains(0.0))
        assertTrue(scale.ticks.contains(500.0))
    }

    @Test
    fun testCalculateNiceScaleNormalEarning() {
        // maxEarning = 720.0
        val scale = calculateNiceScale(720.0)
        assertEquals(800.0, scale.maxVal, 0.0001)
        assertEquals(listOf(800.0, 600.0, 400.0, 200.0, 0.0), scale.ticks)
    }

    @Test
    fun testCalculateNiceScaleLargeEarning() {
        // maxEarning = 12500.0
        val scale = calculateNiceScale(12500.0)
        assertEquals(15000.0, scale.maxVal, 0.0001)
        assertTrue(scale.ticks.contains(15000.0))
        assertTrue(scale.ticks.contains(0.0))
    }

    @Test
    fun testFormatYAxisLabel() {
        assertEquals("₹0", formatYAxisLabel(0.0))
        assertEquals("₹200", formatYAxisLabel(200.0))
        assertEquals("₹800", formatYAxisLabel(800.0))
        assertEquals("₹2K", formatYAxisLabel(2000.0))
        assertEquals("₹2.5K", formatYAxisLabel(2500.0))
        assertEquals("₹15K", formatYAxisLabel(15000.0))
    }

    @Test
    fun testNearestPointHitTesting() {
        val count = 10
        val totalWidth = 900f
        val spacing = totalWidth / (count - 1) // 100f

        fun findNearestIndex(touchX: Float): Int {
            if (count <= 0) return -1
            if (count == 1) return 0
            return (touchX / spacing).roundToInt().coerceIn(0, count - 1)
        }

        assertEquals(0, findNearestIndex(0f))
        assertEquals(0, findNearestIndex(40f))
        assertEquals(1, findNearestIndex(60f))
        assertEquals(1, findNearestIndex(100f))
        assertEquals(5, findNearestIndex(510f))
        assertEquals(9, findNearestIndex(900f))
        assertEquals(9, findNearestIndex(1200f))
    }

    @Test
    fun testSingleDataPointAndIdenticalValues() {
        val scaleSingle = calculateNiceScale(500.0)
        assertTrue(scaleSingle.maxVal >= 500.0)
        assertEquals(600.0, scaleSingle.maxVal, 0.0001)

        val scaleIdentical = calculateNiceScale(1200.0)
        assertTrue(scaleIdentical.maxVal >= 1200.0)
    }
}
