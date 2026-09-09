package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.HoloBg
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.TextGray

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Discovery : Screen("discovery", "HOME", Icons.Filled.Home)
    object Search : Screen("search", "SEARCH", Icons.Filled.Search)
    object Library : Screen("library", "LIBRARY", Icons.Filled.LibraryMusic)
    object Favorites : Screen("favorites", "FAVORITES", Icons.Filled.Favorite)
    object Settings : Screen("settings", "SETTINGS", Icons.Filled.Settings)
    object NowPlaying : Screen("now_playing", "Now Playing", null)
    object Equalizer : Screen("equalizer", "Equalizer", null)
    object Playlist : Screen("playlist", "Playlist", null)
    object Artist : Screen("artist", "Artist", null)
    object Album : Screen("album", "Album", null)
    object Genres : Screen("genres", "Genres", null)
    object CoreDeck : Screen("core_deck", "Core Deck", null)
}

val bottomNavItems = listOf(
    Screen.Discovery,
    Screen.Search,
    Screen.Library,
    Screen.Settings
)

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (bottomNavItems.any { it.route == currentRoute }) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            NavigationBar(
                modifier = Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(32.dp)),
                containerColor = HoloBg,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentRoute == item.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            item.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 10.sp,
                                color = if (isSelected) NeonMagenta else TextGray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonMagenta,
                            unselectedIconColor = TextGray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }
}
