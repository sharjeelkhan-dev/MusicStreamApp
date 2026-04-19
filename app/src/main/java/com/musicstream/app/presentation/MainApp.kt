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
import com.musicstream.app.navigation.NavGraph
import com.musicstream.app.navigation.Screen
import com.musicstream.app.presentation.components.BottomNavBar
import com.musicstream.app.presentation.player.MiniPlayerBar
import com.musicstream.app.presentation.player.PlayerViewModel
import com.musicstream.app.ui.theme.DarkBackground

@Composable
fun MainApp(
    playerViewModel: PlayerViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val playerState by playerViewModel.uiState.collectAsStateWithLifecycle()

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Search.route,
        Screen.Library.route,
        Screen.Profile.route
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Navigation host
        NavGraph(
            navController = navController,
            playerViewModel = playerViewModel,
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    bottom = if (showBottomBar) {
                        if (playerState.currentSong != null) 130.dp else 72.dp
                    } else 0.dp
                ),
            onSongClick = { song ->
                playerViewModel.playSong(song)
            }
        )

        // Mini Player + Bottom Nav
        if (showBottomBar) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Mini player
                AnimatedVisibility(
                    visible = playerState.currentSong != null,
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
