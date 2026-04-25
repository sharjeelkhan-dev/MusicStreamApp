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
import com.musicstream.app.presentation.favorites.FavoritesScreen
import com.musicstream.app.presentation.trending.TrendingScreen
import com.musicstream.app.presentation.auth.AuthScreen
import com.musicstream.app.presentation.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    onSongClick: (Song) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onSongClick = onSongClick,
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onTrendingSeeAllClick = { navController.navigate(Screen.Trending.route) },
                onRecentlyPlayedSeeAllClick = { navController.navigate(Screen.RecentlyPlayed.route) },
                onYourCollectionsSeeAllClick = { 
                    navController.navigate(Screen.Favorites.route)
                },
                onGoToArtist = { artistName ->
                    navController.navigate(Screen.Search.route) // Reuse search for now
                }
            )
        }
        composable(Screen.Trending.route) {
            TrendingScreen(
                onSongClick = onSongClick,
                onBackClick = { navController.popBackStack() }
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
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onSongClick = onSongClick,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onSongClick = onSongClick,
                onGoToArtist = { artistName ->
                    navController.navigate(Screen.Search.route) // Reuse search for now
                }
            )
        }
        composable(Screen.Library.route) {
            LibraryScreen(
                onSongClick = onSongClick,
                onGoToArtist = { artistName ->
                    navController.navigate(Screen.Search.route) // Reuse search for now
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen()
        }
        composable(Screen.Login.route) {
            AuthScreen(
                onLoginClick = { email, password ->
                    // Handle login
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = { name, email, password ->
                    // Handle sign up
                }
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = playerViewModel,
                onBackClick = { navController.popBackStack() },
                onGoToArtist = { artistName ->
                    navController.navigate(Screen.Search.route) // Reuse search for now
                },
                onGoToAlbum = { albumId ->
                    // For now, navigate to search with album name or a placeholder
                    navController.navigate(Screen.Search.route)
                }
            )
        }
    }
}
