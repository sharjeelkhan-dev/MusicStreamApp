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
import com.musicstream.app.domain.model.User
import com.musicstream.app.presentation.MainViewModel
import com.musicstream.app.presentation.notifications.NotificationScreen
import com.musicstream.app.presentation.recently_played.RecentlyPlayedScreen
import com.musicstream.app.presentation.favorites.FavoritesScreen
import com.musicstream.app.presentation.trending.TrendingScreen
import com.musicstream.app.presentation.auth.AuthScreen
import com.musicstream.app.presentation.splash.SplashScreen
import com.musicstream.app.presentation.library.DownloadsScreen
import com.musicstream.app.presentation.library.PlaylistScreen
import com.musicstream.app.presentation.artists.ArtistsScreen
import com.musicstream.app.presentation.media_tools.MediaToolsScreen
import com.musicstream.app.presentation.media_tools.EqualizerScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    mainViewModel: MainViewModel,
    onSongOptionsClick: (Song, Boolean) -> Unit, // Sahi callback registered hai
    songColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onPlaySongs: (List<Song>, Int) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isLoggedIn ->
                    val destination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // 🚨 FIXED CONFIGURATION FOR HOME SCREEN:
        composable(Screen.Home.route) {
            HomeScreen(
                onPlaySongs = onPlaySongs,
                onNotificationClick = { navController.navigate(Screen.Notifications.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onToolsClick = { navController.navigate(Screen.MediaTools.route) },
                onTrendingSeeAllClick = { navController.navigate(Screen.Trending.route) },
                onRecentlyPlayedSeeAllClick = { navController.navigate(Screen.RecentlyPlayed.route) },
                onPlaylistClick = { playlist ->
                    navController.navigate(Screen.Playlist.createRoute(playlist.id))
                },
                onDownloadsClick = {
                    navController.navigate(Screen.Library.createRoute(tab = "downloads"))
                },
                onGoToArtist = { _ ->
                    navController.navigate(Screen.Search.route)
                },
                onGoToPlayer = {
                    navController.navigate(Screen.Player.route)
                },
                // ✅ CONFIGURATION LINK FIXED: Is line ke jodne se dialogue sheet event pass hona start ho jayega.
                onSongOptionsClick = onSongOptionsClick
            )
        }

        composable(Screen.Trending.route) {
            TrendingScreen(
                onPlaySongs = onPlaySongs,
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
                onPlaySongs = onPlaySongs,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onPlaySongs = onPlaySongs,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onPlaySongs = onPlaySongs,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Playlist.route) {
            PlaylistScreen(
                onBackClick = { navController.popBackStack() },
                onPlaySongs = onPlaySongs
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onPlaySongs = onPlaySongs,
                onGoToArtist = { _ ->
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(
            route = Screen.Library.route,
            arguments = listOf(
                androidx.navigation.navArgument("tab") {
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            LibraryScreen(
                onPlaySongs = onPlaySongs,
                onPlaylistClick = { playlist ->
                    navController.navigate(Screen.Playlist.createRoute(playlist.id))
                },
                onGoToArtist = { _ ->
                    navController.navigate(Screen.Search.route)
                }
            )
        }

        composable(Screen.Artists.route) {
            ArtistsScreen(
                onArtistClick = { _ ->
                    navController.navigate(Screen.Search.route)
                },
                onPlaySongs = onPlaySongs,
                onBackClick = { navController.popBackStack() },
                onSongOptionsClick = onSongOptionsClick
            )
        }

        composable(Screen.MediaTools.route) {
            MediaToolsScreen(
                onPlaySongs = onPlaySongs,
                onEqualizerClick = { navController.navigate(Screen.Equalizer.route) },
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Equalizer.route) {
            EqualizerScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
        composable(Screen.Login.route) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = playerViewModel,
                songColor = songColor,
                onBackClick = { navController.popBackStack() },
                onGoToArtist = { _ ->
                    navController.navigate(Screen.Search.route)
                },
                onGoToAlbum = { _ ->
                    navController.navigate(Screen.Search.route)
                }
            )
        }
    }
}