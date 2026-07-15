package com.musicstream.app.presentation.profile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.User
import com.musicstream.app.ui.theme.*

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LocalContext.current
    
    var showAudioQualityDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    ProfileContent(
        state = state,
        onBackClick = onBackClick,
        onSignOut = { viewModel.signOut() },
        onAudioQualityClick = { showAudioQualityDialog = true },
        onThemeClick = { showThemeDialog = true },
        onNotificationsClick = { 
            viewModel.toggleNotifications()
        },
        onLanguageClick = { showLanguageDialog = true },
        onEqualizerClick = { showEqualizerDialog = true },
        onEditProfileClick = { showEditProfileDialog = true }
    )

    if (showEditProfileDialog && state.user != null) {
        EditProfileDialog(
            user = state.user!!,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, email, avatarUrl, bannerUrl ->
                viewModel.updateProfile(name, email, avatarUrl, bannerUrl)
                showEditProfileDialog = false
            }
        )
    }

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
fun ProfileContent(
    state: ProfileUiState,
    onBackClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onAudioQualityClick: () -> Unit = {},
    onThemeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onEqualizerClick: () -> Unit = {},
    onEditProfileClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .verticalScroll(scrollState)
        ) {
            // Immersive Header with Banner and Profile Info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
            ) {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(Gradients.profileBanner)
                ) {
                    if (state.user?.bannerUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = state.user.bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Overlay for contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.5f))
                                )
                            )
                    )
                }

                // Back Button
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        null,
                        tint = Color.White)
                }

                // Edit Button
                IconButton(
                    onClick = onEditProfileClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.2f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.edit_pen_icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Profile Info Card (Floating)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Card(
                        modifier = Modifier
                            .size(110.dp)
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape),
                        shape = CircleShape,
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.user?.avatarUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = state.user.avatarUrl,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Rounded.Person, 
                                    null, 
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = state.user?.name ?: "Guest User",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = state.user?.email ?: "Join the stream",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(label = "Playlists", value = state.playlistsCount.toString())
                ProfileStat(label = "Favorites", value = state.favoritesCount.toString())
                ProfileStat(label = "Downloads", value = state.downloadsCount.toString())
            }

            Spacer(Modifier.height(32.dp))

            // Settings Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SettingsGroup(title = "MUSIC PREFERENCES") {
                    SettingsItem(
                        ImageVector.vectorResource(id = R.drawable.audio_tune_icon),
                        iconColor = AccentCyan,
                        title = "Audio Quality",
                        value = state.audioQuality,
                        onClick = onAudioQualityClick
                    )
                    SettingsItem(
                        ImageVector.vectorResource
                            (id = R.drawable.music_player_equalizer_round_outline_icon),
                        iconColor = AccentOrange,
                        title = "Equalizer",
                        value = state.equalizer,
                        onClick = onEqualizerClick
                    )
                }

                SettingsGroup(title = "APP SETTINGS") {
                    SettingsItem(
                        ImageVector.vectorResource
                            (id = R.drawable.paint_palette_icon),
                        iconColor = AccentPurple,
                        title = "Appearance",
                        value = state.theme,
                        onClick = onThemeClick
                    )
                    SettingsItem(
                        icon = ImageVector.vectorResource
                            (id = R.drawable.notification_alarm_buzzer_icon),
                        iconColor = AccentAmber,
                        title = "Notifications",
                        value = state.notifications,
                        onClick = onNotificationsClick
                    )
                    SettingsItem(
                        icon = ImageVector.vectorResource(id = R.drawable.language_translator_icon),
                        iconColor = AccentGreen,
                        title = "Language",
                        value = state.language,
                        onClick = onLanguageClick
                    )
                }

                SettingsGroup(title = "ACCOUNT") {
                    SettingsItem(
                        icon = ImageVector.vectorResource(id = R.drawable.logout_icon),
                        iconColor = MusicStreamTheme.colors.signOutButton,
                        title = "Sign Out",
                        value = "",
                        onClick = onSignOut,
                        textColor = MusicStreamTheme.colors.signOutButton
                    )
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(start = 12.dp, bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor =
                MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    onClick: () -> Unit,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var avatarUrl by remember { mutableStateOf(user.avatarUrl) }
    var bannerUrl by remember { mutableStateOf(user.bannerUrl) }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { avatarUrl = it.toString() }
    }
    val bannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { bannerUrl = it.toString() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner and Avatar selectors...
                Box(modifier = Modifier.fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme
                        .colorScheme.surfaceVariant)
                    .clickable
                    { bannerLauncher.launch("image/*") }) {
                    if (bannerUrl.isNotEmpty())
                        AsyncImage(model = bannerUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt,
                            null,
                            tint = Color.White)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme
                        .colorScheme.surfaceVariant).clickable
                    { avatarLauncher.launch("image/*") }) {
                    if (avatarUrl.isNotEmpty())
                        AsyncImage(model = avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop)
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CameraAlt,
                            null,
                            tint = Color.White)
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, email, avatarUrl, bannerUrl) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Save Changes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()) {
                Text("Cancel",
                    color = MaterialTheme
                        .colorScheme.onSurfaceVariant)
            }
        }
    )
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
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(28.dp),
        title = null,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center
                )
                
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected)
                                MaterialTheme.colorScheme
                                    .primary.copy(alpha = 0.1f)
                            else Color.Transparent)
                            .clickable { onOptionSelected(option) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Done",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    )
}

@Preview(showBackground = true)
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

@Preview(name = "Light Mode", showBackground = true)
@Composable
fun SettingsDialogLightPreview() {
    MusicStreamTheme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize()) {
            SettingsDialog(
                title = "Audio Quality",
                options = listOf("Low", "Normal", "High (320kbps)", "Ultra (Hi-Fi)"),
                selectedOption = "Normal",
                onOptionSelected = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Edit Profile Dark", showBackground = true)
@Composable
fun EditProfileDialogDarkPreview() {
    MusicStreamTheme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize()) {
            EditProfileDialog(
                user = MockData.currentUser,
                onDismiss = {},
                onSave = { _, _, _, _ -> }
            )
        }
    }
}
