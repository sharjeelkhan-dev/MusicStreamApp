package com.musicstream.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.domain.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class NotificationUiState(
    val notifications: List<Notification> = emptyList(),
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    // Internal list to maintain state for this demo/local session
    private var notificationList = mutableListOf<Notification>()

    init {
        loadInitialNotifications()
    }

    private fun loadInitialNotifications() {
        viewModelScope.launch {
            // Start with some default ones
            notificationList = mutableListOf(
                Notification("1", "Welcome to MusicStream", "Explore the latest global hits from YouTube!", "Just now", NotificationType.GENERAL),
                Notification("2", "New Playlist Created", "Your 'Songs' playlist is ready to be filled.", "2h ago", NotificationType.PLAYLIST_UPDATE)
            )
            
            // Try to fetch a trending song to announce as a "New Release"
            try {
                musicRepository.getTrendingSongs().first().firstOrNull()?.let { song ->
                    notificationList.add(0, Notification(
                        id = UUID.randomUUID().toString(),
                        title = "New Release: ${song.title}",
                        message = "New track by ${song.artist} is now trending globally!",
                        time = "Recent",
                        type = NotificationType.NEW_RELEASE
                    ))
                }
            } catch (e: Exception) {
                // Ignore errors for initial setup
            }

            _uiState.update { it.copy(notifications = notificationList.toList(), isLoading = false) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Simulate fetching new notifications
            kotlinx.coroutines.delay(1000)
            
            try {
                val trending = musicRepository.getTrendingSongs().first().shuffled().take(1)
                trending.forEach { song ->
                    val newNotif = Notification(
                        id = UUID.randomUUID().toString(),
                        title = "Fresh Hit!",
                        message = "Listen to '${song.title}' by ${song.artist}",
                        time = "Now",
                        type = NotificationType.NEW_RELEASE
                    )
                    if (!notificationList.any { it.title == newNotif.title && it.message == newNotif.message }) {
                        notificationList.add(0, newNotif)
                    }
                }
            } catch (e: Exception) {}

            _uiState.update { it.copy(notifications = notificationList.toList(), isRefreshing = false) }
        }
    }

    fun clearNotification(id: String) {
        notificationList.removeIf { it.id == id }
        _uiState.update { it.copy(notifications = notificationList.toList()) }
    }

    fun clearAll() {
        notificationList.clear()
        _uiState.update { it.copy(notifications = emptyList()) }
    }
}
