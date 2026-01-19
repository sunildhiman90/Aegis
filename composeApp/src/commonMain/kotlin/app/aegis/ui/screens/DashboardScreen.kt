package app.aegis.ui.screens

import aegis.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.domain.model.Incident
import app.aegis.domain.model.IncidentType
import app.aegis.ui.components.*
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import app.aegis.ui.viewmodel.DashboardViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main Dashboard Screen
 * Shows either Active Protection or Setup Required based on permissions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    hasOverlayPermission: Boolean = true,
    hasAccessibilityPermission: Boolean = true,
    sensitivityLevel: String? = null,
    onSettingsClick: () -> Unit = {},
    onOpenOverlaySettings: () -> Unit = {},
    onOpenAccessibilitySettings: () -> Unit = {},
    onViewAllActivity: () -> Unit = {},
    viewModel: DashboardViewModel = koinViewModel(),
) {
    val colors = AegisTheme.colors
    val scrollState = rememberScrollState()
    val needsSetup = !hasOverlayPermission || !hasAccessibilityPermission

    val latestIncidents by viewModel.latestIncidents.collectAsState()
    val trustedContactsCount by viewModel.trustedContactsCount.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Left-aligned title for tab screens
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.app_name),
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary,
                        )
                    },
                    actions = {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(Res.string.nav_settings),
                                tint = colors.textSecondary,
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
                    .verticalScroll(scrollState),
        ) {
            if (needsSetup) {
                SetupRequiredContent(
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                )
            } else {
                ActiveProtectionContent(
                    sensitivityLevel = sensitivityLevel ?: stringResource(Res.string.dashboard_high_protection),
                    trustedContactsCount = trustedContactsCount,
                    incidents = latestIncidents,
                    onViewAllActivity = onViewAllActivity,
                )
            }
        }
    }
}

@Composable
private fun ActiveProtectionContent(
    sensitivityLevel: String,
    trustedContactsCount: Int,
    incidents: List<Incident>,
    onViewAllActivity: () -> Unit,
) {
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Shield Status
        ShieldStatusIcon(isActive = true)

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(Res.string.dashboard_active_title),
            style = AegisTypography.headlineLarge,
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.dashboard_active_subtitle),
            style = AegisTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions Section
        Text(
            text = stringResource(Res.string.dashboard_quick_actions),
            style = AegisTypography.overline,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(28.dp),
                    )
                },
                title = stringResource(Res.string.dashboard_sensitivity),
                subtitle = sensitivityLevel,
                modifier = Modifier.weight(1f),
            )

            StatCard(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = colors.trustBadge,
                        modifier = Modifier.size(28.dp),
                    )
                },
                title = stringResource(Res.string.dashboard_trusted),
                subtitle = stringResource(Res.string.dashboard_contacts, trustedContactsCount),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Incident Log Section
        SectionHeader(
            title = stringResource(Res.string.dashboard_incident_log),
            action = stringResource(Res.string.dashboard_view_all),
            onActionClick = onViewAllActivity,
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (incidents.isEmpty()) {
            DashboardIncidentsEmptyState()
        } else {
            incidents.forEach { incident ->
                DashboardIncidentItem(incident = incident)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Empty state for Incident Log in Dashboard - matches design
 */
@Composable
private fun DashboardIncidentsEmptyState() {
    val colors = AegisTheme.colors

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Icon with shield and magnifier
        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(32.dp),
            )
            // Small search badge
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(colors.background),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.dashboard_no_incidents_title),
            style = AegisTypography.titleMedium,
            color = colors.textPrimary,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.dashboard_monitoring_desc),
            style = AegisTypography.bodySmall,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.dashboard_no_incidents_message),
            style = AegisTypography.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Maps Incident to UI for Dashboard
 */
@Composable
private fun DashboardIncidentItem(incident: Incident) {
    val colors = AegisTheme.colors

    val (icon, iconColor) =
        when {
            incident.isBlocked -> Icons.Default.Warning to colors.error

            incident.type == IncidentType.OTHER -> Icons.Outlined.Refresh to colors.textSecondary

            incident.type in
                    listOf(
                        IncidentType.SCAM_CALL,
                        IncidentType.PHISHING_LINK,
                        IncidentType.DANGEROUS_APP,
                        IncidentType.POLICE_IMPERSONATION,
                        IncidentType.SEXTORTION,
                    )
                -> Icons.Outlined.Warning to colors.warning

            else -> Icons.Default.CheckCircle to colors.primary
        }

    val title =
        when (incident.type) {
            IncidentType.SCAM_CALL -> stringResource(Res.string.incident_blocked_scam_call)
            IncidentType.PHISHING_LINK -> stringResource(Res.string.incident_blocked_link)
            IncidentType.DANGEROUS_APP -> stringResource(Res.string.incident_dangerous_app)
            IncidentType.POLICE_IMPERSONATION -> stringResource(Res.string.incident_police_impersonation)
            IncidentType.SEXTORTION -> stringResource(Res.string.incident_sextortion)
            IncidentType.OTHER -> stringResource(Res.string.incident_system_update)
        }

    IncidentLogItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        },
        title = title,
        description = incident.description,
        timestamp = "",
        iconBackgroundColor = iconColor,
        onClick = {},
    )
}

@Composable
private fun SetupRequiredContent(
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
) {
    val colors = AegisTheme.colors
    val issuesCount = listOf(!hasOverlayPermission, !hasAccessibilityPermission).count { it }

    Column(
        modifier = Modifier.padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Protection Card with yellow/amber background (matching design)
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.warning.copy(alpha = 0.15f))
                    .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Warning Shield Icon
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.warning),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.emoji_shield),
                    style = AegisTypography.displayMedium,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(Res.string.dashboard_protection_label),
                style = AegisTypography.headlineSmall,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(Res.string.dashboard_protection_inactive).split("\n")[1],
                style = AegisTypography.headlineLarge,
                color = colors.textPrimary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.dashboard_at_risk_message),
                style = AegisTypography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Issues Alert
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = colors.warning,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    // String format with param
                    text = stringResource(Res.string.dashboard_issues_found, issuesCount),
                    style = AegisTypography.titleMedium,
                    color = colors.warning,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Required Permissions Header
        Text(
            text = stringResource(Res.string.dashboard_required_permissions),
            style = AegisTypography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(Res.string.dashboard_complete_steps),
            style = AegisTypography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.fillMaxWidth(),
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
                        modifier = Modifier.size(24.dp),
                    )
                },
                title = stringResource(Res.string.permission_overlay_title),
                description = stringResource(Res.string.permission_overlay_description),
                buttonText = stringResource(Res.string.permission_overlay_button),
                onButtonClick = onOpenOverlaySettings,
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
                        modifier = Modifier.size(24.dp),
                    )
                },
                title = stringResource(Res.string.permission_accessibility_title),
                description = stringResource(Res.string.permission_accessibility_description),
                buttonText = stringResource(Res.string.permission_accessibility_button),
                onButtonClick = onOpenAccessibilitySettings,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Privacy note
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.permission_privacy_note),
                style = AegisTypography.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
