package com.example.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * Single Canonical Main Destinations for PrepOS Navigation
 */
sealed class MainDestination(val route: String) {
    data object Home : MainDestination("home")
    data object Study : MainDestination("study")
    data object Plan : MainDestination("plan")
    data object Practice : MainDestination("practice")
    data object AskAI : MainDestination("ask_ai")
    data object Settings : MainDestination("settings")

    companion object {
        fun navigateTo(navController: NavController, destination: MainDestination) {
            navController.navigate(destination.route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        }

        fun navigateToRoute(navController: NavController, route: String) {
            navController.navigate(route) {
                launchSingleTop = true
                restoreState = true
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
        }
    }
}
