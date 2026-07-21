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
        val AVATAR = stringPreferencesKey("user_avatar")
        val BANNER = stringPreferencesKey("user_banner")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val REGISTERED_EMAILS = stringSetPreferencesKey("registered_emails")
    }
    override fun getCurrentUser(): Flow<User> = dataStore.data.map { p -> User(p[UserKeys.ID] ?: UUID.randomUUID().toString(), p[UserKeys.NAME] ?: "Guest User", p[UserKeys.EMAIL] ?: "", p[UserKeys.AVATAR] ?: "", p[UserKeys.BANNER] ?: "") }
    override fun isLoggedIn(): Flow<Boolean> = dataStore.data.map { it[UserKeys.IS_LOGGED_IN] ?: false }
    override suspend fun isEmailRegistered(email: String): Boolean = (dataStore.data.first()[UserKeys.REGISTERED_EMAILS] ?: emptySet()).contains(email)
    override suspend fun updateUser(user: User) { dataStore.edit { p -> p[UserKeys.ID] = user.id; p[UserKeys.NAME] = user.name; p[UserKeys.EMAIL] = user.email; p[UserKeys.IS_LOGGED_IN] = true; p[UserKeys.REGISTERED_EMAILS] = (p[UserKeys.REGISTERED_EMAILS] ?: emptySet()) + user.email } }
    override suspend fun signOut() { withContext(Dispatchers.IO) { favoriteDao.deleteAllFavorites(); playlistDao.deleteAllPlaylists(); dataStore.edit { it[UserKeys.IS_LOGGED_IN] = false } } }
}
