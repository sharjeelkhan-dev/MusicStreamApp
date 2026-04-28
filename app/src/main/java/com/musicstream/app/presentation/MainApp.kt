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
