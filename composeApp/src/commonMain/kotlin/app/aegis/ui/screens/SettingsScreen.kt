package app.aegis.ui.screens

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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.aegis.domain.model.AppThemeMode
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography

/**
 * Sensitivity levels - discrete values only
 */
enum class SensitivityLevel(val displayName: String) {
    LOW("Low"),
    BALANCED("Balanced"),
    AGGRESSIVE("Aggressive")
}

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
    onThemeModeChange: (AppThemeMode) -> Unit = {}
) {
    val colors = AegisTheme.colors
    var currentSensitivity by remember { mutableStateOf(sensitivityLevel) }
    var currentThemeMode by remember { mutableStateOf(themeMode) }
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Centered title for Settings screen
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "App Settings",
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colors.textPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background
                    )
                )
                // Divider below action bar
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp) // Top padding for content
        ) {
            // Protection Levels Section
            SectionTitle(title = "PROTECTION LEVELS")

            Spacer(modifier = Modifier.height(12.dp))


            // Sensitivity Card with discrete slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Scam Detection Sensitivity",
                            style = AegisTypography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Adjust how aggressively the AI filters unknown calls.",
                            style = AegisTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Discrete level selector (3 options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SensitivityLevel.entries.forEach { level ->
                        val isSelected = currentSensitivity == level
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentSensitivity = level
                                    onSensitivityChange(level)
                                }
                                .background(
                                    if (isSelected) colors.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) colors.primary
                                        else colors.cardBorder
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = level.displayName,
                                style = AegisTypography.labelMedium,
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Permissions Section
            SectionTitle(title = "APP PERMISSIONS")

            Spacer(modifier = Modifier.height(12.dp))

            // Screen Overlay Permission
            SettingsToggleItem(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "Screen Overlay",
                subtitle = if (hasOverlayPermission) "Active" else "Disabled",
                subtitleColor = if (hasOverlayPermission) colors.success else colors.textSecondary,
                checked = hasOverlayPermission,
                onCheckedChange = { onOpenOverlaySettings() }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Accessibility Permission
            SettingsToggleItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "Accessibility Services",
                subtitle = if (hasAccessibilityPermission) "Active" else "Action Needed",
                subtitleColor = if (hasAccessibilityPermission) colors.success else colors.warning,
                checked = hasAccessibilityPermission,
                onCheckedChange = { onOpenAccessibilitySettings() },
                showWarning = !hasAccessibilityPermission
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Accessibility permissions are required for Aegis to analyze incoming call screens in real-time.",
                style = AegisTypography.caption,
                color = colors.textTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Appearance Section
            SectionTitle(title = "APPEARANCE")

            Spacer(modifier = Modifier.height(12.dp))

            // Theme Mode Selector (3 options: Light, Dark, System)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Theme Mode",
                            style = AegisTypography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose your preferred app theme.",
                            style = AegisTypography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = when (currentThemeMode) {
                            AppThemeMode.LIGHT -> "☀️"
                            AppThemeMode.DARK -> "🌙"
                            AppThemeMode.SYSTEM_DEFAULT -> "🤖"
                        },
                        style = AegisTypography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Discrete theme mode selector (3 options)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        val displayName = when (mode) {
                            AppThemeMode.LIGHT -> "Light"
                            AppThemeMode.DARK -> "Dark"
                            AppThemeMode.SYSTEM_DEFAULT -> "System"
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    currentThemeMode = mode
                                    onThemeModeChange(mode)
                                }
                                .background(
                                    if (isSelected) colors.primary.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) colors.primary
                                        else colors.cardBorder
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayName,
                                style = AegisTypography.labelMedium,
                                color = if (isSelected) colors.primary else colors.textSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Trusted Contacts Section
            SectionTitle(
                title = "TRUSTED CONTACTS",
                action = "View All",
                onActionClick = onViewTrustedContacts
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample trusted contacts
            TrustedContactItem(name = "Mom", detail = "Mobile • +1 (555) 012-3456")
            Spacer(modifier = Modifier.height(8.dp))
            TrustedContactItem(name = "Partner", detail = "Home • +1 (555) 098-7654")

            Spacer(modifier = Modifier.height(32.dp))

            // Advanced Section
            SectionTitle(title = "ADVANCED")

            Spacer(modifier = Modifier.height(12.dp))

            // Factory Reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .clickable { onFactoryReset() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Factory Reset Aegis",
                    style = AegisTypography.titleMedium,
                    color = colors.error
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version
            Text(
                text = "Aegis v2.4.1 (Build 209)",
                style = AegisTypography.caption,
                color = colors.textTertiary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    action: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AegisTypography.labelMedium,
            color = colors.textSecondary
        )
        if (action != null) {
            Text(
                text = action,
                style = AegisTypography.labelMedium,
                color = colors.primary,
                modifier = Modifier.clickable { onActionClick?.invoke() }
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
    showWarning: Boolean = false
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            icon()
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showWarning) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = subtitle,
                    style = AegisTypography.bodySmall,
                    color = subtitleColor
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.primary,
                checkedTrackColor = colors.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.cardBorder
            )
        )
    }
}

@Composable
private fun TrustedContactItem(
    name: String,
    detail: String
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable { }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.trustBadge.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = colors.trustBadge,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary
            )
            Text(
                text = detail,
                style = AegisTypography.bodySmall,
                color = colors.textSecondary
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary
        )
    }
}
