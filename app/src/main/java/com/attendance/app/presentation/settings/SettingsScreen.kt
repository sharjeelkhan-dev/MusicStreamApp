package com.attendance.app.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.attendance.app.R
import com.attendance.app.presentation.theme.*

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Permission launcher for notifications (Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.toggleNotifications(true)
        }
    }

    // Show snackbar for backup messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.backupMessage) {
        state.backupMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearBackupMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
    ) { paddingValues ->
        SettingsContent(
            state = state,
            onBack = onBack,
            onToggleDarkMode = viewModel::toggleDarkMode,
            onToggleNotifications = { enabled ->
                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.toggleNotifications(true)
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    viewModel.toggleNotifications(enabled)
                }
            },
            onToggleBiometric = viewModel::toggleBiometric,
            onCreateBackup = viewModel::createBackup,
            onRestoreBackup = viewModel::restoreBackup,
            modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val headerBg = if (isDark) MaterialTheme.colorScheme.surface else PrimaryGreenDark
    val headerContent = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Fixed Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .statusBarsPadding()
                .height(115.dp)
                .padding(top = 16.dp, bottom = 20.dp)
                .padding(horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = headerContent)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = headerContent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage Your Preferences",
                        style = MaterialTheme.typography.labelMedium,
                        color = headerContent.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Appearance
            item {
                SettingsSectionHeader("APPEARANCE")
                SettingsToggleItem(
                    iconPainter = painterResource(id = R.drawable.night_icon),
                    title = "Dark Mode",
                    subtitle = "Switch to dark theme",
                    isChecked = state.isDarkMode,
                    onToggle = onToggleDarkMode
                )
            }

            // Notifications
            item {
                SettingsSectionHeader("NOTIFICATIONS")
                SettingsToggleItem(
                    iconPainter = painterResource(id = R.drawable.alarm_icon),
                    title = "Attendance Reminders",
                    subtitle = "Get daily reminders to take attendance",
                    isChecked = state.isNotificationsEnabled,
                    onToggle = onToggleNotifications
                )
            }

            // Security
            item {
                SettingsSectionHeader("SECURITY")
                SettingsToggleItem(
                    icon = Icons.Default.Fingerprint,
                    title = "Biometric Authentication",
                    subtitle = "Require fingerprint to open app",
                    isChecked = state.isBiometricEnabled,
                    onToggle = onToggleBiometric
                )
            }

            // Data
            item {
                SettingsSectionHeader("DATA")
                SettingsActionItem(
                    iconPainter = painterResource(id = R.drawable.cloud_backup_icon),
                    title = "Create Backup",
                    subtitle = "Export data as backup file",
                    onClick = onCreateBackup
                )
                SettingsActionItem(
                    iconPainter = painterResource(id = R.drawable.reload_sync_icon),
                    title = "Restore Data",
                    subtitle = "Import from backup file",
                    onClick = onRestoreBackup
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val primaryColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else PrimaryGreen
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onPrimary else Color.White,
                    checkedTrackColor = primaryColor
                )
            )
        }
    }
}

@Composable
private fun SettingsActionItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val primaryColor = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.primary else PrimaryGreen
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconPainter != null) {
                Icon(
                    painter = iconPainter,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=0.7f))
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Header Light")
@Composable
fun SettingsHeaderLightPreview() {
    AttendanceTheme(darkTheme = false) {
        val headerBg = PrimaryGreenDark
        val headerContent = Color.White
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .statusBarsPadding()
                .height(115.dp)
                .padding(top = 16.dp, bottom = 20.dp)
                .padding(horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = headerContent)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = headerContent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "manage your preference",
                        style = MaterialTheme.typography.labelMedium,
                        color = headerContent.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Header Dark")
@Composable
fun SettingsHeaderDarkPreview() {
    AttendanceTheme(darkTheme = true) {
        val headerBg = MaterialTheme.colorScheme.surface
        val headerContent = MaterialTheme.colorScheme.onSurface
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .statusBarsPadding()
                .height(115.dp)
                .padding(top = 16.dp, bottom = 20.dp)
                .padding(horizontal = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = headerContent)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        color = headerContent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    )
                    Text(
                        text = "manage your preference",
                        style = MaterialTheme.typography.labelMedium,
                        color = headerContent.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    AttendanceTheme {
        SettingsContent(
            state = SettingsState(
                isDarkMode = true,
                isNotificationsEnabled = true,
                isBiometricEnabled = false
            ),
            onBack = {},
            onToggleDarkMode = {},
            onToggleNotifications = {},
            onToggleBiometric = {},
            onCreateBackup = {},
            onRestoreBackup = {}
        )
    }
}
