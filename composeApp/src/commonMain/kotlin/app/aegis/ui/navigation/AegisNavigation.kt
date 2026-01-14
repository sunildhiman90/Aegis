package app.aegis.ui.navigation

import aegis.composeapp.generated.resources.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource

/**
 * Type-safe navigation routes for Aegis app
 * Using serializable objects/data classes for type-safe navigation
 */

// === TOP-LEVEL DESTINATIONS ===
// Main tabs container
@Serializable
data object TabsRoute

// Non-tab destinations (full-screen)
@Serializable
data object SettingsRoute

@Serializable
data object OnboardingRoute

@Serializable
data object TrustedContactsRoute

@Serializable
data object FullReportRoute

// === TAB DESTINATIONS (nested inside TabsRoute) ===
@Serializable
data object DashboardRoute

@Serializable
data object ActivityRoute

@Serializable
data object ProfileRoute

/**
 * Bottom navigation item
 */
data class BottomNavItem<T : Any>(
    val route: T,
    val labelRes: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = DashboardRoute,
        labelRes = Res.string.nav_dashboard,
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    ),
    BottomNavItem(
        route = ActivityRoute,
        labelRes = Res.string.nav_activity,
        selectedIcon = Icons.AutoMirrored.Filled.List,
        unselectedIcon = Icons.AutoMirrored.Outlined.List
    ),
    BottomNavItem(
        route = ProfileRoute,
        labelRes = Res.string.nav_profile,
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )
)
