package com.musicstream.app.navigation

sealed class Screen(val route: String) {
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
    data object Album : Screen("album/{albumId}") {
        fun createRoute(albumId: String) = "album/$albumId"
    }
}
