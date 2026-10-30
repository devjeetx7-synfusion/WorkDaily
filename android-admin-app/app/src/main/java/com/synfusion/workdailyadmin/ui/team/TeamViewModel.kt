package com.synfusion.workdailyadmin.ui.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.AdminTeamMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeamUiState {
    object Loading : TeamUiState()
    data class Success(val team: List<AdminTeamMember>) : TeamUiState()
    data class Error(val message: String) : TeamUiState()
}

class TeamViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TeamUiState>(TeamUiState.Loading)
    val uiState: StateFlow<TeamUiState> = _uiState.asStateFlow()

    init {
        loadTeam()
    }

    fun loadTeam() {
        viewModelScope.launch {
            _uiState.value = TeamUiState.Loading
            when (val res = repository.getAdminTeam()) {
                is Result.Success -> _uiState.value = TeamUiState.Success(res.data)
                is Result.Error -> _uiState.value = TeamUiState.Error(res.message)
            }
        }
    }

    fun assignRole(targetUid: String, email: String, role: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.assignAdminRole(targetUid, email, role)) {
                is Result.Success -> {
                    loadTeam()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
