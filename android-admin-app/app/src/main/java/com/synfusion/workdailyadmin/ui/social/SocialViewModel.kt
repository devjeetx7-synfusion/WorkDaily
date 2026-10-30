package com.synfusion.workdailyadmin.ui.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.UserSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SocialUiState {
    object Loading : SocialUiState()
    data class Success(val users: List<UserSummary>) : SocialUiState()
    data class Error(val message: String) : SocialUiState()
}

class SocialViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SocialUiState>(SocialUiState.Loading)
    val uiState: StateFlow<SocialUiState> = _uiState.asStateFlow()

    var searchQuery = MutableStateFlow("")

    init {
        loadSocialProfiles()
    }

    fun loadSocialProfiles() {
        viewModelScope.launch {
            _uiState.value = SocialUiState.Loading
            when (val res = repository.getUsers(searchQuery = searchQuery.value, limit = 30)) {
                is Result.Success -> _uiState.value = SocialUiState.Success(res.data)
                is Result.Error -> _uiState.value = SocialUiState.Error(res.message)
            }
        }
    }

    fun toggleVerification(targetUid: String, currentStatus: Boolean, reason: String) {
        viewModelScope.launch {
            repository.updateUserStatus(
                targetUid = targetUid,
                accountStatus = null,
                isVerified = !currentStatus,
                restrictions = null,
                reason = reason
            )
            loadSocialProfiles()
        }
    }
}
