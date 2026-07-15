package com.musicstream.app.presentation.library

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.PremiumHeader
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.*

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    DownloadsContent(
        state = state,
        onPlaySongs = onPlaySongs,
        onBackClick = onBackClick,
        onFavoriteClick = { viewModel.toggleFavorite(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onDeleteDownload = { viewModel.deleteDownload(it) },
        onRefresh = viewModel::refresh,
        currentPlayingSong = playerState.currentSong,
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = { playerViewModel.togglePlayPause() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    state: DownloadsUiState,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    onFavoriteClick: (Song) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    // 1. Recomposition Safe State (Stable IDs)
    var selectedSongIdForPlaylist by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedSongIdForOptions by rememberSaveable { mutableStateOf<String?>(null) }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    // Map ID back to object only when needed for the BottomSheet
    val allSongs = remember(state.songs, state.downloadingSongsList) {
        state.downloadingSongsList + state.songs
    }
    val selectedSongForOptions = remember(selectedSongIdForOptions, allSongs) {
        allSongs.find { it.id == selectedSongIdForOptions }
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
            Column(modifier = Modifier.fillMaxSize()) {
                PremiumHeader(
                    title = "Downloads",
                    onBackClick = onBackClick,
                    modifier = Modifier.statusBarsPadding()
                )

                if (state.songs.isEmpty() && state.downloadingSongsList.isEmpty() && !state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Active Downloads Section
                        if (state.downloadingSongsList.isNotEmpty()) {
                            item(key = "active_downloads_header") {
                                Text(
                                    text = "Active Downloads",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                                )
                            }
                            // 2. Stable Keys for Items (Using song.id)
                            itemsIndexed(
                                items = state.downloadingSongsList,
                                key = { _, song -> "dl_${song.id}" }
                            ) { index, song ->
                                val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                                SongListItem(
                                    song = song,
                                    onSongClick = { onPlaySongs(allSongs, index) },
                                    onDownloadClick = { onDownloadSong(it) },
                                    onAddClick = {
                                        Log.d("DownloadsScreen", "Click on Add for song: ${song.title}")
                                        selectedSongIdForOptions = it.id
                                    },
                                    downloadProgress = state.downloadingSongs[song.id],
                                    isPlaying = isSongPlaying,
                                    onPlayPauseClick = {
                                        if (currentPlayingSong?.id == song.id) onTogglePlayPause()
                                        else onPlaySongs(allSongs, index)
                                    }
                                )
                            }
                        }

                        // Completed Downloads Section
                        itemsIndexed(
                            items = state.songs,
                            key = { _, song -> "comp_${song.id}" }
                        ) { index, song ->
                            val actualIndex = state.downloadingSongsList.size + index
                            val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                            SongListItem(
                                song = song,
                                onSongClick = { onPlaySongs(allSongs, actualIndex) },
                                onDownloadClick = { onDownloadSong(it) },
                                onAddClick = { 
                                    Log.d("DownloadsScreen", "Click on Add for song: ${song.title}")
                                    selectedSongIdForOptions = it.id
                                },
                                downloadProgress = state.downloadingSongs[song.id],
                                isPlaying = isSongPlaying,
                                onPlayPauseClick = {
                                    if (currentPlayingSong?.id == song.id) onTogglePlayPause()
                                    else onPlaySongs(allSongs, actualIndex)
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 3. Overlays (Outside scrolling container for stability) ---

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showCreateDialog = false 
                    selectedSongIdForPlaylist = null
                },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("New Playlist", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp)
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
                    }) { Text("Cancel") }
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
                onDismissRequest = { selectedSongIdForPlaylist = null }
            )
        }

        if (selectedSongForOptions != null) {
            val context = LocalContext.current
            SongOptionsBottomSheet(
                song = selectedSongForOptions,
                onDismissRequest = {
                    selectedSongIdForOptions = null
                },
                onFavoriteClick = { song ->
                    onFavoriteClick(song)
                    selectedSongIdForOptions = null
                },
                onAddToPlaylistClick = { songId ->
                    selectedSongIdForPlaylist = songId
                    selectedSongIdForOptions = null
                },
                onDownloadClick = { song ->
                    onDownloadSong(song)
                    selectedSongIdForOptions = null
                },
                onDeleteDownloadClick = { songId ->
                    onDeleteDownload(songId)
                    selectedSongIdForOptions = null
                },
                onShareClick = { _ ->
                    selectedSongForOptions.let { songToShare ->
                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Check out this song '${songToShare.title}' by ${songToShare.artist} on Music Stream!")
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                        context.startActivity(shareIntent)
                    }
                    selectedSongIdForOptions = null
                },
                onGoToArtistClick = { _ ->
                    selectedSongIdForOptions = null
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DownloadsScreenPreview() {
    MusicStreamTheme {
        DownloadsContent(
            state = DownloadsUiState(
                songs = MockData.trendingSongs.take(5),
                playlists = MockData.playlists
            )
        )
    }
}
