package com.synfusion.attendance.notifications.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.notifications.model.NotificationCategory
import com.synfusion.attendance.notifications.model.NotificationEvent
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotificationCenterUiState(
    val notifications: List<NotificationEvent> = emptyList(),
    val selectedCategoryFilter: NotificationCategory? = null,
    val unreadCount: Int = 0,
    val isLoading: Boolean = false
)

class NotificationCenterViewModel(application: Application) : AndroidViewModel(application) {

    private val notificationDao = AttendanceDatabase.getInstance(application).notificationDao()

    private val _selectedCategory = MutableStateFlow<NotificationCategory?>(null)

    val uiState: StateFlow<NotificationCenterUiState> = combine(
        notificationDao.getAllNotifications(),
        notificationDao.getUnreadCount(),
        _selectedCategory
    ) { allEntities, unreadCount, category ->
        val events = allEntities.map { it.toDomain() }
        val filtered = if (category == null) events else events.filter { it.category == category }

        NotificationCenterUiState(
            notifications = filtered,
            selectedCategoryFilter = category,
            unreadCount = unreadCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotificationCenterUiState(isLoading = true)
    )

    fun selectCategoryFilter(category: NotificationCategory?) {
        _selectedCategory.value = category
    }

    fun markAsRead(id: String) {
        viewModelScope.launch {
            notificationDao.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationDao.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            notificationDao.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationDao.clearAll()
        }
    }
}
