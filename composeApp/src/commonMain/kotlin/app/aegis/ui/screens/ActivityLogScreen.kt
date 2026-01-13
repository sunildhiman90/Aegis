package app.aegis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.aegis.ui.components.IncidentLogItem
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography

/**
 * Activity Log Screen - shows incident history with filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen() {
    val colors = AegisTheme.colors
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Threats", "Safe Scans")
    val scrollState = rememberScrollState()

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
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                style = AegisTypography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = colors.primary,
                            selectedLabelColor = colors.textPrimary,
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

            // Activity List
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                // Today Section
                ActivityDateHeader(date = "TODAY")
                Spacer(modifier = Modifier.height(12.dp))

                IncidentLogItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Blocked Suspicious Link",
                    description = "2 mins ago • Phishing Protection",
                    timestamp = "",
                    iconBackgroundColor = colors.error,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(8.dp))

                IncidentLogItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Daily Scan Completed",
                    description = "2 hours ago • No issues found",
                    timestamp = "",
                    iconBackgroundColor = colors.primary,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Yesterday Section
                ActivityDateHeader(date = "YESTERDAY")
                Spacer(modifier = Modifier.height(12.dp))

                IncidentLogItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = colors.warning,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Unknown Caller Detected",
                    description = "4:32 PM • Caller ID",
                    timestamp = "",
                    iconBackgroundColor = colors.warning,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(8.dp))

                IncidentLogItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.success,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Wi-Fi Network Verified",
                    description = "9:15 AM • Home Network",
                    timestamp = "",
                    iconBackgroundColor = colors.success,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Oct 24 Section
                ActivityDateHeader(date = "OCT 24")
                Spacer(modifier = Modifier.height(12.dp))

                IncidentLogItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    title = "Definitions Updated",
                    description = "11:00 AM • Auto-update",
                    timestamp = "",
                    iconBackgroundColor = colors.primary,
                    onClick = {}
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
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
