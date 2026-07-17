package com.dailywork.attedance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.data.FirestoreRepository
import com.dailywork.attedance.data.local.AppDatabase
import com.dailywork.attedance.access.FeatureAccessController

class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    private val syncActionDao = AppDatabase.getInstance(context).syncActionDao()
    private val firestoreRepository = FirestoreRepository(syncActionDao = syncActionDao)
    private val accessController = FeatureAccessController.getInstance(context)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, firestoreRepository, accessController) as T
        }
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, firestoreRepository, accessController) as T
        }
        if (modelClass.isAssignableFrom(PassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassbookViewModel(repository, firestoreRepository, accessController) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkersViewModel(repository, firestoreRepository, accessController) as T
        }
        if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerDetailViewModel(repository, firestoreRepository, accessController) as T
        }
        if (modelClass.isAssignableFrom(WorkerHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerHistoryViewModel(repository, firestoreRepository, accessController) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
