package com.musicstream.app.presentation.home
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        state = state,
        onSongClick = onSongClick,
        onNotificationClick = onNotificationClick,
        onRecentlyPlayedSeeAllClick = onRecentlyPlayedSeeAllClick,
        onCreatePlaylist = { viewModel.createPlaylist(it) },
        onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
        onToggleFavorite = { viewModel.toggleFavorite(it) }
    )
}

@Composable
fun HomeContent(
    state: HomeUiState,
    onSongClick: (Song) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onRecentlyPlayedSeeAllClick: () -> Unit = {},
    onCreatePlaylist: (String) -> Unit = {},
    onAddSongToPlaylist: (String, String) -> Unit = { _, _ -> },
    onToggleFavorite: (String) -> Unit = {}
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
            .verticalScroll(scrollState)
            .statusBarsPadding()
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Greeting + Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = state.greeting,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Discover",
                    color = TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = DarkCardSurface,
                    onClick = { onNotificationClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = AccentPurple
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Featured Card
        state.featuredSong?.let { featured ->
            FeaturedCard(
                song = featured,
                onClick = { onSongClick(featured) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Trending Section
        SectionHeader(
            title = "Trending",
            emoji = "🔥",
            onSeeAllClick = { }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrendingRow(
            songs = state.trendingSongs,
            onSongClick = onSongClick,
            onMoreClick = { selectedSongForOptions = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recently Played
        SectionHeader(
            title = "Recently Played",
            emoji = "🕐",
            onSeeAllClick = { onRecentlyPlayedSeeAllClick() }
        )
        Spacer(modifier = Modifier.height(4.dp))
        state.recentlyPlayed.take(5).forEach { song ->
            SongListItem(
                song = song,
                onSongClick = onSongClick,
                onFavoriteClick = { onToggleFavorite(it) },
                onMoreClick = { selectedSongForOptions = it }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Your Playlists
        SectionHeader(
            title = "Your Playlists",
            emoji = "🎵",
            onSeeAllClick = { }
        )
        Spacer(modifier = Modifier.height(8.dp))
        PlaylistRow(playlists = state.playlists)

        Spacer(modifier = Modifier.height(24.dp))

        // New Releases
        SectionHeader(
            title = "New Releases",
            emoji = "✨",
            onSeeAllClick = { }
        )
        Spacer(modifier = Modifier.height(8.dp))
        NewSongsRow(
            songs = state.newSongs,
            onSongClick = onSongClick,
            onMoreClick = { selectedSongForOptions = it }
        )

        // Bottom padding for nav bar
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun FeaturedCard(
    song: Song,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            //.offset(y = (-10).dp)
            .padding(horizontal = 24.dp)
            .height(200.dp),
        shape = RoundedCornerShape(28.dp),
        color = Color(0xFFFFC107), // Golden yellow base
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFD54F),
                                Color(0xFFFFA000)
                            )
                        )
                    )
            )

            // Decorative Circle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 40.dp)
                    .size(180.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "✦ FEATURED",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 36.sp
                    )
                    Text(
                        text = "${song.artist} • ${MockData.formatPlayCount(song.playCount)} plays",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.2f),
                    onClick = onClick
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Play Now",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendingRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit = {}
) {
    val gradients = listOf(
        Gradients.trendingPink,
        Gradients.trendingPurple,
        Gradients.trendingOrange,
        Gradients.playlistBlue,
        Gradients.playlistPink
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(songs) { song ->
            TrendingCard(
                title = song.title,
                artist = song.artist,
                gradient = gradients[songs.indexOf(song) % gradients.size],
                coverUrl = song.coverUrl,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Composable
private fun PlaylistRow(
    playlists: List<com.musicstream.app.domain.model.Playlist>
) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(playlists) { playlist ->
            PlaylistCard(
                name = playlist.name,
                songCount = playlist.songCount,
                gradient = gradients[playlists.indexOf(playlist) % gradients.size]
            )
        }
    }
}

@Composable
private fun NewSongsRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onMoreClick: (Song) -> Unit = {}
) {
    val gradients = listOf(
        Gradients.playlistGreen,
        Gradients.playlistBlue,
        Gradients.songThumbOrange,
        Gradients.songThumbPink,
        Gradients.trendingPurple
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(songs) { song ->
            TrendingCard(
                title = song.title,
                artist = song.artist,
                gradient = gradients[songs.indexOf(song) % gradients.size],
                coverUrl = song.coverUrl,
                onClick = { onSongClick(song) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A12)
@Composable
fun HomeScreenPreview() {
    MusicStreamTheme {
        HomeContent(
            state = HomeUiState(
                greeting = "Good Morning 👋",
                featuredSong = MockData.featuredSong,
                trendingSongs = MockData.trendingSongs,
                recentlyPlayed = MockData.recentlyPlayed,
                newSongs = MockData.trendingSongs,
                playlists = MockData.playlists,
                isLoading = false
            )
        )
    }
}
