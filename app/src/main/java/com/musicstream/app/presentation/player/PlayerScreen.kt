package com.musicstream.app.presentation.player

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1040),
                        DarkBackground
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    onClick = { onBackClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
                Text(
                    text = "NOW PLAYING",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.08f),
                    onClick = {
                        Toast.makeText(context, "More options coming soon!", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Album Art
            state.currentSong?.let { song ->
                val thumbGradients = listOf(
                    Gradients.songThumbPink,
                    Gradients.songThumbOrange,
                    Gradients.songThumbBlue,
                    Gradients.songThumbGreen,
                    Gradients.trendingPurple
                )
                
                Surface(
                    modifier = Modifier
                        .size(320.dp)
                        .padding(8.dp),
                    shape = RoundedCornerShape(40.dp),
                    shadowElevation = 24.dp,
                    color = DarkCardSurface
                ) {
                    if (song.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(thumbGradients[song.gradientIndex % thumbGradients.size]),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Song Info
                Text(
                    text = song.title,
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = song.artist,
                    color = AccentPurple,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Seek Bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = state.progress,
                    onValueChange = { viewModel.seekTo(it) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentOrange,
                        activeTrackColor = AccentOrange,
                        inactiveTrackColor = SeekBarInactive
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(state.currentPosition),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(state.duration),
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        imageVector = Icons.Filled.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (state.isShuffleOn) AccentPurple else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.previousSong() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Play/Pause
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color.White,
                    onClick = { viewModel.togglePlayPause() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Next
                IconButton(onClick = { viewModel.nextSong() }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Repeat
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        imageVector = when (state.repeatMode) {
                            RepeatMode.ONE -> Icons.Filled.RepeatOne
                            else -> Icons.Filled.Repeat
                        },
                        contentDescription = "Repeat",
                        tint = if (state.repeatMode != RepeatMode.OFF) AccentPurple else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Bottom actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed control
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkCardSurface)
                        .clickable {
                            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                            val curIdx = speeds.indexOf(state.playbackSpeed)
                            val nextIdx = (curIdx + 1) % speeds.size
                            viewModel.setPlaybackSpeed(speeds[nextIdx])
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "${state.playbackSpeed}x",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Equalizer,
                    contentDescription = "Equalizer",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp).clickable {
                        Toast.makeText(context, "Equalizer coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )

                Icon(
                    imageVector = if (state.currentSong?.isFavorite == true) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (state.currentSong?.isFavorite == true) AccentOrange else TextSecondary,
                    modifier = Modifier.size(22.dp).clickable {
                        state.currentSong?.let { viewModel.toggleFavorite(it.id) }
                    }
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = "Queue",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp).clickable {
                        Toast.makeText(context, "Queue management coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )

                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp).clickable {
                        Toast.makeText(context, "Sharing coming soon!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

@Preview(showBackground = true)
@Composable
fun PlayerScreenPreview() {
    MusicStreamTheme {
        PlayerScreen(onBackClick = {})
    }
}
