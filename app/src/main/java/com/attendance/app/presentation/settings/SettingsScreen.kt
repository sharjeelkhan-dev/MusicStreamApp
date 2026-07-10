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
            onSetAttendanceDate = viewModel::setAttendanceDate,
            onSetAiApiKey = viewModel::setAiApiKey,
            onSignOut = viewModel::signOut,
            modifier = Modifier
                .padding(bottom = paddingValues
                    .calculateBottomPadding())
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    state: SettingsState,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onSetAttendanceDate: (String?) -> Unit,
    onSetAiApiKey: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Custom Header Layout (Fixed at top)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryGreenDark)
                .statusBarsPadding()
                .height(70.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Text Content
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 65.dp, top = 12.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontSize = 25.sp, // Kept original size
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "Manage Your Preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp, // Kept original size
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth().fillMaxSize().weight(1f)) {
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
                    SettingsSectionHeader("AI CONFIGURATION")
                    SettingsAiKeyItem(
                        apiKey = state.aiApiKey,
                        onKeyChange = onSetAiApiKey
                    )
                    
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
                    
                    var showDatePicker by remember { mutableStateOf(false) }
                    if (showDatePicker) {
                        val datePickerState = rememberDatePickerState(
                            initialSelectedDateMillis = System.currentTimeMillis()
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            shape = RoundedCornerShape(16.dp),
                            colors = DatePickerDefaults.colors(
                                containerColor = Color.White,
                            ),
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                            val date = java.time.Instant.ofEpochMilli(it)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toLocalDate()
                                            onSetAttendanceDate(date.toString())
                                        }
                                        showDatePicker = false
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryGreen)
                                ) {
                                    Text("OK", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showDatePicker = false },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                                ) {
                                    Text("CANCEL", fontWeight = FontWeight.Bold)
                                }
                            }
                        ) {
                            val selectedDate = datePickerState.selectedDateMillis?.let {
                                java.time.Instant.ofEpochMilli(it)
                                    .atZone(java.time.ZoneId.of("UTC"))
                                    .toLocalDate()
                            } ?: java.time.LocalDate.now()

                            // Custom Green Header to match the classic Android look from the image
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PrimaryGreen)
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = selectedDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedDate.dayOfMonth.toString(),
                                    color = Color.White,
                                    fontSize = 80.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 80.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = selectedDate.month.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = selectedDate.year.toString(),
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            DatePicker(
                                state = datePickerState,
                                showModeToggle = false,
                                title = null,
                                headline = null,
                                colors = DatePickerDefaults.colors(
                                    containerColor = Color.White,
                                    selectedDayContainerColor = PrimaryGreen,
                                    selectedDayContentColor = Color.White,
                                    todayContentColor = PrimaryGreen,
                                    todayDateBorderColor = PrimaryGreen,
                                    weekdayContentColor = Color.Gray,
                                    navigationContentColor = PrimaryGreen,
                                    yearContentColor = Color.Black,
                                    currentYearContentColor = PrimaryGreen,
                                    selectedYearContainerColor = PrimaryGreen,
                                    selectedYearContentColor = Color.White
                                )
                            )
                        }
                    }

                    SettingsActionItem(
                        icon = Icons.Default.CalendarToday,
                        title = "Attendance Date",
                        subtitle = if (!state.attendanceDate.isNullOrEmpty()) "Custom Date: ${state.attendanceDate}" else "Set custom date for marking",
                        onClick = { showDatePicker = true },
                        trailingIcon = if (!state.attendanceDate.isNullOrEmpty()) Icons.Default.Close else null,
                        onTrailingIconClick = { onSetAttendanceDate(null) }
                    )

                    SettingsActionItem(
                        iconPainter = painterResource(id = R.drawable.cloud_backup_icon),
                        title = "Cloud Backup",
                        subtitle = "Upload your data to Firebase Cloud",
                        onClick = onCreateBackup
                    )
                    SettingsActionItem(
                        iconPainter = painterResource(id = R.drawable.reload_sync_icon),
                        title = "Sync from Cloud",
                        subtitle = "Download data from your account",
                        onClick = onRestoreBackup
                    )
                }

                // Account
                item {
                    SettingsSectionHeader("ACCOUNT")
                    SettingsActionItem(
                        iconPainter = painterResource(id = R.drawable.logout_icon),
                        title = "Sign Out",
                        subtitle = state.userEmail ?: "Sign out of your account",
                        onClick = onSignOut
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsAiKeyItem(
    apiKey: String,
    onKeyChange: (String) -> Unit
) {
    val isDark = LocalIsDarkMode.current
    val primaryColor = if (isDark) MaterialTheme.colorScheme.primary else PrimaryGreen
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Gemini API Key", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Use your own key for Gemini 2.0 (optional)", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = onKeyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter API Key (AIza...)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    cursorColor = primaryColor
                )
            )
            if (apiKey.isEmpty()) {
                Text(
                    text = "* Defaulting to Firebase Managed AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
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
                containerColor = MaterialTheme.colorScheme.surface,
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
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
                containerColor = MaterialTheme.colorScheme.surface,
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
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (trailingIcon != null && onTrailingIconClick != null) {
                    IconButton(onClick = onTrailingIconClick) {
                        Icon(
                            imageVector = trailingIcon,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Settings Preview")
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
            onRestoreBackup = {},
            onSetAttendanceDate = {},
            onSetAiApiKey = {},
            onSignOut = {}
        )
    }
}
