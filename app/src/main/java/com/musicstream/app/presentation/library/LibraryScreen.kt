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
import com.musicstream.app.ui.theme.*

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LibraryContent(
        state = state,
        onTabSelect = viewModel::selectTab,
        onPlaylistSelect = viewModel::selectPlaylist,
        onCreatePlaylist = viewModel::createPlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
        onToggleFavorite = viewModel::toggleFavorite,
        onDownloadSong = viewModel::downloadSong,
        onDeleteDownload = viewModel::deleteDownload,
        addSongToPlaylist = viewModel::addSongToPlaylist,
        onSongClick = onSongClick,
        onGoToArtist = onGoToArtist,
        onRefresh = viewModel::refresh
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    state: LibraryUiState,
    onTabSelect: (LibraryTab) -> Unit,
    onPlaylistSelect: (Playlist?) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveSongFromPlaylist: (String, String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onDeleteDownload: (String) -> Unit,
    addSongToPlaylist: (String, String) -> Unit,
    onSongClick: (Song) -> Unit,
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
                    songToRemoveFromPlaylist = Pair(state.selectedPlaylist!!, song)
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
                    val tabName = if (tab == LibraryTab.Songs) "New Songs" else tab.name
                    Surface(
                        modifier = Modifier.height(40.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                        onClick = { onTabSelect(tab) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
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

                        // Downloads Shortcut Section
                        DownloadedShortcutCard(
                            count = state.downloads.size,
                            onClick = { onTabSelect(LibraryTab.Downloads) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloads",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.downloads.isNotEmpty()) {
                                Text(
                                    text = "See all",
                                    color = AccentPurple,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { onTabSelect(LibraryTab.Downloads) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        val allDownloads = (state.downloadingSongsList + state.downloads).distinctBy { it.id }

                        if (allDownloads.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No downloaded songs yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            allDownloads.take(5).forEach { song ->
                                SongListItem(
                                    song = song,
                                    onSongClick = onSongClick,
                                    onFavoriteClick = { onToggleFavorite(song.id) },
                                    onMoreClick = { selectedSongForOptions = it },
                                    downloadProgress = state.downloadingSongs[song.id]
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
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

    // Playlist Detail Overlay
        if (state.selectedPlaylist != null) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                PlaylistDetailView(
                    playlist = state.selectedPlaylist!!,
                    songs = state.playlistSongs,
                    onBackClick = { onPlaylistSelect(null) },
                    onSongClick = onSongClick,
                    onFavoriteClick = { onToggleFavorite(it) },
                    onMoreClick = { selectedSongForOptions = it },
                    downloadingSongs = state.downloadingSongs
                )
            }
        }
    }


@Composable
private fun DownloadedShortcutCard(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPurple),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Downloads",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "$count songs downloaded",
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

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme
                    .colorScheme
                    .onSurface
                    .copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp)
            )
            .background(MaterialTheme
                .colorScheme
                .surfaceVariant
                .copy(alpha = 0.5f))
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
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
                    imageVector = Icons.Filled.MusicNote,
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
                    imageVector = Icons.Filled.Download,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
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
private fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onMoreClick: (Song) -> Unit,
    downloadingSongs: Map<String, Int> = emptyMap()
) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Playlist",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Playlist Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(gradients[playlist.gradientIndex % gradients.size]),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = "${playlist.songCount} songs • Updated today",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs[0]) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = { if (songs.isNotEmpty()) onSongClick(songs.shuffled()[0]) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Songs Header
        Text(
            text = "Songs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // Songs List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp)
        ) {
            if (songs.isEmpty()) {
                EmptyState("No songs in this playlist", "Add some songs to get started")
            } else {
                songs.forEach { song ->
                    SongListItem(
                        song = song,
                        onSongClick = onSongClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = { onMoreClick(song) },
                        downloadProgress = downloadingSongs[song.id]
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song?,
    onDismissRequest: () -> Unit,
    onFavoriteClick: (String) -> Unit,
    onAddToPlaylistClick: (String) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteDownloadClick: (String) -> Unit,
    onRemoveFromPlaylistClick: ((Song) -> Unit)? = null,
    onShareClick: (String) -> Unit,
    onGoToArtistClick: (String) -> Unit
) {
    if (song == null) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Song Info Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = song.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = song.artist,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            OptionItem(
                icon = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                text = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { onFavoriteClick(song.id) }
            )
            OptionItem(
                icon = Icons.Outlined.PlaylistAdd,
                text = "Add to Playlist",
                onClick = { onAddToPlaylistClick(song.id) }
            )
            
            if (song.localPath != null) {
                OptionItem(
                    icon = Icons.Outlined.Delete,
                    text = "Delete Download",
                    iconColor = FavoriteRed,
                    textColor = FavoriteRed,
                    onClick = { onDeleteDownloadClick(song.id) }
                )
            } else {
                OptionItem(
                    icon = Icons.Outlined.Download,
                    text = "Download",
                    onClick = { onDownloadClick(song) }
                )
            }

            if (onRemoveFromPlaylistClick != null) {
                OptionItem(
                    icon = Icons.Outlined.PlaylistRemove,
                    text = "Remove from Playlist",
                    iconColor = FavoriteRed,
                    textColor = FavoriteRed,
                    onClick = { onRemoveFromPlaylistClick(song) }
                )
            }

            OptionItem(
                icon = Icons.Outlined.Share,
                text = "Share",
                onClick = { onShareClick(song.id) }
            )
            OptionItem(
                icon = Icons.Outlined.Person,
                text = "Go to Artist",
                onClick = { onGoToArtistClick(song.artist) }
            )
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

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun PlaylistDetailPreview() {
    MusicStreamTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            PlaylistDetailView(
                playlist = MockData.playlists[0],
                songs = MockData.trendingSongs,
                onBackClick = {},
                onSongClick = {},
                onFavoriteClick = {},
                onMoreClick = {}
            )
        }
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
            onSongClick = {}
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
            onSongClick = {}
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
            onSongClick = {}
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
            onSongClick = {}
        )
    }
}
