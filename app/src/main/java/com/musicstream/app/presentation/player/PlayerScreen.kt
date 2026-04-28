package com.musicstream.app.presentation.player

import android.annotation.SuppressLint
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*

@SuppressLint("QueryPermissionsNeeded")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onGoToAlbum: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showQueue by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    PlayerContent(
        state = state,
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
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
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

@Composable
fun PlayerContent(
    state: PlayerUiState,
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
    onShareClick: () -> Unit,
    onEqualizerClick: () -> Unit,
    onAddToPlaylist: (String, String) -> Unit,
    onSetSleepTimer: (Int) -> Unit,
    onGoToArtist: (String) -> Unit,
    onGoToAlbum: (String) -> Unit
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                LazyColumn {
                    itemsIndexed(state.playlists) { _, playlist ->
                        ListItem(
                            headlineContent = { Text(playlist.name) },
                            modifier = Modifier.clickable {
                                state.currentSong?.id?.let { onAddToPlaylist(playlist.id, it) }
                                showPlaylistDialog = false
                            },
                            leadingContent = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPlaylistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
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
                            headlineContent = { Text(label) },
                            modifier = Modifier.clickable {
                                onSetSleepTimer(minutes)
                                showSleepTimerDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            val onBackgroundColor = MaterialTheme.colorScheme.onBackground

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = onBackgroundColor,
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NOW PLAYING",
                        color = onBackgroundColor.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = state.currentSong?.title ?: "Unknown",
                        color = onBackgroundColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "More",
                            tint = onBackgroundColor,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist") },
                            onClick = { 
                                showMoreMenu = false
                                showPlaylistDialog = true
                            },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Sleep Timer") },
                            onClick = { 
                                showMoreMenu = false
                                showSleepTimerDialog = true
                            },
                            leadingIcon = { Icon(Icons.Default.Timer, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Go to Artist") },
                            onClick = { 
                                showMoreMenu = false
                                state.currentSong?.artist?.let { onGoToArtist(it) }
                            },
                            leadingIcon = { Icon(Icons.Default.Person, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Go to Album") },
                            onClick = { 
                                showMoreMenu = false
                                state.currentSong?.album?.let { onGoToAlbum(it) }
                            },
                            leadingIcon = { Icon(Icons.Default.Album, null) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Album Art Carousel Look
            state.currentSong?.let { song ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Side card (background effect)
                    Surface(
                        modifier = Modifier
                            .size(280.dp)
                            .offset(x = 100.dp)
                            .clip(RoundedCornerShape(32.dp)),
                        color = onBackgroundColor.copy(alpha = 0.05f)
                    ) {}

                    // Main Card
                    Surface(
                        modifier = Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(40.dp)),
                        shadowElevation = if (isSystemInDarkTheme()) 0.dp else 24.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (song.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(Color(0xFFFFD600), Color(0xFFFFAB00))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = onBackgroundColor.copy(alpha = 0.2f),
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Song Title & Artist Left Aligned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val colors = listOf(AccentPink, AccentOrange, AccentBlue, AccentGreen, AccentPurple)
                        val songColor = colors[song.gradientIndex % colors.size]

                        Text(
                            text = song.title,
                            color = onBackgroundColor,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            color = songColor.copy(alpha = 0.8f),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    IconButton(onClick = { onFavoriteClick(song.id) }) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) Color.Red else onBackgroundColor.copy(alpha = 0.3f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Seek Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentSong = state.currentSong
                val songColor = if (currentSong != null) {
                    val colors = listOf(AccentPink, AccentOrange, AccentBlue, AccentGreen, AccentPurple)
                    colors[currentSong.gradientIndex % colors.size]
                } else Color(0xFF7C4DFF)

                // Seek Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Custom track background
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(onBackgroundColor.copy(alpha = 0.1f))
                    )
                    
                    // Custom active track
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(songColor.copy(alpha = 0.7f), songColor)
                                )
                            )
                    )

                    Slider(
                        value = state.progress,
                        onValueChange = { onSeekTo(it) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Transparent,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent
                        )
                    )

                    // Custom Stylish Thumb
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(state.progress)
                            .height(48.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = 10.dp) // Center the thumb on the progress point
                                .size(20.dp, 20.dp)
                                .clip(CircleShape)
                                .background(if (isSystemInDarkTheme()) Color.White else Color.Black)
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(songColor)
                            )
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(state.currentPosition),
                        color = onBackgroundColor.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formatTime(state.duration),
                        color = onBackgroundColor.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val songColor = state.currentSong?.let { 
                    val colors = listOf(AccentPink, AccentOrange, AccentBlue, AccentGreen, AccentPurple)
                    colors[it.gradientIndex % colors.size]
                } ?: Color(0xFF7C4DFF)

                IconButton(onClick = { onToggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleOn) songColor else onBackgroundColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { onPreviousSong() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = onBackgroundColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Play Button with Glow
                Box(contentAlignment = Alignment.Center) {
                    val currentSongColor = state.currentSong?.let { 
                        val colors = listOf(AccentPink, AccentOrange, AccentBlue, AccentGreen, AccentPurple)
                        colors[it.gradientIndex % colors.size]
                    } ?: Color(0xFFFFD600)
                    
                    Surface(
                        modifier = Modifier.size(86.dp),
                        shape = CircleShape,
                        color = currentSongColor.copy(alpha = 0.15f)
                    ) {}
                    
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = CircleShape,
                        color = Color.Transparent,
                        onClick = { onTogglePlayPause() }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(currentSongColor.copy(alpha = 0.9f), currentSongColor)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (state.isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = { onNextSong() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = onBackgroundColor,
                        modifier = Modifier.size(44.dp)
                    )
                }

                IconButton(onClick = { onToggleRepeat() }) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) songColor else onBackgroundColor.copy(alpha = 0.4f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-18).dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed 1x
                Surface(
                    color = onBackgroundColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable {
                        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                        val curIdx = speeds.indexOf(state.playbackSpeed)
                        val nextIdx = (curIdx + 1) % speeds.size
                        onPlaybackSpeedChange(speeds[nextIdx])
                    }
                ) {
                    Text(
                        text = "${state.playbackSpeed}x",
                        color = onBackgroundColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onEqualizerClick() }) {
                    Icon(Icons.Filled.Equalizer, null, tint = onBackgroundColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("EQ", color = onBackgroundColor.copy(alpha = 0.5f), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onQueueClick() }) {
                    Icon(Icons.AutoMirrored.Filled.List, null, tint = onBackgroundColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Queue", color = onBackgroundColor.copy(alpha = 0.5f), fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onShareClick() }) {
                    Icon(Icons.Outlined.Share, null, tint = onBackgroundColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = onBackgroundColor.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom Visualizer Bars
            VisualizerBars(
                isPlaying = state.isPlaying,
                songColor = state.currentSong?.let { 
                    val colors = listOf(AccentPink, AccentOrange, AccentBlue, AccentGreen, AccentPurple)
                    colors[it.gradientIndex % colors.size]
                } ?: Color(0xFFFFD600)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun VisualizerBars(isPlaying: Boolean, songColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = 15.dp)
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val barCount = 30
        val infiniteTransition = rememberInfiniteTransition(label = "visualizer")

        repeat(barCount) { _ ->
            // Create a unique animation for each bar
            val duration = remember { (400..800).random() }
            val baseHeight = remember { (10..20).random().toFloat() }
            val targetHeight = remember { (25..40).random().toFloat() }

            val height by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = baseHeight,
                    targetValue = targetHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                        repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                    ),
                    label = "barHeight"
                )
            } else {
                remember { mutableFloatStateOf(baseHeight) }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(songColor)
            )
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
                    title = "Golden Hour",
                    artist = "Sunset Boys",
                    streamUrl = "",
                    coverUrl = "",
                    duration = 264000
                ),
                isPlaying = true,
                progress = 0.3f,
                currentPosition = 80000,
                duration = 264000,
                isShuffleOn = true,
                repeatMode = RepeatMode.ALL
            ),
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
            onGoToAlbum = {}
        )
    }
}
