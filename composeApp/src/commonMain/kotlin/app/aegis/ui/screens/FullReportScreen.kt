package app.aegis.ui.screens

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
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import app.aegis.ui.viewmodel.PrivacyReportStats
import app.aegis.ui.viewmodel.ProfileViewModel
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
    val colors = AegisTheme.colors
    val scrollState = rememberScrollState()
    
    val privacyStats by viewModel.privacyStats.collectAsState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Privacy Report",
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
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
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
                text = "THREATS BLOCKED",
                style = AegisTypography.overline,
                color = colors.textSecondary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ThreatsBreakdownCard(stats = privacyStats)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Processing Summary Section
            Text(
                text = "PROCESSING SUMMARY",
                style = AegisTypography.overline,
                color = colors.textSecondary
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
    val colors = AegisTheme.colors
    
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
                text = "🛡️",
                style = AegisTypography.displayMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "${stats.totalThreatsBlocked}",
            style = AegisTypography.displayLarge,
            color = colors.primary
        )
        
        Text(
            text = "Total Threats Blocked",
            style = AegisTypography.titleMedium,
            color = colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "All processing happens locally on your device",
            style = AegisTypography.bodySmall,
            color = colors.textTertiary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ThreatsBreakdownCard(stats: PrivacyReportStats) {
    val colors = AegisTheme.colors
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        ThreatRow(
            label = "Scam Calls Blocked",
            count = stats.scamCallsBlocked,
            iconColor = colors.error
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.divider
        )
        
        ThreatRow(
            label = "Phishing Links Blocked",
            count = stats.phishingLinksBlocked,
            iconColor = colors.warning
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.divider
        )
        
        ThreatRow(
            label = "Sextortion Attempts Blocked",
            count = stats.sextortionBlocked,
            iconColor = colors.error
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.divider
        )
        
        ThreatRow(
            label = "Police Impersonation Blocked",
            count = stats.policeImpersonationBlocked,
            iconColor = colors.error
        )
        
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = colors.divider
        )
        
        ThreatRow(
            label = "Dangerous Apps Blocked",
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
    val colors = AegisTheme.colors
    
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
            style = AegisTypography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        Text(
            text = "$count",
            style = AegisTypography.titleMedium,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun ProcessingSummaryCard(stats: PrivacyReportStats) {
    val colors = AegisTheme.colors
    
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
                    text = "Data Processed On-Device",
                    style = AegisTypography.bodyMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = "No cloud uploads",
                    style = AegisTypography.caption,
                    color = colors.textTertiary
                )
            }
            
            Text(
                text = stats.dataProcessedMb,
                style = AegisTypography.titleMedium,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun PrivacyAssuranceCard() {
    val colors = AegisTheme.colors
    
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
                text = "Privacy Assured",
                style = AegisTypography.titleMedium,
                color = colors.success
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "All threat detection and analysis happens locally on your device. Your data never leaves your phone and is never uploaded to any cloud servers.",
            style = AegisTypography.bodySmall,
            color = colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Aegis uses on-device AI models to protect your privacy while keeping you safe from scams.",
            style = AegisTypography.caption,
            color = colors.textTertiary
        )
    }
}
