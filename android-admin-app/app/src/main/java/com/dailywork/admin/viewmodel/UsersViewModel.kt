package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.User
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortOrder {
    RECENT_ACTIVITY, JOINED_DATE
}

@HiltViewModel
class UsersViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _roleFilter = MutableStateFlow<String?>(null)
    val roleFilter: StateFlow<String?> = _roleFilter

    private val _premiumFilter = MutableStateFlow<Boolean?>(null)
    val premiumFilter: StateFlow<Boolean?> = _premiumFilter

    private val _blockedFilter = MutableStateFlow<Boolean?>(null)
    val blockedFilter: StateFlow<Boolean?> = _blockedFilter

    private val _sortOrder = MutableStateFlow(SortOrder.RECENT_ACTIVITY)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _userLimit = MutableStateFlow(50L)

    private val _isPerformingAction = MutableStateFlow(false)
    val isPerformingAction: StateFlow<Boolean> = _isPerformingAction

    val filteredUsers: StateFlow<List<User>> = combine(
        _allUsers,
        _searchQuery,
        _roleFilter,
        _premiumFilter,
        _blockedFilter,
        _sortOrder
    ) { flows ->
        val users = flows[0] as List<User>
        val query = flows[1] as String
        val role = flows[2] as String?
        val premium = flows[3] as Boolean?
        val blocked = flows[4] as Boolean?
        val sort = flows[5] as SortOrder

        val q = query.trim().lowercase()
        users
            .filter { user ->
                q.isBlank() || user.name.lowercase().contains(q) || user.email.lowercase().contains(q)
            }
            .filter { user -> role == null || user.role == role }
            .filter { user -> premium == null || user.isPremium == premium }
            .filter { user -> blocked == null || user.isBlocked == blocked }
            .sortedWith { u1, u2 ->
                when (sort) {
                    SortOrder.RECENT_ACTIVITY -> u2.lastActive.compareTo(u1.lastActive)
                    SortOrder.JOINED_DATE -> u2.createdAt.compareTo(u1.createdAt)
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadUsers()
    }

    fun refresh() {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _userLimit.collectLatest { limit ->
                repository.getAllUsers(limit).collectLatest {
                    _allUsers.value = it
                }
            }
        }
    }

    fun loadMore() {
        _userLimit.value += 50
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoleFilter(role: String?) {
        _roleFilter.value = role
    }

    fun setPremiumFilter(premium: Boolean?) {
        _premiumFilter.value = premium
    }

    fun setBlockedFilter(blocked: Boolean?) {
        _blockedFilter.value = blocked
    }

    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun toggleBlockStatus(userId: String, isBlocked: Boolean, reason: String = "") {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.setUserBlockedStatus(userId, isBlocked, reason)
            _isPerformingAction.value = false
        }
    }

    fun togglePremiumStatus(userId: String, isPremium: Boolean, expiryDays: Int = 0) {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.setUserPremiumStatus(userId, isPremium, expiryDays)
            _isPerformingAction.value = false
        }
    }

    fun changeRole(userId: String, role: String) {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.setUserRole(userId, role)
            _isPerformingAction.value = false
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            _isPerformingAction.value = true
            repository.deleteUser(userId)
            _isPerformingAction.value = false
        }
    }

    fun getUser(userId: String): Flow<User?> = repository.getUser(userId)
}
