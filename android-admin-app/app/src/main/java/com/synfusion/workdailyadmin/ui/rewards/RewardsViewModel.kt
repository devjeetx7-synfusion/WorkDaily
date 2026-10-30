package com.synfusion.workdailyadmin.ui.rewards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.DashboardStats
import com.synfusion.workdailyadmin.data.models.RedemptionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class RewardsUiState {
    object Loading : RewardsUiState()
    data class Success(
        val stats: DashboardStats,
        val pendingRedemptions: List<RedemptionRecord> = emptyList()
    ) : RewardsUiState()
    data class Error(val message: String) : RewardsUiState()
}

class RewardsViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RewardsUiState>(RewardsUiState.Loading)
    val uiState: StateFlow<RewardsUiState> = _uiState.asStateFlow()

    init {
        loadRewardsOverview()
    }

    fun loadRewardsOverview() {
        viewModelScope.launch {
            _uiState.value = RewardsUiState.Loading
            when (val res = repository.getDashboardStats()) {
                is Result.Success -> {
                    _uiState.value = RewardsUiState.Success(
                        stats = res.data,
                        pendingRedemptions = emptyList()
                    )
                }
                is Result.Error -> _uiState.value = RewardsUiState.Error(res.message)
            }
        }
    }

    fun adjustUserCoins(targetUid: String, amount: Int, reason: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.adjustUserCoins(targetUid, amount, reason)) {
                is Result.Success -> {
                    loadRewardsOverview()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }

    fun processRedemption(targetUid: String, redemptionId: String, status: String, notes: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.processRedemption(targetUid, redemptionId, status, notes)) {
                is Result.Success -> {
                    loadRewardsOverview()
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
