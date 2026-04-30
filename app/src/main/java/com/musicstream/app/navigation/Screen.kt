package com.musicstream.app.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Profile : Screen("profile")
    data object Player : Screen("player")
    data object Login : Screen("login")
    data object Notifications : Screen("notifications")
    data object RecentlyPlayed : Screen("recently_played")
    data object Favorites : Screen("favorites")
    data object Trending : Screen("trending")
    data object Downloads : Screen("downloads")
    data object Playlist : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    data object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
}
