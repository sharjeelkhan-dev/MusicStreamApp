package com.musicstream.app.presentation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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

// 1. UI State Model for Theme Sync
data class ThemeState(
    val songId: String,
    val color: Color
)

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

    // Persistent color cache
    val colorCache = remember { mutableStateMapOf<String, Color>() }

    // 2. Main Implementation (The "Sync" Logic)
    // This state object holds BOTH ID and Color to ensure they never desync
    var themeState by remember {
        mutableStateOf(ThemeState("", Gradients.songThumbColors[0]))
    }

    // Sync Logic: Immediate update on song change
    LaunchedEffect(playerState.currentSong?.id) {
        val song = playerState.currentSong
        if (song == null) {
            themeState = ThemeState("", Gradients.songThumbColors[0])
            return@LaunchedEffect
        }

        // Set immediate color (Cached or matching fallback)
        val initialColor = colorCache[song.id] ?: Gradients.songThumbColors[song.gradientIndex % Gradients.songThumbColors.size]
        themeState = ThemeState(song.id, initialColor)
    }

    // Background Extraction Logic
    LaunchedEffect(playerState.currentSong?.id, isDark) {
        val song = playerState.currentSong ?: return@LaunchedEffect
        if (colorCache.containsKey(song.id)) return@LaunchedEffect

        if (song.coverUrl.isNotEmpty()) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(song.coverUrl)
                .allowHardware(enable = false)
                .build()

            try {
                val result = loader.execute(request).drawable
                result?.let { drawable ->
                    val bitmap = drawable.toBitmap(width = 128, height = 128)
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).generate()
                    }
                    
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
                        colorCache[song.id] = extracted

                        // Update themeState ONLY if we are still on the same song
                        if (themeState.songId == song.id) {
                            themeState = themeState.copy(color = extracted)
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
        Screen.Library.route,
        Screen.Artists.route
    )

    val showMiniPlayer = (isLoggedIn == true) &&
            (playerState.currentSong != null) &&
            (currentRoute != Screen.Player.route) &&
            (currentRoute != Screen.Login.route) &&
            (currentRoute != Screen.Splash.route)

    // Using Crossfade on the themeState object to ensure smooth, non-mixing transition
    Crossfade(
        targetState = themeState,
        animationSpec = tween(600),
        label = "songThemeTransition"
    ) { state ->
        val songColor = state.color
        // IMPORTANT: Use the song from the queue that matches this specific transition state
        // This ensures the Image, Title, and Color all crossfade together perfectly.
        val displayedSong = playerState.queue.find { it.id == state.songId } ?: playerState.currentSong
        
        // SOLID BACKGROUND FIX: Add a solid background to prevent transparency mixing
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White)
        ) {
            NavGraph(
                navController = navController,
                playerViewModel = playerViewModel,
                mainViewModel = mainViewModel,
                songColor = songColor,
                modifier = Modifier.fillMaxSize(),
                onPlaySongs = { songs, index ->
                    playerViewModel.playSongs(songs, index)
                }
            )

            // Overlay UI logic
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
                        MiniPlayerBar(
                            song = displayedSong,
                            isPlaying = playerState.isPlaying,
                            progress = playerState.progress,
                            songColor = songColor,
                            onPlayPauseClick = { playerViewModel.togglePlayPause() },
                            onNextClick = { playerViewModel.nextSong() },
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
}
