package app.aegis.ui.screens

import aegis.composeapp.generated.resources.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.ui.theme.*
import app.aegis.ui.theme.*
import app.aegis.ui.viewmodel.PrivacyReportStats
import app.aegis.ui.viewmodel.ProfileViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full Report Screen - Detailed privacy report with threats breakdown
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullReportScreen(
    onBackClick: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel()
) {
    val colors = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    val privacyStats by viewModel.privacyStats.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.full_report_title),
                            style = MaterialTheme.typography.headlineMedium,
                            color = colors.onSurface
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.nav_back),
                                tint = colors.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.background
                    )
                )
                HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            // Summary Card
            SummaryCard(stats = privacyStats)

            Spacer(modifier = Modifier.height(24.dp))

            // Threats Breakdown Section
            Text(
                text = stringResource(Res.string.full_report_threats_blocked),
                style = MaterialTheme.typography.overline,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ThreatsBreakdownCard(stats = privacyStats)

            Spacer(modifier = Modifier.height(24.dp))

            // Processing Summary Section
            Text(
                text = stringResource(Res.string.full_report_processing_summary),
                style = MaterialTheme.typography.overline,
                color = colors.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProcessingSummaryCard(stats = privacyStats)

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Assurance Section
            PrivacyAssuranceCard()

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SummaryCard(stats: PrivacyReportStats) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.primary.copy(alpha = 0.1f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.emoji_shield),
                style = MaterialTheme.typography.displayMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "${stats.totalThreatsBlocked}",
            style = MaterialTheme.typography.displayLarge,
            color = colors.primary
        )

        Text(
            text = stringResource(Res.string.full_report_total_threats_blocked),
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.full_report_local_processing),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThreatsBreakdownCard(stats: PrivacyReportStats) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        ThreatRow(
            label = stringResource(Res.string.full_report_scam_calls),
            count = stats.scamCallsBlocked,
            iconColor = colors.error
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.outlineVariant
        )

        ThreatRow(
            label = stringResource(Res.string.full_report_phishing_links),
            count = stats.phishingLinksBlocked,
            iconColor = colors.warning
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.outlineVariant
        )

        ThreatRow(
            label = stringResource(Res.string.full_report_sextortion),
            count = stats.sextortionBlocked,
            iconColor = colors.error
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.outlineVariant
        )

        ThreatRow(
            label = stringResource(Res.string.full_report_police_impersonation),
            count = stats.policeImpersonationBlocked,
            iconColor = colors.error
        )

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.outlineVariant
        )

        ThreatRow(
            label = stringResource(Res.string.full_report_dangerous_apps),
            count = stats.dangerousAppsBlocked,
            iconColor = colors.warning
        )
    }
}

@Composable
private fun ThreatRow(
    label: String,
    count: Int,
    iconColor: Color
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            color = colors.onSurface
        )
    }
}

@Composable
private fun ProcessingSummaryCard(stats: PrivacyReportStats) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.full_report_data_processed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface
                )
                Text(
                    text = stringResource(Res.string.full_report_no_cloud),
                    style = MaterialTheme.typography.caption,
                    color = colors.textTertiary
                )
            }

            Text(
                text = stats.dataProcessedMb,
                style = MaterialTheme.typography.titleMedium,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun PrivacyAssuranceCard() {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.success.copy(alpha = 0.1f))
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(Res.string.full_report_privacy_assured),
                style = MaterialTheme.typography.titleMedium,
                color = colors.success
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.full_report_privacy_message),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.full_report_privacy_note),
            style = MaterialTheme.typography.caption,
            color = colors.textTertiary
        )
    }
}
