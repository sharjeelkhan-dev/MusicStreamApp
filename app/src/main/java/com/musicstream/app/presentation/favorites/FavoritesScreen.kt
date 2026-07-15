package com.musicstream.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(
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
fun FavoritesContent(
    state: FavoritesUiState,
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
                })
                { Text("Cancel") }
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
                showCreateDialog = true
            },
            onDismissRequest = { selectedSongIdForPlaylist = null }
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

            // ❌ DONO KO NULL KIYA TAAMI FAVORITES VIEW MEIN HIDE HO JAYEIN
            onDeleteDownloadClick = null,
            onRemoveFromPlaylistClick = null,

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

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumHeader(
                title = "Liked Songs",
                onBackClick = onBackClick,
                modifier = Modifier.statusBarsPadding()
            )

            if (state.songs.isEmpty() && !state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No liked songs yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    itemsIndexed(
                        items = state.songs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                        SongListItem(
                            song = song,
                            onSongClick = { onPlaySongs(state.songs, index) },
                            onFavoriteClick = { onFavoriteClick(song) },
                            onDownloadClick = { onDownloadSong(it) },
                            onAddClick = { selectedSongForOptions = it },
                            downloadProgress = state.downloadingSongs[song.id],
                            isPlaying = isSongPlaying,
                            onPlayPauseClick = {
                                if (currentPlayingSong?.id == song.id) onTogglePlayPause()
                                else onPlaySongs(state.songs, index)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FavoritesScreenPreview() {
    MusicStreamTheme {
        FavoritesContent(
            state = FavoritesUiState(
                songs = MockData.recentlyPlayed,
                playlists = MockData.playlists
            )
        )
    }
}