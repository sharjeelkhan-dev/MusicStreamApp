package com.musicstream.app.presentation.library
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PlaylistSelectionBottomSheet
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import com.musicstream.app.ui.theme.Gradients
import com.musicstream.app.ui.theme.MusicStreamTheme
import kotlinx.coroutines.yield

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
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
    onToggleFavorite: (Song) -> Unit,
    onDownloadSong: (Song) -> Unit,
    onDeleteDownload: (String) -> Unit,
    addSongToPlaylist: (String, String) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onGoToArtist: (String) -> Unit = {},
    onRefresh: () -> Unit = {}
) {
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
            shape = RoundedCornerShape(28.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MusicStreamTheme.colors.favoriteActive),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { playlistToDelete = null },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (songToRemoveFromPlaylist != null) {
        AlertDialog(
            onDismissRequest = { songToRemoveFromPlaylist = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
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
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MusicStreamTheme.colors.favoriteActive),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Remove", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { songToRemoveFromPlaylist = null },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
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
            song = selectedSongForOptions,
            onDismissRequest = {
                selectedSongForOptions = null
            },
            onFavoriteClick = { song ->
                onToggleFavorite(song)
                selectedSongForOptions = null
            },
            onAddToPlaylistClick = { songId ->
                selectedSongIdForPlaylist = songId
                selectedSongForOptions = null
            },
            onDownloadClick = { song ->
                onDownloadSong(song)
                selectedSongForOptions = null
            },
            onDeleteDownloadClick = { songId ->
                onDeleteDownload(songId)
                selectedSongForOptions = null
            },
            onRemoveFromPlaylistClick = if (state.selectedPlaylist != null) {
                { song ->
                    songToRemoveFromPlaylist = Pair(state.selectedPlaylist, song)
                    selectedSongForOptions = null
                }
            } else null,
            onShareClick = { _ ->
                val songToShare = selectedSongForOptions
                if (songToShare != null) {
                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT,
                            "Check out this song '${songToShare.title}'" +
                                    " by ${songToShare.artist} on Music Stream!")
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                }
                selectedSongForOptions = null
            },
            onGoToArtistClick = { artistName ->
                onGoToArtist(artistName)
                selectedSongForOptions = null
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 6.dp,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Create New Playlist",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Playlist Name",
                            color = MaterialTheme.colorScheme
                                .onSurfaceVariant.copy(alpha = 0.6f)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Create Playlist", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateDialog = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
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
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            // Title and Subtitle (Premium Style)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Your Library",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Tab Chips (Premium Filter Style)
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().offset(y = (-20).dp),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(LibraryTab.entries.toTypedArray()) { tab ->
                        val isSelected = state.selectedTab == tab
                        val tabName = if (tab == LibraryTab.Songs)
                            "New Songs" else tab.name
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTabSelect(tab) },
                            label = { 
                                Text(
                                    text = tabName,
                                    color = if (isSelected) Color.White else Color.Black,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 20.dp)
                                ) 
                            },
                            shape = RoundedCornerShape(30.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) null else
                                FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = false,
                                borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            // Global Downloads Status (Visible across all tabs)
            val downloadingSongsList = state.downloadingSongsList
            val progressMap = state.downloadingSongs

            if (downloadingSongsList.isNotEmpty()) {
                item {
                    Text(
                        text = "Active Downloads",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }

                items(
                    items = downloadingSongsList,
                    key = { it.id }
                ) { song ->
                    val index = downloadingSongsList.indexOf(song)
                    SongListItem(
                        song = song,
                        onSongClick = { onPlaySongs(downloadingSongsList, index) },
                        onFavoriteClick = { onToggleFavorite(song) },
                        onDownloadClick = { onDownloadSong(it) },
                        onAddClick = { selectedSongForOptions = it },
                        downloadProgress = progressMap[song.id]
                    )
                }

                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                }
            }

            // Content Area based on Selected Tab
            when (state.selectedTab) {
                LibraryTab.Playlists -> {
                    item {
                        CreatePlaylistCard(onClick = { showCreateDialog = true })
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Your Playlists",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-15).dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    items(
                        items = state.playlists,
                        key = { it.id }
                    ) { playlist ->
                        PlaylistListItem(
                            playlist = playlist,
                            onClick = { onPlaylistSelect(playlist) },
                            onDeleteClick = { playlistToDelete = playlist }
                        )
                    }
                }

                LibraryTab.Songs -> {
                    if (state.songs.isEmpty()) {
                        item { EmptyState("No songs found",
                            "Try searching for some music") }
                    } else {
                        itemsIndexed(
                            items = state.songs,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            SongListItem(
                                song = song,
                                modifier = Modifier.offset(y = (-30).dp),
                                onSongClick = { onPlaySongs(state.songs, index) },
                                onFavoriteClick = { onToggleFavorite(song) },
                                onDownloadClick = { onDownloadSong(it) },
                                onAddClick = { selectedSongForOptions = it },
                                downloadProgress = state.downloadingSongs[song.id]
                            )
                        }
                    }
                }

                LibraryTab.Downloads -> {
                    val allDownloads = (state.downloadingSongsList + state.downloads).distinctBy { it.id }
                    if (allDownloads.isEmpty()) {
                        item { EmptyState("No downloads yet", "Downloaded songs will appear here") }
                    } else {
                        item {
                            Text(
                                text = "${allDownloads.size} songs downloaded",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp).offset(y = (-25).dp)
                            )
                        }
                        itemsIndexed(
                            items = allDownloads,
                            key = { _, song -> song.id }
                        ) { index, song ->
                            SongListItem(
                                modifier = Modifier.offset(y = (-20).dp),
                                song = song,
                                onSongClick = { onPlaySongs(allDownloads, index) },
                                onFavoriteClick = { onToggleFavorite(song) },
                                onDownloadClick = { onDownloadSong(it) },
                                onAddClick = { selectedSongForOptions = it },
                                downloadProgress = state.downloadingSongs[song.id]
                            )
                        }
                    }
                }
            }
            
            // Bottom spacer for edge-to-edge
            item {
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                Spacer(Modifier.height(70.dp))
            }
        }
    }
}

