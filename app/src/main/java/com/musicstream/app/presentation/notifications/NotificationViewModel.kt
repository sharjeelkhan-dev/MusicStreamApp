package com.musicstream.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.domain.repository.MusicRepository
import com.musicstream.app.domain.repository.NotificationRepository
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
    private val notificationRepository: NotificationRepository,
    private val musicRepository: MusicRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState(isLoading = true))
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            notificationRepository.getNotifications().collect { notifications ->
                _uiState.update { it.copy(notifications = notifications, isLoading = false) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Try to fetch a trending song to announce as a "New Release"
            try {
                musicRepository.getTrendingSongs().first().shuffled().firstOrNull()?.let { song ->
                    notificationRepository.addNotification(
                        Notification(
                            id = UUID.randomUUID().toString(),
                            title = "Fresh Hit: ${song.title}",
                            message = "New track by ${song.artist} is now trending!",
                            time = "Just now",
                            type = NotificationType.NEW_RELEASE
                        )
                    )
                }
            } catch (e: Exception) {}

            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun clearNotification(id: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            notificationRepository.clearAll()
        }
    }
}
