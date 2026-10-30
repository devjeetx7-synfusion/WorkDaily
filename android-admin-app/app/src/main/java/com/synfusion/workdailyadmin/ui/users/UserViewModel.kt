package com.synfusion.workdailyadmin.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.workdailyadmin.data.AdminRepository
import com.synfusion.workdailyadmin.data.Result
import com.synfusion.workdailyadmin.data.models.UserDetails
import com.synfusion.workdailyadmin.data.models.UserRestrictions
import com.synfusion.workdailyadmin.data.models.UserSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserListState {
    object Loading : UserListState()
    data class Success(val users: List<UserSummary>) : UserListState()
    data class Error(val message: String) : UserListState()
}

sealed class UserDetailState {
    object Idle : UserDetailState()
    object Loading : UserDetailState()
    data class Success(val details: UserDetails) : UserDetailState()
    data class Error(val message: String) : UserDetailState()
}

class UserViewModel @JvmOverloads constructor(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _userListState = MutableStateFlow<UserListState>(UserListState.Loading)
    val userListState: StateFlow<UserListState> = _userListState.asStateFlow()

    private val _userDetailState = MutableStateFlow<UserDetailState>(UserDetailState.Idle)
    val userDetailState: StateFlow<UserDetailState> = _userDetailState.asStateFlow()

    var searchQuery = MutableStateFlow("")
    var statusFilter = MutableStateFlow<String?>(null)

    init {
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _userListState.value = UserListState.Loading
            when (val res = repository.getUsers(searchQuery.value, statusFilter.value)) {
                is Result.Success -> _userListState.value = UserListState.Success(res.data)
                is Result.Error -> _userListState.value = UserListState.Error(res.message)
            }
        }
    }

    fun loadUserDetails(targetUid: String) {
        viewModelScope.launch {
            _userDetailState.value = UserDetailState.Loading
            when (val res = repository.getUserDetails(targetUid)) {
                is Result.Success -> _userDetailState.value = UserDetailState.Success(res.data)
                is Result.Error -> _userDetailState.value = UserDetailState.Error(res.message)
            }
        }
    }

    fun updateUserStatus(
        targetUid: String,
        status: String?,
        isVerified: Boolean?,
        restrictions: UserRestrictions?,
        reason: String,
        onComplete: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            when (val res = repository.updateUserStatus(targetUid, status, isVerified, restrictions, reason)) {
                is Result.Success -> {
                    loadUsers()
                    if (_userDetailState.value is UserDetailState.Success) {
                        loadUserDetails(targetUid)
                    }
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }

    fun revokeSessions(targetUid: String, reason: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.revokeSessions(targetUid, reason)) {
                is Result.Success -> onComplete(true, null)
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }

    fun addUserNote(targetUid: String, note: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            when (val res = repository.addUserNote(targetUid, note)) {
                is Result.Success -> {
                    loadUserDetails(targetUid)
                    onComplete(true, null)
                }
                is Result.Error -> onComplete(false, res.message)
            }
        }
    }
}
