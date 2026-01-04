package com.babymonitor.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.babymonitor.ui.screens.camera.CameraScreen
import com.babymonitor.ui.screens.home.HomeScreen
import com.babymonitor.ui.screens.settings.SettingsScreen
import com.babymonitor.ui.screens.viewer.ViewerScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Camera : Screen("camera")
    object Viewer : Screen("viewer")
    object Settings : Screen("settings")
}

@Composable
fun BabyMonitorNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToViewer = { navController.navigate(Screen.Viewer.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Viewer.route) {
            ViewerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
