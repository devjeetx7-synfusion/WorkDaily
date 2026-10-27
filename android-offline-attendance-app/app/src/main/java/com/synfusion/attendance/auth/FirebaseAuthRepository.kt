package com.synfusion.attendance.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Firebase implementation of [OnlineAuthRepository].
 * Handles Google Sign-In via Android Credential Manager API and manages Firebase Auth sessions.
 * Completely decoupled from local WorkDaily workspace identities and offline data.
 */
class FirebaseAuthRepository(
    private val context: Context
) : OnlineAuthRepository {

    private val _authState = MutableStateFlow<OnlineAuthState>(OnlineAuthState.Idle)
    override val authState: StateFlow<OnlineAuthState> = _authState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        initializeFirebase()
    }

    private fun initializeFirebase() {
        try {
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                val auth = FirebaseAuth.getInstance()
                firebaseAuth = auth
                authListener = FirebaseAuth.AuthStateListener { authInstance ->
                    val firebaseUser = authInstance.currentUser
                    if (firebaseUser != null) {
                        _authState.value = OnlineAuthState.Authenticated(mapFirebaseUser(firebaseUser))
                    } else {
                        if (_authState.value is OnlineAuthState.Authenticated || _authState.value is OnlineAuthState.Loading) {
                            _authState.value = OnlineAuthState.Unauthenticated
                        }
                    }
                }
                auth.addAuthStateListener(authListener!!)
                val current = auth.currentUser
                if (current != null) {
                    _authState.value = OnlineAuthState.Authenticated(mapFirebaseUser(current))
                } else {
                    _authState.value = OnlineAuthState.Unauthenticated
                }
            } else {
                _authState.value = OnlineAuthState.Error("Firebase is not initialized. Please configure google-services.json.")
            }
        } catch (e: Throwable) {
            _authState.value = OnlineAuthState.Error("Firebase initialization deferred: ${e.message ?: "Configuration required"}")
        }
    }

    override fun getCurrentUser(): OnlineAccount? {
        return try {
            firebaseAuth?.currentUser?.let { mapFirebaseUser(it) }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signInWithGoogle(context: Context, webClientId: String?): OnlineAuthState = withContext(Dispatchers.IO) {
        val auth = firebaseAuth
        if (auth == null) {
            val errorState = OnlineAuthState.Error("Firebase is not configured on this device.")
            _authState.value = errorState
            return@withContext errorState
        }

        val clientId = webClientId?.takeIf { it.isNotBlank() }
        if (clientId == null) {
            val errorState = OnlineAuthState.Error("OAuth Web Client ID is required for Google Sign-In.")
            _authState.value = errorState
            return@withContext errorState
        }

        _authState.value = OnlineAuthState.Loading

        try {
            val credentialManager = CredentialManager.create(context)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(firebaseCredential).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val account = mapFirebaseUser(firebaseUser)
                val successState = OnlineAuthState.Authenticated(account)
                _authState.value = successState
                return@withContext successState
            } else {
                val failureState = OnlineAuthState.Error("Authentication succeeded but no user details were returned.")
                _authState.value = failureState
                return@withContext failureState
            }
        } catch (e: GetCredentialCancellationException) {
            val cancelledState = OnlineAuthState.Cancelled
            _authState.value = cancelledState
            return@withContext cancelledState
        } catch (e: GetCredentialException) {
            val msg = e.message ?: "Credential error"
            val errorState = if (msg.contains("network", ignoreCase = true) || msg.contains("connection", ignoreCase = true)) {
                OnlineAuthState.NetworkUnavailable
            } else {
                OnlineAuthState.Error("Sign-in failed: $msg")
            }
            _authState.value = errorState
            return@withContext errorState
        } catch (e: Exception) {
            val msg = e.message ?: "Authentication failed"
            val errorState = if (msg.contains("network", ignoreCase = true) || msg.contains("offline", ignoreCase = true)) {
                OnlineAuthState.NetworkUnavailable
            } else {
                OnlineAuthState.Error("Google Sign-In failed: $msg")
            }
            _authState.value = errorState
            return@withContext errorState
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                _authState.value = OnlineAuthState.Error("Sign-out failed: ${e.message ?: "Unknown error"}")
                return@withContext
            }
            try {
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {
                // Credential Manager may be unavailable on non-GMS devices or in unit tests
            }
            // AuthStateListener will fire and set Unauthenticated after Firebase sign-out completes.
            // Set it explicitly here as well for immediate UI feedback.
            _authState.value = OnlineAuthState.Unauthenticated
        }
    }

    private fun mapFirebaseUser(user: FirebaseUser): OnlineAccount {
        return OnlineAccount(
            uid = user.uid,
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            isEmailVerified = user.isEmailVerified
        )
    }
}
