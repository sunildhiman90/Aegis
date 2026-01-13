package app.aegis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography

/**
 * Profile Screen - Privacy profile with device info and settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isProtected: Boolean = true,
    deviceId: String = "AE-882-991-X",
    threatsBlocked: Int = 142,
    dataProcessed: String = "12MB",
    subscriptionType: String = "Plan: Free",
    onCopyId: () -> Unit = {},
    onViewReport: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {}
) {
    val colors = AegisTheme.colors
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                // Left-aligned title for tab screens
                TopAppBar(
                    title = {
                        Text(
                            text = "Profile",
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
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Protection Status
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (isProtected)
                            Brush.linearGradient(listOf(colors.success, colors.success.copy(alpha = 0.7f)))
                        else
                            Brush.linearGradient(listOf(colors.warning, colors.warning.copy(alpha = 0.7f)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️",
                    style = AegisTypography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isProtected) "Protected" else "At Risk",
                style = AegisTypography.headlineMedium,
                color = if (isProtected) colors.success else colors.warning
            )

            Text(
                text = if (isProtected) "System is active and monitoring" else "Setup permissions on Dashboard to activate protection",
                style = AegisTypography.bodyMedium,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Device ID Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DEVICE ID",
                        style = AegisTypography.overline,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = deviceId,
                    style = AegisTypography.headlineSmall,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCopyId,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Copy ID", style = AegisTypography.button)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This anonymous ID is used for encrypted support requests only.",
                    style = AegisTypography.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Report Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.3f),
                                colors.trustBadge.copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Privacy Report",
                    style = AegisTypography.titleLarge,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.background.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Local Processing Summary",
                        style = AegisTypography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$threatsBlocked threats blocked locally. $dataProcessed of data processed on-device without cloud upload.",
                        style = AegisTypography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "View Full Report",
                    style = AegisTypography.labelMedium,
                    color = colors.primary,
                    modifier = Modifier.clickable { onViewReport() }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // General Section
            Text(
                text = "General",
                style = AegisTypography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuItem(
                icon = Icons.Default.Star,
                title = "Subscription",
                subtitle = subscriptionType,
                actionText = "UPGRADE",
                actionColor = colors.primary,
                onClick = onSubscriptionClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onSettingsClick
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileMenuItem(
                icon = Icons.Default.Email,
                title = "Support",
                subtitle = "Encrypted Channel",
                onClick = onSupportClick
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Version
            Text(
                text = "Aegis v2.4.1 (Build 8821)",
                style = AegisTypography.caption,
                color = colors.textTertiary
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    actionText: String? = null,
    actionColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    val colors = AegisTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AegisTypography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        if (actionText != null) {
            Text(
                text = actionText,
                style = AegisTypography.labelMedium,
                color = actionColor
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary
        )
    }
}
