package com.musicstream.app.presentation.search
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Genre
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onGoToArtist: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    SearchContent(
        state = state,
        onPlaySongs = onPlaySongs,
        onRefresh = { viewModel.refresh() },
        onQueryChange = { viewModel.onQueryChange(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onDeleteDownload = { viewModel.deleteDownload(it) },
        onGoToArtist = onGoToArtist,
        onClearHistory = { viewModel.clearHistory() },
        onDeleteHistoryItem = { viewModel.deleteHistoryItem(it) },
        currentPlayingSong = playerState.currentSong,
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = { playerViewModel.togglePlayPause() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    state: SearchUiState,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onRefresh: () -> Unit = {},
    onQueryChange: (String) -> Unit = {},
    onToggleFavorite: (Song) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onDownloadSong: (Song) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    onDeleteHistoryItem: (String) -> Unit = {},
    initialFocused: Boolean = false
) {
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

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
                selectedSongIdForPlaylist = null
                showCreateDialog = true
            },
            onDismissRequest = {
                selectedSongIdForPlaylist = null
            }
        )
    }

    if (selectedSongForOptions != null) {
        val context = LocalContext.current
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
            onGoToArtistClick = { artistName: String ->
                onGoToArtist(artistName)
                selectedSongForOptions = null
            }
        )
    }

    var isSearchBarFocused by remember { mutableStateOf(initialFocused) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchBarFocused) {
        focusManager.clearFocus()
        isSearchBarFocused = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val scrollState = rememberScrollState()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Header (Premium Style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Browse",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar Area with Dropdown logic
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .zIndex(10f)
            ) {
                Column {
                    // Search Bar
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-20).dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.magnifying_glass_icon),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            BasicTextField(
                                value = state.query,
                                onValueChange = { onQueryChange(it) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface // Yeh line text color ko dark/light mode ke mutabiq fix kar degi
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .onFocusChanged { isSearchBarFocused = it.isFocused },
                                decorationBox = { innerTextField ->
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        if (state.query.isEmpty()) {
                                            Text(
                                                text = "Search songs, artists, albums...",
                                                color = MaterialTheme.colorScheme
                                                    .onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 16.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                },
                                singleLine = true
                            )
                            if (state.query.isNotEmpty()) {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close_line_icon),
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Dropdown History Overlay (Visible when focused and query is empty)
                if (isSearchBarFocused && state.query.isBlank() && state.searchHistory.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = 44.dp) // Overlay below the search bar
                            .graphicsLayer {
                                clip = true
                                shape = RoundedCornerShape(24.dp)
                                shadowElevation = 20f
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme
                                .colorScheme.surface.copy(alpha = 0.98f)
                        ),
                        elevation = CardDefaults
                            .cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent searches",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onClearHistory) {
                                    Text(
                                        "Clear All",
                                        fontSize = 12.sp,
                                        modifier = Modifier.offset(x = 8.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            state.searchHistory.take(6).forEach { historyQuery ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .offset(y = (-15).dp)
                                        .clickable { 
                                            onQueryChange(historyQuery)
                                            focusManager.clearFocus()
                                            isSearchBarFocused = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Image or Icon based on query (mocking the rich look)
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val mockImageUrl = when {
                                            historyQuery.contains("Singh", true) || historyQuery.contains("Dosanjh", true) -> "https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=100"
                                            historyQuery.contains("Rock", true) -> "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=100"
                                            historyQuery.contains("Pop", true) -> "https://images.unsplash.com/photo-1493225255756-d9584f8606e9?q=80&w=100"
                                            else -> null
                                        }

                                        if (mockImageUrl != null) {
                                            AsyncImage(
                                                model = mockImageUrl,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            val iconRes = when {
                                                historyQuery.contains("Song", true) || historyQuery.length > 15 -> R.drawable.music_song_file_icon
                                                historyQuery.any { it.isUpperCase() } -> R.drawable.audio_tune_icon
                                                else -> R.drawable.history_line_icon
                                            }
                                            Icon(
                                                painter = painterResource(id = iconRes),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(16.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = historyQuery,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                        val typeLabel = when {
                                            historyQuery.contains("Pop", true) || historyQuery.contains("Rock", true) -> "Genre"
                                            historyQuery.contains("Singh", true) || historyQuery.contains("Dosanjh", true) -> "Artist"
                                            else -> "Song"
                                        }
                                        Text(
                                            text = typeLabel,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteHistoryItem(historyQuery) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.close_line_icon),
                                            contentDescription = "Remove",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.query.isBlank()) {
                // Section: Explore Genres
                Text(
                    text = "EXPLORE GENRES",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 26.dp).offset(y = (-40).dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Genre Grid (Redesigned)
                GenreGrid(
                    genres = state.genres,
                    onGenreClick = { onQueryChange(it) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Section: Trending Searches
                Text(
                    text = "TRENDING SEARCHES",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 26.dp).offset(y = (-50).dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                state.trendingSearches.forEach { search ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-50).dp)
                            .padding(horizontal = 24.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        onClick = { onQueryChange(search) }
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = search,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // Search Results
                if (state.isSearching) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (state.searchResults.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.no_search_result_icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No results found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    state.searchResults.forEachIndexed { index, song ->
                        val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                        key(song.id) {
                            SongListItem(
                                song = song,
                                modifier = Modifier.offset(y = (-35).dp),
                                showThumbnail = true,
                                onSongClick = { 
                                    onPlaySongs(state.searchResults, index) 
                                },
                                onFavoriteClick = { onToggleFavorite(song) },
                                onDownloadClick = { onDownloadSong(it) },
                                onAddClick = { selectedSongForOptions = it },
                                downloadProgress = state.downloadingSongs[song.id],
                                isPlaying = isSongPlaying,
                                onPlayPauseClick = {
                                    if (currentPlayingSong?.id == song.id) {
                                        onTogglePlayPause()
                                    } else {
                                        onPlaySongs(state.searchResults, index)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Bottom spacer for edge-to-edge
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(100.dp))
        }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        genres.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().offset(y = (-40).dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { genre ->
                    val imageUrl = when {
                        genre.name.contains("pop", ignoreCase = true) -> "https://images.unsplash.com/photo-1548778052-311f4bc2b502?q=80&w=687"
                        genre.name.contains("hip-hop", ignoreCase = true) || genre.name.contains("rap", ignoreCase = true) -> "https://plus.unsplash.com/premium_photo-1710107447132-063440229ee4?q=80&w=687"
                        genre.name.contains("punjabi", ignoreCase = true) -> "https://images.unsplash.com/photo-1554772593-cc0206eee02b?q=80&w=687"
                        genre.name.contains("hindi", ignoreCase = true) || genre.name.contains("bollywood", ignoreCase = true) -> "https://images.unsplash.com/photo-1524230507669-5ff97982bb5e?q=80&w=664"
                        genre.name.contains("rock", ignoreCase = true) -> "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?q=80&w=800"
                        genre.name.contains("classical", ignoreCase = true) -> "https://images.unsplash.com/photo-1526142684086-7ebd69df27a5?q=80&w=1170"
                        genre.name.contains("jazz", ignoreCase = true) -> "https://images.unsplash.com/photo-1511192336575-5a79af67a629?q=80&w=800"
                        genre.name.contains("lo-fi", ignoreCase = true) || genre.name.contains("focus", ignoreCase = true) -> "https://images.unsplash.com/photo-1712507123246-476b08ae363f?q=80&w=1175"
                        else -> "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?q=80&w=800"
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        onClick = { onGenreClick(genre.name) }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme
                                    .colorScheme.surfaceVariant)
                        ) {
                            // Genre Image background
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.music_song_file_icon),
                                placeholder = painterResource(id = R.drawable.music_song_file_icon)
                            )

                            // Gradient Overlay (Darker at bottom for text readability)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.2f),
                                                Color.Black.copy(alpha = 0.6f)
                                            )
                                        )
                                    )
                            )

                            // Subtle icon background (Optional, keeping it for style)
                            Icon(
                                painter = painterResource(id = R.drawable.audio_tune_icon),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier
                                    .size(80.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 20.dp, y = 20.dp)
                            )
                            
                            Text(
                                text = genre.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(16.dp).align(Alignment.TopStart)
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

@Preview(showBackground = true, backgroundColor = 0xFFF8F8FC)
@Composable
fun SearchContentPreview() {
    MusicStreamTheme {
        SearchContent(
            state = SearchUiState(
                genres = MockData.genres,
                trendingSearches = MockData.trendingSearches,
                searchHistory = listOf("Arijit Singh", "Rock Music", "Diljit Dosanjh", "Latest Pop Hits"),
                playlists = MockData.playlists
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F8FC)
@Composable
fun SearchContentWithResultsPreview() {
    MusicStreamTheme {
        SearchContent(
            state = SearchUiState(
                query = "song",
                searchResults = MockData.trendingSongs,
                genres = MockData.genres,
                trendingSearches = MockData.trendingSearches,
                playlists = MockData.playlists,
                downloadingSongs = mapOf(MockData.trendingSongs[0].id to 45)
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F8FC, name = "Search History Dropdown")
@Composable
fun SearchHistoryDropdownPreview() {
    MusicStreamTheme {
        SearchContent(
            state = SearchUiState(
                genres = MockData.genres,
                trendingSearches = MockData.trendingSearches,
                searchHistory = listOf("Arijit Singh", "Rock Music", "Diljit Dosanjh", "Latest Pop Hits"),
                playlists = MockData.playlists
            ),
            initialFocused = true
        )
    }
}
