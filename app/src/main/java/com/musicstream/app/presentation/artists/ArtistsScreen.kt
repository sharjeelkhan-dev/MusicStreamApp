package com.musicstream.app.presentation.artists
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.musicstream.app.domain.model.Artist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.WideSongListItem
import com.musicstream.app.ui.theme.*

@Composable
fun ArtistsScreen(
    viewModel: ArtistsViewModel = hiltViewModel(),
    onArtistClick: (String) -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ArtistsScreenContent(
        state = state,
        onArtistClick = onArtistClick,
        onFilterChange = { viewModel.setFilter(it) },
        onToggleFavorite = { viewModel.toggleFavorite(it) },
        onToggleLikedSongs = { viewModel.toggleShowingLikedSongs() },
        onPlaySongs = onPlaySongs
    )
}

@Composable
fun ArtistsScreenContent(
    state: ArtistsUiState,
    onArtistClick: (String) -> Unit = {},
    onFilterChange: (String) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onToggleLikedSongs: () -> Unit = {},
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> }
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header with Back button if showing Liked Songs
        if (state.isShowingLikedSongs) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleLikedSongs) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Text(
                    text = "Liked Songs",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        } else {
            // Filter Chips (Only shown when not in Liked Songs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-7).dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = state.selectedFilter == "All",
                    onClick = { onFilterChange("All") },
                    label = { Text("All") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Black,
                        selectedLabelColor = Color.Black,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = null
                )
            }
        }

        // Content
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.isShowingLikedSongs) {
                // Liked Songs List
                items(state.favoriteSongs, span = { GridItemSpan(2) }) { song ->
                    val index = state.favoriteSongs.indexOf(song)
                    WideSongListItem(
                        song = song,
                        onSongClick = { onPlaySongs(state.favoriteSongs, index) },
                        onFavoriteClick = { onToggleFavorite(song.id) },
                        onMoreClick = { /* Handle more */ }
                    )
                }
            } else {
                // Liked Songs Card (Special) - Now Full Width
                item(span = { GridItemSpan(2) }) {
                    ArtistGridItem(
                        name = "Liked Songs",
                        icon = Icons.Default.Favorite,
                        isSpecial = true,
                        onClick = onToggleLikedSongs
                    )
                }

                // Artists
                items(state.artists) { artist ->
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
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme
                    .colorScheme.
                    surfaceVariant.
                    copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B1E)
@Composable
fun ArtistsScreenPreview() {
    MusicStreamTheme(darkTheme = true) {
        ArtistsScreenContent(
            state = ArtistsUiState(
                artists = listOf(
                    Artist("1", "The Weeknd"),
                    Artist("2", "Dua Lipa"),
                    Artist("3", "Justin Bieber")
                ),
                favoriteSongs = emptyList()
            )
        )
    }
}