@Composable
private fun CreatePlaylistCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-20).dp)
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.plus_line_icon),
                    contentDescription = "Create Playlist",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(25.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Create Playlist",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Add songs that match your mood",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(24.dp)
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
            .padding(horizontal = 24.dp, vertical = 8.dp).offset(y = (-30).dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replace your old Box artwork block with this premium 3D Mini-Stack Artwork
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .padding(end = 6.dp, bottom = 6.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                val currentGradient = gradients[playlist.gradientIndex % gradients.size]
                val density = LocalDensity.current

                for (i in 2 downTo 0) {
                    val offsetX = (i * 4.5f).dp
                    val offsetY = (-i * 3.5f).dp
                    val rotationAngle = (i * 3.5f)
                    val layerAlpha = if (i == 0) 1f else 0.5f

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                translationX = with(density) { offsetX.toPx() }
                                translationY = with(density) { offsetY.toPx() }
                                rotationZ = rotationAngle
                                alpha = layerAlpha
                            }
                            .clip(RoundedCornerShape(14.dp))
                            .background(currentGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        // Content elements inside the front card only
                        if (i == 0) {
                            // Subtle bottom-to-top gradient shadow for layout contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                                        )
                                    )
                            )

                            // Central high-contrast music logo icon
                            Icon(
                                painter = painterResource(id = R.drawable.audio_tune_icon),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playlist.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${playlist.songCount} songs • Updated today",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.recycle_bin_line_icon),
                    contentDescription = "Delete Playlist",
                    tint = FavoriteRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                modifier = Modifier.size(20.dp)
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
                downloads = MockData.trendingSongs,
                downloadingSongsList = listOf(MockData.featuredSong),
                downloadingSongs = mapOf(MockData.featuredSong.id to 65),
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
            onPlaySongs = { _, _ -> },
            onGoToArtist = {},
            onRefresh = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun LibraryScreenCompletedDownloadsPreview() {
    MusicStreamTheme {
        LibraryContent(
            state = LibraryUiState(
                selectedTab = LibraryTab.Downloads,
                downloads = MockData.trendingSongs,
                downloadingSongsList = emptyList(),
                downloadingSongs = emptyMap(),
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
            onPlaySongs = { _, _ -> },
            onGoToArtist = {},
            onRefresh = {}
        )
    }
}

