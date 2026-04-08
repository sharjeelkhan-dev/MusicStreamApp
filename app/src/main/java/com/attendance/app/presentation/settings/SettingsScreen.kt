package com.attendance.app.presentation.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.attendance.app.presentation.theme.PrimaryGreen
import com.attendance.app.presentation.theme.PrimaryGreenDark
import com.attendance.app.presentation.theme.AttendanceTheme
import com.attendance.app.presentation.theme.LocalIsDarkMode
import com.attendance.app.R
import com.attendance.app.presentation.components.StandardHeader
import com.attendance.app.presentation.components.VerticalScrollbar

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
    val isDark = LocalIsDarkMode.current
    val listState = rememberLazyListState()

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Custom Settings Header
            SettingsHeader(
                title = "Settings",
                subtitle = "Manage Your Preferences",
                onBack = onBack
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    state = listState,
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

                VerticalScrollbar(
                    lazyListState = listState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    val backgroundColor = PrimaryGreenDark
    val contentColor = Color.White
    val secondaryContentColor = contentColor.copy(alpha = 0.85f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp,
                    top = 16.dp).offset(y = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryContentColor,
            modifier = Modifier
                .padding(start = 60.dp)
                .offset(y = (8).dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsToggleItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
    
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = Color.Gray.copy(alpha = 0.2f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            onClick = { onToggle(!isChecked) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (iconPainter != null) {
                    Icon(
                        painter = iconPainter,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = isChecked,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = primaryColor,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = if (isDark) Color(0xFF424242) else Color(0xFFEEEEEE),
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsActionItem(
    icon: ImageVector? = null,
    iconPainter: androidx.compose.ui.graphics.painter.Painter? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = Color.Gray.copy(alpha = 0.2f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
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
                    Text(
                        text = title, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = Color.Gray
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Header Light")
@Composable
fun SettingsHeaderLightPreview() {
    AttendanceTheme(darkTheme = false) {
        SettingsHeader(
            title = "Settings",
            subtitle = "Manage Your Preferences",
            onBack = {}
        )
    }
}

@Preview(showBackground = true, name = "Settings Header Dark")
@Composable
fun SettingsHeaderDarkPreview() {
    AttendanceTheme(darkTheme = true) {
        SettingsHeader(
            title = "Settings",
            subtitle = "Manage Your Preferences",
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    AttendanceTheme(darkTheme = false) {
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
