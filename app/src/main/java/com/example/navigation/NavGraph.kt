package com.example.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.screens.*
import com.example.ui.components.MiniPlayer
import com.example.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen() {
    val navController = rememberNavController()
    val viewModel: MusicViewModel = viewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isBottomBarVisible = bottomNavItems.any { it.route == currentRoute }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF070707),
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp),
                modifier = Modifier.width(300.dp)
            ) {
                SidebarContent(
                    viewModel = viewModel,
                    navController = navController,
                    onCloseDrawer = { scope.launch { drawerState.close() } }
                )
            }
        },
        gesturesEnabled = true
    ) {
        Scaffold(
            bottomBar = {
                Column {
                    MiniPlayer(
                        viewModel = viewModel,
                        onNavigateToNowPlaying = { navController.navigate(Screen.NowPlaying.route) },
                        isVisible = isBottomBarVisible
                    )
                    BottomNavigationBar(navController)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Discovery.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Discovery.route) { 
                    DiscoveryScreen(
                        navController = navController, 
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    ) 
                }
                composable(Screen.Search.route) { 
                    SearchScreen(
                        navController = navController, 
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    ) 
                }
                composable(Screen.Library.route) { 
                    LibraryScreen(
                        navController = navController, 
                        viewModel = viewModel,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    ) 
                }
                composable(Screen.Favorites.route) { FavoritesScreen(navController, viewModel) }
                composable(Screen.Settings.route) { SettingsScreen(navController, viewModel) }
                composable(Screen.NowPlaying.route) { NowPlayingScreen(navController, viewModel) }
                composable(Screen.Equalizer.route) { EqualizerScreen(viewModel) }
                composable(Screen.Playlist.route) { PlaylistScreen(navController, viewModel) }
                composable(Screen.Artist.route) { ArtistScreen() }
                composable(Screen.Album.route) { AlbumScreen() }
                composable(Screen.Genres.route) { GenresScreen() }
                composable(Screen.CoreDeck.route) { CoreDeckScreen(navController, viewModel) }
                composable("custom_playlist") { CustomPlaylistScreen(navController, viewModel) }
            }
        }
    }
}
