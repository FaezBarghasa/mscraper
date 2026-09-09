package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.ui.screens.*

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Playlists : Screen("playlists")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Library.route) { LibraryScreen() }
        composable(Screen.Playlists.route) { PlaylistScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
    }
}
