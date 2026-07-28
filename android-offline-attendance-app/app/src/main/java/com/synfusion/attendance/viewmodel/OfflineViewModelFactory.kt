package com.synfusion.attendance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synfusion.attendance.data.AttendanceDatabase
import com.synfusion.attendance.data.LocalAttendanceRepository
import com.synfusion.attendance.data.OfflinePreferencesRepository

class OfflineViewModelFactory(
    private val repository: OfflinePreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    private val db = AttendanceDatabase.getInstance(context)
    private val localAttendanceRepository = LocalAttendanceRepository(
        userDao = db.userDao(),
        workerDao = db.workerDao(),
        attendanceDao = db.attendanceDao(),
        summaryDao = db.summaryDao()
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OfflineAuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineAuthViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineDashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineDashboardViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineCalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineCalendarViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineStatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineStatsViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineSettingsViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineWorkersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineWorkersViewModel(repository, localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineWorkerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineWorkerDetailViewModel(localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflineWorkerHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflineWorkerHistoryViewModel(localAttendanceRepository) as T
        }
        if (modelClass.isAssignableFrom(OfflinePassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OfflinePassbookViewModel(repository, localAttendanceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
