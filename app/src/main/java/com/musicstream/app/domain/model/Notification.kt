package com.musicstream.app.domain.model

data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType = NotificationType.GENERAL
)

enum class NotificationType {
    NEW_RELEASE,
    PLAYLIST_UPDATE,
    GENERAL,
    PROMOTION
}
