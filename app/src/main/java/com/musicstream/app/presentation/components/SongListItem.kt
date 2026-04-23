package com.musicstream.app.presentation.components

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongListItem(
    song: Song,
    onSongClick: (Song) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
    onDownloadClick: (Song) -> Unit = {},
    onMoreClick: (Song) -> Unit = {},
    onLongClick: (Song) -> Unit = {},
    downloadProgress: Int? = null,
    modifier: Modifier = Modifier
) {
    val thumbGradients = listOf(
        Gradients.songThumbPink,
        Gradients.songThumbOrange,
        Gradients.songThumbBlue,
        Gradients.songThumbGreen,
        Gradients.trendingPurple
    )
    val gradient = thumbGradients[song.gradientIndex % thumbGradients.size]

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    onClick = { onSongClick(song) },
                    onLongClick = { onLongClick(song) }
                )
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            if (song.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title and artist
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.title,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                if (downloadProgress != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier
                                .width(60.dp)
                                .height(3.dp)
                                .clip(CircleShape),
                            color = AccentPurple,
                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$downloadProgress%",
                            color = AccentPurple,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = song.artist,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite icon
            IconButton(
                onClick = { onFavoriteClick(song.id) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isFavorite) FavoriteRed else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Download icon
            if (song.localPath == null && downloadProgress == null) {
                IconButton(
                    onClick = { onDownloadClick(song) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (song.localPath != null) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = "Downloaded",
                    tint = AccentPurple,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(6.dp)
                )
            }

            IconButton(
                onClick = { onMoreClick(song) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
