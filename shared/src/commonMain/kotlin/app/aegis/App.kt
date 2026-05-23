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
import org.koin.compose.koinInject

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
    onUpdateStatusBar: () -> Unit = {},
) {
    // Initialize Repository
    val settingsRepository = koinInject<AppSettingsRepository>()

    // Theme mode with System Default support - reactive state
    val systemDarkTheme = isSystemInDarkTheme()
    var themeMode by remember { mutableStateOf(settingsRepository.getThemeMode()) }
    val isDarkTheme =
        when (themeMode) {
            app.aegis.domain.model.AppThemeMode.LIGHT -> false
            app.aegis.domain.model.AppThemeMode.DARK -> true
            app.aegis.domain.model.AppThemeMode.SYSTEM_DEFAULT -> systemDarkTheme
        }

    // Language state - reactive to changes
    var appCurrentLanguageCode by remember { mutableStateOf(settingsRepository.getLanguageCode()) }

    // Language change callback
    val onCurrentLanguageChange: (String) -> Unit = { code ->
        appCurrentLanguageCode = code
        settingsRepository.setLanguageCode(code)
    }

    app.aegis.ui.localization.LocalizedApp(language = appCurrentLanguageCode) {
        AegisTheme(darkTheme = isDarkTheme) {
            val colors = androidx.compose.material3.MaterialTheme.colorScheme
            val mainNavController = rememberNavController()

            // Determine start destination
            val isOnboardingComplete = remember { settingsRepository.isOnboardingComplete }
            val startDestination =
                if (isOnboardingComplete) {
                    TabsRoute
                } else {
                    OnboardingRoute
                }

            // Main NavHost - top-level navigation
            NavHost(
                navController = mainNavController,
                startDestination = startDestination,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(colors.background),
            ) {
                // Tabs container (has its own nested NavHost with bottom navigation)
                composable<TabsRoute> {
                    TabsScreen(
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onNavigateToSettings = { mainNavController.navigate(SettingsRoute) },
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        onViewFullReport = { mainNavController.navigate(FullReportRoute) },
                        onViewTrustedContacts = { mainNavController.navigate(TrustedContactsRoute) },
                    )
                }

                // Full-screen destinations (no bottom nav)
                composable<SettingsRoute> {
                    val settingsViewModel = koinInject<app.aegis.ui.viewmodel.SettingsViewModel>()
                    SettingsScreen(
                        onBackClick = { mainNavController.popBackStack() },
                        hasOverlayPermission = hasOverlayPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                        themeMode = themeMode,
                        sensitivityLevel = settingsViewModel.getSensitivity(),
                        onSensitivityChange = { level ->
                            settingsViewModel.setSensitivity(level)
                        },
                        onThemeModeChange = { mode ->
                            themeMode = mode // Update local state for immediate UI update
                            settingsRepository.setThemeMode(mode) // Persist to storage
                            onUpdateStatusBar() // Update status bar colors
                        },
                        onLanguageChange = onCurrentLanguageChange,
                        onViewTrustedContacts = { mainNavController.navigate(TrustedContactsRoute) },
                    )
                }

                composable<TrustedContactsRoute> {
                    TrustedContactsScreen(
                        onBackClick = { mainNavController.popBackStack() },
                    )
                }

                composable<FullReportRoute> {
                    FullReportScreen(
                        onBackClick = { mainNavController.popBackStack() },
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
                        },
                    )
                }
            }
        }
    }
}
