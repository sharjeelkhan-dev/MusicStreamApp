package com.musicstream.app.data.repository

import com.musicstream.app.data.local.dao.NotificationDao
import com.musicstream.app.data.local.entity.NotificationEntity
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getNotifications(): Flow<List<Notification>> =
        notificationDao.getAllNotifications().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    override suspend fun deleteNotification(id: String) {
        notificationDao.deleteNotification(id)
    }

    override suspend fun clearAll() {
        notificationDao.clearAllNotifications()
    }

    private fun NotificationEntity.toDomain() = Notification(
        id = id,
        title = title,
        message = message,
        time = time,
        type = NotificationType.valueOf(type)
    )

    private fun Notification.toEntity() = NotificationEntity(
        id = id,
        title = title,
        message = message,
        time = time,
        type = type.name,
        timestamp = System.currentTimeMillis()
    )
}
