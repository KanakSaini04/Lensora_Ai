package com.lensora.ai.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lensora.ai.ui.camera.CameraScreen
import com.lensora.ai.ui.gallery.QuickViewScreen
import com.lensora.ai.ui.probe.ProbeLabScreen
import com.lensora.ai.ui.settings.SettingsScreen
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.PitchBlackBackground
import com.lensora.ai.ui.theme.RoyalBlue

sealed class NavScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    object Camera : NavScreen("camera", "Camera", Icons.Outlined.PhotoCamera)
    object Studio : NavScreen("studio", "Studio", Icons.Outlined.AutoFixHigh)
    object Gallery : NavScreen("gallery", "Gallery", Icons.Outlined.PhotoLibrary)
    object Vibes : NavScreen("vibes", "Vibes", Icons.Outlined.Style)
    object Settings : NavScreen("settings", "Settings", Icons.Outlined.Settings)

    companion object {
        val items = listOf(Camera, Studio, Gallery, Vibes, Settings)
    }
}

const val ROUTE_PROBE_LAB = "probe_lab"
const val ROUTE_QUICK_VIEW = "quick_view"

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevelRoute = NavScreen.items.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (isTopLevelRoute) {
                NavigationBar(
                    containerColor = CharcoalSurface,
                    contentColor = ColorWhite,
                ) {
                    NavScreen.items.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RoyalBlue,
                                selectedTextColor = RoyalBlue,
                                indicatorColor = CharcoalSurface,
                                unselectedIconColor = ColorMutedGray,
                                unselectedTextColor = ColorMutedGray,
                            ),
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavScreen.Camera.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(NavScreen.Camera.route) {
                CameraScreen(
                    onOpenQuickView = { navController.navigate(ROUTE_QUICK_VIEW) }
                )
            }
            composable(NavScreen.Studio.route) { TabContent(title = "Studio Screen") }
            composable(NavScreen.Gallery.route) { TabContent(title = "Gallery Screen") }
            composable(NavScreen.Vibes.route) { TabContent(title = "Vibes Screen") }
            composable(NavScreen.Settings.route) {
                SettingsScreen(
                    onOpenProbeLab = { navController.navigate(ROUTE_PROBE_LAB) }
                )
            }
            composable(ROUTE_PROBE_LAB) {
                ProbeLabScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(ROUTE_QUICK_VIEW) {
                QuickViewScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun TabContent(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlackBackground),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = ColorWhite,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    com.lensora.ai.ui.theme.Lensora_AiTheme {
        AppNavigation()
    }
}
