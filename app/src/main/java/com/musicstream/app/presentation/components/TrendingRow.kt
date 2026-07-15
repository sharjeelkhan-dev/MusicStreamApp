package com.musicstream.app.presentation.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    onDownloadClick: (Song) -> Unit = {},
    downloadingSongs: Map<String, Int> = emptyMap(),
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val gradients = listOf(
        Gradients.trendingPink,
        Gradients.trendingPurple,
        Gradients.trendingOrange,
        Gradients.playlistBlue,
        Gradients.playlistPink
    )

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = (-16).dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(
            items = songs,
            key = { _, song -> song.id }
        ) { index, song ->
            TrendingCard(
                title = song.title,
                artist = song.artist,
                gradient = gradients[index % gradients.size],
                coverUrl = song.coverUrl,
                onClick = { onSongClick(song) },
                onLongClick = { onLongClick(song) },
                onDownloadClick = { onDownloadClick(song) },
                downloadProgress = downloadingSongs[song.id],
                isDownloaded = song.localPath != null,
                modifier = Modifier.width(180.dp) // Adjusted width to show cards side-by-side
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrendingRowPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .padding(vertical = 20.dp))
        {
            TrendingRow(
                songs = MockData.trendingSongs,
                onSongClick = {}
            )
        }
    }
}
