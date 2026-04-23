package com.musicstream.app.presentation.search
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        state = state,
        onSongClick = onSongClick,
        onRefresh = { viewModel.refresh() },
        onQueryChange = { viewModel.onQueryChange(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onGoToArtist = onGoToArtist
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onSongClick: (Song) -> Unit = {},
    onRefresh: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onDownloadSong: (Song) -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()
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
            onDownloadClick = { onDownloadSong(selectedSongForOptions!!) },
            onShareClick = { /* TODO: Implement Share */ },
            onGoToArtistClick = { onGoToArtist(selectedSongForOptions!!.artist) }
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.background,
                color = AccentPurple
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Browse",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = state.query,
                        onValueChange = { onQueryChange(it) },
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(AccentPurple),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (state.query.isEmpty()) {
                                    Text(
                                        text = "What do you want to listen to?",
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (state.query.isBlank()) {
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
                GenreGrid(
                    genres = state.genres,
                    onGenreClick = { onQueryChange(it) }
                )

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
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                color = MaterialTheme.colorScheme.onBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Search Results
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        )
                    }
                } else {
                    state.searchResults.forEach { song ->
                        SongListItem(
                            song = song,
                            onSongClick = onSongClick,
                            onFavoriteClick = { onToggleFavorite(it) },
                            onDownloadClick = { onDownloadSong(it) },
                            onMoreClick = { selectedSongForOptions = it },
                            downloadProgress = state.downloadingSongs[song.id]
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            Spacer(modifier = Modifier.height(80.dp))
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
private fun GenreGrid(
    genres: List<Genre>,
    onGenreClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        genres.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { genre ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp),
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onGenreClick(genre.name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Gradients.getGenreGradient(genre.gradientKey))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = genre.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.TopStart)
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
