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
    val privacy: String = "Friends only",
    val isLoading: Boolean = false
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

    fun updateAudioQuality() {
        val qualities = listOf("Low", "Normal", "High (320kbps)", "Ultra (Hi-Fi)")
        val currentIndex = qualities.indexOf(_uiState.value.audioQuality)
        val nextIndex = (currentIndex + 1) % qualities.size
        _uiState.update { it.copy(audioQuality = qualities[nextIndex]) }
    }

    fun updateTheme() {
        val themes = listOf("Dark Mode", "Light Mode", "System Default")
        val currentIndex = themes.indexOf(_uiState.value.theme)
        val nextIndex = (currentIndex + 1) % themes.size
        _uiState.update { it.copy(theme = themes[nextIndex]) }
    }

    fun toggleNotifications() {
        val status = if (_uiState.value.notifications == "On") "Off" else "On"
        _uiState.update { it.copy(notifications = status) }
    }

    fun updateLanguage() {
        val languages = listOf("English", "Spanish", "French", "German", "Hindi")
        val currentIndex = languages.indexOf(_uiState.value.language)
        val nextIndex = (currentIndex + 1) % languages.size
        _uiState.update { it.copy(language = languages[nextIndex]) }
    }

    fun updateEqualizer() {
        val presets = listOf("Flat", "Bass Boost", "Electronic", "Rock", "Pop", "Custom")
        val currentIndex = presets.indexOf(_uiState.value.equalizer)
        val nextIndex = (currentIndex + 1) % presets.size
        _uiState.update { it.copy(equalizer = presets[nextIndex]) }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
