package app.aegis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.ui.components.*
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography

/**
 * Main Dashboard Screen
 * Shows either Active Protection or Setup Required based on permissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String = "Sarah",

    hasOverlayPermission: Boolean = true,
    hasAccessibilityPermission: Boolean = true,
    sensitivityLevel: String = "High Protection",
    trustedContactsCount: Int = 3,
    onSettingsClick: () -> Unit = {},
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onViewAllActivity: () -> Unit = {}
) {
    val colors = AegisTheme.colors
    val scrollState = rememberScrollState()
    val needsSetup = !hasOverlayPermission || !hasAccessibilityPermission

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Left-aligned title for tab screens
                TopAppBar(
                    title = {
                        Text(
                            text = "Aegis",
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = colors.textSecondary
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
        ) {
            if (needsSetup) {
                SetupRequiredContent(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings
                )
            } else {
                ActiveProtectionContent(
                    sensitivityLevel = sensitivityLevel,
                    trustedContactsCount = trustedContactsCount,
                    onViewAllActivity = onViewAllActivity
                )
            }
        }
    }
}

@Composable
private fun ActiveProtectionContent(
    sensitivityLevel: String,
    trustedContactsCount: Int,
    onViewAllActivity: () -> Unit
) {
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Shield Status
        ShieldStatusIcon(isActive = true)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Aegis is Active",
            style = AegisTypography.headlineLarge,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your device is monitored and\nprotected from scams.",
            style = AegisTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = "Sensitivity",
                subtitle = sensitivityLevel,
                modifier = Modifier.weight(1f)
            )

            /*
            // Trusted Contacts - Hidden for now as feature is not enabled
            StatCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.trustBadge,
                        modifier = Modifier.size(28.dp)
                    )
                },
                title = "Trusted",
                subtitle = "$trustedContactsCount Contacts",
                modifier = Modifier.weight(1f)
            )
            */
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Incident Log Section
        SectionHeader(
            title = "Incident Log",
            action = "View All",
            onActionClick = onViewAllActivity
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Sample incidents
        IncidentLogItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = colors.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "Blocked Scam Call",
            description = "Video call attempt blocked",
            timestamp = "Today\n2:30 PM",
            iconBackgroundColor = colors.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        IncidentLogItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "Scanned Chats",
            description = "5 safe messages verified",
            timestamp = "Today\n10:00 AM",
            iconBackgroundColor = colors.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        IncidentLogItem(
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = "System Update",
            description = "Protection database updated",
            timestamp = "Yesterday",
            iconBackgroundColor = colors.textSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SetupRequiredContent(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val colors = AegisTheme.colors
    val issuesCount = listOf(!hasOverlayPermission, !hasAccessibilityPermission).count { it }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Protection Card with yellow/amber background (matching design)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.warning.copy(alpha = 0.15f))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Warning Shield Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.warning),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    style = AegisTypography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Protection:",
                style = AegisTypography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = "INACTIVE",
                style = AegisTypography.headlineLarge,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your device is currently at risk. Aegis\ncannot protect you until\npermissions are granted.",
                style = AegisTypography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Issues Alert
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$issuesCount Issues Found",
                    style = AegisTypography.titleMedium,
                    color = colors.warning
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Required Permissions Header
        Text(
            text = "Required Permissions",
            style = AegisTypography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Complete these steps to activate protection",
            style = AegisTypography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Overlay Permission Card
        if (!hasOverlayPermission) {
            PermissionCard(
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "Enable Screen Overlay",
                description = "Required to show warnings over apps like WhatsApp so you don't miss scam alerts.",
                buttonText = "Open Overlay Settings →",
                onButtonClick = onOpenOverlaySettings
            )

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accessibility Permission Card
        if (!hasAccessibilityPermission) {
            PermissionCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = "Enable Scanning",
                description = "Allows Aegis to scan incoming text messages and websites for potential threats.",
                buttonText = "Open Accessibility Settings →",
                onButtonClick = onOpenAccessibilitySettings
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Aegis values your privacy. These permissions are\nused strictly on-device to detect scams and are\nnever used to track your personal activity.",
                style = AegisTypography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
