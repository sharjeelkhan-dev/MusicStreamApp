package com.musicstream.app.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import com.musicstream.app.navigation.NavGraph
import com.musicstream.app.navigation.Screen
import com.musicstream.app.presentation.components.BottomNavBar
import com.musicstream.app.presentation.player.MiniPlayerBar
import com.musicstream.app.presentation.player.PlayerViewModel

@Composable
fun MainApp(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    val isMainScreen = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Profile.route
    )
    val showMiniPlayer = playerState.currentSong != null && currentRoute != Screen.Player.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (isMainScreen || showMiniPlayer) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Mini player
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
                                onClick = { navController.navigate(Screen.Player.route) }
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
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        // Navigation host
        NavGraph(
            navController = navController,
            playerViewModel = playerViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            onSongClick = { song ->
                playerViewModel.playSong(song)
            }
        )
    }
}
