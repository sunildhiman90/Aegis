package app.aegis.ui.screens

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
import app.aegis.ui.theme.AegisTypography
import app.aegis.ui.viewmodel.ActivityFilter
import app.aegis.ui.viewmodel.ActivityLogViewModel
import app.aegis.ui.viewmodel.GroupedIncidents
import org.koin.compose.viewmodel.koinViewModel

/**
 * Activity Log Screen - shows incident history with filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    viewModel: ActivityLogViewModel = koinViewModel()
) {
    val colors = AegisTheme.colors
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
                            text = "Activity",
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary
                        )
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
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = filter.displayName,
                                style = AegisTypography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = Color.White,
                            containerColor = colors.surface,
                            labelColor = colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = colors.cardBorder,
                            selectedBorderColor = colors.primary,
                            enabled = true,
                            selected = selectedFilter == filter
                        )
                    )
                }
            }

            // Content: Empty state or Activity List
            if (groupedIncidents.isEmpty()) {
                ActivityEmptyState()
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                ) {
                    groupedIncidents.forEach { group ->
                        ActivityDateHeader(date = group.dateLabel)
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
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon container matching design
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(colors.surface),
            contentAlignment = Alignment.Center
        ) {
            // Outer shadow effect
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                // List icon placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.primary.copy(alpha = 0.6f))
                        )
                    }
                }
                // Shield badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 8.dp, y = 8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🛡️",
                        style = AegisTypography.labelSmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your Security Journal",
            style = AegisTypography.headlineMedium,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You haven't had any security incidents yet. When Aegis blocks a threat or scans a message, the details will be listed here.",
            style = AegisTypography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ActivityDateHeader(date: String) {
    val colors = AegisTheme.colors
    Text(
        text = date,
        style = AegisTypography.labelMedium,
        color = colors.textSecondary
    )
}

/**
 * Maps Incident data to IncidentLogItem UI
 */
@Composable
private fun IncidentLogItemFromData(incident: Incident) {
    val colors = AegisTheme.colors

    val (icon, iconColor) = getIncidentIconAndColor(incident.type, incident.isBlocked, colors)

    IncidentLogItem(
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        },
        title = getIncidentTitle(incident.type),
        description = incident.description,
        timestamp = "",
        iconBackgroundColor = iconColor,
        onClick = {}
    )
}

/**
 * Get icon and color based on incident type
 */
@Composable
private fun getIncidentIconAndColor(type: IncidentType, isBlocked: Boolean, colors: app.aegis.ui.theme.AegisColorScheme): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    return when {
        isBlocked -> Icons.Default.Warning to colors.error
        type == IncidentType.OTHER -> Icons.Outlined.Refresh to colors.primary
        type in listOf(IncidentType.SCAM_CALL, IncidentType.PHISHING_LINK, IncidentType.DANGEROUS_APP, 
            IncidentType.POLICE_IMPERSONATION, IncidentType.SEXTORTION) -> Icons.Default.Warning to colors.warning
        else -> Icons.Default.CheckCircle to colors.success
    }
}

/**
 * Get human-readable title for incident type
 */
private fun getIncidentTitle(type: IncidentType): String {
    return when (type) {
        IncidentType.SCAM_CALL -> "Scam Call Detected"
        IncidentType.PHISHING_LINK -> "Phishing Link Blocked"
        IncidentType.DANGEROUS_APP -> "Dangerous App Warning"
        IncidentType.POLICE_IMPERSONATION -> "Police Impersonation Detected"
        IncidentType.SEXTORTION -> "Sextortion Attempt Blocked"
        IncidentType.OTHER -> "System Update"
    }
}
