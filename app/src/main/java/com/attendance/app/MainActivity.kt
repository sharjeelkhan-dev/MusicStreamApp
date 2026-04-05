package com.attendance.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.data.preferences.PreferencesManager
import com.attendance.app.presentation.navigation.AppNavigation
import com.attendance.app.presentation.theme.AttendanceTheme
import com.attendance.app.presentation.auth.AuthScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by preferencesManager.darkModeFlow.collectAsStateWithLifecycle(initialValue = false)
            val isBiometricEnabled by preferencesManager.biometricEnabledFlow.collectAsStateWithLifecycle(initialValue = false)
            
            var isAuthenticated by remember { mutableStateOf(false) }

            // Trigger biometric prompt when enabled and not authenticated
            LaunchedEffect(isBiometricEnabled, isAuthenticated) {
                if (isBiometricEnabled && !isAuthenticated) {
                    promptBiometricAuth(
                        onSuccess = { isAuthenticated = true }
                    )
                }
            }

            AttendanceTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isBiometricEnabled && !isAuthenticated) {
                        AuthScreen(
                            onUnlockClick = {
                                promptBiometricAuth(onSuccess = { isAuthenticated = true })
                            }
                        )
                    } else {
                        AppNavigation()
                    }
                }
            }
        }
    }

    private fun promptBiometricAuth(onSuccess: () -> Unit) {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authentication Required")
            .setSubtitle("Log in using your biometric credential")
            .setNegativeButtonText("Cancel")
            .build()
            
        biometricPrompt.authenticate(promptInfo)
    }
}
