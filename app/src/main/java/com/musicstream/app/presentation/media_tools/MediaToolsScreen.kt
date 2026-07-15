package com.musicstream.app.presentation.media_tools
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.musicstream.app.domain.model.Song
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.ui.theme.*

/**
 * Entry point for MediaToolsScreen.
 * Handles ViewModel instantiation and provides a safe fallback for Android Studio Previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaToolsScreen(
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onEqualizerClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        // Safe fallback for Previews to avoid ViewModel instantiation issues
        MediaToolsContent(
            uiState = MediaToolsUiState(),
            snackbarHostState = remember { SnackbarHostState() },
            onVideoToAudio = {},
            onAudioConverter = {},
            onEqualizer = onEqualizerClick,
            onVolumeBooster = {},
            onNoiseReducer = {},
            onTagEditor = {},
            onLyricsFinder = {},
            onBackClick = onBackClick
        )
    } else {
        MediaToolsScreen(
            viewModel = hiltViewModel(),
            onPlaySongs = onPlaySongs,
            onEqualizerClick = onEqualizerClick,
            onBackClick = onBackClick
        )
    }
}

/**
 * Stateful version of MediaToolsScreen that takes a ViewModel.
 * This version handles side effects like file pickers and conversion completion events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaToolsScreen(
    viewModel: MediaToolsViewModel,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onEqualizerClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.convertVideoToAudio(it) }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.boostVolume(it, 1.5f) }
    }

    val noisePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.reduceNoise(it) }
    }

    val tagPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.updateMetadata(it, "My Awesome Track", "Android Music") }
    }

    LaunchedEffect(Unit) {
        viewModel.onConversionComplete.collect { song ->
            onPlaySongs(listOf(song), 0)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.message.collect { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    MediaToolsContent(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onVideoToAudio = { videoPickerLauncher.launch("video/*") },
        onAudioConverter = { audioPickerLauncher.launch("audio/*") },
        onEqualizer = onEqualizerClick,
        onVolumeBooster = { audioPickerLauncher.launch("audio/*") },
        onNoiseReducer = { noisePickerLauncher.launch("audio/*") },
        onTagEditor = { tagPickerLauncher.launch("audio/*") },
        onLyricsFinder = { /* TODO */ },
        onBackClick = onBackClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaToolsContent(
    uiState: MediaToolsUiState,
    snackbarHostState: SnackbarHostState,
    onVideoToAudio: () -> Unit,
    onAudioConverter: () -> Unit,
    onEqualizer: () -> Unit,
    onVolumeBooster: () -> Unit,
    onNoiseReducer: () -> Unit,
    onTagEditor: () -> Unit,
    onLyricsFinder: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            modifier = Modifier.size(48.dp)
                                .offset(x = (-35).dp, y = (-3).dp),
                            shape = CircleShape,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            onClick = onBackClick
                        ) {
                            Box(modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Media Tools",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.offset(x = (-35).dp,y = (-5).dp),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                item {
                    ToolCategory(
                        title = "Conversion Tools",
                        tools = listOf(
                            ToolItem(
                                title = "Video to Audio",
                                description = "Extract high-quality audio from video files (MP4, MKV to MP3, WAV)",
                                iconRes = R.drawable.dvd_disk_icon,
                                color = MaterialTheme.colorScheme.tertiary,
                                onClick = onVideoToAudio
                            ),
                            ToolItem(
                                title = "Audio Converter",
                                description = "Convert between different audio formats (FLAC to MP3, AAC to WAV)",
                                iconRes = R.drawable.service_tools_icon,
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = onAudioConverter
                            )
                        )
                    )
                }

                item {
                    ToolCategory(
                        title = "Audio Enhancement",
                        tools = listOf(
                            ToolItem(
                                title = "Equalizer & FX",
                                description = "Advanced 10-band equalizer with bass boost and 3D reverb",
                                iconRes = R.drawable.filters_icon,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onEqualizer
                            ),
                            ToolItem(
                                title = "Volume Booster",
                                description = "Safely boost volume levels for quiet recordings",
                                iconRes = R.drawable.speaker_icon,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                onClick = onVolumeBooster
                            ),
                            ToolItem(
                                title = "Noise Reducer",
                                description = "Remove background hiss and noise from your audio",
                                iconRes = R.drawable.bell_silent_icon,
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = onNoiseReducer
                            )
                        )
                    )
                }

                item {
                    ToolCategory(
                        title = "Tagging & Organization",
                        tools = listOf(
                            ToolItem(
                                title = "ID3 Tag Editor",
                                description = "Edit song titles, artists, albums, and album artwork",
                                iconRes = R.drawable.folder_edit_icon,
                                color = MaterialTheme.colorScheme.primary,
                                onClick = onTagEditor
                            ),
                            ToolItem(
                                title = "Lyrics Finder",
                                description = "Automatically find and embed lyrics into your audio files",
                                iconRes = R.drawable.music_song_file_icon,
                                color = MaterialTheme.colorScheme.secondary,
                                onClick = onLyricsFinder
                            )
                        )
                    )
                }

                item {
                    Spacer(Modifier.navigationBarsPadding())
                }
            }

            if (uiState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                uiState.statusMessage ?: "Processing...",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCategory(
    title: String,
    tools: List<ToolItem>
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp).offset(y = (-25).dp)
        )
        tools.forEach { tool ->
            ToolCard(tool)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

data class ToolItem(
    val title: String,
    val description: String,
    val iconRes: Int,
    val color: Color,
    val onClick: () -> Unit = {}
)

@Composable
fun ToolCard(tool: ToolItem) {
    Card(
        onClick = { tool.onClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp).offset(y = (-15).dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(tool.color.copy(alpha = 0.8f), tool.color)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = tool.iconRes),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tool.description,
                    modifier = Modifier.offset(y = 3.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 14.sp
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.offset(y = 5.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MediaToolsScreenPreview() {
    MusicStreamTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MediaToolsContent(
                uiState = MediaToolsUiState(),
                snackbarHostState = remember { SnackbarHostState() },
                onVideoToAudio = {},
                onAudioConverter = {},
                onEqualizer = {},
                onVolumeBooster = {},
                onNoiseReducer = {},
                onTagEditor = {},
                onLyricsFinder = {},
                onBackClick = {}
            )
        }
    }
}
