package com.musicstream.app.presentation.components
import android.annotation.SuppressLint
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.ui.theme.MusicStreamTheme
import com.musicstream.app.ui.theme.Gradients

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GradientCard(
    gradient: Brush,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        content = content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlaylistCard(
    name: String,
    songCount: Int,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.width(155.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism effect for the icon container
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (songCount >= 0) {
            Text(
                text = "$songCount songs",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp).offset(y = (-6.5).dp)
            )
        } else {
            // Case for "Liked Songs" or special playlists
            Text(
                text = "Personalized",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 4.dp).offset(y = (-6.5).dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrendingCard(
    title: String,
    artist: String,
    gradient: Brush,
    coverUrl: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    downloadProgress: Int? = null,
    isDownloaded: Boolean = false
) {
    Column(
        modifier = modifier.width(165.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(gradient)
                .combinedClickable(
                    onClick = { onClick() },
                    onLongClick = { onLongClick() }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(56.dp)
                )
            }

            if (downloadProgress != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier.size(40.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                    Text(
                        text = "$downloadProgress%",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isDownloaded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        
        Text(
            text = artist,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp).offset(y = (-6.5).dp)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B1E)
@Composable
fun GradientCardPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            GradientCard(
                gradient = Gradients.featured,
                modifier = Modifier.size(200.dp, 100.dp)
            ) {
                Text(
                    text = "Featured Card",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B1E)
@Composable
fun PlaylistCardPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlaylistCard(
                name = "Today's Top Hits",
                songCount = 50,
                gradient = Gradients.playlistBlue
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0B1E)
@Composable
fun TrendingCardPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            TrendingCard(
                title = "Starboy",
                artist = "The Weeknd",
                gradient = Gradients.trendingPink
            )
        }
    }
}
