package com.musicstream.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val audioQuality: String = "High (320kbps)",
    val theme: String = "Dark Mode",
    val notifications: String = "On",
    val language: String = "English",
    val plan: String = "Premium",
    val devices: String = "2 active",
    val equalizer: String = "Custom",
    val privacy: String = "Friends only"
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
