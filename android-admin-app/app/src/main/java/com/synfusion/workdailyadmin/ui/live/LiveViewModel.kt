package com.synfusion.workdailyadmin.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.LiveRoomItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LiveUiState {
    object Loading : LiveUiState()
    data class Success(val rooms: List<LiveRoomItem>) : LiveUiState()
    data class Error(val message: String) : LiveUiState()
}

class LiveViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<LiveUiState>(LiveUiState.Loading)
    val uiState: StateFlow<LiveUiState> = _uiState.asStateFlow()

    init {
        loadLiveRooms()
    }

    fun loadLiveRooms() {
        viewModelScope.launch {
            _uiState.value = LiveUiState.Loading
            when (val res = repository.getLiveRooms(isLive = true)) {
                is Result.Success -> _uiState.value = LiveUiState.Success(res.data)
                is Result.Error -> _uiState.value = LiveUiState.Error(res.message)
            }
        }
    }

    fun endRoom(roomId: String, reason: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.endLiveRoom(roomId, reason)) {
                is Result.Success -> {
                    loadLiveRooms()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
