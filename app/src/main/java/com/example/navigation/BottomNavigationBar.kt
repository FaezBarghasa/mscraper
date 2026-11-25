package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    object Discovery : Screen("discovery", "Home", Icons.Filled.Home)
    object Search : Screen("search", "Search", Icons.Filled.Search)
    object Library : Screen("library", "Library", Icons.Filled.LibraryMusic)
    object Favorites : Screen("favorites", "Favorites", Icons.Filled.Favorite)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
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

/**
 * Q4 2026 Floating Frosted Capsule Bottom Navigation Bar
 * Features:
 * - Floating capsule pill above Android gesture / navigation insets
 * - Frosted blur background with subtle hairline white highlight
 * - Active icon pill indicator with spring accent transitions
 */
@Composable
fun BottomNavigationBar(navController: NavController, accentColor: Color = PrimaryAccent) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (bottomNavItems.any { it.route == currentRoute }) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            NavigationBar(
                modifier = Modifier
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black.copy(alpha = 0.5f),
                        spotColor = Color.Black.copy(alpha = 0.8f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, HairlineBorder, RoundedCornerShape(28.dp)),
                containerColor = SurfaceElevated.copy(alpha = 0.94f),
                tonalElevation = 0.dp
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
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextTertiary,
                            indicatorColor = accentColor.copy(alpha = 0.14f)
                        )
                    )
                }
            }
        }
    }
}
