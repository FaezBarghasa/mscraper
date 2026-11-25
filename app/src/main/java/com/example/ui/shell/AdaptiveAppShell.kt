package com.example.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.navigation.Screen
import com.example.navigation.bottomNavItems
import com.example.ui.theme.*

enum class WindowType {
    COMPACT,    // Phones (< 600dp)
    MEDIUM,     // Foldables & Tablets (600dp..840dp)
    EXPANDED    // Desktop & Ultra-wide (>= 840dp)
}

@Composable
fun rememberWindowType(): WindowType {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 600 -> WindowType.COMPACT
        screenWidth < 840 -> WindowType.MEDIUM
        else -> WindowType.EXPANDED
    }
}

/**
 * Adaptive Multiplatform Shell for Android, Tablets, Foldables, and Desktop.
 * Adapts seamlessly between Compact (Floating Bottom Bar), Medium (Nav Rail),
 * and Expanded (3-Pane Spotify / Apple Music Desktop layout).
 */
@Composable
fun AdaptiveAppShell(
    navController: NavController,
    accentColor: Color = PrimaryAccent,
    content: @Composable (PaddingValues) -> Unit
) {
    val windowType = rememberWindowType()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    when (windowType) {
        WindowType.COMPACT -> {
            // Mobile layout handled by Scaffold with BottomNavigationBar
            content(PaddingValues(bottom = 80.dp))
        }
        WindowType.MEDIUM -> {
            // Foldable / Tablet Navigation Rail Layout
            Row(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
                NavigationRail(
                    containerColor = SurfaceElevated,
                    contentColor = TextPrimary,
                    modifier = Modifier
                        .fillMaxHeight()
                        .border(width = 1.dp, color = HairlineBorder)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationRailItem(
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
                            label = { Text(item.title) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = accentColor,
                                selectedTextColor = accentColor,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary,
                                indicatorColor = accentColor.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    content(PaddingValues(0.dp))
                }
            }
        }
        WindowType.EXPANDED -> {
            // Desktop 3-Pane Layout (Sidebar Navigation | Main Content Feed)
            Row(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
                // Left Desktop Sidebar
                Column(
                    modifier = Modifier
                        .width(240.dp)
                        .fillMaxHeight()
                        .background(SurfaceElevated)
                        .border(1.dp, HairlineBorder)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "M-SCRAPER",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 24.dp, start = 8.dp)
                    )

                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.08f) else Color.Transparent)
                                .clickable {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.startDestinationId)
                                        launchSingleTop = true
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = item.title,
                                    tint = if (isSelected) accentColor else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) TextPrimary else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                // Center Main Content Feed
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    content(PaddingValues(0.dp))
                }
            }
        }
    }
}
