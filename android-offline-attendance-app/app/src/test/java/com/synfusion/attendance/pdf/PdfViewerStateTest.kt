package com.synfusion.attendance.pdf

import com.synfusion.attendance.ui.pdf.PdfViewMode
import com.synfusion.attendance.ui.pdf.PdfViewerState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfViewerStateTest {

    @Test
    fun testDefaultStateValues() {
        val state = PdfViewerState()
        assertNull(state.pdfUri)
        assertEquals("", state.fileName)
        assertEquals(1, state.currentPage)
        assertEquals(0, state.totalPages)
        assertEquals(1.0f, state.zoom, 0.001f)
        assertEquals(PdfViewMode.VERTICAL, state.viewMode)
        assertFalse(state.fabExpanded)
        assertTrue(state.showTouchIndicators)
        assertTrue(state.isLoading)
        assertNull(state.errorMessage)
        assertEquals("", state.fileSizeStr)
    }

    @Test
    fun testCustomStateValues() {
        val state = PdfViewerState(
            fileName = "Passbook_2026.pdf",
            currentPage = 2,
            totalPages = 5,
            zoom = 2.0f,
            viewMode = PdfViewMode.HORIZONTAL,
            fabExpanded = true,
            showTouchIndicators = false,
            isLoading = false,
            errorMessage = null,
            fileSizeStr = "1.2 MB"
        )

        assertEquals("Passbook_2026.pdf", state.fileName)
        assertEquals(2, state.currentPage)
        assertEquals(5, state.totalPages)
        assertEquals(2.0f, state.zoom, 0.001f)
        assertEquals(PdfViewMode.HORIZONTAL, state.viewMode)
        assertTrue(state.fabExpanded)
        assertFalse(state.showTouchIndicators)
        assertFalse(state.isLoading)
        assertEquals("1.2 MB", state.fileSizeStr)
    }
}
