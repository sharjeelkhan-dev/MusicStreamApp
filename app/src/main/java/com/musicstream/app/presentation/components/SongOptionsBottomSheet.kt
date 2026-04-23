package com.musicstream.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.FavoriteRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    onDismissRequest: () -> Unit,
    onFavoriteClick: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onGoToArtistClick: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)) }
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
                // Placeholder for Album Art
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // Options List
            OptionItem(
                icon = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = if (song.isFavorite) "Remove from Favorites" else "Add to Favorites",
                iconColor = if (song.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    onFavoriteClick()
                    onDismissRequest()
                }
            )

            OptionItem(
                icon = Icons.Outlined.PlaylistAdd,
                label = "Add to Playlist",
                onClick = {
                    onAddToPlaylistClick()
                    // Don't dismiss here as it will open another sheet
                }
            )

            OptionItem(
                icon = Icons.Outlined.Download,
                label = "Download Song",
                onClick = {
                    onDownloadClick()
                    onDismissRequest()
                }
            )

            OptionItem(
                icon = Icons.Outlined.Share,
                label = "Share",
                onClick = {
                    onShareClick()
                    onDismissRequest()
                }
            )

            OptionItem(
                icon = Icons.Outlined.Person,
                label = "View Artist",
                onClick = {
                    onGoToArtistClick()
                    onDismissRequest()
                }
            )
        }
    }
}

@Composable
private fun OptionItem(
    icon: ImageVector,
    label: String,
    iconColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
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
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
