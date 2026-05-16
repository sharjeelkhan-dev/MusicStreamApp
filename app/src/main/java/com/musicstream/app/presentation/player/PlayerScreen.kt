package com.musicstream.app.presentation.player

import android.annotation.SuppressLint
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.MusicStreamTheme

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    homeViewModel: com.musicstream.app.presentation.home.HomeViewModel = hiltViewModel(), // Add HomeViewModel
    songColor: Color,
    onBackClick: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onGoToAlbum: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val homeState by homeViewModel.uiState.collectAsStateWithLifecycle() // Get Home state
    val context = LocalContext.current
    var showQueue by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) } // Add state for History
    val sheetState = rememberModalBottomSheetState()
    
    PlayerContent(
        state = state,
        songColor = songColor,
        onBackClick = onBackClick,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onNextSong = { viewModel.nextSong() },
        onPreviousSong = { viewModel.previousSong() },
        onSeekTo = { viewModel.seekTo(it) },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onToggleRepeat = { viewModel.toggleRepeat() },
        onPlaybackSpeedChange = { viewModel.setPlaybackSpeed(it) },
        onFavoriteClick = { songId -> viewModel.toggleFavorite(songId) },
        onQueueClick = { showQueue = true },
        onHistoryClick = { showHistory = true }, // Pass callback
        onShareClick = {
            state.currentSong?.let { song ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Listening to ${song.title} by ${song.artist} on Music Stream!")
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        },
        onEqualizerClick = {
            val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                putExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
                putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        },
        onAddToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onSetSleepTimer = { viewModel.setSleepTimer(it) },
        onGoToArtist = { artistName ->
            onBackClick()
            onGoToArtist(artistName)
        },
        onGoToAlbum = { albumId ->
            onBackClick()
            onGoToAlbum(albumId)
        }
    )

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { 
                if (showQueue) showQueue = false 
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            QueueList(
                queue = state.queue,
                currentIndex = state.currentIndex,
                onSongClick = { song ->
                    viewModel.playSong(song)
                    if (showQueue) showQueue = false
                }
            )
        }
    }

    if (showHistory) {
        ModalBottomSheet(
            onDismissRequest = { showHistory = false },
            sheetState = rememberModalBottomSheetState(),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            HistoryList(
                songs = homeState.recentlyPlayed,
                onSongClick = { song ->
                    viewModel.playSong(song)
                    showHistory = false
                }
            )
        }
    }
}

