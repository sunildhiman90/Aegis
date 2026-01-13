package app.aegis

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.aegis.ui.navigation.*
import app.aegis.ui.screens.*
import app.aegis.ui.theme.AegisTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Main Aegis App entry point
 * Uses nested navigation: Main NavHost -> TabsScreen (with nested tabs NavHost)
 */
@Composable
@Preview
fun App(
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {}
) {
    // Theme state - null means follow system
    var themeMode by remember { mutableStateOf<Boolean?>(null) }
    val useDarkTheme = themeMode ?: isSystemInDarkTheme()

    AegisTheme(darkTheme = useDarkTheme) {
        val colors = AegisTheme.colors
        val mainNavController = rememberNavController()

        // Permission states
        var hasOverlayPermission by remember { mutableStateOf(false) }
        var hasAccessibilityPermission by remember { mutableStateOf(false) }

        // Main NavHost - top-level navigation
        NavHost(
            navController = mainNavController,
            startDestination = TabsRoute,
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // Tabs container (has its own nested NavHost with bottom navigation)
            composable<TabsRoute> {
                TabsScreen(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onNavigateToSettings = { mainNavController.navigate(SettingsRoute) },
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
            }

            // Full-screen destinations (no bottom nav)
            composable<SettingsRoute> {
                SettingsScreen(
                    onBackClick = { mainNavController.popBackStack() },
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                    isDarkTheme = useDarkTheme,
                    onThemeChange = { isDark -> themeMode = isDark }
                )
            }

            composable<OnboardingRoute> {
                OnboardingScreen(
                    onSkip = { mainNavController.popBackStack() },
                    onComplete = { mainNavController.popBackStack() }
                )
            }
        }
    }
}