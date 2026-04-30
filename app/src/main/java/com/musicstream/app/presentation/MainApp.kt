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

@Composable
fun MainApp(
    isLoggedInParam: Boolean, // Name changed to avoid conflict
    playerViewModel: PlayerViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()
    val isLoggedIn by mainViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Dynamic color for MiniPlayer
    var songColor by remember { mutableStateOf(AccentPurple) }
    
    LaunchedEffect(playerState.currentSong?.coverUrl) {
        playerState.currentSong?.coverUrl?.let { url ->
            if (url.isNotEmpty()) {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .allowHardware(false)
                    .build()
                
                val result = loader.execute(request).drawable
                result?.let { drawable ->
                    val bitmap = drawable.toBitmap()
                    Palette.from(bitmap).generate { palette ->
                        palette?.vibrantSwatch?.rgb?.let { color ->
                            songColor = Color(color)
                        } ?: palette?.dominantSwatch?.rgb?.let { color ->
                            songColor = Color(color)
                        }
                    }
                }
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
        Screen.Profile.route
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
            modifier = Modifier.fillMaxSize(),
            onSongClick = { song ->
                playerViewModel.playSong(song)
            },
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
