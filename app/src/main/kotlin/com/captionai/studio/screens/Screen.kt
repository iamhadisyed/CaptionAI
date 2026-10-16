package com.captionai.studio.screens

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Result : Screen("result")
    object Trending : Screen("trending")
    object Favorites : Screen("favorites")
    object History : Screen("history")
    object Settings : Screen("settings")
}
