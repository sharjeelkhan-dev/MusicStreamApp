package com.musicstream.app.presentation.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.ui.theme.*

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(
        state = state,
        onSongClick = onSongClick,
        onBackClick = onBackClick,
        onFavoriteClick = { viewModel.toggleFavorite(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDownloadSong = { viewModel.downloadSong(it) }
    )
}

@Composable
fun FavoritesContent(
    state: FavoritesUiState,
    onSongClick: (Song) -> Unit = {},
    onBackClick: () -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onCreatePlaylist: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {}
) {
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        cursorColor = AccentPurple,
                        focusedLabelColor = AccentPurple,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreatePlaylist(newPlaylistName)
                            newPlaylistName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create", color = AccentPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            onDismissRequest = {
                selectedSongIdForPlaylist = null
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Liked Songs",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (state.songs.isEmpty() && !state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No liked songs yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 16.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(state.songs) { song ->
                    SongListItem(
                        song = song,
                        onSongClick = onSongClick,
                        onFavoriteClick = onFavoriteClick,
                        onDownloadClick = onDownloadSong,
                        onMoreClick = { selectedSongIdForPlaylist = it.id },
                        downloadProgress = state.downloadingSongs[song.id]
                    )
                }
            }
        }
    }
}
