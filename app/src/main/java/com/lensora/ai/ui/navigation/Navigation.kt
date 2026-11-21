package com.lensora.ai.ui.navigation

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lensora.ai.ui.camera.CameraScreen
import com.lensora.ai.ui.gallery.GalleryScreen
import com.lensora.ai.ui.gallery.QuickViewScreen
import com.lensora.ai.ui.probe.ProbeLabScreen
import com.lensora.ai.ui.settings.SettingsScreen
import com.lensora.ai.ui.studio.StudioScreen
import com.lensora.ai.ui.theme.CharcoalSurface
import com.lensora.ai.ui.theme.ColorMutedGray
import com.lensora.ai.ui.theme.ColorWhite
import com.lensora.ai.ui.theme.RoyalBlue
import com.lensora.ai.ui.vibes.VibesScreen

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

fun shouldShowBottomBarForRoute(route: String?): Boolean {
    val isTopLevelRoute = NavScreen.items.any { it.route == route }
    return isTopLevelRoute && route != NavScreen.Camera.route
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val haptic = LocalHapticFeedback.current

    val isTopLevelRoute = NavScreen.items.any { it.route == currentRoute }

    // Predictive back handler for nested screens & secondary tabs
    if (!isTopLevelRoute && currentRoute != null) {
        BackHandler {
            navController.popBackStack()
        }
    } else if (isTopLevelRoute && currentRoute != NavScreen.Camera.route) {
        BackHandler {
            navController.navigate(NavScreen.Camera.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBarForRoute(currentRoute)) {
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
                                    try {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    } catch (_: Exception) {}

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
            enterTransition = {
                slideInHorizontally(initialOffsetX = { 250 }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280))
            },
            exitTransition = {
                slideOutHorizontally(targetOffsetX = { -250 }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280))
            },
            popEnterTransition = {
                slideInHorizontally(initialOffsetX = { -250 }, animationSpec = tween(280)) + fadeIn(animationSpec = tween(280))
            },
            popExitTransition = {
                slideOutHorizontally(targetOffsetX = { 250 }, animationSpec = tween(280)) + fadeOut(animationSpec = tween(280))
            },
        ) {
            composable(NavScreen.Camera.route) {
                CameraScreen(
                    onOpenQuickView = { navController.navigate(ROUTE_QUICK_VIEW) },
                    onNavigateToStudio = {
                        navController.navigate(NavScreen.Studio.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(NavScreen.Studio.route) { StudioScreen() }
            composable(NavScreen.Gallery.route) {
                GalleryScreen(
                    onPhotoClick = { uri, filterName ->
                        val encodedUri = Uri.encode(uri.toString())
                        navController.navigate("$ROUTE_QUICK_VIEW?initialUri=$encodedUri&filter=$filterName")
                    },
                )
            }
            composable(NavScreen.Vibes.route) { VibesScreen() }
            composable(NavScreen.Settings.route) {
                SettingsScreen(
                    onOpenProbeLab = { navController.navigate(ROUTE_PROBE_LAB) },
                )
            }
            composable(ROUTE_PROBE_LAB) {
                ProbeLabScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = "$ROUTE_QUICK_VIEW?initialUri={initialUri}&filter={filter}",
                arguments = listOf(
                    navArgument("initialUri") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("filter") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val initialUri = backStackEntry.arguments?.getString("initialUri")
                val filter = backStackEntry.arguments?.getString("filter")
                QuickViewScreen(
                    initialUri = initialUri,
                    filterName = filter,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun AppNavigationPreview() {
    com.lensora.ai.ui.theme.Lensora_AiTheme {
        AppNavigation()
    }
}
