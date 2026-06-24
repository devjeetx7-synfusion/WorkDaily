package com.dailywork.admin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailywork.admin.data.model.User
import com.dailywork.admin.data.repository.AdminFirestoreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val repository: AdminFirestoreRepository
) : ViewModel() {

    private val _allUsers = MutableStateFlow<List<User>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val premiumUsers: StateFlow<List<User>> = combine(_allUsers, _searchQuery) { users, query ->
        val q = query.trim().lowercase()
        users.filter { it.isPremium }
             .filter { q.isBlank() || it.name.lowercase().contains(q) || it.email.lowercase().contains(q) }
             .sortedByDescending { it.premiumInfo?.activatedAt ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getAllUsers(1000).collectLatest { users ->
                _allUsers.value = users
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}
