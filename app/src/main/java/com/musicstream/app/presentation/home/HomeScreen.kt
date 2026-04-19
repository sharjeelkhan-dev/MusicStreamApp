package com.musicstream.app.presentation.home

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.*
import com.musicstream.app.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onSongClick: (Song) -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

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
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = state.greeting,
                    color = AccentOrange,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Discover",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCardSurface)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = AccentAmber,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Profile",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
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

        Spacer(modifier = Modifier.height(24.dp))

        // Trending Section
        SectionHeader(
            title = "Trending",
            emoji = "🔥",
            onSeeAllClick = { }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TrendingRow(
            songs = state.trendingSongs,
            onSongClick = onSongClick
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Recently Played
        SectionHeader(
            title = "Recently Played",
            emoji = "🕐",
            onSeeAllClick = { }
        )
        Spacer(modifier = Modifier.height(4.dp))
        state.recentlyPlayed.forEach { song ->
            SongListItem(
                song = song,
                onSongClick = onSongClick,
                onFavoriteClick = { viewModel.toggleFavorite(it) }
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

        // Bottom padding for nav bar
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
private fun FeaturedCard(
    song: Song,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Gradients.featured)
            .clickable { onClick() }
    ) {
        if (song.coverUrl.isNotEmpty()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Enhanced Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
        } else {
            // Abstract background for when no image is present
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Gradients.featured)
            )
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp, y = 20.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "✦ FEATURED",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Column {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " • ",
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${MockData.formatPlayCount(song.playCount)} plays",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .clickable { onClick() }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Listen Now",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val gradients = listOf(
        Gradients.trendingPink,
        Gradients.trendingPurple,
        Gradients.trendingOrange,
        Gradients.playlistBlue,
        Gradients.playlistPink
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
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
