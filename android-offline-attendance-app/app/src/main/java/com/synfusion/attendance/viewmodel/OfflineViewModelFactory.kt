package com.synfusion.attendance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synfusion.attendance.data.DrillDownRepository
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository

class OfflineViewModelFactory(
    private val repository: OfflinePreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    private val localAttendanceRepository = LocalAttendanceRepository(context)
    private val drillDownRepository = DrillDownRepository(context)

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfflineAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineAuthViewModel(repository, localAttendanceRepository) as T
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
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
