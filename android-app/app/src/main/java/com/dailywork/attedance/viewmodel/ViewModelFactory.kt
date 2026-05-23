package com.dailywork.attedance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dailywork.attedance.data.UserPreferencesRepository
import com.dailywork.attedance.data.FirestoreRepository
import com.dailywork.attedance.data.local.AppDatabase
import com.dailywork.attedance.billing.BillingManager
import com.dailywork.attedance.billing.BillingRepository
import com.dailywork.attedance.billing.SubscriptionVerifier
import com.dailywork.attedance.premium.PremiumManager
import com.dailywork.attedance.ui.premium.PremiumViewModel

class ViewModelFactory(
    private val repository: UserPreferencesRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    private val syncActionDao = AppDatabase.getInstance(context).syncActionDao()
    private val firestoreRepository = FirestoreRepository(syncActionDao = syncActionDao)

    private val subscriptionVerifier = SubscriptionVerifier()

    // Handle circular dependency using setter injection
    val billingManager: BillingManager by lazy {
        BillingManager(context)
    }

    val premiumManager: PremiumManager by lazy {
        PremiumManager(repository, subscriptionVerifier, billingManager).also {
            billingManager.setPurchaseHandler(it)
        }
    }

    val billingRepository: BillingRepository by lazy {
        BillingRepository(billingManager)
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(PassbookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PassbookViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkersViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkersViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerDetailViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(WorkerHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkerHistoryViewModel(repository, firestoreRepository) as T
        }
        if (modelClass.isAssignableFrom(PremiumViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PremiumViewModel(repository, billingRepository, billingManager, premiumManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
