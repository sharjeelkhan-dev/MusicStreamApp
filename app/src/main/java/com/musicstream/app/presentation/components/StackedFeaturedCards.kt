package com.musicstream.app.presentation.components
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun StackedFeaturedCards(
    songs: List<Song>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    currentPlayingSong: Song? = null,
    isPlaying: Boolean = false,
    onSongClick: (Song) -> Unit,
    onFavoriteClick: (Song) -> Unit,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    // Smooth reset when swiping ends
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "swipeOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .offset(y = (-30).dp)
            .padding(horizontal = 24.dp)
            .pointerInput(songs.size, currentIndex) { // ADD currentIndex to keys
                detectHorizontalDragGestures(
                    onDragCancel = { offsetX = 0f },
                    onDragEnd = {
                        // SENSITIVITY FIX: Lower threshold for easier swiping
                        if (offsetX > 120) {
                            if (currentIndex > 0) {
                                onIndexChange(currentIndex - 1)
                            }
                        } else if (offsetX < -120) {
                            if (currentIndex < (songs.size - 1)) {
                                onIndexChange(currentIndex + 1)
                            }
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // We show up to 3 cards in the stack
        val stackSize = 3
        val visibleRange = songs.indices.filter { 
            (it >= currentIndex) && (it < currentIndex + stackSize)
        }.reversed()

        visibleRange.forEach { index ->
            val song = songs[index]
            val relativeIndex = index - currentIndex
            val isFrontCard = relativeIndex == 0
            val isCurrentSong = currentPlayingSong?.id == song.id

            // Animate properties based on relative index
            val rotation by animateFloatAsState(
                targetValue = when (relativeIndex) {
                    0 -> 0f
                    1 -> -7f
                    else -> -15f
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "rotation"
            )
            
            val cardOffsetX by animateDpAsState(
                targetValue = when (relativeIndex) {
                    0 -> 0.dp
                    1 -> (-25).dp
                    else -> (-50).dp
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "offsetX"
            )
            
            val cardOffsetY by animateDpAsState(
                targetValue = when (relativeIndex) {
                    0 -> 0.dp
                    1 -> 15.dp
                    else -> 30.dp
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "offsetY"
            )

            val scale by animateFloatAsState(
                targetValue = when (relativeIndex) {
                    0 -> 1f
                    1 -> 0.92f
                    else -> 0.85f
                },
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                label = "scale"
            )

            FeaturedStackCard(
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                onClick = { 
                    if (isCurrentSong) onTogglePlayPause() else onSongClick(song)
                },
                onFavoriteClick = { onFavoriteClick(song) },
                isFrontCard = isFrontCard,
                modifier = Modifier
                    .offset { 
                        if (isFrontCard) {
                            IntOffset(animatedOffsetX.roundToInt(), (kotlin.math.abs(animatedOffsetX) * 0.1f).roundToInt())
                        } else {
                            IntOffset(0, 0)
                        }
                    }
                    .offset(x = cardOffsetX, y = cardOffsetY)
                    .graphicsLayer {
                        rotationZ = rotation + (if(isFrontCard) animatedOffsetX * 0.05f else 0f)
                        scaleX = scale
                        scaleY = scale
                        alpha = if (relativeIndex >= stackSize) 0f else 1f
                    }
                    .fillMaxHeight(0.85f)
                    .aspectRatio(0.82f)
            )
        }
    }
}

@Composable
fun FeaturedStackCard(
    song: Song,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    isFrontCard: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFrontCard) 12.dp else 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Image (Blurred for artistic effect)
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.music_song_file_icon)
                    .error(R.drawable.music_song_file_icon)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(25.dp)
                    .graphicsLayer { alpha = 0.6f }
            )

            // Main Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.coverUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.music_song_file_icon)
                    .error(R.drawable.music_song_file_icon)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(32.dp))
                    .graphicsLayer { alpha = if (isFrontCard) 1f else 0.8f }
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 200f
                        )
                    )
            )

            // Song Info & Play Button
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top: Song Title and Artist
                Column {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom: Play Controls
                if (isFrontCard) {
                    // Front Card Pill Play Button
                    Surface(
                        modifier = Modifier
                            .wrapContentSize(),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Black.copy(alpha = 0.45f),
                        onClick = onClick
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(4.dp)
                                .padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    MusicStreamTheme.colors.featuredGradientStart,
                                                    MusicStreamTheme.colors.featuredGradientEnd
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = if (isCurrentSong && isPlaying)
                                                R.drawable.pause_button_icon 
                                            else
                                                R.drawable.play_button_icon  
                                        ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCurrentSong && isPlaying) "Pause Music" else "Play Music",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Background Card Circle Play Button
                    Box(
                        modifier = Modifier.align(Alignment.End),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(
                                id = if (isCurrentSong && isPlaying)
                                    R.drawable.pause_button_icon 
                                else
                                    R.drawable.play_button_icon  
                            ),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Central Floating Favorite Heart
            if (isFrontCard) {
                Surface(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(54.dp),
                    shape = CircleShape,
                    color = MusicStreamTheme.colors.favoriteActive.copy(alpha = 0.2f),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = MusicStreamTheme.colors.favoriteActive,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StackedFeaturedCardsPreview() {
    var index by remember { mutableIntStateOf(0) }
    MusicStreamTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            StackedFeaturedCards(
                songs = MockData.trendingSongs,
                currentIndex = index,
                onIndexChange = { index = it },
                onSongClick = {},
                onFavoriteClick = {},
                onTogglePlayPause = {}
            )
        }
    }
}
