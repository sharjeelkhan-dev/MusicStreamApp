package com.musicstream.app.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Profile : Screen("profile")
    data object Player : Screen("player")
    data object Login : Screen("login")
    data object Notifications : Screen("notifications")
}
