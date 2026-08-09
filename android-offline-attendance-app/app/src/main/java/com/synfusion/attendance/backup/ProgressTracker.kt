package com.synfusion.attendance.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class RestoreState {
    IDLE,
    PICKING_FILE,
    READING_BACKUP,
    VALIDATING,
    IMPORTING,
    VERIFYING,
    PREPARING_REPLACEMENT,
    REPLACING_DATABASE,
    RESTORING_PREFERENCES,
    RESTORE_SUCCESS,
    RESTARTING_APP,
    RESTORE_FAILED
}

class ProgressTracker {
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    private val _state = MutableStateFlow(RestoreState.IDLE)
    val state: StateFlow<RestoreState> = _state

    private var isCancelled = false

    fun updateProgress(value: Float, currentStatus: String = "") {
        _progress.value = value.coerceIn(0f, 100f)
        if (currentStatus.isNotEmpty()) {
            _status.value = currentStatus
        }
    }

    fun updateState(newState: RestoreState) {
        _state.value = newState
    }

    fun cancel() {
        isCancelled = true
    }

    fun checkCancelled() {
        if (isCancelled) {
            throw CancellationException("Operation was cancelled by user.")
        }
    }
}

class CancellationException(message: String) : Exception(message)
