package com.musicstream.app.presentation
import androidx.compose.animation.*
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
import com.musicstream.app.ui.theme.AccentPurple
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun MainApp(
    // Name changed to avoid conflict
    playerViewModel: PlayerViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Persistent color cache to avoid re-extraction
    val colorCache = remember { mutableStateMapOf<String, Color>() }
    
    // The color we are currently displaying (and animating towards)
    var activeColor by remember { mutableStateOf(AccentPurple) }

    // Update activeColor from cache immediately when song changes
    LaunchedEffect(playerState.currentSong?.id) {
        val currentId = playerState.currentSong?.id
        if (currentId != null) {
            colorCache[currentId]?.let { activeColor = it }
        }
    }

    // Smooth color transition
    val songColor by animateColorAsState(
        targetValue = activeColor,
        animationSpec = tween(500),
        label = "miniPlayerColor"
    )
    
    LaunchedEffect(playerState.currentSong?.id) {
        val song = playerState.currentSong ?: return@LaunchedEffect
        
        // Only extract if not in cache
        if (!colorCache.containsKey(song.id) && song.coverUrl.isNotEmpty()) {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(song.coverUrl)
                .allowHardware(false)
                .build()
            
            try {
                val result = loader.execute(request).drawable
                result?.let { drawable ->
                    val bitmap = drawable.toBitmap(width = 128, height = 128)
                    Palette.from(bitmap).generate { palette ->
                        val color = palette?.vibrantSwatch?.rgb 
                            ?: palette?.lightVibrantSwatch?.rgb
                            ?: palette?.darkVibrantSwatch?.rgb
                            ?: palette?.dominantSwatch?.rgb
                        
                        if (color != null) {
                            val extracted = Color(color)
                            colorCache[song.id] = extracted
                        }
                    }
                }
            } catch (_: Exception) {
                // Fail gracefully
            }
        }
    }

    // Global navigation for Sign Out
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn == false && currentRoute != Screen.Splash.route && currentRoute != Screen.Login.route) {
            playerViewModel.pauseSong() // pauseSong function PlayerViewModel mein hona chahiye
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

    // FIX: Added check for Screen.Player.route to prevent double player
    val showMiniPlayer = isLoggedIn == true &&
            playerState.currentSong != null &&
            currentRoute != Screen.Player.route && // Player screen par hide karein
            currentRoute != Screen.Login.route &&  // Login screen par hide karein
            currentRoute != Screen.Splash.route

    Box(modifier = Modifier.fillMaxSize()) {
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
        if (isLoggedIn == true && (isMainScreen || showMiniPlayer)) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Mini Player with Animation
                AnimatedVisibility(
                    visible = showMiniPlayer,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    MiniPlayerBar(
                        song = playerState.currentSong,
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
                    // Window insets for screens like Player (if mini player was hidden)
                    Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}
