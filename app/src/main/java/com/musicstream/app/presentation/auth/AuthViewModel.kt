package com.musicstream.app.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.musicstream.app.R
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.domain.repository.UserRepository
import com.musicstream.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val isLoginSuccessful: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val notificationRepository: NotificationRepository,
    private val credentialManager: CredentialManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, pasword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email, pasword).await()
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: email.substringBefore("@"),
                        email = email,
                        avatarUrl = firebaseUser.photoUrl?.toString() ?: ""
                    )
                    userRepository.updateUser(user)
                    
                    notificationRepository.addNotification(
                        com.musicstream.app.domain.model.Notification(
                            id = java.util.UUID.randomUUID().toString(),
                            title = "Welcome back, ${user.name}!",
                            message = "Glad to see you again. Let's play some music!",
                            time = "Just now",
                            type = com.musicstream.app.domain.model.NotificationType.GENERAL
                        )
                    )
                    
                    _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
            }
        }
    }

    fun signUp(name: String, email: String, pasword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.createUserWithEmailAndPassword(email, pasword).await()
                val firebaseUser = auth.currentUser
                if (firebaseUser != null) {
                    val user = User(
                        id = firebaseUser.uid,
                        name = name,
                        email = email
                    )
                    userRepository.updateUser(user)
                    _uiState.update { it.copy(isLoading = false, successMessage = "Account created! Please Login.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Sign up failed") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun signInWithGoogle(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(context.getString(R.string.default_web_client_id))
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)

                    auth.signInWithCredential(authCredential).await()

                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        val user = User(
                            id = firebaseUser.uid,
                            name = firebaseUser.displayName ?: "",
                            email = firebaseUser.email ?: "",
                            avatarUrl = firebaseUser.photoUrl?.toString() ?: ""
                        )
                        userRepository.updateUser(user)
                        
                        notificationRepository.addNotification(
                            com.musicstream.app.domain.model.Notification(
                                id = java.util.UUID.randomUUID().toString(),
                                title = "Welcome, ${user.name}!",
                                message = "Successfully signed in with Google.",
                                time = "Just now",
                                type = com.musicstream.app.domain.model.NotificationType.GENERAL
                            )
                        )

                        _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Unexpected credential type") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Google sign-in failed") }
            }
        }
    }
}
