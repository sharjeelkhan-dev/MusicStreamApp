package com.musicstream.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
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
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.FavoriteRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song?,
    onDismissRequest: () -> Unit,
    onFavoriteClick: (String) -> Unit,
    onAddToPlaylistClick: (String) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onDeleteDownloadClick: (String) -> Unit = {},
    onRemoveFromPlaylistClick: ((Song) -> Unit)? = null,
    onShareClick: (String) -> Unit = {},
    onGoToArtistClick: (String) -> Unit = {}
) {
    if (song == null) return

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
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
                icon = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                text = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                onClick = { 
                    onFavoriteClick(song.id)
                    onDismissRequest()
                },
                iconColor = if (song.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            OptionItem(
                icon = Icons.Outlined.PlaylistAdd,
                text = "Add to Playlist",
                onClick = { 
                    onAddToPlaylistClick(song.id)
                    onDismissRequest()
                }
            )
            
            if (song.localPath != null) {
                OptionItem(
                    icon = Icons.Outlined.Delete,
                    text = "Delete Download",
                    iconColor = FavoriteRed,
                    textColor = FavoriteRed,
                    onClick = { 
                        onDeleteDownloadClick(song.id)
                        onDismissRequest()
                    }
                )
            } else {
                OptionItem(
                    icon = Icons.Outlined.Download,
                    text = "Download",
                    onClick = { 
                        onDownloadClick(song)
                        onDismissRequest()
                    }
                )
            }

            if (onRemoveFromPlaylistClick != null) {
                OptionItem(
                    icon = Icons.Outlined.PlaylistRemove,
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
                icon = Icons.Outlined.Share,
                text = "Share",
                onClick = { 
                    onShareClick(song.id)
                    onDismissRequest()
                }
            )
            OptionItem(
                icon = Icons.Outlined.Person,
                text = "Go to Artist",
                onClick = { 
                    onGoToArtistClick(song.artist)
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
            imageVector = icon,
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
