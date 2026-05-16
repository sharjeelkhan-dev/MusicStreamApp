package com.musicstream.app.presentation.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.media_tools.MediaToolsScreen
import com.musicstream.app.presentation.player.PlayerContent
import com.musicstream.app.presentation.player.PlayerUiState
import com.musicstream.app.presentation.player.RepeatMode
import com.musicstream.app.ui.theme.AccentPurple
import com.musicstream.app.ui.theme.MusicStreamTheme

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
            onFavoriteClick = {},
            onQueueClick = {},
            onShareClick = {},
            onEqualizerClick = {},
            onAddToPlaylist = { _, _ -> },
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
            onFavoriteClick = {},
            onQueueClick = {},
            onShareClick = {},
            onEqualizerClick = {},
            onAddToPlaylist = { _, _ -> },
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
