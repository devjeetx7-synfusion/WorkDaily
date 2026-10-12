package com.synfusion.attendance.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.synfusion.attendance.auth.OnlineAccount
import com.synfusion.attendance.auth.OnlineAuthRepository
import com.synfusion.attendance.auth.OnlineAuthState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing online authentication state and actions.
 * Prevents duplicate sign-in attempts while loading and maps authentication states cleanly.
 */
class OnlineAuthViewModel(
    private val onlineAuthRepository: OnlineAuthRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    val authState: StateFlow<OnlineAuthState> = onlineAuthRepository.authState

    fun signInWithGoogle(context: Context, webClientId: String? = null): Job {
        if (authState.value is OnlineAuthState.Loading) {
            // Protect against repeated sign-in taps while loading
            return Job().apply { complete() }
        }

        return viewModelScope.launch(ioDispatcher) {
            onlineAuthRepository.signInWithGoogle(context, webClientId)
        }
    }

    fun signOut(): Job {
        return viewModelScope.launch(ioDispatcher) {
            onlineAuthRepository.signOut()
        }
    }

    fun getCurrentAccount(): OnlineAccount? {
        return onlineAuthRepository.getCurrentUser()
    }
}
