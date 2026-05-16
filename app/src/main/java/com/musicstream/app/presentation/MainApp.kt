package com.musicstream.app.presentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.musicstream.app.presentation.player.MiniPlayerBar
import com.musicstream.app.presentation.player.PlayerViewModel
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.palette.graphics.Palette
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.musicstream.app.ui.theme.Gradients
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainApp(
    playerViewModel: PlayerViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    // 1. Persistent Reactive Cache
    val colorCache = remember { mutableStateMapOf<String, Color>() }

    val currentSong = playerState.currentSong
    val currentSongId = currentSong?.id ?: ""

    // 2. Main Color Logic (The "Smooth-Refine" Fix)
    // We keep track of the PREVIOUS song's color to show as fallback
    var lastValidColor by remember { mutableStateOf(Gradients.songThumbColors[0]) }
    
    // Calculate color strictly: Cache > Previous Song Color (No random fallback splash)
    val songColor by animateColorAsState(
        targetValue = colorCache[currentSongId] ?: lastValidColor,
        animationSpec = tween(600),
        label = "mainColorAnimation"
    )

    // Update lastValidColor only when a high-quality extraction completes
    LaunchedEffect(currentSongId) {
        colorCache[currentSongId]?.let { lastValidColor = it }
    }

    // 3. Background Extraction Logic
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

    val isMainScreen = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.MediaTools.route,
        Screen.Library.route,
        Screen.Artists.route
    )

    val showMiniPlayer = (isLoggedIn == true) &&
            (playerState.currentSong != null) &&
            (currentRoute != Screen.Player.route) &&
            (currentRoute != Screen.Login.route) &&
            (currentRoute != Screen.Splash.route)

    // STATIC ROOT Box to prevent Scroll Reset
    // We removed the Crossfade from around NavGraph because it destroys screen state (scrolling).
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main App Content
        NavGraph(
            navController = navController,
            playerViewModel = playerViewModel,
            mainViewModel = mainViewModel,
            songColor = songColor,
            modifier = Modifier.fillMaxSize(),
            onPlaySongs = { songs, index ->
                playerViewModel.playSongs(songs, index)
            },
        )

        // Overlay UI logic (MiniPlayer and BottomNav)
        if ((isLoggedIn == true) && (isMainScreen || showMiniPlayer)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                // Mini Player with Animation
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut(),
                ) {
                    // Use a nested Crossfade JUST for the Mini Player metadata to keep its transition clean
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

                // Bottom Navigation
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
    }
}
