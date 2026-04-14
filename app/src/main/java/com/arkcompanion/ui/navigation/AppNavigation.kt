package com.arkcompanion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import com.arkcompanion.ui.screens.ArcsScreen
import com.arkcompanion.ui.screens.HideoutScreen
import com.arkcompanion.ui.screens.ItemsScreen
import com.arkcompanion.ui.screens.ItemDetailScreen
import com.arkcompanion.ui.screens.ArcDetailScreen
import com.arkcompanion.ui.screens.MoreScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Items : Screen("items", "Items", Icons.Default.List)
    object Arcs : Screen("arcs", "Arcs", Icons.Default.Home)
    object Hideout : Screen("hideout", "Hideout", Icons.Default.Build)
    object More : Screen("more", "More", Icons.Default.MoreVert)
    
    object ItemDetail : Screen("item_detail/{itemId}", "Item Detail", Icons.Default.List) {
        fun createRoute(itemId: String) = "item_detail/$itemId"
    }
    object ArcDetail : Screen("arc_detail/{arcId}", "ARC Detail", Icons.Default.Home) {
        fun createRoute(arcId: String) = "arc_detail/$arcId"
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        Screen.Items,
        Screen.Arcs,
        Screen.Hideout,
        Screen.More
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(navController, startDestination = Screen.Items.route, Modifier.padding(innerPadding)) {
            composable(Screen.Items.route) { 
                ItemsScreen(onItemClick = { itemId -> 
                    navController.navigate(Screen.ItemDetail.createRoute(itemId))
                }) 
            }
            composable(Screen.Arcs.route) { 
                ArcsScreen(onArcClick = { arcId ->
                    navController.navigate(Screen.ArcDetail.createRoute(arcId))
                }) 
            }
            composable(Screen.Hideout.route) { HideoutScreen() }
            composable(Screen.More.route) { MoreScreen(navController) }
            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                ItemDetailScreen(itemId = itemId, onBackClick = { navController.popBackStack() })
            }
            composable(
                route = Screen.ArcDetail.route,
                arguments = listOf(navArgument("arcId") { type = NavType.StringType })
            ) { backStackEntry ->
                val arcId = backStackEntry.arguments?.getString("arcId") ?: ""
                ArcDetailScreen(arcId = arcId, onBackClick = { navController.popBackStack() })
            }
        }
    }
}
