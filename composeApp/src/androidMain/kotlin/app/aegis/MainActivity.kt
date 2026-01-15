package app.aegis

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.di.initKoin
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinContext
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Initialize Koin
        initKoin {
            androidContext(this@MainActivity)
        }

        setStatusBarColor(isPrimaryStatusBar = false)

        setContent {
            KoinContext {
                // Permission states with actual checking
                var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission()) }
                var hasAccessibilityPermission by remember { mutableStateOf(checkAccessibilityPermission()) }

                // Re-check permissions when resuming from settings
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer =
                        LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                hasOverlayPermission = checkOverlayPermission()
                                hasAccessibilityPermission = checkAccessibilityPermission()
                            }
                        }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                App(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onOpenOverlaySettings = {
                        if (!checkOverlayPermission()) {
                            try {
                                val intent =
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        "package:$packageName".toUri(),
                                    )
                                startActivity(intent)
                            } catch (e: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                startActivity(intent)
                            }
                        }
                    },
                    onOpenAccessibilitySettings = {
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        startActivity(intent)
                    },
                    onUpdateStatusBar = {
                        setStatusBarColor(isPrimaryStatusBar = false)
                    },
                )
            }
        }
    }

    private fun checkOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun checkAccessibilityPermission(): Boolean {
        val accessibilityManager = getSystemService(ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val enabledServices =
            accessibilityManager?.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK,
            ) ?: emptyList()

        return enabledServices.any {
            it.resolveInfo?.serviceInfo?.packageName == packageName
        }
    }

    private fun setStatusBarColor(isPrimaryStatusBar: Boolean) {
        val settingsRepository = get<AppSettingsRepository>()

        // Get system dark theme state
        val isSystemDarkTheme =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }

        // Determine actual dark theme based on mode
        val themeMode = settingsRepository.getThemeMode()
        val isDarkTheme =
            when (themeMode) {
                app.aegis.domain.model.AppThemeMode.LIGHT -> false
                app.aegis.domain.model.AppThemeMode.DARK -> true
                app.aegis.domain.model.AppThemeMode.SYSTEM_DEFAULT -> isSystemDarkTheme
            }

        val statusBarStyle =
            if (isPrimaryStatusBar) {
                if (!isDarkTheme) {
                    // SystemBarStyle.dark will use light icons (white icons)
                    SystemBarStyle.dark(
                        scrim = android.graphics.Color.TRANSPARENT,
                    )
                } else {
                    SystemBarStyle.light(
                        scrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                    )
                }
            } else {
                if (isDarkTheme) {
                    SystemBarStyle.dark(
                        scrim = android.graphics.Color.TRANSPARENT,
                    )
                } else {
                    // SystemBarStyle.light will use dark icons (gray icons)
                    SystemBarStyle.light(
                        scrim = android.graphics.Color.TRANSPARENT,
                        darkScrim = android.graphics.Color.TRANSPARENT,
                    )
                }
            }
        enableEdgeToEdge(
            statusBarStyle = statusBarStyle,
            navigationBarStyle = statusBarStyle,
        )
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
