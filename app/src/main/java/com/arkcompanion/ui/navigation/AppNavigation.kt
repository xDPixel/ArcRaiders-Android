package com.arkcompanion.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arkcompanion.ui.screens.ArcsScreen
import com.arkcompanion.ui.screens.ArcsViewModel
import com.arkcompanion.ui.screens.EventsScheduleScreen
import com.arkcompanion.ui.screens.EventsScheduleViewModel
import com.arkcompanion.ui.screens.HideoutScreen
import com.arkcompanion.ui.screens.ItemsScreen
import com.arkcompanion.ui.screens.ItemsViewModel
import com.arkcompanion.ui.screens.ItemDetailScreen
import com.arkcompanion.ui.screens.ArcDetailScreen
import com.arkcompanion.ui.screens.MoreScreen
import com.arkcompanion.ui.screens.QuestDetailScreen
import com.arkcompanion.ui.screens.SettingsScreen
import com.arkcompanion.ui.screens.SettingsViewModel
import com.arkcompanion.ui.screens.TraderDetailScreen
import com.arkcompanion.ui.screens.TradersScreen
import com.arkcompanion.ui.screens.TradersViewModel

import androidx.compose.material.icons.filled.ShoppingCart

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Items : Screen("items", "Items", Icons.Default.List)
    object Arcs : Screen("arcs", "Arcs", Icons.Default.Home)
    object Hideout : Screen("hideout", "Hideout", Icons.Default.Build)
    object More : Screen("more", "More", Icons.Default.MoreVert)
    object EventsSchedule : Screen("events_schedule", "Events Schedule", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Build)
    
    object TraderDetail : Screen("trader_detail/{traderName}", "Trader Detail", Icons.Default.ShoppingCart) {
        fun createRoute(traderName: String) = "trader_detail/$traderName"
    }
    
    object QuestDetail : Screen("quest_detail/{questId}", "Quest Detail", Icons.Default.List) {
        fun createRoute(questId: String) = "quest_detail/$questId"
    }
    
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
    // Scope the ViewModels to the activity/navhost level so data is never destroyed on tab switch
    val itemsViewModel: ItemsViewModel = viewModel()
    val arcsViewModel: ArcsViewModel = viewModel()
    val tradersViewModel: TradersViewModel = viewModel()
    val eventsScheduleViewModel: EventsScheduleViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()

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
                ItemsScreen(
                    onItemClick = { itemId -> 
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    viewModel = itemsViewModel
                ) 
            }
            composable("traders") {
                TradersScreen(
                    onTraderClick = { traderName ->
                        navController.navigate(Screen.TraderDetail.createRoute(traderName))
                    },
                    viewModel = tradersViewModel
                )
            }
            composable(
                route = Screen.TraderDetail.route,
                arguments = listOf(navArgument("traderName") { type = NavType.StringType })
            ) { backStackEntry ->
                val traderName = backStackEntry.arguments?.getString("traderName") ?: ""
                TraderDetailScreen(
                    traderName = traderName,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    onQuestClick = { questId ->
                        navController.navigate(Screen.QuestDetail.createRoute(questId))
                    },
                    viewModel = tradersViewModel
                )
            }
            composable(
                route = Screen.QuestDetail.route,
                arguments = listOf(navArgument("questId") { type = NavType.StringType })
            ) { backStackEntry ->
                val questId = backStackEntry.arguments?.getString("questId") ?: ""
                QuestDetailScreen(
                    questId = questId,
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId ->
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    },
                    viewModel = tradersViewModel
                )
            }
            composable(Screen.Arcs.route) { 
                ArcsScreen(
                    onArcClick = { arcId ->
                        navController.navigate(Screen.ArcDetail.createRoute(arcId))
                    },
                    viewModel = arcsViewModel
                ) 
            }
            composable(Screen.Hideout.route) { 
                HideoutScreen(
                    onItemClick = { itemId -> 
                        navController.navigate(Screen.ItemDetail.createRoute(itemId))
                    }
                ) 
            }
            composable(Screen.More.route) { MoreScreen(navController) }
            composable(Screen.EventsSchedule.route) {
                EventsScheduleScreen(
                    onBackClick = { navController.popBackStack() },
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    viewModel = eventsScheduleViewModel
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBackClick = { navController.popBackStack() },
                    viewModel = settingsViewModel
                )
            }
            composable(
                route = Screen.ItemDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.StringType })
            ) { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                ItemDetailScreen(
                    itemId = itemId, 
                    onBackClick = { navController.popBackStack() },
                    viewModel = itemsViewModel
                )
            }
            composable(
                route = Screen.ArcDetail.route,
                arguments = listOf(navArgument("arcId") { type = NavType.StringType })
            ) { backStackEntry ->
                val arcId = backStackEntry.arguments?.getString("arcId") ?: ""
                ArcDetailScreen(
                    arcId = arcId, 
                    onBackClick = { navController.popBackStack() },
                    onItemClick = { itemId -> navController.navigate(Screen.ItemDetail.createRoute(itemId)) },
                    viewModel = arcsViewModel
                )
            }
        }
    }
}
