package app.aegis.ui.screens

import aegis.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.aegis.domain.model.AppThemeMode
import app.aegis.domain.model.TrustedContact
import app.aegis.models.SensitivityLevel
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import app.aegis.ui.viewmodel.SettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * App Settings Screen - Full screen without bottom navigation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit = {},
    sensitivityLevel: SensitivityLevel = SensitivityLevel.BALANCED,
    hasOverlayPermission: Boolean = true,
    hasAccessibilityPermission: Boolean = false,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM_DEFAULT,
    onSensitivityChange: (SensitivityLevel) -> Unit = {},
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onViewTrustedContacts: () -> Unit = {},
    onFactoryReset: () -> Unit = {},
    onThemeModeChange: (AppThemeMode) -> Unit = {},
    onLanguageChange: (String) -> Unit = {},
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val colors = AegisTheme.colors
    var currentSensitivity by remember { mutableStateOf(sensitivityLevel) }
    var currentThemeMode by remember { mutableStateOf(themeMode) }
    val scrollState = rememberScrollState()

    val topContacts by viewModel.topContacts.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    // Collect ViewModel states
    val languageUiState by viewModel.languageUiState.collectAsState()
    val settingsUiState by viewModel.settingsScreenUiState.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("") }


    // Factory Reset Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = stringResource(Res.string.settings_factory_reset_title),
                    style = AegisTypography.headlineSmall,
                    color = colors.error,
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.settings_factory_reset_message),
                    style = AegisTypography.bodyMedium,
                    color = colors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        onFactoryReset()
                    },
                ) {
                    Text(stringResource(Res.string.settings_reset_confirm), color = colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(Res.string.settings_cancel), color = colors.textSecondary)
                }
            },
        )
    }

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Centered title for Settings screen
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.settings_title),
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.nav_back),
                                tint = colors.textPrimary,
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                        ),
                )
                // Divider below action bar
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp), // Top padding for content
        ) {
            // Protection Levels Section
            SectionTitle(title = stringResource(Res.string.settings_section_protection))

            Spacer(modifier = Modifier.height(12.dp))

            // Sensitivity Card with discrete selector
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
            ) {
                // Title and Icon Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_sensitivity_title),
                            style = AegisTypography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.settings_sensitivity_desc),
                            style = AegisTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Discrete level selector (3 options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SensitivityLevel.entries.forEach { level ->
                        val isSelected = currentSensitivity == level
                        val displayName =
                            when (level) {
                                SensitivityLevel.LOW -> stringResource(Res.string.settings_sensitivity_low)
                                SensitivityLevel.BALANCED -> stringResource(Res.string.settings_sensitivity_balanced)
                                SensitivityLevel.AGGRESSIVE -> stringResource(Res.string.settings_sensitivity_aggressive)
                            }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentSensitivity = level
                                        onSensitivityChange(level)
                                    }.background(
                                        if (isSelected) {
                                            colors.primary.copy(alpha = 0.15f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ).padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                colors.primary
                                            } else {
                                                colors.cardBorder
                                            },
                                        ),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayName,
                                style = AegisTypography.labelMedium,
                                color = if (isSelected) colors.primary else colors.textSecondary,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Info Card
                val (infoIcon, infoColor, infoBg) =
                    when (currentSensitivity) {
                        SensitivityLevel.LOW -> {
                            Triple(
                                Icons.Outlined.BatteryStd,
                                colors.success,
                                colors.success.copy(alpha = 0.1f),
                            )
                        }

                        SensitivityLevel.BALANCED -> {
                            Triple(
                                Icons.Outlined.Shield,
                                colors.primary,
                                colors.primary.copy(alpha = 0.1f),
                            )
                        }

                        SensitivityLevel.AGGRESSIVE -> {
                            Triple(
                                Icons.Outlined.Security,
                                colors.warning,
                                colors.warning.copy(alpha = 0.1f),
                            )
                        }
                    }

                Surface(
                    color = infoBg,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = infoIcon,
                            contentDescription = null,
                            tint = infoColor,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))

                        val warningText =
                            when (currentSensitivity) {
                                SensitivityLevel.LOW -> stringResource(Res.string.settings_sensitivity_low_warning)
                                SensitivityLevel.BALANCED -> stringResource(Res.string.settings_sensitivity_balanced_warning)
                                SensitivityLevel.AGGRESSIVE -> stringResource(Res.string.settings_sensitivity_aggressive_warning)
                            }

                        Text(
                            text = warningText,
                            style = AegisTypography.bodySmall,
                            color = colors.textPrimary,
                        )
                    }
                }
            } // End of Sensitivity Card

            Spacer(modifier = Modifier.height(32.dp))

            // App Permissions Section
            SectionTitle(title = stringResource(Res.string.settings_section_permissions))

            Spacer(modifier = Modifier.height(12.dp))

            // Screen Overlay Permission
            SettingsToggleItem(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                title = stringResource(Res.string.permission_overlay_title),
                subtitle =
                    if (hasOverlayPermission) {
                        stringResource(Res.string.status_active)
                    } else {
                        stringResource(
                            Res.string.status_disabled,
                        )
                    },
                subtitleColor = if (hasOverlayPermission) colors.success else colors.textSecondary,
                checked = hasOverlayPermission,
                onCheckedChange = { onOpenOverlaySettings() },
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility Permission
            SettingsToggleItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp),
                    )
                },
                title = stringResource(Res.string.permission_accessibility_title),
                subtitle =
                    if (hasAccessibilityPermission) {
                        stringResource(Res.string.status_active)
                    } else {
                        stringResource(
                            Res.string.status_action_needed,
                        )
                    },
                subtitleColor = if (hasAccessibilityPermission) colors.success else colors.warning,
                checked = hasAccessibilityPermission,
                onCheckedChange = { onOpenAccessibilitySettings() },
                showWarning = !hasAccessibilityPermission,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.settings_accessibility_note),
                style = AegisTypography.caption,
                color = colors.textTertiary,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            SectionTitle(title = stringResource(Res.string.settings_section_appearance))

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Mode Selector (3 options: Light, Dark, System)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_theme_title),
                            style = AegisTypography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.settings_theme_desc),
                            style = AegisTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Text(
                        text =
                            when (currentThemeMode) {
                                AppThemeMode.LIGHT -> "☀️"
                                AppThemeMode.DARK -> "🌙"
                                AppThemeMode.SYSTEM_DEFAULT -> "🤖"
                            },
                        style = AegisTypography.titleMedium,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Discrete theme mode selector (3 options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        val displayName =
                            when (mode) {
                                AppThemeMode.LIGHT -> stringResource(Res.string.settings_theme_light)
                                AppThemeMode.DARK -> stringResource(Res.string.settings_theme_dark)
                                AppThemeMode.SYSTEM_DEFAULT -> stringResource(Res.string.settings_theme_system)
                            }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        currentThemeMode = mode
                                        onThemeModeChange(mode)
                                    }.background(
                                        if (isSelected) {
                                            colors.primary.copy(alpha = 0.15f)
                                        } else {
                                            Color.Transparent
                                        },
                                    ).padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) {
                                                colors.primary
                                            } else {
                                                colors.cardBorder
                                            },
                                        ),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayName,
                                style = AegisTypography.labelMedium,
                                color = if (isSelected) colors.primary else colors.textSecondary,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Language Selection
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .clickable {
                            showLanguageDialog = true
                            selectedLanguage = languageUiState.languages?.find {
                                it.code == languageUiState.selectedLanguageCode
                            }?.name ?: "English"
                        }
                        .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.settings_language_title),
                            style = AegisTypography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = languageUiState.languages?.find {
                                it.code == languageUiState.selectedLanguageCode
                            }?.name ?: "English",
                            style = AegisTypography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = colors.textTertiary,
                    )
                }
            }

            // Language Selection Dialog
            if (showLanguageDialog) {
                app.aegis.ui.components.LanguageSelectionDialog(
                    languages = languageUiState.languages ?: emptyList(),
                    showDialog = showLanguageDialog,
                    selectedLanguage = selectedLanguage,
                    onDismissRequest = {
                        showLanguageDialog = false
                    },
                    onLanguageSelected = { language ->
                        selectedLanguage = language.name
                        // Trigger ViewModel event to change language
                        viewModel.onSettingsScreenUiEvent(
                            app.aegis.ui.events.SettingsScreenUiEvent.OnCurrentLanguageChange(language.code)
                        )
                        // Also trigger the app-level callback
                        onLanguageChange(language.code)
                        showLanguageDialog = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Trusted Contacts Section
            SectionTitle(
                title = stringResource(Res.string.settings_section_contacts),
                action = stringResource(Res.string.dashboard_view_all),
                onActionClick = onViewTrustedContacts,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic trusted contacts from database
            if (topContacts.isEmpty()) {
                // Empty state for contacts
                TrustedContactEmptyState(onViewTrustedContacts)
            } else {
                topContacts.forEach { contact ->
                    TrustedContactItem(
                        name = contact.name,
                        detail = "${contact.relationship} • ${contact.phoneNumber}",
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Advanced Section
            SectionTitle(title = stringResource(Res.string.settings_section_advanced))

            Spacer(modifier = Modifier.height(12.dp))

            // Factory Reset
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .clickable { showResetDialog = true }
                        .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(Res.string.settings_factory_reset_title),
                    style = AegisTypography.titleMedium,
                    color = colors.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version
            Text(
                text = stringResource(Res.string.profile_version, "2.4.1", "209"),
                style = AegisTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))
        } // End of Main Column
    }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AegisTypography.labelMedium,
            color = colors.textSecondary,
        )
        if (action != null) {
            Text(
                text = action,
                style = AegisTypography.labelMedium,
                color = colors.primary,
                modifier = Modifier.clickable { onActionClick?.invoke() },
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    subtitleColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    showWarning: Boolean = false,
) {
    val colors = AegisTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = subtitle,
                    style = AegisTypography.bodySmall,
                    color = subtitleColor,
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = colors.primary,
                    checkedTrackColor = colors.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.cardBorder,
                ),
        )
    }
}

@Composable
private fun TrustedContactItem(
    name: String,
    detail: String,
) {
    val colors = AegisTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .clickable { }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.trustBadge.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colors.trustBadge,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = detail,
                style = AegisTypography.bodySmall,
                color = colors.textSecondary,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
        )
    }
}

/**
 * Empty state for Trusted Contacts section in Settings
 */
@Composable
private fun TrustedContactEmptyState(onAddContacts: () -> Unit) {
    val colors = AegisTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .clickable { onAddContacts() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(24.dp),
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.settings_no_contacts),
                style = AegisTypography.titleMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(Res.string.settings_no_contacts_desc),
                style = AegisTypography.bodySmall,
                color = colors.textSecondary,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
        )
    }
}
