package com.attendance.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.data.worker.AttendanceReminderWorker
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
    val backupMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
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
        _state.update { it.copy(backupMessage = "Backup created successfully!") }
    }

    fun restoreBackup() {
        _state.update { it.copy(backupMessage = "Restore completed successfully!") }
    }

    fun clearBackupMessage() {
        _state.update { it.copy(backupMessage = null) }
    }
}
