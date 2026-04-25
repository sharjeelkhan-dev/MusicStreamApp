package com.musicstream.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Playlist
import com.musicstream.app.ui.theme.Gradients
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun PlaylistRow(
    playlists: List<Playlist>,
    onPlaylistClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
    onPlaylistLongClick: (Playlist) -> Unit = {}
) {
    val gradients = listOf(
        Gradients.playlistBlue,
        Gradients.playlistPink,
        Gradients.playlistGreen,
        Gradients.trendingOrange,
        Gradients.trendingPurple
    )

    LazyRow(
        modifier = modifier.offset(y = (-10).dp),
        contentPadding = PaddingValues(start = 24.dp, end = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            PlaylistCard(
                name = "Liked Songs",
                songCount = -1,
                gradient = Gradients.featured,
                onClick = { }
            )
        }

        items(playlists) { playlist ->
            PlaylistCard(
                name = playlist.name,
                songCount = playlist.songCount,
                gradient = gradients[playlists.indexOf(playlist) % gradients.size],
                onClick = { onPlaylistClick(playlist) },
                onLongClick = { onPlaylistLongClick(playlist) }
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B1E)
@Composable
fun YourCollectionsPreview() {
    MusicStreamTheme(darkTheme = true) {
        Column(
            modifier = Modifier
                .background(MaterialTheme
                    .colorScheme
                    .background)
                .padding(vertical = 20.dp)
        ) {
            SectionHeader(
                title = "Your Collections",
                emoji = "🎵",
                onSeeAllClick = { }
            )
            PlaylistRow(
                playlists = MockData.playlists,
                onPlaylistClick = {}
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaylistRowPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(vertical = 20.dp)) {
            PlaylistRow(
                playlists = MockData.playlists,
                onPlaylistClick = {}
            )
        }
    }
}
