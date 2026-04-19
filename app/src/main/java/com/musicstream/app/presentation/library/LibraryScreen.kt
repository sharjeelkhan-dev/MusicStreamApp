package com.musicstream.app.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.ui.theme.*

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    if (selectedSongIdForPlaylist != null) {
        PlaylistSelectionBottomSheet(
            playlists = state.playlists,
            onPlaylistSelected = { playlist: Playlist ->
                viewModel.addSongToPlaylist(playlist.id, selectedSongIdForPlaylist!!)
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

    if (selectedSongForOptions != null) {
        com.musicstream.app.presentation.components.SongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismissRequest = { selectedSongForOptions = null },
            onFavoriteClick = { viewModel.toggleFavorite(selectedSongForOptions!!.id) },
            onAddToPlaylistClick = { selectedSongIdForPlaylist = selectedSongForOptions!!.id },
            onDownloadClick = { /* TODO: Implement Download */ },
            onShareClick = { /* TODO: Implement Share */ },
            onGoToArtistClick = { /* TODO: Navigate to Artist */ }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = DarkCardSurface,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        cursorColor = AccentPurple,
                        focusedLabelColor = AccentPurple,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
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
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Your Library",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Tab Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LibraryTab.entries.forEach { tab ->
                val isSelected = state.selectedTab == tab
                Surface(
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isSelected) AccentPurple else DarkCardSurface,
                    onClick = { viewModel.selectTab(tab) }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.name,
                            color = if (isSelected) Color.White else TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            when (state.selectedTab) {
                LibraryTab.Playlists -> {
                    // Create Playlist Card
                    CreatePlaylistCard(onClick = { showCreateDialog = true })

                    Spacer(modifier = Modifier.height(12.dp))

                    // Playlist List
                    state.playlists.forEach { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { viewModel.togglePlaylist(playlist.id) }
                        )
                    }
                }

                LibraryTab.Favorites -> {
                    if (state.favorites.isEmpty()) {
                        EmptyState("No favorites yet", "Songs you love will appear here")
                    } else {
                        state.favorites.forEach { song ->
                            SongListItem(
                                song = song,
                                onSongClick = onSongClick,
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onMoreClick = { selectedSongForOptions = it }
                            )
                        }
                    }
                }

                LibraryTab.Downloads -> {
                    if (state.downloads.isEmpty()) {
                        EmptyState("No downloads yet", "Downloaded songs will appear here")
                    } else {
                        state.downloads.forEach { song ->
                            SongListItem(
                                song = song,
                                onSongClick = onSongClick,
                                onFavoriteClick = { viewModel.toggleFavorite(it) },
                                onMoreClick = { selectedSongForOptions = it }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = DarkElevated,
                shape = RoundedCornerShape(16.dp)
            )
            .background(DarkCardSurface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPurple.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Create Playlist",
                tint = AccentPurple,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Create Playlist",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Add your favorite songs",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PlaylistListItem(playlist: Playlist, onClick: () -> Unit) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(DarkCardSurface)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(gradients[playlist.gradientIndex % gradients.size]),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Playlist • ${playlist.songCount} songs",
                    color = Color(0xFF7B7BFF),
                    fontSize = 13.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = TextTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = TextTertiary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.MusicNote,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenPreview() {
    MusicStreamTheme {
        LibraryScreen(
            onSongClick = {}
        )
    }
}
