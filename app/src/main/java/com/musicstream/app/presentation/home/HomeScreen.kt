package com.musicstream.app.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*
import com.musicstream.app.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    playerViewModel: com.musicstream
        .app.presentation
        .player.
    PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTrendingSeeAllClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onGoToPlayer: () -> Unit = {}
) {
    val state by viewModel
        .uiState
        .collectAsStateWithLifecycle()
    val isRefreshing by viewModel
        .isRefreshing
        .collectAsStateWithLifecycle()
    val playerState by playerViewModel
        .uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onPlaySongs = onPlaySongs,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onToolsClick = onToolsClick,
        onTrendingSeeAllClick = onTrendingSeeAllClick,
        onRecentlyPlayedSeeAllClick = onRecentlyPlayedSeeAllClick,
        onPlaylistClick = onPlaylistClick,
        onDownloadsClick = onDownloadsClick,
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDeletePlaylist = { viewModel.deletePlaylist(it) },
        onDeleteAllPlaylists = { viewModel.deleteAllPlaylists() },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId,
            songId) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onDeleteDownload = { viewModel.deleteDownload(it) },
        onGoToArtist = onGoToArtist,
        currentPlayingSong = playerState.currentSong,
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = { playerViewModel.togglePlayPause() },
        onGoToPlayer = onGoToPlayer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    onTrendingSeeAllClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylist: (String) -> Unit = {},
    onDeleteAllPlaylists: () -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {},
    onDeleteDownload: (String) -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {},
    onGoToPlayer: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAllDialog = false
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete All Playlists") },
            text = { Text("Are you sure you want to delete all playlists?" +
                    " This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllPlaylists()
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete '${playlistToDelete?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeletePlaylist(playlistToDelete!!.id)
                        playlistToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text("Cancel")
                }
            }
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
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
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
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
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
            onFavoriteClick = { songId ->
                onToggleFavorite(songId)
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
            onShareClick = { _ ->
                selectedSongForOptions?.let { songToShare ->
                    val sendIntent: android.content.Intent = android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_TEXT,
                            "Check out this song '${songToShare.title}'" +
                                    " by ${songToShare.artist} on Music Stream!")
                        type = "text/plain"
                    }
                    val shareIntent = android.content.Intent
                        .createChooser(sendIntent,
                        null)
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
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme
                .colorScheme.background)
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
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Music Stream",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically)
                {
                    IconButton(
                        onClick = { onNotificationClick() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.notification_alarm_buzzer_icon),
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { onToolsClick() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.service_tools_icon),
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Card(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        onClick = onProfileClick
                    ) {
                        if (state.user?.avatarUrl?.isNotEmpty() == true) {
                            AsyncImage(
                                model = state.user.avatarUrl,
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.silhouette_male_icon),
                                contentDescription = "Profile",
                                tint = Color.LightGray,
                                modifier = Modifier.size(25.dp).offset(y = 7.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.featuredSongs.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    StackedFeaturedCards(
                        songs = state.featuredSongs,
                        currentPlayingSong = currentPlayingSong,
                        isPlaying = isPlaying,
                        onSongClick = { song ->
                            val index = state.featuredSongs.indexOf(song)
                            onPlaySongs(state.featuredSongs,
                                index)
                        },
                        onFavoriteClick = onToggleFavorite,
                        onTogglePlayPause = onTogglePlayPause
                    )
                    
                    // Page Indicator dots
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) { i ->
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if
                                            (i == 1) AccentPurple
                                    else Color.LightGray.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Trending Section
            SectionHeader(
                title = "Trending Now",
                onSeeAllClick = { onTrendingSeeAllClick() }
            )
            
            TrendingRow(
                songs = state.trendingSongs,
                onSongClick = { song ->
                    val index = state.trendingSongs.indexOf(song)
                    onPlaySongs(state.trendingSongs, index)
                },
                onLongClick = { song -> selectedSongForOptions = song },
                downloadingSongs = state.downloadingSongs
            )

            // Bottom spacer for edge-to-edge scrolling behind navigation bars
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(70.dp)) // Extra space for mini player and bottom nav
        }
    }
}

@Preview(showBackground = true)
@Composable
fun YourCollectionsPreview() {
    MusicStreamTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 20.dp)
        ) {
            SectionHeader(
                title = "Your Collections",
                modifier = Modifier.offset(y = (-55).dp),
                emoji = "🎵",
                onSeeAllClick = null
            )
            PlaylistRow(
                playlists = MockData.playlists,
                modifier = Modifier.offset(y = (-55).dp),
                onPlaylistClick = {},
                onDownloadsClick = {},
                onPlaylistLongClick = {},
                downloadCount = 5
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MusicStreamTheme {
        HomeContent(
            state = HomeUiState(
                greeting = "Good Morning 👋",
                featuredSongs = MockData.trendingSongs.take(3),
                trendingSongs = MockData.trendingSongs,
                recentlyPlayed = MockData.recentlyPlayed,
                playlists = MockData.playlists
            ),
            isRefreshing = false,
            onRefresh = {},
            onGoToArtist = {}
        )
    }
}
