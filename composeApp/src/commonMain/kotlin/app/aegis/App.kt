package app.aegis

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.aegis.data.settings.AppSettingsRepository
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
    hasOverlayPermission: Boolean = false,
    hasAccessibilityPermission: Boolean = false,
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
) {
    // Initialize Repository
    val settingsRepository = remember { AppSettingsRepository() }

    // Theme state
    // Better theme initialization that respects system changes unless overridden
    val systemDark = isSystemInDarkTheme()

    // Actually, let's keep it simple:
    // If settings has a value, use it. Else use system.
    // The repository method I wrote handles "defaults to system if not set" but needs the system value passed in.
    val shouldUseDarkTheme = remember(systemDark) {
        settingsRepository.isDarkTheme(systemDark)
    }
    // We need a stable state we can update
    var currentThemeIsDark by remember { mutableStateOf(shouldUseDarkTheme) }

    AegisTheme(darkTheme = currentThemeIsDark) {
        val colors = AegisTheme.colors
        val mainNavController = rememberNavController()

        // Determine start destination
        val isOnboardingComplete = remember { settingsRepository.isOnboardingComplete }
        val startDestination = if (isOnboardingComplete) {
            TabsRoute
        } else {
            OnboardingRoute
        }

        // Main NavHost - top-level navigation
        NavHost(
            navController = mainNavController,
            startDestination = startDestination,
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
                    isDarkTheme = currentThemeIsDark,
                    onThemeChange = { isDark ->
                        currentThemeIsDark = isDark
                        settingsRepository.setDarkTheme(isDark)
                    },
                    onViewTrustedContacts = { mainNavController.navigate(TrustedContactsRoute) }
                )
            }

            composable<TrustedContactsRoute> {
                TrustedContactsScreen(
                    onBackClick = { mainNavController.popBackStack() }
                )
            }

            composable<OnboardingRoute> {
                OnboardingScreen(
                    onSkip = {
                        settingsRepository.isOnboardingComplete = true
                        mainNavController.navigate(TabsRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    },
                    onComplete = {
                        settingsRepository.isOnboardingComplete = true
                        mainNavController.navigate(TabsRoute) {
                            popUpTo(OnboardingRoute) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}