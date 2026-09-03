package com.synfusion.attendance.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory manager for manual Festival Preview / Test Mode.
 *
 * Guarantees:
 * - Pure presentation-layer override.
 * - Non-persistent (resets to null on app restart).
 * - Never modifies system clock, device date, calendar dates, attendance logs, or financial calculations.
 */
object FestivalPreviewManager {

    private val _activePreviewEvent = MutableStateFlow<FestivalEvent?>(null)
    val activePreviewEvent: StateFlow<FestivalEvent?> = _activePreviewEvent.asStateFlow()

    /**
     * Sets a specific festival for manual Home preview.
     */
    fun setPreviewFestival(event: FestivalEvent) {
        _activePreviewEvent.value = event
    }

    /**
     * Clears manual preview override and restores real festival detection.
     */
    fun clearPreview() {
        _activePreviewEvent.value = null
    }

    /**
     * Checks if preview mode is currently active.
     */
    fun isPreviewActive(): Boolean {
        return _activePreviewEvent.value != null
    }

    /**
     * Returns the effective Special Home Event for Home screen rendering.
     * Priority:
     * 1) Active Preview Festival (if set)
     * 2) Real Special Festival for today's date
     * 3) null (Normal Home mode)
     */
    fun getEffectiveSpecialHomeEvent(day: Int, month: Int, year: Int): FestivalEvent? {
        return _activePreviewEvent.value ?: FestivalRepository.getTodaySpecialHomeEvent(day, month, year)
    }
}
