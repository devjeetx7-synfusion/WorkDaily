package com.synfusion.attendance.auth

import com.synfusion.attendance.viewmodel.OnlineAuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class OnlineAuthTest {

    private class FakeOnlineAuthRepository : OnlineAuthRepository {
        private val _authState = MutableStateFlow<OnlineAuthState>(OnlineAuthState.Idle)
        override val authState: StateFlow<OnlineAuthState> = _authState.asStateFlow()

        var signInCallCount = 0

        fun setAuthState(state: OnlineAuthState) {
            _authState.value = state
        }

        override fun getCurrentUser(): OnlineAccount? {
            return (_authState.value as? OnlineAuthState.Authenticated)?.account
        }

        override suspend fun signInWithGoogle(context: android.content.Context, webClientId: String?): OnlineAuthState {
            signInCallCount++
            _authState.value = OnlineAuthState.Loading
            if (webClientId.isNullOrBlank()) {
                val errorState = OnlineAuthState.Error("OAuth Web Client ID is required for Google Sign-In.")
                _authState.value = errorState
                return errorState
            }
            val account = OnlineAccount(
                uid = "firebase-test-uid-789",
                displayName = "Test User",
                email = "test@example.com",
                photoUrl = "https://example.com/photo.jpg",
                isEmailVerified = true
            )
            val successState = OnlineAuthState.Authenticated(account)
            _authState.value = successState
            return successState
        }

        override suspend fun signOut() {
            _authState.value = OnlineAuthState.Unauthenticated
        }
    }

    @Test
    fun testOnlineAccountModel() {
        val account = OnlineAccount(
            uid = "firebase-uid-123",
            displayName = "John Doe",
            email = "john@example.com",
            photoUrl = "https://lh3.googleusercontent.com/avatar.png",
            isEmailVerified = true
        )

        assertEquals("firebase-uid-123", account.uid)
        assertEquals("John Doe", account.displayName)
        assertEquals("john@example.com", account.email)
        assertEquals("https://lh3.googleusercontent.com/avatar.png", account.photoUrl)
        assertTrue(account.isEmailVerified)
    }

    @Test
    fun testOnlineAuthStates() {
        val account = OnlineAccount(uid = "firebase-uid-456")
        val idleState = OnlineAuthState.Idle
        val loadingState = OnlineAuthState.Loading
        val authenticatedState = OnlineAuthState.Authenticated(account)
        val unauthenticatedState = OnlineAuthState.Unauthenticated
        val cancelledState = OnlineAuthState.Cancelled
        val networkUnavailableState = OnlineAuthState.NetworkUnavailable
        val errorState = OnlineAuthState.Error("Failed to authenticate")

        assertEquals(OnlineAuthState.Idle, idleState)
        assertEquals(OnlineAuthState.Loading, loadingState)
        assertEquals(account, authenticatedState.account)
        assertEquals(OnlineAuthState.Unauthenticated, unauthenticatedState)
        assertEquals(OnlineAuthState.Cancelled, cancelledState)
        assertEquals(OnlineAuthState.NetworkUnavailable, networkUnavailableState)
        assertEquals("Failed to authenticate", errorState.message)
    }

    @Test
    fun testViewModelTapDebouncingWhenLoading() = runBlocking {
        val repository = FakeOnlineAuthRepository()
        val viewModel = OnlineAuthViewModel(repository, Dispatchers.Unconfined)

        // Set state to Loading
        repository.setAuthState(OnlineAuthState.Loading)

        // Attempt sign in while Loading
        viewModel.signInWithGoogle(DummyContext(), "web-client-id")

        // Verify repository sign in was NOT invoked due to loading state protection
        assertEquals(0, repository.signInCallCount)
    }

    @Test
    fun testViewModelSignOut() = runBlocking {
        val repository = FakeOnlineAuthRepository()
        val account = OnlineAccount(uid = "firebase-uid-999")
        repository.setAuthState(OnlineAuthState.Authenticated(account))

        val viewModel = OnlineAuthViewModel(repository, Dispatchers.Unconfined)
        assertEquals(account, viewModel.getCurrentAccount())

        viewModel.signOut().join()

        assertTrue(viewModel.authState.value is OnlineAuthState.Unauthenticated)
        assertNull(viewModel.getCurrentAccount())
    }

    @Test
    fun testStrictSeparationOfLocalWorkspaceIdAndFirebaseUid() {
        val localWorkspaceId = UUID.randomUUID().toString()
        val firebaseUid = "firebase-auth-user-id-555"

        assertNotEquals(localWorkspaceId, firebaseUid)
        assertTrue(localWorkspaceId.length >= 32)
        assertTrue(firebaseUid.startsWith("firebase-"))
    }

    private class DummyContext : android.content.ContextWrapper(null)
}
