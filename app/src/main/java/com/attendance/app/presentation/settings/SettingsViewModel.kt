package com.attendance.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.data.worker.AttendanceReminderWorker
import com.attendance.app.domain.repository.AuthRepository
import com.attendance.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

data class SettingsState(
    val isDarkMode: Boolean = false,
    val isNotificationsEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val attendanceDate: String? = null,
    val aiApiKey: String = "",
    val backupMessage: String? = null,
    val userEmail: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        _state.update { it.copy(userEmail = authRepository.currentUserEmail) }
        viewModelScope.launch {
            preferencesManager.darkModeFlow.collect { dark ->
                _state.update { it.copy(isDarkMode = dark) }
            }
        }
        viewModelScope.launch {
            preferencesManager.notificationsEnabledFlow.collect { enabled ->
                _state.update { it.copy(isNotificationsEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.biometricEnabledFlow.collect { enabled ->
                _state.update { it.copy(isBiometricEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            preferencesManager.attendanceDateFlow.collect { date ->
                _state.update { it.copy(attendanceDate = date) }
            }
        }
        viewModelScope.launch {
            preferencesManager.aiApiKeyFlow.collect { key ->
                _state.update { it.copy(aiApiKey = key ?: "") }
            }
        }
    }

    fun setAiApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.setAiApiKey(key)
        }
    }

    fun setAttendanceDate(date: String?) {
        viewModelScope.launch {
            preferencesManager.setAttendanceDate(date)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setDarkMode(enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNotificationsEnabled(enabled)
            if (enabled) {
                scheduleReminder()
            } else {
                cancelReminder()
            }
        }
    }

    private fun scheduleReminder() {
        val workRequest = PeriodicWorkRequestBuilder<AttendanceReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(Constraints.Builder().build())
            .addTag("attendance_reminder")
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "attendance_reminder_work",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    private fun cancelReminder() {
        WorkManager.getInstance(context).cancelUniqueWork("attendance_reminder_work")
    }

    fun toggleBiometric(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setBiometricEnabled(enabled) }
    }

    fun createBackup() {
        viewModelScope.launch {
            _state.update { it.copy(backupMessage = "Uploading to Cloud...") }
            syncRepository.uploadDataToCloud().fold(
                onSuccess = { _state.update { it.copy(backupMessage = "Cloud Backup successful!") } },
                onFailure = { e -> _state.update { it.copy(backupMessage = "Cloud Backup failed: ${e.message}") } }
            )
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            _state.update { it.copy(backupMessage = "Downloading from Cloud...") }
            syncRepository.downloadDataFromCloud().fold(
                onSuccess = { _state.update { it.copy(backupMessage = "Cloud Restore successful!") } },
                onFailure = { e -> _state.update { it.copy(backupMessage = "Cloud Restore failed: ${e.message}") } }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearBackupMessage() {
        _state.update { it.copy(backupMessage = null) }
    }
}
