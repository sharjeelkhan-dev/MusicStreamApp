package com.musicstream.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.musicstream.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    private object PreferencesKeys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val THEME = stringPreferencesKey("theme")
        val NOTIFICATIONS = androidx.datastore.preferences.core.booleanPreferencesKey("notifications")
        val LANGUAGE = stringPreferencesKey("language")
        val EQUALIZER = stringPreferencesKey("equalizer")
        val BASS_BOOST = intPreferencesKey("bass_boost")
        val VIRTUALIZER = intPreferencesKey("virtualizer")
        fun bandKey(band: Int) = intPreferencesKey("eq_band_$band")
    }
    override fun getAudioQuality() = dataStore.data.map { it[PreferencesKeys.AUDIO_QUALITY] ?: "High (320kbps)" }
    override suspend fun setAudioQuality(quality: String) { dataStore.edit { it[PreferencesKeys.AUDIO_QUALITY] = quality } }
    override fun getTheme() = dataStore.data.map { it[PreferencesKeys.THEME] ?: "System Default" }
    override suspend fun setTheme(theme: String) { dataStore.edit { it[PreferencesKeys.THEME] = theme } }
    override fun getNotificationsEnabled() = dataStore.data.map { it[PreferencesKeys.NOTIFICATIONS] ?: true }
    override suspend fun setNotificationsEnabled(enabled: Boolean) { dataStore.edit { it[PreferencesKeys.NOTIFICATIONS] = enabled } }
    override fun getLanguage() = dataStore.data.map { it[PreferencesKeys.LANGUAGE] ?: "English" }
    override suspend fun setLanguage(language: String) { dataStore.edit { it[PreferencesKeys.LANGUAGE] = language } }
    override fun getEqualizerPreset() = dataStore.data.map { it[PreferencesKeys.EQUALIZER] ?: "Flat" }
    override suspend fun setEqualizerPreset(preset: String) { dataStore.edit { it[PreferencesKeys.EQUALIZER] = preset } }
    override fun getBassBoostLevel() = dataStore.data.map { it[PreferencesKeys.BASS_BOOST] ?: 0 }
    override suspend fun setBassBoostLevel(level: Int) { dataStore.edit { it[PreferencesKeys.BASS_BOOST] = level } }
    override fun getVirtualizerLevel() = dataStore.data.map { it[PreferencesKeys.VIRTUALIZER] ?: 0 }
    override suspend fun setVirtualizerLevel(level: Int) { dataStore.edit { it[PreferencesKeys.VIRTUALIZER] = level } }
    override fun getEqualizerBandLevels() = dataStore.data.map { p -> (0 until 10).mapNotNull { i -> p[PreferencesKeys.bandKey(i)]?.let { i to it } }.toMap() }
    override suspend fun setEqualizerBandLevel(band: Int, level: Int) { dataStore.edit { it[PreferencesKeys.bandKey(band)] = level } }
}