@Composable
fun HistoryList(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Recently Played",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(songs) { _, song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueList(
    queue: List<Song>,
    currentIndex: Int,
    onSongClick: (Song) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Up Next",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp)
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(queue) { index, song ->
                val isPlaying = index == currentIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSongClick(song) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (song.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                            color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Playing",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerContent(
    state: PlayerUiState,
    songColor: Color,
    onBackClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextSong: () -> Unit,
    onPreviousSong: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onQueueClick: () -> Unit,
    onHistoryClick: () -> Unit, // Add this
    onShareClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit
) {
    // Silence unused parameter warnings
    SideEffect {
        onToggleShuffle
        onToggleRepeat
        onPlaybackSpeedChange
        onFavoriteClick
        onQueueClick
        onShareClick
        onEqualizerClick
        onGoToAlbum
    }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Add to Playlist", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                LazyColumn {
                    itemsIndexed(state.playlists) { _, playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable {
                                state.currentSong?.id?.let { onAddToPlaylist(playlist.id, it) }
                            },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null, tint = MaterialTheme.colorScheme.onSurface) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Sleep Timer", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    val timers = listOf(
                        "Off" to 0,
                        "5 minutes" to 5,
                        "15 minutes" to 15,
                        "30 minutes" to 30,
                        "45 minutes" to 45,
                        "1 hour" to 60
                    )
                    timers.forEach { (label, minutes) ->
                        ListItem(
                            headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
                            modifier = Modifier.clickable {
                                onSetSleepTimer(minutes)
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Full screen blurred background image
        state.currentSong?.let { song ->
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .blur(60.dp)
            )
            // Gradient overlay to fade bottom into background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, backgroundColor),
                            startY = 500f
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.05f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box {
                    Row {
                        IconButton(
                            onClick = onHistoryClick,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.05f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "More",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        containerColor = Color.White
                    ) {
                        val menuItemColors = MenuDefaults.itemColors(
                            textColor = Color.Black,
                            leadingIconColor = Color.Black
                        )
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { 
                                showMoreMenu = false
                                showPlaylistDialog = true
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                            colors = menuItemColors
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer") },
                            onClick = { 
                                showMoreMenu = false
                                showSleepTimerDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Timer, null) },
                            colors = menuItemColors
                        )
                        DropdownMenuItem(
                            text = { Text("Go to Artist") },
                            onClick = { 
                                showMoreMenu = false
                                state.currentSong?.artist?.let { onGoToArtist(it) }
                            },
                            leadingIcon = { Icon(Icons.Default.Person, null) },
                            colors = menuItemColors
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Main Album Art - Removed to match image, background is the image
            state.currentSong?.let { song ->
                // Song Info Aligned Centered like in image
                Text(
                    text = song.title,
                    color = onBackgroundColor,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = onBackgroundColor.copy(alpha = 0.6f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
                
                Text(
                    text = "A limitless soundscape driving your vibe beyond every horizon. 🎧",
                    color = onBackgroundColor.copy(alpha = 0.5f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // Waveform Seek Area & Controls - White Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatTime(state.currentPosition),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "-" + formatTime(state.duration - state.currentPosition),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Waveform visualizer with seeking capability
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        VisualizerBars(
                            isPlaying = state.isPlaying,
                            songColor = songColor,
                            progress = state.progress,
                            onSeek = onSeekTo
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onPreviousSong,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(onBackgroundColor.copy(alpha = 0.04f))
                        ) {
                            Icon(painter = painterResource(id = R.drawable.reset_update_icon),
                                null,
                                tint = onBackgroundColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp))
                        }

                        // Play Button
                        Surface(
                            modifier = Modifier.size(72.dp),
                            shape = CircleShape,
                            color = songColor,
                            onClick = onTogglePlayPause,
                            shadowElevation = 6.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNextSong,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(onBackgroundColor.copy(alpha = 0.06f))
                        ) {
                            Icon(painter = painterResource(id = R.drawable.forward_restore_icon),
                                null,
                                tint = onBackgroundColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun VisualizerBars(
    isPlaying: Boolean,
    songColor: Color,
    progress: Float,
    onSeek: (Float) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val barCount = 45
            repeat(barCount) { index ->
                val infiniteTransition = rememberInfiniteTransition(label = "bars")
                val heightScale by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = (400..800).random(), easing = FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "barHeight"
                )
                
                // Generate a pseudo-random base height
                val baseHeight = when(index % 10) {
                    0 -> 15f
                    1 -> 25f
                    2 -> 40f
                    3 -> 55f
                    4 -> 45f
                    5 -> 30f
                    else -> 20f
                }
                
                val finalHeight = if (isPlaying) (baseHeight * heightScale).coerceAtLeast(10f) else baseHeight * 0.5f
                val isPlayed = (index.toFloat() / barCount) <= progress
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(finalHeight.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isPlayed) songColor else Color.Black.copy(alpha = 0.1f))
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    MusicStreamTheme {
        PlayerContent(
            state = PlayerUiState(
                currentSong = Song(
                    id = "1",
                    title = "Endless Journey",
                    artist = "Echo Coast",
                    streamUrl = "",
                    coverUrl = "",
                    duration = 352000
                ),
                isPlaying = true,
                progress = 0.6f,
                currentPosition = 352000 * 6 / 10,
                duration = 352000
            ),
            songColor = Color(0xFF9162FF),
            onBackClick = {},
            onTogglePlayPause = {},
            onNextSong = {},
            onPreviousSong = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onPlaybackSpeedChange = {},
            onFavoriteClick = {},
            onQueueClick = {},
            onShareClick = {},
            onEqualizerClick = {},
            onAddToPlaylist = { _, _ -> },
            onSetSleepTimer = {},
            onGoToArtist = {},
            onGoToAlbum = {},
            onHistoryClick = {}
        )
    }
}
