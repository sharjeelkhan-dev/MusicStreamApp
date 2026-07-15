package com.musicstream.app.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.data.MockData
import com.musicstream.app.ui.theme.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    
    PlaylistScreenContent(
        state = state,
        onBackClick = onBackClick,
        onPlaySongs = onPlaySongs,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onAddSongToPlaylist = { pid, sid -> viewModel.addSongToPlaylist(pid, sid) },
        onRemoveSongFromPlaylist = { viewModel.removeSongFromPlaylist(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onDeleteDownload = { viewModel.deleteDownload(it) },
        currentPlayingSong = playerState.currentSong,
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = { playerViewModel.togglePlayPause() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreenContent(
    state: PlaylistUiState,
    onBackClick: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onRemoveSongFromPlaylist: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var songToRemove by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (songToRemove != null) {
        AlertDialog(
            onDismissRequest = { songToRemove = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.RemoveCircleOutline,
                        contentDescription = null,
                        tint = FavoriteRed,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Remove Song",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${songToRemove?.title}' from this playlist?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = { songToRemove = null },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                    Button(
                        onClick = {
                            onRemoveSongFromPlaylist(songToRemove!!.id)
                            songToRemove = null
                        },
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MusicStreamTheme.colors.favoriteActive),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedSongForOptions != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismissRequest = { 
                selectedSongForOptions = null 
            },
            onFavoriteClick = { song: Song ->
                onToggleFavorite(song)
                selectedSongForOptions = null
            },
            onAddToPlaylistClick = { songId: String ->
                selectedSongIdForPlaylist = songId
                selectedSongForOptions = null
            },
            onDownloadClick = { song: Song ->
                onDownloadSong(song)
                selectedSongForOptions = null
            },
            onDeleteDownloadClick = { songId: String ->
                onDeleteDownload(songId)
                selectedSongForOptions = null
            },
            onRemoveFromPlaylistClick = { song: Song ->
                songToRemove = song
                selectedSongForOptions = null
            },
            onShareClick = { _: String ->
                selectedSongForOptions?.let { songToShare ->
                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT, "Check out this song '${songToShare.title}' by ${songToShare.artist} on Music Stream!")
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
                selectedSongForOptions = null
            },
            onGoToArtistClick = { _: String ->
                selectedSongForOptions = null
            }
        )
    }

    if (selectedSongIdForPlaylist != null) {
        PlaylistSelectionBottomSheet(
            playlists = state.playlists,
            onPlaylistSelected = { playlist ->
                onAddSongToPlaylist(playlist.id, selectedSongIdForPlaylist!!)
                selectedSongIdForPlaylist = null
            },
            onCreatePlaylistClick = {
                selectedSongIdForPlaylist = null
                showCreateDialog = true
            },
            onDismissRequest = {
                selectedSongIdForPlaylist = null
            }
        )
    }

    // Determine representative gradient
    val gradientIndex = state.playlist?.gradientIndex ?: 0
    val gradients = listOf(Gradients.playlistBlue,
        Gradients.playlistPink, Gradients.playlistGreen,
        Gradients.trendingPurple, Gradients.trendingPink)
    val mainGradient = gradients[gradientIndex % gradients.size]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 220.dp)
            ) {
                item {
                    // Custom Immersive Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Nav Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Card(
                                modifier = Modifier.size(48.dp),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                onClick = onBackClick
                            ) {
                                Box(modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center)
                                {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        "Back",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Card(
                                onClick = { /* More options */ },
                                modifier = Modifier.size(44.dp),
                                shape = CircleShape,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults
                                    .cardElevation(defaultElevation = 2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        "More",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Large Art with shadow
                        Card(
                            modifier = Modifier.size(220.dp),
                            shape = RoundedCornerShape(32.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(mainGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.audio_tune_icon),
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(90.dp)
                                )

                                // Glass overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.White.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.2f))
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        val displayTitle = if (state.playlist?.id?.startsWith("new_") == true) "Create playlist" else (state.playlist?.name ?: "Playlist")
                        Text(
                            text = displayTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.5).sp
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 10.dp)
                        ) {
                            Text(
                                text = "${state.songs.size} tracks",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Updated recently",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        
                        Spacer(Modifier.height(28.dp))
                    }
                }

                itemsIndexed(
                    items = state.songs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                    SongListItem(
                        song = song,
                        modifier = Modifier.offset(y = (-25).dp),
                        onSongClick = { onPlaySongs(state.songs, index) },
                        onFavoriteClick = { onToggleFavorite(song) },
                        onDownloadClick = { onDownloadSong(it) },
                        onAddClick = { selectedSongForOptions = it },
                        downloadProgress = state.downloadingSongs[song.id],
                        isPlaying = isSongPlaying,
                        onPlayPauseClick = {
                            if (currentPlayingSong?.id == song.id) {
                                onTogglePlayPause()
                            } else {
                                onPlaySongs(state.songs, index)
                            }
                        }
                    )
                }

                if (state.songs.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.empty_folder_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = "Your playlist is currently empty",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 48.dp)
                            )
                        }
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .offset(y = 25.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent,
                                MaterialTheme.colorScheme
                                    .background.copy(alpha = 0.95f)),
                            startY = 0f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(bottom = if (currentPlayingSong != null) 100.dp else 24.dp)
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Button(
                    onClick = { if (state.songs.isNotEmpty())
                        onPlaySongs(state.songs, 0) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (state.playlist?.id?.startsWith("new_") == true) "Add Songs" else "Play",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    MusicStreamTheme {
        PlaylistScreenContent(
            state = PlaylistUiState(
                playlist = MockData.playlists[0],
                songs = MockData.trendingSongs,
                isLoading = false
            ),
            onBackClick = {},
            onPlaySongs = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {}
        )
    }
}
