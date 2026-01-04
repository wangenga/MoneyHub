package com.finance.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.finance.app.ui.analytics.AnalyticsScreen
import com.finance.app.ui.category.AddEditCategoryScreen
import com.finance.app.ui.category.CategoryManagementScreen
import com.finance.app.ui.settings.SettingsScreen
import com.finance.app.ui.transaction.AddEditTransactionScreen
import com.finance.app.ui.transaction.TransactionListScreen

/**
 * Main navigation component for the authenticated app
 */
@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Handle back navigation for main screens
    BackHandler(enabled = currentDestination?.route in BottomNavigationItem.items.map { it.route }) {
        // If we're on a main screen, don't handle back - let the system handle it (exit app)
        // This prevents getting stuck in the app
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Only show bottom navigation for main screens
            if (currentDestination?.route in BottomNavigationItem.items.map { it.route }) {
                NavigationBar {
                    BottomNavigationItem.items.forEach { item ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) {
                                        item.selectedIcon
                                    } else {
                                        item.unselectedIcon
                                    },
                                    contentDescription = null // NavigationBarItem handles this
                                )
                            },
                            label = { Text(item.title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(item.route) {
                                    // Pop up to the start destination of the graph to
                                    // avoid building up a large stack of destinations
                                    // on the back stack as users select items
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    // Avoid multiple copies of the same destination when
                                    // reselecting the same item
                                    launchSingleTop = true
                                    // Restore state when reselecting a previously selected item
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription = if (isSelected) {
                                    "${item.title}, selected"
                                } else {
                                    "Navigate to ${item.title}"
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavigationRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main bottom navigation screens
            composable(NavigationRoutes.HOME) {
                TransactionListScreen(
                    onAddTransaction = {
                        navController.navigate(NavigationRoutes.ADD_TRANSACTION)
                    },
                    onEditTransaction = { transactionId ->
                        navController.navigate(NavigationRoutes.editTransaction(transactionId))
                    }
                )
            }

            composable(NavigationRoutes.REPORTS,
                deepLinks = listOf(navDeepLink { uriPattern = NavigationRoutes.DEEP_LINK_REPORTS })
            ) {
                AnalyticsScreen()
            }

            composable(NavigationRoutes.SETTINGS) {
                SettingsScreen()
            }

            // Transaction screens
            composable(NavigationRoutes.ADD_TRANSACTION,
                deepLinks = listOf(navDeepLink { uriPattern = NavigationRoutes.DEEP_LINK_ADD_TRANSACTION })
            ) {
                AddEditTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(NavigationRoutes.EDIT_TRANSACTION,
                deepLinks = listOf(navDeepLink { uriPattern = NavigationRoutes.DEEP_LINK_TRANSACTION })
            ) {
                AddEditTransactionScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            // Category screens
            composable(NavigationRoutes.CATEGORY_MANAGEMENT) {
                CategoryManagementScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAddCategory = {
                        navController.navigate(NavigationRoutes.ADD_CATEGORY)
                    },
                    onEditCategory = { categoryId ->
                        navController.navigate(NavigationRoutes.editCategory(categoryId))
                    }
                )
            }

            composable(NavigationRoutes.ADD_CATEGORY) {
                AddEditCategoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(NavigationRoutes.EDIT_CATEGORY) {
                AddEditCategoryScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}