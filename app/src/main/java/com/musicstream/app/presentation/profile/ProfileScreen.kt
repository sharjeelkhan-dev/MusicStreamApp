package com.musicstream.app.presentation.profile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.ui.theme.*

import com.musicstream.app.data.MockData

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }

    ProfileContent(
        state = state,
        onSignOut = { viewModel.signOut() },
        onAudioQualityClick = { showAudioQualityDialog = true },
        onThemeClick = { showThemeDialog = true },
        onNotificationsClick = { viewModel.toggleNotifications() },
        onLanguageClick = { showLanguageDialog = true },
        onEqualizerClick = { showEqualizerDialog = true }
    )

    if (showAudioQualityDialog) {
        SettingsDialog(
            title = "Audio Quality",
            options = listOf("Low", "Normal", "High (320kbps)", "Ultra (Hi-Fi)"),
            selectedOption = state.audioQuality,
            onOptionSelected = {
                viewModel.setAudioQuality(it)
                showAudioQualityDialog = false
            },
            onDismiss = { showAudioQualityDialog = false }
        )
    }

    if (showThemeDialog) {
        SettingsDialog(
            title = "Theme",
            options = listOf("Dark Mode", "Light Mode", "System Default"),
            selectedOption = state.theme,
            onOptionSelected = {
                viewModel.setTheme(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showLanguageDialog) {
        SettingsDialog(
            title = "Language",
            options = listOf("English", "Spanish", "French", "German", "Hindi"),
            selectedOption = state.language,
            onOptionSelected = {
                viewModel.setLanguage(it)
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showEqualizerDialog) {
        SettingsDialog(
            title = "Equalizer",
            options = listOf("Flat", "Bass Boost", "Electronic", "Rock", "Pop", "Custom"),
            selectedOption = state.equalizer,
            onOptionSelected = {
                viewModel.setEqualizerPreset(it)
                showEqualizerDialog = false
            },
            onDismiss = { showEqualizerDialog = false }
        )
    }
}

@Composable
fun SettingsDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    onSignOut: () -> Unit = {},
    onAudioQualityClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
    ) {
        // Gradient Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF8B5CF6),
                            Color(0xFFD946EF),
                            Color(0xFFFF8C00)
                        )
                    )
                )
        )

        // Avatar and Edit Profile
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-40).dp)
                .padding(horizontal = 20.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = "Avatar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Edit Profile button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // User Info
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-20).dp)
                .padding(horizontal = 20.dp)
        ) {
            state.user?.let { user ->
                Text(
                    text = user.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.email,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    if (user.isPremium) {
                        Text(
                            text = "✨ Premium",
                            color = PremiumGold,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    StatItem(value = user.songCount.toString(), label = "Songs", modifier = Modifier.weight(1f))
                    StatItem(value = user.playlistCount.toString(), label = "Playlists", modifier = Modifier.weight(1f))
                    StatItem(value = user.followingCount.toString(), label = "Following", modifier = Modifier.weight(1f))
                    StatItem(value = user.followersCount.toString(), label = "Followers", modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // SETTINGS label
        Text(
            text = "SETTINGS",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Settings List
        SettingsItem(
            icon = Icons.Rounded.MusicNote,
            iconColor = AccentPurple,
            title = "Audio Quality",
            value = state.audioQuality,
            onClick = onAudioQualityClick
        )
        SettingsItem(
            icon = Icons.Rounded.NightsStay,
            iconColor = AccentAmber,
            title = "Theme",
            value = state.theme,
            onClick = onThemeClick
        )
        SettingsItem(
            icon = Icons.Rounded.Notifications,
            iconColor = AccentOrange,
            title = "Notifications",
            value = state.notifications,
            onClick = onNotificationsClick
        )
        SettingsItem(
            icon = Icons.Rounded.Language,
            iconColor = AccentGreen,
            title = "Language",
            value = state.language,
            onClick = onLanguageClick
        )
        SettingsItem(
            icon = Icons.Rounded.BarChart,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = "Equalizer",
            value = state.equalizer,
            onClick = onEqualizerClick
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Sign Out Button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            shape = CircleShape,
            border = BorderStroke(1.dp,
                MaterialTheme
                    .colorScheme
                    .error
                    .copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Sign Out",
                fontSize = 15.sp,
                color = Color.Red,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick() 
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun ProfileScreenPreview() {
    MusicStreamTheme {
        ProfileContent(
            state = ProfileUiState(
                user = MockData.currentUser,
                isLoading = false
            )
        )
    }
}
