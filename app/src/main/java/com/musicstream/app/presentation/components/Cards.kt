package com.musicstream.app.presentation.components
import android.annotation.SuppressLint
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.musicstream.app.ui.theme.TextPrimary
import com.musicstream.app.ui.theme.TextSecondary
import androidx.compose.ui.tooling.preview.Preview
import com.musicstream.app.ui.theme.MusicStreamTheme
import com.musicstream.app.ui.theme.Gradients

@Composable
fun GradientCard(
    gradient: Brush,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(gradient)
            .clickable { onClick() },
        content = content
    )
}

@Composable
fun PlaylistCard(
    name: String,
    songCount: Int,
    gradient: Brush,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.width(150.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            // Glassmorphism effect for the icon container
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = name,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Text(
            text = "$songCount songs",
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun TrendingCard(
    title: String,
    artist: String,
    gradient: Brush,
    coverUrl: String = "",
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = modifier.width(160.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(gradient)
                .clickable { onClick() },
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
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Text(
            text = artist,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
