package com.musicstream.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.UserRepository
import com.musicstream.app.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val audioQuality: String = "High (320kbps)",
    val theme: String = "System Default",
    val notifications: String = "On",
    val language: String = "English",
    val equalizer: String = "Custom",
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = combine(
        _uiState,
        settingsRepository.getAudioQuality(),
        settingsRepository.getTheme(),
        settingsRepository.getNotificationsEnabled(),
        settingsRepository.getLanguage(),
        settingsRepository.getEqualizerPreset()
    ) { params: Array<Any> ->
        val state = params[0] as ProfileUiState
        val audio = params[1] as String
        val theme = params[2] as String
        val notifications = params[3] as Boolean
        val language = params[4] as String
        val equalizer = params[5] as String

        state.copy(
            audioQuality = audio,
            theme = theme,
            notifications = if (notifications) "On" else "Off",
            language = language,
            equalizer = equalizer
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun setAudioQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setAudioQuality(quality)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            val isEnabled = uiState.value.notifications == "On"
            settingsRepository.setNotificationsEnabled(!isEnabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setEqualizerPreset(preset: String) {
        viewModelScope.launch {
            settingsRepository.setEqualizerPreset(preset)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            userRepository.signOut()
        }
    }
}
