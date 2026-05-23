package app.aegis.ui.screens

import aegis.shared.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Refresh
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
import app.aegis.ui.components.IncidentLogItem
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.*
import app.aegis.ui.viewmodel.ActivityFilter
import app.aegis.ui.viewmodel.ActivityLogViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Activity Log Screen - shows incident history with filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(viewModel: ActivityLogViewModel = koinViewModel()) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val groupedIncidents by viewModel.groupedIncidents.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Left-aligned title for tab screens
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.nav_activity),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurface,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                        ),
                )
                // Divider below action bar
                HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
            }
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            // Filter Chips
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ActivityFilter.entries.forEach { filter ->
                    val label =
                        when (filter) {
                            ActivityFilter.ALL -> stringResource(Res.string.activity_filter_all)
                            ActivityFilter.THREATS -> stringResource(Res.string.activity_filter_threats)
                            ActivityFilter.SAFE_SCANS -> stringResource(Res.string.activity_filter_safe)
                        }

                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = Color.White,
                                containerColor = colors.surface,
                                labelColor = colors.onSurfaceVariant,
                            ),
                        border =
                            FilterChipDefaults.filterChipBorder(
                                borderColor = colors.outlineVariant,
                                selectedBorderColor = colors.primary,
                                enabled = true,
                                selected = selectedFilter == filter,
                            ),
                    )
                }
            }

            // Content: Empty state or Activity List
            if (groupedIncidents.isEmpty()) {
                ActivityEmptyState()
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp),
                ) {
                    groupedIncidents.forEach { group ->
                        ActivityDateHeader(dateLabel = group.dateLabel)
                        Spacer(modifier = Modifier.height(12.dp))

                        group.incidents.forEach { incident ->
                            IncidentLogItemFromData(incident = incident)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * Empty state for Activity Log - matches design
 */
@Composable
private fun ActivityEmptyState() {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Icon container matching design
        Box(
            modifier =
                Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            // Outer shadow effect
            Box(
                modifier =
                    Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(colors.background),
                contentAlignment = Alignment.Center,
            ) {
                // List icon placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    repeat(3) {
                        Box(
                            modifier =
                                Modifier
                                    .width(32.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(colors.primary.copy(alpha = 0.6f)),
                        )
                    }
                }
                // Shield badge
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.emoji_shield),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = stringResource(Res.string.activity_empty_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.activity_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActivityDateHeader(dateLabel: String) {
    val colors = MaterialTheme.colorScheme
    val text =
        when (dateLabel) {
            "TODAY" -> stringResource(Res.string.activity_today)
            "YESTERDAY" -> stringResource(Res.string.activity_yesterday)
            else -> dateLabel
        }

    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.onSurfaceVariant,
    )
}

/**
 * Maps Incident data to IncidentLogItem UI
 */
@Composable
private fun IncidentLogItemFromData(incident: Incident) {
    val colors = MaterialTheme.colorScheme

    val (icon, iconColor) = getIncidentIconAndColor(incident.type, incident.isBlocked, colors)

    // Determine title resource
    val titleRes = getIncidentTitleRes(incident.type)

    IncidentLogItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
        },
        title = stringResource(titleRes),
        description = incident.description,
        timestamp = "", // This would use stringResource(Res.string.time_minutes_ago, ...) if dynamic
        iconBackgroundColor = iconColor,
        onClick = {},
    )
}

/**
 * Get icon and color based on incident type
 */
@Composable
private fun getIncidentIconAndColor(
    type: IncidentType,
    isBlocked: Boolean,
    colors: ColorScheme,
): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> =
    when {
        isBlocked -> Icons.Default.Warning to colors.error

        type == IncidentType.OTHER -> Icons.Outlined.Refresh to colors.primary

        type in
                listOf(
                    IncidentType.SCAM_CALL,
                    IncidentType.SCAM_TEXT,
                    IncidentType.PHISHING_LINK,
                    IncidentType.PHISHING_LINK,
                    IncidentType.DANGEROUS_APP,
                    IncidentType.POLICE_IMPERSONATION,
                    IncidentType.SEXTORTION,
                )
            -> Icons.Default.Warning to colors.warning

        else -> Icons.Default.CheckCircle to colors.success
    }

/**
 * Get string resource for incident title
 */
private fun getIncidentTitleRes(type: IncidentType): StringResource =
    when (type) {
        IncidentType.SCAM_CALL -> Res.string.incident_blocked_scam_call
        IncidentType.SCAM_TEXT -> Res.string.incident_scam_text
        IncidentType.PHISHING_LINK -> Res.string.incident_blocked_link
        IncidentType.DANGEROUS_APP -> Res.string.incident_dangerous_app
        IncidentType.POLICE_IMPERSONATION -> Res.string.incident_police_impersonation
        IncidentType.SEXTORTION -> Res.string.incident_sextortion
        IncidentType.OTHER -> Res.string.incident_system_update
    }
