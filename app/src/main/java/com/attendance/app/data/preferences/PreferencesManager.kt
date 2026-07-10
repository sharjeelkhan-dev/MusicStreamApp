package com.attendance.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "attendance_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        val SELECTED_CLASS_ID_KEY = longPreferencesKey("selected_class_id")
        val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
        val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
        val ATTENDANCE_DATE_KEY = stringPreferencesKey("attendance_date")
        val AI_API_KEY = stringPreferencesKey("ai_api_key")
    }

    val darkModeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[DARK_MODE_KEY] ?: false
    }.distinctUntilChanged()

    val selectedClassIdFlow: Flow<Long> = dataStore.data.map { prefs ->
        prefs[SELECTED_CLASS_ID_KEY] ?: -1L
    }.distinctUntilChanged()

    val notificationsEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[NOTIFICATIONS_ENABLED_KEY] ?: false
    }.distinctUntilChanged()

    val biometricEnabledFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED_KEY] ?: false
    }.distinctUntilChanged()

    val attendanceDateFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[ATTENDANCE_DATE_KEY]
    }.distinctUntilChanged()

    val aiApiKeyFlow: Flow<String?> = dataStore.data.map { prefs ->
        prefs[AI_API_KEY]
    }.distinctUntilChanged()

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[DARK_MODE_KEY] = enabled }
    }

    suspend fun setSelectedClassId(classId: Long) {
        dataStore.edit { prefs -> prefs[SELECTED_CLASS_ID_KEY] = classId }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED_KEY] = enabled }
    }

    suspend fun setAttendanceDate(date: String?) {
        dataStore.edit { prefs ->
            if (date.isNullOrEmpty()) {
                prefs.remove(ATTENDANCE_DATE_KEY)
            } else {
                prefs[ATTENDANCE_DATE_KEY] = date
            }
        }
    }

    suspend fun setAiApiKey(apiKey: String?) {
        dataStore.edit { prefs ->
            if (apiKey.isNullOrBlank()) {
                prefs.remove(AI_API_KEY)
            } else {
                prefs[AI_API_KEY] = apiKey.trim()
            }
        }
    }
}