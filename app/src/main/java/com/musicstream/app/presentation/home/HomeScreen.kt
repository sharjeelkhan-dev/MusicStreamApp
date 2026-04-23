package com.musicstream.app.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.pulltorefresh.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onTrendingSeeAllClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onYourCollectionsSeeAllClick: () -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        onSongClick = onSongClick,
        onNotificationClick = onNotificationClick,
        onTrendingSeeAllClick = onTrendingSeeAllClick,
        onRecentlyPlayedSeeAllClick = onRecentlyPlayedSeeAllClick,
        onYourCollectionsSeeAllClick = onYourCollectionsSeeAllClick,
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onDeletePlaylist = { viewModel.deletePlaylist(it) },
        onDeleteAllPlaylists = { viewModel.deleteAllPlaylists() },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onDownloadSong = { viewModel.downloadSong(it) },
        onGoToArtist = onGoToArtist
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeContent(
    state: HomeUiState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onSongClick: (Song) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onTrendingSeeAllClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onYourCollectionsSeeAllClick: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onDeletePlaylist: (String) -> Unit = {},
    onDeleteAllPlaylists: () -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onToggleFavorite: (String) -> Unit = {},
    onDownloadSong: (Song) -> Unit = {},
    onGoToArtist: (String) -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()
    var selectedSongIdForPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Delete All Playlists") },
            text = { Text("Are you sure you want to delete all playlists? This cannot be undone.") },
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
            onAddToPlaylistClick = { 
                selectedSongIdForPlaylist = selectedSongForOptions!!.id
                selectedSongForOptions = null
            },
            onDownloadClick = { 
                selectedSongForOptions?.let { onDownloadSong(it) }
            },
            onShareClick = { 
                val shareIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Listening to ${selectedSongForOptions?.title} by ${selectedSongForOptions?.artist} on MusicStream!")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share song via"))
            },
            onGoToArtistClick = { 
                selectedSongForOptions?.artist?.let { onGoToArtist(it) }
                selectedSongForOptions = null
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
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
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
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
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme
                            .colorScheme
                            .onBackground
                            .copy(alpha = 0.05f),
                        onClick = { onNotificationClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme
                            .colorScheme
                            .onBackground
                            .copy(alpha = 0.05f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = Color(0xFFCE93D8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            state.featuredSong?.let { featured ->
                FeaturedCard(
                    song = featured,
                    onClick = { onSongClick(featured) },
                    onLongClick = { selectedSongForOptions = featured }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Trending Section
            SectionHeader(
                title = "Trending",
                emoji = "🔥",
                onSeeAllClick = { onTrendingSeeAllClick() }
            )
            TrendingRow(
                songs = state.trendingSongs,
                onSongClick = onSongClick,
                onLongClick = { song -> selectedSongForOptions = song },
                downloadingSongs = state.downloadingSongs
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Recently Played
            SectionHeader(
                title = "Recently Played",
                emoji = "🕐",
                onSeeAllClick = { onRecentlyPlayedSeeAllClick() }
            )
            state.recentlyPlayed.take(5).forEach { song ->
                SongListItem(
                    song = song,
                    modifier = Modifier.offset(y = (-10).dp),
                    onSongClick = onSongClick,
                    onFavoriteClick = { onToggleFavorite(it) },
                    onDownloadClick = { onDownloadSong(it) },
                    onMoreClick = { selectedSongForOptions = it },
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
                emoji = "🎵",
                onSeeAllClick = { onYourCollectionsSeeAllClick() }
            )
            PlaylistRow(
                playlists = state.playlists,
                onPlaylistClick = { /* Navigate to Playlist */ },
                modifier = Modifier,
                onPlaylistLongClick = { playlistToDelete = it }
            )
            
            Spacer(modifier = Modifier.height(120.dp))
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
