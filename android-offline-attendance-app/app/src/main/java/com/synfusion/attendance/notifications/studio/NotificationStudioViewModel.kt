package com.synfusion.attendance.notifications.studio

import android.app.Application
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.R
import com.synfusion.attendance.notifications.engine.WorkDailyNotificationCoordinator
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationPayload
import com.synfusion.attendance.notifications.registry.NotificationTemplateRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationStudioUiState(
    val currentIndex: Int = 0,
    val selectedCategory: NotificationCategory? = null,
    val isDarkModePreview: Boolean = false,
    val isAutoCarouselRunning: Boolean = false,
    val previewMode: StudioPreviewMode = StudioPreviewMode.RICH_IN_APP
)

enum class StudioPreviewMode {
    RICH_IN_APP,
    COLLAPSED_SYSTEM,
    EXPANDED_SYSTEM
}

class NotificationStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val coordinator = WorkDailyNotificationCoordinator(application)
    private val allTemplates = NotificationTemplateRegistry.CATALOG_TEMPLATES

    private val _uiState = MutableStateFlow(NotificationStudioUiState())
    val uiState: StateFlow<NotificationStudioUiState> = _uiState.asStateFlow()

    private var carouselJob: Job? = null
    private var soundPool: SoundPool? = null
    private var testSoundId: Int = 0

    init {
        initSoundPool()
    }

    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(audioAttributes)
            .build()
        testSoundId = soundPool?.load(getApplication(), R.raw.reward_coin_collect, 1) ?: 0
    }

    val filteredTemplates: List<NotificationPayload>
        get() {
            val cat = _uiState.value.selectedCategory
            return if (cat == null) allTemplates else allTemplates.filter { it.event.category == cat }
        }

    val currentTemplate: NotificationPayload?
        get() {
            val list = filteredTemplates
            if (list.isEmpty()) return null
            val idx = _uiState.value.currentIndex.coerceIn(0, list.size - 1)
            return list[idx]
        }

    fun nextTemplate() {
        val list = filteredTemplates
        if (list.isEmpty()) return
        val nextIdx = (_uiState.value.currentIndex + 1) % list.size
        _uiState.update { it.copy(currentIndex = nextIdx) }
    }

    fun previousTemplate() {
        val list = filteredTemplates
        if (list.isEmpty()) return
        val prevIdx = if (_uiState.value.currentIndex - 1 < 0) list.size - 1 else _uiState.value.currentIndex - 1
        _uiState.update { it.copy(currentIndex = prevIdx) }
    }

    fun selectCategory(category: NotificationCategory?) {
        _uiState.update { it.copy(selectedCategory = category, currentIndex = 0) }
    }

    fun toggleDarkModePreview() {
        _uiState.update { it.copy(isDarkModePreview = !it.isDarkModePreview) }
    }

    fun setPreviewMode(mode: StudioPreviewMode) {
        _uiState.update { it.copy(previewMode = mode) }
    }

    fun toggleAutoCarousel() {
        val running = !_uiState.value.isAutoCarouselRunning
        _uiState.update { it.copy(isAutoCarouselRunning = running) }

        if (running) {
            carouselJob = viewModelScope.launch {
                while (true) {
                    delay(3000)
                    nextTemplate()
                }
            }
        } else {
            carouselJob?.cancel()
            carouselJob = null
        }
    }

    fun playTestSound() {
        if (testSoundId != 0) {
            soundPool?.play(testSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun triggerLocalTestNotification() {
        val payload = currentTemplate ?: return
        coordinator.postNotification(payload.event, triggerSystemAlert = true)
    }

    override fun onCleared() {
        super.onCleared()
        soundPool?.release()
        soundPool = null
    }
}
