package com.musicstream.app.presentation.library
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.data.MockData
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.*
import com.musicstream.app.R

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onPlaylistClick: (Playlist) -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        state = state,
        onTabSelect = viewModel::selectTab,
        onPlaylistSelect = onPlaylistClick,
        onCreatePlaylist = viewModel::createPlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
        onToggleFavorite = viewModel::toggleFavorite,
        onDownloadSong = viewModel::downloadSong,
        onDeleteDownload = viewModel::deleteDownload,
        addSongToPlaylist = viewModel::addSongToPlaylist,
        onSongClick = onSongClick,
        onPlaySongs = onPlaySongs,
        onGoToArtist = onGoToArtist,
        onRefresh = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onTabSelect: (LibraryTab) -> Unit,
    onPlaylistSelect: (Playlist) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onDeleteDownload: (String) -> Unit,
    addSongToPlaylist: (String, String) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onGoToArtist: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var songToRemoveFromPlaylist by remember { mutableStateOf<Pair<Playlist, Song>?>(null) }
    var newPlaylistName by remember { mutableStateOf("") }
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    tint = FavoriteRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Playlist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${playlistToDelete!!.name}'? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(playlistToDelete!!.id)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FavoriteRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { playlistToDelete = null },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (songToRemoveFromPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToRemoveFromPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.RemoveCircleOutline,
                    contentDescription = null,
                    tint = FavoriteRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Remove Song",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Remove '${songToRemoveFromPlaylist!!.second.title}' from '${songToRemoveFromPlaylist!!.first.name}'?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRemoveSongFromPlaylist(songToRemoveFromPlaylist!!.first.id, songToRemoveFromPlaylist!!.second.id)
                        songToRemoveFromPlaylist = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FavoriteRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { songToRemoveFromPlaylist = null },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            },
            shape = RoundedCornerShape(28.dp)
        )
    }

    if (selectedSongIdForPlaylist != null) {
        PlaylistSelectionBottomSheet(
            playlists = state.playlists,
            onPlaylistSelected = { playlist: Playlist ->
                addSongToPlaylist(playlist.id, selectedSongIdForPlaylist!!)
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
            song = selectedSongForOptions,
            onDismissRequest = { selectedSongForOptions = null },
            onFavoriteClick = { onToggleFavorite(selectedSongForOptions!!.id) },
            onAddToPlaylistClick = { 
                selectedSongIdForPlaylist = selectedSongForOptions!!.id
                selectedSongForOptions = null
            },
            onDownloadClick = { onDownloadSong(selectedSongForOptions!!) },
            onDeleteDownloadClick = {
                onDeleteDownload(it)
                selectedSongForOptions = null
            },
            onRemoveFromPlaylistClick = if (state.selectedPlaylist != null) {
                { song ->
                    songToRemoveFromPlaylist = Pair(state.selectedPlaylist, song)
                    selectedSongForOptions = null
                }
            } else null,
            onShareClick = { /* TODO: Implement Share */ },
            onGoToArtistClick = { onGoToArtist(it) }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    Text("Create", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Title
            Text(
                text = "Your Library",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1).sp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(LibraryTab.entries.toTypedArray()) { tab ->
                    val isSelected = state.selectedTab == tab
                    val tabName = if (tab == LibraryTab.Songs)
                        "New Songs" else tab.name
                    Card(
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) AccentPurple else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
                        onClick = { onTabSelect(tab) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabName,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Content Area
            Column(modifier = Modifier.fillMaxWidth()) {
                // Global Downloads Status (Visible across all tabs)
                val downloadingSongsList = state.downloadingSongsList
                val progressMap = state.downloadingSongs
                
                if (downloadingSongsList.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Active Downloads",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                        
                        downloadingSongsList.forEach { song ->
                            SongListItem(
                                song = song,
                                onSongClick = onSongClick,
                                onFavoriteClick = { onToggleFavorite(song.id) },
                                onDownloadClick = { onDownloadSong(it) },
                                onMoreClick = { selectedSongForOptions = it },
                                downloadProgress = progressMap[song.id]
                            )
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                    }
                }

                when (state.selectedTab) {
                    LibraryTab.Playlists -> {
                        // Create Playlist Card
                        CreatePlaylistCard(onClick = { showCreateDialog = true })

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Your Playlists",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Playlist List
                        state.playlists.forEach { playlist ->
                            PlaylistListItem(
                                playlist = playlist,
                                onClick = { onPlaylistSelect(playlist) },
                                onDeleteClick = { playlistToDelete = playlist }
                            )
                        }
                    }

                    LibraryTab.Songs -> {
                        if (state.songs.isEmpty()) {
                            EmptyState("No songs found", "Try searching for some music")
                        } else {
                            state.songs.forEach { song ->
                                SongListItem(
                                    song = song,
                                    onSongClick = onSongClick,
                                    onFavoriteClick = { onToggleFavorite(song.id) },
                                    onDownloadClick = { onDownloadSong(it) },
                                    onMoreClick = { selectedSongForOptions = it },
                                    downloadProgress = state.downloadingSongs[song.id]
                                )
                            }
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
                                    onFavoriteClick = { onToggleFavorite(song.id) },
                                    onMoreClick = { selectedSongForOptions = it },
                                    downloadProgress = state.downloadingSongs[song.id]
                                )
                            }
                        }
                    }

                    LibraryTab.Downloads -> {
                        val allDownloads = (state.downloadingSongsList + state.downloads).distinctBy { it.id }
                        if (allDownloads.isEmpty()) {
                            EmptyState("No downloads yet", "Downloaded songs will appear here")
                        } else {
                            Text(
                                text = "${allDownloads.size} songs downloaded",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )
                            allDownloads.forEach { song ->
                                SongListItem(
                                    song = song,
                                    onSongClick = onSongClick,
                                    onFavoriteClick = { onToggleFavorite(song.id) },
                                    onMoreClick = { selectedSongForOptions = it },
                                    downloadProgress = state.downloadingSongs[song.id]
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom spacer for edge-to-edge
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(140.dp))
        }
    }
}



@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme
                .colorScheme
                .onSurface
                .copy(alpha = 0.05f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add your favorite songs",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlaylistListItem(
    playlist: Playlist,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradients[playlist.gradientIndex % gradients.size]),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.audio_tune_icon),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Playlist • ${playlist.songCount} songs",
                    color = AccentPurple,
                    fontSize = 13.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                        contentDescription = "Delete Playlist",
                        tint = FavoriteRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
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
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenPlaylistsPreview() {
    MusicStreamTheme {
        LibraryContent(
            state = LibraryUiState(
                selectedTab = LibraryTab.Playlists,
                playlists = MockData.playlists,
                isLoading = false
            ),
            onTabSelect = {},
            onPlaylistSelect = {},
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onRemoveSongFromPlaylist = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {},
            addSongToPlaylist = { _, _ -> },
            onDeleteDownload = {},
            onSongClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenSongsPreview() {
    MusicStreamTheme {
        LibraryContent(
            state = LibraryUiState(
                selectedTab = LibraryTab.Songs,
                songs = MockData.trendingSongs,
                isLoading = false
            ),
            onTabSelect = {},
            onPlaylistSelect = {},
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onRemoveSongFromPlaylist = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {},
            addSongToPlaylist = { _, _ -> },
            onDeleteDownload = {},
            onSongClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenDownloadsPreview() {
    MusicStreamTheme {
        LibraryContent(
            state = LibraryUiState(
                selectedTab = LibraryTab.Downloads,
                downloads = MockData.trendingSongs.take(3),
                downloadingSongs = mapOf(MockData.trendingSongs[3].id to 45),
                isLoading = false
            ),
            onTabSelect = {},
            onPlaylistSelect = {},
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onRemoveSongFromPlaylist = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {},
            addSongToPlaylist = { _, _ -> },
            onDeleteDownload = {},
            onSongClick = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenFavoritesPreview() {
    MusicStreamTheme {
        LibraryContent(
            state = LibraryUiState(
                selectedTab = LibraryTab.Favorites,
                favorites = MockData.trendingSongs.filter { it.isFavorite },
                isLoading = false
            ),
            onTabSelect = {},
            onPlaylistSelect = {},
            onCreatePlaylist = {},
            onDeletePlaylist = {},
            onRemoveSongFromPlaylist = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {},
            addSongToPlaylist = { _, _ -> },
            onDeleteDownload = {},
            onSongClick = {},
            onRefresh = {}
        )
    }
}
