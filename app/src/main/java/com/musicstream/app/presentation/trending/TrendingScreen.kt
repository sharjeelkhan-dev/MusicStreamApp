package com.musicstream.app.presentation.trending
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.domain.model.Song
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.data.MockData
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendingScreen(
    viewModel: TrendingViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    TrendingContent(
        state = state,
        onRefresh = { viewModel.refresh() },
        onPlaySongs = onPlaySongs,
        onBackClick = onBackClick,
        onFavoriteClick = { viewModel.toggleFavorite(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
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
fun TrendingContent(
    state: TrendingUiState,
    onRefresh: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    onFavoriteClick: (Song) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCreateDialog = false 
                selectedSongIdForPlaylist = null
            },
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
                            selectedSongIdForPlaylist = null
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showCreateDialog = false
                    selectedSongIdForPlaylist = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (selectedSongIdForPlaylist != null) {
        PlaylistSelectionBottomSheet(
            playlists = state.playlists,
            onPlaylistSelected = { playlist: Playlist ->
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

    if (selectedSongForOptions != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        SongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismissRequest = { 
                selectedSongForOptions = null 
            },
            onFavoriteClick = { song: Song ->
                onFavoriteClick(song)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 200.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
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
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(20.dp))

                        Text(
                            text = "Trending Songs",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                itemsIndexed(state.songs) { index, song ->
                    val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                    SongListItem(
                        song = song,
                        showThumbnail = true,
                        modifier = Modifier.offset(y = (-10).dp),
                        onSongClick = { onPlaySongs(state.songs, index) },
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

                if (state.songs.isEmpty() && !state.isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No trending songs available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
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
                        listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                        startY = 0f))
                .navigationBarsPadding()
                .padding(bottom = if (currentPlayingSong != null) 100.dp else 24.dp)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Button(
                onClick = { if (state.songs.isNotEmpty()) onPlaySongs(state.songs, 0) },
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Play All Trending",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun TrendingScreenPreview() {
    MusicStreamTheme {
        TrendingContent(
            state = TrendingUiState(
                songs = MockData.trendingSongs,
                playlists = MockData.playlists
            ),
            onRefresh = {},
            onBackClick = {}
        )
    }
}
