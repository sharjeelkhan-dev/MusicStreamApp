package com.musicstream.app.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
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
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onTrendingSeeAllClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onPlaylistClick: (Playlist) -> Unit = {},
    onDownloadsClick: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {},
    onGoToPlayer: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onPlaySongs = onPlaySongs,
        onNotificationClick = onNotificationClick,
        onProfileClick = onProfileClick,
        onTrendingSeeAllClick = onTrendingSeeAllClick,
        onRecentlyPlayedSeeAllClick = onRecentlyPlayedSeeAllClick,
        onPlaylistClick = onPlaylistClick,
        onDownloadsClick = onDownloadsClick,
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDeletePlaylist = { viewModel.deletePlaylist(it) },
        onDeleteAllPlaylists = { viewModel.deleteAllPlaylists() },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onDownloadSong = { viewModel.downloadSong(it) },
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
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete All Playlists") },
            text = { Text("Are you sure you want to delete all playlists? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllPlaylists()
                    }
                ) {
                    Text("Delete All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { },
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
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { },
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
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
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
            },
            onDismissRequest = {
            }
        )
    }

    if (selectedSongForOptions != null) {
        SongOptionsBottomSheet(
            song = selectedSongForOptions,
            onDismissRequest = { },
            onFavoriteClick = { onToggleFavorite(it) },
            onAddToPlaylistClick = { 
                selectedSongIdForPlaylist = it
            },
            onDownloadClick = { onDownloadSong(it) },
            onShareClick = { /* Shared component already handles basic intent or could pass custom one */ },
            onGoToArtistClick = { onGoToArtist(it) }
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
            // Greeting + Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = state.greeting,
                        color = MaterialTheme
                            .colorScheme
                            .onBackground
                            .copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Discover",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme
                                .colorScheme
                                .onBackground
                                .copy(alpha = 0.05f)
                        ),
                        onClick = { onNotificationClick() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Card(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme
                                .colorScheme
                                .onBackground
                                .copy(alpha = 0.05f)
                        ),
                        onClick = onProfileClick
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (state.user?.avatarUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = state.user.avatarUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.silhouette_male_icon),
                                    contentDescription = "Profile",
                                    tint = Color(0xFFCE93D8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val displaySong = currentPlayingSong ?: state.featuredSong
            displaySong?.let { song ->
                FeaturedCard(
                    song = song,
                    onPlayClick = onTogglePlayPause,
                    onCardClick = onGoToPlayer,
                    onLongClick = { },
                    isPlaying = isPlaying && (currentPlayingSong?.id == song.id)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trending Section
            SectionHeader(
                title = "Trending",
                modifier = Modifier.offset(y = (-5).dp),
                iconRes = R.drawable.fire_alarm_icon,
                onSeeAllClick =
                    { onTrendingSeeAllClick() }
            )
            TrendingRow(
                songs = state.trendingSongs,
                modifier = Modifier.offset(y = (-25).dp),
                onSongClick = { song -> 
                    val index = state.trendingSongs.indexOf(song)
                    onPlaySongs(state.trendingSongs, index)
                },
                onLongClick = { _ -> },
                downloadingSongs = state.downloadingSongs
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recently Played
            SectionHeader(
                title = "Recently Played",
                modifier = Modifier.offset(y = (-25).dp),
                iconRes = R.drawable.clock_line_icon,
                onSeeAllClick = { onRecentlyPlayedSeeAllClick() }
            )
            state.recentlyPlayed.take(5).forEach { song ->
                val index = state.recentlyPlayed.indexOf(song)
                SongListItem(
                    song = song,
                    onSongClick = { onPlaySongs(state.recentlyPlayed, index) },
                    modifier = Modifier.offset(y = (-45).dp),
                    onFavoriteClick = { onToggleFavorite(it) },
                    onDownloadClick = { onDownloadSong(it) },
                    onMoreClick = { },
                    onLongClick = { 
                        selectedSongIdForPlaylist = it.id
                    },
                    downloadProgress = state.downloadingSongs[song.id]
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Your Collections
            SectionHeader(
                title = "Your Collections",
                modifier = Modifier.offset(y = (-30).dp),
                emoji = "🎵",
                onSeeAllClick = null
            )
            PlaylistRow(
                playlists = state.playlists,
                modifier = Modifier.offset(y = (-30).dp),
                onPlaylistClick = onPlaylistClick,
                onDownloadsClick = onDownloadsClick,
                onPlaylistLongClick = { playlistToDelete = it },
                downloadCount = state.downloads.size,
                showDownloads = false
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
                modifier = Modifier.offset(y = (-40).dp),
                emoji = "🎵",
                onSeeAllClick = null
            )
            PlaylistRow(
                playlists = MockData.playlists,
                modifier = Modifier.offset(y = (-40).dp),
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
                featuredSong = MockData.featuredSong,
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
