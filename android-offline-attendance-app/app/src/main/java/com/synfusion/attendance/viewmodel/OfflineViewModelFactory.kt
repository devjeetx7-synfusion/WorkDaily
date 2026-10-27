package com.synfusion.attendance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synfusion.attendance.auth.FirebaseAuthRepository
import com.synfusion.attendance.auth.OnlineAuthRepository
import com.synfusion.attendance.data.AndroidWorkspaceStorageManager
import com.synfusion.attendance.data.DrillDownRepository
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository
import com.synfusion.attendance.online.FirestoreOnlineProfileRepository
import com.synfusion.attendance.online.FirestorePublicProfileRepository
import com.synfusion.attendance.online.OnlineProfileRepository
import com.synfusion.attendance.online.PublicProfileRepository

class OfflineViewModelFactory(
    private val repository: OfflinePreferencesRepository,
    private val context: Context,
    private val onlineAuthRepository: OnlineAuthRepository = FirebaseAuthRepository(context.applicationContext),
    private val onlineProfileRepository: OnlineProfileRepository = FirestoreOnlineProfileRepository(),
    private val publicProfileRepository: PublicProfileRepository = FirestorePublicProfileRepository()
) : ViewModelProvider.Factory {

    private val localAttendanceRepository = LocalAttendanceRepository(context)
    private val drillDownRepository = DrillDownRepository(context)
    private val workspaceStorageManager = AndroidWorkspaceStorageManager(context.applicationContext, repository)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfflineAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineAuthViewModel(
                repository = repository,
                localAttendanceRepository = localAttendanceRepository,
                onlineProfileRepository = onlineProfileRepository,
                publicProfileRepository = publicProfileRepository,
                workspaceStorageManager = workspaceStorageManager
            ) as T
        }
        if (modelClass.isAssignableFrom(OfflineDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineDashboardViewModel(repository, localAttendanceRepository, drillDownRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineCalendarViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineStatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineStatsViewModel(repository, localAttendanceRepository, drillDownRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineSettingsViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflinePassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflinePassbookViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineAllTimePassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineAllTimePassbookViewModel(repository, localAttendanceRepository, drillDownRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineOnboardingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineOnboardingViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(OnlineAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnlineAuthViewModel(onlineAuthRepository) as T
        }
        if (modelClass.isAssignableFrom(OnlineProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OnlineProfileViewModel(onlineProfileRepository, publicProfileRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
