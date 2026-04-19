package com.musicstream.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.home.HomeScreen
import com.musicstream.app.presentation.search.SearchScreen
import com.musicstream.app.presentation.library.LibraryScreen
import com.musicstream.app.presentation.profile.ProfileScreen
import com.musicstream.app.presentation.player.PlayerScreen
import com.musicstream.app.presentation.player.PlayerViewModel

import com.musicstream.app.presentation.notifications.NotificationScreen
import com.musicstream.app.presentation.recently_played.RecentlyPlayedScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onSongClick = onSongClick,
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onRecentlyPlayedSeeAllClick = { navController.navigate(Screen.RecentlyPlayed.route) }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.RecentlyPlayed.route) {
            RecentlyPlayedScreen(
                onSongClick = onSongClick,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(onSongClick = onSongClick)
        }
        composable(Screen.Library.route) {
            LibraryScreen(onSongClick = onSongClick)
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
