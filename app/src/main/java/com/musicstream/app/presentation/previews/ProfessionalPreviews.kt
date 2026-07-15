package com.musicstream.app.presentation.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.media_tools.MediaToolsScreen
import com.musicstream.app.presentation.player.PlayerContent
import com.musicstream.app.presentation.player.PlayerUiState
import com.musicstream.app.presentation.player.RepeatMode
import com.musicstream.app.ui.theme.AccentPurple
import com.musicstream.app.ui.theme.MusicStreamTheme

@Preview(showBackground = true, name = "Media Notification - Mini Player")
@Composable
fun MediaNotificationPreview() {
    MusicStreamTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .height(120.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=200",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Info and Controls
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Die With A Smile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Lady Gaga & Bruno Mars",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Media Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.SkipPrevious,
                                null,
                                modifier = Modifier.size(24.dp))
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Pause,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.SkipNext,
                                null,
                                modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Close,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Download Notification Progress")
@Composable
fun DownloadNotificationPreview() {
    MusicStreamTheme {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.media3_notification_small_icon),
                            null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Downloading Track",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Winning Speech - Karan Aujla",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "65%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentPurple
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { 0.65f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = AccentPurple,
                    trackColor = AccentPurple.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Professional Player - Light")
@Composable
fun ProfessionalPlayerLightPreview() {
    MusicStreamTheme(darkTheme = false) {
        PlayerContent(
            state = PlayerUiState(
                currentSong = Song(
                    id = "1",
                    title = "Echoes of the Night",
                    artist = "Luna Ray",
                    streamUrl = "",
                    coverUrl = "https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=800&q=80",
                    duration = 320000
                ),
                isPlaying = true,
                progress = 0.45f,
                currentPosition = 144000,
                duration = 320000,
                isShuffleOn = true,
                repeatMode = RepeatMode.ALL
            ),
            songColor = AccentPurple,
            onBackClick = {},
            onTogglePlayPause = {},
            onNextSong = {},
            onPreviousSong = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onPlaybackSpeedChange = {},
            onFavoriteClick = { _ -> },
            onQueueClick = {},
            onShareClick = {},
            onEqualizerClick = {},
            onAddToPlaylist = { _, _ -> },
            onDownloadClick = {},
            onSetSleepTimer = {},
            onGoToArtist = {},
            onGoToAlbum = {},
            onHistoryClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Professional Player - Dark")
@Composable
fun ProfessionalPlayerDarkPreview() {
    MusicStreamTheme(darkTheme = true) {
        PlayerContent(
            state = PlayerUiState(
                currentSong = Song(
                    id = "2",
                    title = "Midnight City",
                    artist = "M83",
                    streamUrl = "",
                    coverUrl = "https://images.unsplash.com/photo-1493225255756-d9584f8606e9?w=800&q=80",
                    duration = 243000
                ),
                isPlaying = true,
                progress = 0.7f,
                currentPosition = 170000,
                duration = 243000,
                isShuffleOn = false,
                repeatMode = RepeatMode.ONE
            ),
            songColor = Color(0xFF00E676),
            onBackClick = {},
            onTogglePlayPause = {},
            onNextSong = {},
            onPreviousSong = {},
            onSeekTo = {},
            onToggleShuffle = {},
            onToggleRepeat = {},
            onPlaybackSpeedChange = {},
            onFavoriteClick = { _ -> },
            onQueueClick = {},
            onShareClick = {},
            onEqualizerClick = {},
            onAddToPlaylist = { _, _ -> },
            onDownloadClick = {},
            onSetSleepTimer = {},
            onGoToArtist = {},
            onGoToAlbum = {},
            onHistoryClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Media Tools - Light")
@Composable
fun MediaToolsLightPreview() {
    MusicStreamTheme(darkTheme = false) {
        MediaToolsScreen()
    }
}

@Preview(showBackground = true, name = "Media Tools - Dark")
@Composable
fun MediaToolsDarkPreview() {
    MusicStreamTheme(darkTheme = true) {
        MediaToolsScreen()
    }
}
