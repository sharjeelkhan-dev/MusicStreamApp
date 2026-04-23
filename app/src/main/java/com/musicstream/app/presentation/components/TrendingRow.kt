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
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.Gradients
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun TrendingRow(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onLongClick: (Song) -> Unit = {},
    downloadingSongs: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier.offset(y = (-10).dp)
) {
    val gradients = listOf(
        Gradients.trendingPink,
        Gradients.trendingPurple,
        Gradients.trendingOrange,
        Gradients.playlistBlue,
        Gradients.playlistPink
    )

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(start = 24.dp, end = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        items(songs) { song ->
            TrendingCard(
                title = song.title,
                artist = song.artist,
                gradient = gradients[songs.indexOf(song) % gradients.size],
                coverUrl = song.coverUrl,
                onClick = { onSongClick(song) },
                onLongClick = { onLongClick(song) },
                downloadProgress = downloadingSongs[song.id],
                isDownloaded = song.localPath != null
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrendingRowPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier
            .background(MaterialTheme
                .colorScheme.background)
            .padding(vertical = 20.dp))
        {
            TrendingRow(
                songs = MockData.trendingSongs,
                onSongClick = {}
            )
        }
    }
}
