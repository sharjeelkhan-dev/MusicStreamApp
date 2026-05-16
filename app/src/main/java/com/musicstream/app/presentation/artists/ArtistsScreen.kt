package com.musicstream.app.presentation.artists
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Artist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.PremiumHeader
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.ui.theme.*

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel = hiltViewModel(),
    playerViewModel: com.musicstream.app.presentation.player.PlayerViewModel = hiltViewModel(),
    onArtistClick: (String) -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    ArtistsScreenContent(
        state = state,
        onArtistClick = onArtistClick,
        onFilterChange = { viewModel.setFilter(it) },
        onToggleLikedSongs = { viewModel.toggleShowingLikedSongs() },
        onPlaySongs = onPlaySongs,
        onBackClick = onBackClick,
        currentPlayingSong = playerState.currentSong,
        isPlaying = playerState.isPlaying,
        onTogglePlayPause = { playerViewModel.togglePlayPause() }
    )
}

@Composable
fun ArtistsScreenContent(
    state: ArtistsUiState,
    onArtistClick: (String) -> Unit = {},
    onFilterChange: (String) -> Unit = {},
    onToggleLikedSongs: () -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> },
    onBackClick: () -> Unit = {},
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onTogglePlayPause: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isShowingLikedSongs) {
            // Liked Songs Header (Centered Style like image)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Card(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        onClick = onToggleLikedSongs
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = "Liked Songs",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Liked Songs List (Vertical)
            Box(modifier = Modifier.fillMaxSize().offset(y = (-15).dp)) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(state.favoriteSongs.size) { index ->
                        val song = state.favoriteSongs[index]
                        val isSongPlaying = currentPlayingSong?.id == song.id && isPlaying
                        SongListItem(
                            song = song,
                            showThumbnail = true,
                            onSongClick = { onPlaySongs(state.favoriteSongs, index) },
                            isPlaying = isSongPlaying,
                            onPlayPauseClick = {
                                if (currentPlayingSong?.id == song.id) onTogglePlayPause()
                                else onPlaySongs(state.favoriteSongs, index)
                            }
                        )
                    }
                    
                    if (state.favoriteSongs.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(64.dp), contentAlignment = Alignment.Center) {
                                Text("No liked songs yet ❤️", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        } else {
            // Artists Browser UI
            PremiumHeader(
                title = "Browse",
                onBackClick = onBackClick,
                modifier = Modifier.statusBarsPadding()
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth().offset(y = (-30).dp)
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == "All",
                    onClick = { onFilterChange("All") },
                    label = { Text("All Artists") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AccentPurple,
                        selectedLabelColor = Color.White
                    ),
                    border = null
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().offset(y = (-25).dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    ArtistGridItem(
                        name = "Liked Songs",
                        icon = Icons.Default.Favorite,
                        isSpecial = true,
                        onClick = onToggleLikedSongs
                    )
                }

                gridItemsIndexed(state.artists) { _, artist ->
                    ArtistGridItem(
                        name = artist.name,
                        icon = Icons.Default.Person,
                        onClick = { onArtistClick(artist.name) }
                    )
                }
            }
        }
    }
}

@Composable
fun ArtistGridItem(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSpecial: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(82.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .then(
                        if (isSpecial) Modifier.background(Gradients.profileBanner)
                        else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSpecial) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 12.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ArtistsScreenPreview() {
    MusicStreamTheme {
        ArtistsScreenContent(
            state = ArtistsUiState(
                artists = listOf(
                    Artist("1", "The Weeknd"),
                    Artist("2", "Dua Lipa")
                ),
                favoriteSongs = emptyList()
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LikedSongsPreview() {
    MusicStreamTheme {
        ArtistsScreenContent(
            state = ArtistsUiState(
                isShowingLikedSongs = true,
                favoriteSongs = MockData.trendingSongs.take(5),
                artists = emptyList()
            )
        )
    }
}
