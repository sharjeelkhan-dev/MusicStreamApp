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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
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
                    .background(DarkCardSurface)
                    .border(3.dp, DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Avatar",
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Edit Profile button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(y = 48.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, DarkElevated, RoundedCornerShape(20.dp))
                    .background(DarkCardSurface)
                    .clickable { }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Edit Profile",
                    color = TextPrimary,
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
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.email,
                        color = TextSecondary,
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
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Settings List
        SettingsItem(
            icon = Icons.Filled.MusicNote,
            iconColor = AccentPurple,
            title = "Audio Quality",
            value = state.audioQuality
        )
        SettingsItem(
            icon = Icons.Filled.DarkMode,
            iconColor = AccentAmber,
            title = "Theme",
            value = state.theme
        )
        SettingsItem(
            icon = Icons.Filled.Notifications,
            iconColor = AccentOrange,
            title = "Notifications",
            value = state.notifications
        )
        SettingsItem(
            icon = Icons.Filled.Language,
            iconColor = AccentGreen,
            title = "Language",
            value = state.language
        )
        SettingsItem(
            icon = Icons.Filled.Diamond,
            iconColor = AccentCyan,
            title = "Plan",
            value = state.plan
        )
        SettingsItem(
            icon = Icons.Filled.Devices,
            iconColor = AccentBlue,
            title = "Devices",
            value = state.devices
        )
        SettingsItem(
            icon = Icons.Filled.Equalizer,
            iconColor = TextSecondary,
            title = "Equalizer",
            value = state.equalizer
        )
        SettingsItem(
            icon = Icons.Filled.Lock,
            iconColor = AccentAmber,
            title = "Privacy",
            value = state.privacy
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign Out Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, SignOutBorder, RoundedCornerShape(28.dp))
                .background(Color.Transparent)
                .clickable { viewModel.signOut() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sign Out",
                color = SignOutRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
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
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = TextSecondary,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
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
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = TextSecondary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(18.dp)
        )
    }
}
