package com.musicstream.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.AccentPurple
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun StackedFeaturedCards(
    songs: List<Song>,
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (String) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .offset(x = 10.dp, y = (-30).dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        val cards = songs.take(3)
        
        cards.asReversed().forEachIndexed { index, song ->
            val actualIndex = cards.size - 1 - index
            val isFrontCard = actualIndex == cards.size - 1
            val isCurrentSong = currentPlayingSong?.id == song.id
            
            val rotation = when (actualIndex) {
                0 -> -15f
                1 -> -7f
                else -> 4f
            }
            val offsetX = when (actualIndex) {
                0 -> (-45).dp
                1 -> (-20).dp
                else -> 10.dp
            }
            val offsetY = when (actualIndex) {
                0 -> 30.dp
                1 -> 15.dp
                else -> 0.dp
            }

            FeaturedStackCard(
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                onClick = { 
                    if (isCurrentSong) onTogglePlayPause() else onSongClick(song)
                },
                onFavoriteClick = { onFavoriteClick(song.id) },
                isFrontCard = isFrontCard,
                modifier = Modifier
                    .offset(x = offsetX, y = offsetY)
                    .rotate(rotation)
                    .fillMaxHeight(0.85f)
                    .aspectRatio(0.82f)
            )
        }
    }
}

@Composable
fun FeaturedStackCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFrontCard: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFrontCard) 12.dp else 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image (Blurred for artistic effect like FeaturedCard)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.music_song_file_icon)
                    .error(R.drawable.music_song_file_icon)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(25.dp)
                    .graphicsLayer { alpha = 0.6f }
            )

            // Main Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.music_song_file_icon)
                    .error(R.drawable.music_song_file_icon)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .graphicsLayer { alpha = if (isFrontCard) 1f else 0.8f }
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Song Info & Play Button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Song Title and Artist
                Column {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom: Play Controls
                if (isFrontCard) {
                    // Front Card Pill Play Button
                    Surface(
                        modifier = Modifier
                            .wrapContentSize(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        onClick = onClick
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = AccentPurple
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCurrentSong && isPlaying) "Pause Music" else "Play Music",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Background Card Circle Play Button
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.8f),
                        onClick = onClick
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isCurrentSong && isPlaying)
                                    Icons.Default.Pause else
                                        Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                                    .offset(x = if (isCurrentSong && isPlaying)
                                        0.dp else 1.dp)
                            )
                        }
                    }
                }
            }

            // Central Floating Favorite Heart
            if (isFrontCard) {
                Surface(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(54.dp),
                    shape = CircleShape,
                    color = Color(0xFFFFD54F).copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFEF5350),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StackedFeaturedCardsPreview() {
    MusicStreamTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            StackedFeaturedCards(
                songs = MockData.trendingSongs,
                onSongClick = {},
                onFavoriteClick = {},
                onTogglePlayPause = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FeaturedStackCardPreview() {
    MusicStreamTheme {
        Box(
            modifier = Modifier
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            FeaturedStackCard(
                song = MockData.featuredSong,
                isCurrentSong = false,
                isPlaying = false,
                onClick = {},
                onFavoriteClick = {},
                isFrontCard = true,
                modifier = Modifier
                    .width(240.dp)
                    .height(300.dp)
            )
        }
    }
}
