package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.repository.FCMRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: FCMRepository
) : ViewModel() {

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending

    fun sendNotification(target: String, title: String, message: String) {
        viewModelScope.launch {
            _isSending.value = true
            repository.sendNotification(target, title, message)
            _isSending.value = false
        }
    }
}
