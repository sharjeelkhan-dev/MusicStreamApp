package com.musicstream.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.SettingsRepository
import com.musicstream.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    val theme: StateFlow<String> = settingsRepository.getTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System Default")

    val isLoggedIn: StateFlow<Boolean?> = userRepository.isLoggedIn()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    suspend fun isEmailRegistered(email: String): Boolean = userRepository.isEmailRegistered(email)

    fun updateUser(user: User) {
        viewModelScope.launch {
            userRepository.updateUser(user)
        }
    }

    fun downloadSong(song: Song) {
        viewModelScope.launch {
            musicRepository.addToRecentlyPlayed(song) // Ensure metadata exists
            com.musicstream.app.worker.AudioDownloadWorker.enqueue(
                context = (musicRepository as com.musicstream.app.data.repository.MusicRepositoryImpl).getContext(),
                song = song
            )
        }
    }
}
