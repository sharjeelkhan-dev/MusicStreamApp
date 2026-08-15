package com.musicstream.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.musicstream.app.data.local.dao.FavoriteDao
import com.musicstream.app.data.local.dao.PlaylistDao
import com.musicstream.app.domain.model.User
import com.musicstream.app.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) : UserRepository {
    private object UserKeys {
        val ID = stringPreferencesKey("user_id")
        val NAME = stringPreferencesKey("user_name")
        val EMAIL = stringPreferencesKey("user_email")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val REGISTERED_EMAILS = stringSetPreferencesKey("registered_emails")
        
        fun avatarKey(email: String) = stringPreferencesKey("avatar_$email")
        fun bannerKey(email: String) = stringPreferencesKey("banner_$email")
    }

    override fun getCurrentUser(): Flow<User> = dataStore.data.map { p ->
        val isLoggedIn = p[UserKeys.IS_LOGGED_IN] ?: false
        if (!isLoggedIn) {
            return@map User(
                id = "guest",
                name = "Guest User",
                email = "",
                avatarUrl = "",
                bannerUrl = ""
            )
        }
        val email = p[UserKeys.EMAIL] ?: ""
        User(
            id = p[UserKeys.ID] ?: UUID.randomUUID().toString(),
            name = p[UserKeys.NAME] ?: "User",
            email = email,
            avatarUrl = p[UserKeys.avatarKey(email)] ?: "",
            bannerUrl = p[UserKeys.bannerKey(email)] ?: ""
        )
    }

    override fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[UserKeys.IS_LOGGED_IN] ?: false }

    override suspend fun isEmailRegistered(email: String): Boolean = 
        (dataStore.data.first()[UserKeys.REGISTERED_EMAILS] ?: emptySet()).contains(email)

    override suspend fun updateUser(user: User) {
        val localAvatar = if (user.avatarUrl.startsWith("content://") || user.avatarUrl.startsWith("file://")) {
            saveImageLocally(user.avatarUrl, "avatar_${user.id}.jpg")
        } else {
            user.avatarUrl
        }

        val localBanner = if (user.bannerUrl.startsWith("content://") || user.bannerUrl.startsWith("file://")) {
            saveImageLocally(user.bannerUrl, "banner_${user.id}.jpg")
        } else {
            user.bannerUrl
        }

        dataStore.edit { p ->
            p[UserKeys.ID] = user.id
            p[UserKeys.NAME] = user.name
            p[UserKeys.EMAIL] = user.email
            p[UserKeys.IS_LOGGED_IN] = true
            p[UserKeys.REGISTERED_EMAILS] = (p[UserKeys.REGISTERED_EMAILS] ?: emptySet()) + user.email
            
            // Account specific storage
            p[UserKeys.avatarKey(user.email)] = localAvatar
            p[UserKeys.bannerKey(user.email)] = localBanner
        }
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            favoriteDao.deleteAllFavorites()
            playlistDao.deleteAllPlaylists()
            dataStore.edit { p ->
                p[UserKeys.IS_LOGGED_IN] = false
                p.remove(UserKeys.ID)
                p.remove(UserKeys.NAME)
                p.remove(UserKeys.EMAIL)
            }
        }
    }

    private suspend fun saveImageLocally(uriString: String, fileName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val uri = android.net.Uri.parse(uriString)
                val inputStream = context.contentResolver.openInputStream(uri)
                val file = java.io.File(context.filesDir, fileName)
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                file.absolutePath
            } catch (e: Exception) {
                uriString
            }
        }
    }
}
