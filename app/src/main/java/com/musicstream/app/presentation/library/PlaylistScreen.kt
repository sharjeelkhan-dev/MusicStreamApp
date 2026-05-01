package com.musicstream.app.presentation.library

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.data.MockData
import com.musicstream.app.ui.theme.MusicStreamTheme
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.ui.theme.AccentPurple
import com.musicstream.app.ui.theme.Gradients

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    PlaylistScreenContent(
        state = state,
        onBackClick = onBackClick,
        onSongClick = onSongClick,
        onPlaySongs = onPlaySongs,
        onToggleFavorite = { viewModel.toggleFavorite(it) },
    ) { viewModel.downloadSong(it) }
}

@Composable
fun PlaylistScreenContent(
    state: PlaylistUiState,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlaySongs: (List<Song>, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDownloadSong: (Song) -> Unit
) {
    var selectedSongForOptions by remember { mutableStateOf<Song?>(null) }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPurple)
        }
    } else {
        state.playlist?.let { playlist ->
            PlaylistDetailView(
                playlist = playlist,
                songs = state.songs,
                onBackClick = onBackClick,
                onSongClick = onSongClick,
                onPlayAllClick = {
                    if (state.songs.isNotEmpty()) {
                        onPlaySongs(state.songs, 0)
                    }
                },
                onShuffleClick = {
                    if (state.songs.isNotEmpty()) {
                        onPlaySongs(state.songs.shuffled(), 0)
                    }
                },
                onFavoriteClick = onToggleFavorite,
                onMoreClick = { selectedSongForOptions = it },
                downloadingSongs = state.downloadingSongs
            )
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Playlist not found", color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    if (selectedSongForOptions != null) {
        SongOptionsBottomSheet(
            song = selectedSongForOptions,
            onDismissRequest = { 
                selectedSongForOptions = null 
            },
            onFavoriteClick = onToggleFavorite,
            onAddToPlaylistClick = { /* Already in a playlist, but could add to others */ },
            onDownloadClick = onDownloadSong,
            onDeleteDownloadClick = { /* Handle delete if needed */ },
            onShareClick = { /* Handle share */ },
            onGoToArtistClick = { /* Handle artist click */ }
        )
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    songs: List<Song>,
    onBackClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onFavoriteClick: (String) -> Unit,
    onMoreClick: (Song) -> Unit,
    downloadingSongs: Map<String, Int> = emptyMap()
) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = "Playlist",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // Playlist Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(gradients[playlist.gradientIndex % gradients.size]),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.audio_tune_icon),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = playlist.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "${playlist.songCount} songs • Updated today",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlayAllClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onShuffleClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Shuffle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Songs Header
        Text(
            text = "Songs",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                )
                .offset(y = (-13).dp),
        )

        // Songs List
        Column(
            modifier = Modifier
                .fillMaxWidth().offset(y = (-13).dp)
                .padding(bottom = 30.dp)
        ) {
            if (songs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text(text = "No songs in this playlist", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                songs.forEach { song ->
                    SongListItem(
                        song = song,
                        onSongClick = onSongClick,
                        onFavoriteClick = onFavoriteClick,
                        onMoreClick = { onMoreClick(song) },
                        downloadProgress = downloadingSongs[song.id]
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistScreenPreview() {
    MusicStreamTheme {
        PlaylistScreenContent(
            state = PlaylistUiState(
                playlist = MockData.playlists[0],
                songs = MockData.recentlyPlayed,
                isLoading = false
            ),
            onBackClick = {},
            onSongClick = {},
            onPlaySongs = { _, _ -> },
            onToggleFavorite = {},
            onDownloadSong = {}
        )
    }
}
