package com.synfusion.attendance.live.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveDurationFormatTest {

    @Test
    fun `formats under a minute`() {
        assertEquals("00:00", formatLiveDuration(0))
        assertEquals("00:07", formatLiveDuration(7))
        assertEquals("00:59", formatLiveDuration(59))
    }

    @Test
    fun `formats minutes and hours`() {
        assertEquals("01:05", formatLiveDuration(65))
        assertEquals("59:59", formatLiveDuration(3_599))
        assertEquals("1:00:00", formatLiveDuration(3_600))
        assertEquals("2:03:04", formatLiveDuration(7_384))
    }

    @Test
    fun `negative and clock skew are clamped`() {
        assertEquals("00:00", formatLiveDuration(-90))
        assertEquals(0L, liveElapsedSeconds(5_000L, 4_000L))
        assertEquals(0L, liveElapsedSeconds(0L, 4_000L))
    }

    @Test
    fun `elapsed seconds derive from the persisted start time`() {
        assertEquals(90L, liveElapsedSeconds(1_000L, 91_000L))
    }
}
