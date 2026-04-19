package com.musicstream.app.presentation.components

import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*

@Composable
fun SongListItem(
    song: Song,
    onSongClick: (Song) -> Unit = {},
    onFavoriteClick: (String) -> Unit = {},
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail with gradient or image
        if (song.coverUrl.isNotEmpty()) {
            AsyncImage(
                model = song.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(gradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = TextPrimary.copy(alpha = 0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = song.artist,
                color = TextSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = song.durationFormatted,
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(end = 4.dp)
        )

        // Favorite icon
        IconButton(
            onClick = { onFavoriteClick(song.id) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) FavoriteRed else FavoriteInactive,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
