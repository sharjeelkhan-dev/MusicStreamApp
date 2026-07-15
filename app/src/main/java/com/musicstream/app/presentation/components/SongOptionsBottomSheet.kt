package com.musicstream.app.presentation.components
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.res.painterResource
import com.musicstream.app.R
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsContent(
    song: Song,
    onFavoriteClick: (Song) -> Unit,
    onAddToPlaylistClick: (String) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteDownloadClick: ((String) -> Unit)? = null,
    onRemoveFromPlaylistClick: ((Song) -> Unit)? = null,
    onShareClick: (String) -> Unit = {},
    onGoToArtistClick: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Song Info Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = song.title,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = song.artist,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

        OptionItem(
            iconRes = if (song.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            text = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
            onClick = {
                onFavoriteClick(song) // ✅ Pass the full song object
                onDismissRequest()
            },
            iconColor = if (song.isFavorite) MusicStreamTheme.colors.favoriteActive else MusicStreamTheme.colors.favoriteInactive
        )
        OptionItem(
            iconRes = R.drawable.music_player_add_playlist_queue_round_outline_icon__1_,
            text = "Add to Playlist",
            onClick = {
                onAddToPlaylistClick(song.id)
                onDismissRequest()
            }
        )

        if (song.localPath != null) {
            if (onDeleteDownloadClick != null) {
                OptionItem(
                    iconRes = R.drawable.recycle_bin_line_icon,
                    text = "Delete Download",
                    iconColor = FavoriteRed,
                    textColor = FavoriteRed,
                    onClick = {
                        onDeleteDownloadClick(song.id)
                        onDismissRequest()
                    }
                )
            }
        } else {
            OptionItem(
                iconRes = R.drawable.round_line_bottom_arrow_icon,
                text = "Download",
                onClick = {
                    onDownloadClick(song)
                    onDismissRequest()
                }
            )
        }

        if (onRemoveFromPlaylistClick != null) {
            OptionItem(
                iconRes = R.drawable.recycle_bin_line_icon,
                text = "Remove from Playlist",
                iconColor = FavoriteRed,
                textColor = FavoriteRed,
                onClick = {
                    onRemoveFromPlaylistClick(song)
                    onDismissRequest()
                }
            )
        }

        OptionItem(
            iconRes = R.drawable.share_line_icon,
            text = "Share",
            onClick = {
                onShareClick(song.id)
                onDismissRequest()
            }
        )
        OptionItem(
            iconRes = R.drawable.silhouette_male_icon,
            text = "Go to Artist",
            onClick = {
                onGoToArtistClick(song.artist)
                onDismissRequest()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song?,
    onDismissRequest: () -> Unit,
    onFavoriteClick: (Song) -> Unit, // ✅ UPDATED: Song object signature fix
    onAddToPlaylistClick: (String) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteDownloadClick: ((String) -> Unit)? = null,
    onRemoveFromPlaylistClick: ((Song) -> Unit)? = null,
    onShareClick: (String) -> Unit = {},
    onGoToArtistClick: (String) -> Unit = {}
) {
    if (song == null) return

    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        SongOptionsContent(
            song = song,
            onFavoriteClick = onFavoriteClick,
            onAddToPlaylistClick = onAddToPlaylistClick,
            onDownloadClick = onDownloadClick,
            onDeleteDownloadClick = onDeleteDownloadClick,
            onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
            onShareClick = onShareClick,
            onGoToArtistClick = onGoToArtistClick,
            onDismissRequest = onDismissRequest
        )
    }
}

@Composable
private fun OptionItem(
    iconRes: Int,
    text: String,
    onClick: () -> Unit,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            color = textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}