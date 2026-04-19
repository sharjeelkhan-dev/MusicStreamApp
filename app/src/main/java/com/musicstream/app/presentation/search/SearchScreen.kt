package com.musicstream.app.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.data.MockData
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        state = state,
        onSongClick = onSongClick,
        onQueryChange = { viewModel.onQueryChange(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) }
    )
}

@Composable
fun SearchContent(
    state: SearchUiState,
    onSongClick: (Song) -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> }
) {
    val scrollState = rememberScrollState()
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

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
                    colors = OutlinedTextFieldDefaults.colors(
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
                    Text("Cancel", color = TextSecondary)
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
                showCreateDialog = true
            },
            onDismissRequest = {
                selectedSongIdForPlaylist = null
            }
        )
    }

    if (selectedSongForOptions != null) {
        SongOptionsBottomSheet(
            song = selectedSongForOptions!!,
            onDismissRequest = { selectedSongForOptions = null },
            onFavoriteClick = { onToggleFavorite(selectedSongForOptions!!.id) },
            onAddToPlaylistClick = { selectedSongIdForPlaylist = selectedSongForOptions!!.id },
            onDownloadClick = { /* TODO: Implement Download */ },
            onShareClick = { /* TODO: Implement Share */ },
            onGoToArtistClick = { /* TODO: Navigate to Artist */ }
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
            text = "Browse",
            color = TextPrimary,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-1).sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Search Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = DarkCardSurface
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { onQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Songs, artists, or genres",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = AccentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = AccentPurple,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (state.query.isBlank()) {
            // Browse content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Genres Label
                Text(
                    text = "EXPLORE GENRES",
                    color = AccentPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )

                // Genre Grid
                GenreGrid(genres = state.genres)

                Spacer(modifier = Modifier.height(24.dp))

                // Trending Searches
                Text(
                    text = "TRENDING SEARCHES",
                    color = AccentPurple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )

                state.trendingSearches.forEach { search ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        color = Color.Transparent,
                        onClick = { onQueryChange(search) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(DarkCardSurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = search,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
        } else {
            // Search Results
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                if (state.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = AccentOrange)
                    }
                } else if (state.searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No results found",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    state.searchResults.forEach { song ->
                        SongListItem(
                            song = song,
                            onSongClick = onSongClick,
                            onFavoriteClick = { onToggleFavorite(it) },
                            onMoreClick = { selectedSongForOptions = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun SearchScreenPreview() {
    MusicStreamTheme {
        SearchContent(
            state = SearchUiState(
                genres = MockData.genres,
                trendingSearches = MockData.trendingSearches,
                playlists = MockData.playlists
            ),
            onSongClick = {}
        )
    }
}

@Composable
private fun GenreGrid(genres: List<Genre>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        genres.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { genre ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(90.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = DarkCardSurface,
                        onClick = { }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Gradients.getGenreGradient(genre.gradientKey))
                        ) {
                            Text(
                                text = genre.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            )
                        }
                    }
                }
                if (row.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

