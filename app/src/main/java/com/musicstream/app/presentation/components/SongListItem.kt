package com.musicstream.app.presentation.components
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.domain.model.Song
import com.musicstream.app.data.MockData
import com.musicstream.app.ui.theme.*
import com.musicstream.app.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    showThumbnail: Boolean = true,
    onSongClick: (Song) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onDownloadClick: (Song) -> Unit = {},
    onMoreClick: (Song) -> Unit = {},
    onLongClick: (Song) -> Unit = {},
    downloadProgress: Int? = null,
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {}
) {
    val thumbGradients = listOf(
        Gradients.songThumbPink,
        Gradients.songThumbOrange,
        Gradients.songThumbBlue,
        Gradients.songThumbGreen,
        Gradients.trendingPurple
    )
    val gradient = thumbGradients[song.gradientIndex % thumbGradients.size]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .combinedClickable(
                    onClick = { onSongClick(song) },
                    onLongClick = { onLongClick(song) }
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail (Optional)
            if (showThumbnail) {
                if (song.coverUrl.isNotEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                            .data(song.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(gradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.audio_tune_icon),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
            } else {
                // If no thumbnail, add some padding to center content better
                Spacer(modifier = Modifier.width(12.dp))
            }

            // Title and artist
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = if (showThumbnail) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(
                    text = song.title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (showThumbnail) TextAlign.Start else TextAlign.Center
                )
                Text(
                    text = song.artist,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (showThumbnail) TextAlign.Start else TextAlign.Center
                )
            }

            // Controls (Plus and Play/Pause like the image)
            IconButton(
                onClick = { onMoreClick(song) },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.plus_line_icon),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    if (isPlaying) onPlayPauseClick() else onSongClick(song)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) AccentPurple else Color.Transparent)
            ) {
                Icon(
                    painter = painterResource(
                        id = if (isPlaying) R.drawable.pause_button_icon
                        else R.drawable.play_button_icon
                    ),
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isPlaying) Color.White else MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(if (isPlaying) 20.dp else 28.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WideSongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onDownloadClick: (Song) -> Unit = {},
    onMoreClick: (Song) -> Unit = {},
    onLongClick: (Song) -> Unit = {},
    downloadProgress: Int? = null,
    isPlaying: Boolean = false,
    onPlayPauseClick: () -> Unit = {}
) {
    SongListItem(
        song = song,
        modifier = modifier,
        onSongClick = onSongClick,
        onFavoriteClick = onFavoriteClick,
        onDownloadClick = onDownloadClick,
        onMoreClick = onMoreClick,
        onLongClick = onLongClick,
        downloadProgress = downloadProgress,
        isPlaying = isPlaying,
        onPlayPauseClick = onPlayPauseClick
    )
}

@Preview(showBackground = true)
@Composable
fun SongListItemPreview() {
    MusicStreamTheme {
        SongListItem(
            song = MockData.trendingSongs[0]
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SongListItemPlayingPreview() {
    MusicStreamTheme {
        SongListItem(
            song = MockData.trendingSongs[0],
            isPlaying = true
        )
    }
}
