package app.aegis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.aegis.ui.navigation.*
import app.aegis.ui.theme.*
import app.aegis.ui.theme.AegisTypography
import org.jetbrains.compose.resources.stringResource

/**
 * TabsScreen - Container for bottom navigation tabs with nested NavHost
 */
@Composable
fun TabsScreen(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onNavigateToSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onViewFullReport: () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val tabsNavController = rememberNavController()
    val navBackStackEntry by tabsNavController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            NavigationBar(
                containerColor = colors.surface,
                contentColor = colors.onSurface,
                tonalElevation = 0.dp
            ) {
                bottomNavItems.forEach { item ->
                    val selected =
                        currentDestination?.hierarchy?.any {
                            it.hasRoute(item.route::class)
                        } == true

                    val label = stringResource(item.labelRes)

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            tabsNavController.navigate(item.route) {
                                popUpTo(tabsNavController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = label,
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = AegisTypography.labelSmall,
                            )
                        },
                        colors =
                            NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.primary,
                                selectedTextColor = colors.primary,
                                unselectedIconColor = colors.onSurfaceVariant,
                                unselectedTextColor = colors.onSurfaceVariant,
                                indicatorColor = colors.primary.copy(alpha = 0.1f),
                            ),
                    )
                }
            }
        },
    ) { _ ->
        // Nested NavHost for tabs - no padding needed, screens handle their own scrolling
        NavHost(
            navController = tabsNavController,
            startDestination = DashboardRoute,
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(colors.background)
                    .padding(bottom = 64.dp),
        ) {
            composable<DashboardRoute> {
                DashboardScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onSettingsClick = onNavigateToSettings,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    onViewAllActivity = {
                        tabsNavController.navigate(ActivityRoute) {
                            popUpTo(tabsNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable<ActivityRoute> {
                ActivityLogScreen()
            }

            composable<ProfileRoute> {
                val isProtected = hasOverlayPermission && hasAccessibilityPermission
                ProfileScreen(
                    isProtected = isProtected,
                    onSettingsClick = onNavigateToSettings,
                    onViewReport = onViewFullReport,
                )
            }
        }
    }
}
