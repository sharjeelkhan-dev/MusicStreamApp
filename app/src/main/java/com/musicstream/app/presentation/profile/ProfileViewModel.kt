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

    // Ab humne _uiState ko khatam kar diya hai kyunki hum
    // direct Repository flows ko combine kar rahe hain.
    val uiState: StateFlow<ProfileUiState> = combine(
        userRepository.getCurrentUser(),
        settingsRepository.getAudioQuality(),
        settingsRepository.getTheme(),
        settingsRepository.getNotificationsEnabled(),
        settingsRepository.getLanguage(),
        settingsRepository.getEqualizerPreset()
    ) { args: Array<Any?> -> // 6 flows ke liye Array use karein
        val user = args[0] as User
        val audio = args[1] as String
        val theme = args[2] as String
        val notifications = args[3] as Boolean
        val language = args[4] as String
        val equalizer = args[5] as String

        ProfileUiState(
            user = user,
            audioQuality = audio,
            theme = theme,
            notifications = if (notifications) "On" else "Off",
            language = language,
            equalizer = equalizer,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfileUiState(isLoading = true)
    )

    fun updateProfile(name: String, email: String, avatarUrl: String, bannerUrl: String) {
        viewModelScope.launch {
            // uiState.value.user se current user ki ID aur baqi info mil jayegi
            uiState.value.user?.let { currentUser ->
                val updatedUser = currentUser.copy(
                    name = name,
                    email = email,
                    avatarUrl = avatarUrl,
                    bannerUrl = bannerUrl
                )
                // Ye repository method database (Room/DataStore) mein save karna chahiye
                userRepository.updateUser(updatedUser)
            }
        }
    }

    // --- Settings Update Functions ---

    fun setAudioQuality(quality: String) {
        viewModelScope.launch { settingsRepository.setAudioQuality(quality) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun toggleNotifications() {
        viewModelScope.launch {
            val isEnabled = uiState.value.notifications == "On"
            settingsRepository.setNotificationsEnabled(!isEnabled)
        }
    }

    fun setLanguage(language: String) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setEqualizerPreset(preset: String) {
        viewModelScope.launch { settingsRepository.setEqualizerPreset(preset) }
    }

    fun signOut() {
        viewModelScope.launch { userRepository.signOut() }
    }
}
