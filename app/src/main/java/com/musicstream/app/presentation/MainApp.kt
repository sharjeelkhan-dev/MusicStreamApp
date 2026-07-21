package com.musicstream.app.presentation
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicstream.app.navigation.NavGraph
import com.musicstream.app.navigation.Screen
import com.musicstream.app.presentation.components.BottomNavBar
import com.musicstream.app.presentation.components.SongOptionsBottomSheet
import com.musicstream.app.presentation.player.MiniPlayerBar
import com.musicstream.app.presentation.player.PlayerViewModel
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.domain.model.Song
import com.musicstream.app.ui.theme.Gradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // Bottom Sheet Tracking States
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSongForSheet by remember { mutableStateOf<Song?>(null) }
    var isSongAlreadyLiked by remember { mutableStateOf(false) }

    // Playlist Selection Dialog State
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var songIdForPlaylist by remember { mutableStateOf("") }

    // Persistent Reactive Cache
    val colorCache = remember { mutableStateMapOf<String, Color>() }
    val currentSong = playerState.currentSong
    val currentSongId = currentSong?.id ?: ""

    // Main Color Logic
    var lastValidColor by remember { mutableStateOf(Gradients.songThumbColors[0]) }

    val songColor by animateColorAsState(
        targetValue = colorCache[currentSongId] ?: lastValidColor,
        animationSpec = tween(600),
        label = "mainColorAnimation"
    )

    LaunchedEffect(currentSongId) {
        colorCache[currentSongId]?.let { lastValidColor = it }
    }

    // Background Extraction Logic
    LaunchedEffect(currentSongId, isDark) {
        val song = currentSong ?: return@LaunchedEffect
        if (colorCache.containsKey(song.id)) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(song.coverUrl)
                    .allowHardware(enable = false)
                    .build()

                val result = loader.execute(request).drawable
                result?.let { drawable ->
                    val bitmap = drawable.toBitmap(width = 128, height = 128)
                    val palette = Palette.from(bitmap).generate()

                    val colorInt = palette.getVibrantColor(palette.getDominantColor(0))

                    if (colorInt != 0) {
                        val hsl = FloatArray(3)
                        androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
                        if (isDark) {
                            if (hsl[2] < 0.45f) hsl[2] = 0.55f
                            if (hsl[1] < 0.5f) hsl[1] = 0.65f
                        } else {
                            if (hsl[2] > 0.45f) hsl[2] = 0.4f
                            if (hsl[1] < 0.5f) hsl[1] = 0.7f
                        }
                        val extracted = Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))

                        withContext(Dispatchers.Main) {
                            colorCache[song.id] = extracted
                            lastValidColor = extracted
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Global navigation for Sign Out
    LaunchedEffect(isLoggedIn) {
        if ((isLoggedIn == false) && (currentRoute != Screen.Splash.route) && (currentRoute != Screen.Login.route)) {
            playerViewModel.pauseSong()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val isMainScreen = currentRoute != null && currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.MediaTools.route,
        Screen.Library.route,
        Screen.Artists.route
    )

    val showMiniPlayer = (isLoggedIn == true) &&
            (playerState.currentSong != null) &&
            (currentRoute != null) &&
            (currentRoute != Screen.Player.route) &&
            (currentRoute != Screen.Login.route) &&
            (currentRoute != Screen.Splash.route)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Content Flow
        NavGraph(
            navController = navController,
            playerViewModel = playerViewModel,
            mainViewModel = mainViewModel,
            songColor = songColor,
            modifier = Modifier.fillMaxSize(),
            onPlaySongs = { songs, index ->
                playerViewModel.playSongs(songs, index)
            },
            onSongOptionsClick = { song, isLiked ->
                selectedSongForSheet = song
                isSongAlreadyLiked = isLiked
                showBottomSheet = true
            }
        )

        // Overlay Elements (Mini Player & Navigation)
        if ((isLoggedIn == true) && (isMainScreen || showMiniPlayer)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    Crossfade(
                        targetState = currentSongId,
                        animationSpec = tween(400),
                        label = "miniPlayerTransition"
                    ) { songId ->
                        val displayedSong = playerState.queue.find { it.id == songId } ?: currentSong
                        MiniPlayerBar(
                            song = displayedSong,
                            isPlaying = playerState.isPlaying,
                            progress = playerState.progress,
                            songColor = songColor,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.nextSong() },
                            onDismiss = { playerViewModel.stopMusic() },
                            onClick = {
                                if (currentRoute != Screen.Player.route) {
                                    navController.navigate(Screen.Player.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }

                if (isMainScreen) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            if (route != currentRoute) {
                                navController.navigate(route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                } else {
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }

        // ✅ FIXED: SHARED BOTTOM SHEET WITH RECOMPOSITION AND ONLINE SYNCHRONIZATION
        if (showBottomSheet && selectedSongForSheet != null) {
            SongOptionsBottomSheet(
                song = selectedSongForSheet?.copy(isFavorite = isSongAlreadyLiked),
                onDismissRequest = { showBottomSheet = false },

                onFavoriteClick = { song ->
                    playerViewModel.toggleFavorite(song)
                    isSongAlreadyLiked = !isSongAlreadyLiked
                    val msg = if (isSongAlreadyLiked) "Added to Liked Songs"
                    else "Removed from Favorites"
                    Toast.makeText(context, msg,
                        Toast.LENGTH_SHORT).show()
                },
                onAddToPlaylistClick = { songId ->
                    songIdForPlaylist = songId
                    showPlaylistDialog = true
                },
                onDownloadClick = { song ->
                    Toast.makeText(context, "Download started: ${song.title}", Toast.LENGTH_SHORT).show()
                    com.musicstream.app.worker.AudioDownloadWorker.enqueue(context, song)
                },
                onDeleteDownloadClick = null,
                onRemoveFromPlaylistClick = null,
                onShareClick = { songId ->
                    try {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT,
                                "Check out this song on MusicStream: ${selectedSongForSheet?.title} by ${selectedSongForSheet?.artist}")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent,
                            "Share Track via"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                onGoToArtistClick = { artistName ->
                    navController.navigate(Screen.Artists.route) {
                        launchSingleTop = true
                    }
                }
            )
        }

        // ✅ FIXED: REAL-TIME PLAYLIST SELECTION WIDGET WITH FULL OBJECT INJECTION
        if (showPlaylistDialog && selectedSongForSheet != null) {
            AlertDialog(
                onDismissRequest = { showPlaylistDialog = false },
                title = { Text(text = "Select Playlist", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    if (playerState.playlists.isEmpty()) {
                        Text(text = "No playlists found.")
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp)) {
                            items(playerState.playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedSongForSheet?.let { song ->
                                                // Yahan confirm karein ki ViewModel function (String, Song) leta hai
                                                playerViewModel.addSongToPlaylist(playlist.id, song)
                                            }
                                            showPlaylistDialog = false
                                            Toast.makeText(context, "Added to ${playlist.name}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(text = playlist.name,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPlaylistDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }}
}