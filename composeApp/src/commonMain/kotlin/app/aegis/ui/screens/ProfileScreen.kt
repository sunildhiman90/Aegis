package app.aegis.ui.screens

import aegis.composeapp.generated.resources.*
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
import androidx.compose.runtime.*
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
import app.aegis.ui.viewmodel.ProfileViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Profile Screen - Privacy profile with device info and settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    isProtected: Boolean = true,
    subscriptionType: String? = null,
    onCopyId: () -> Unit = {},
    onViewReport: () -> Unit = {},
    onSubscriptionClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val colors = AegisTheme.colors
    val scrollState = rememberScrollState()

    val privacyStats by viewModel.privacyStats.collectAsState()
    val deviceId = viewModel.deviceId

    Scaffold(
        containerColor = colors.background,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.profile_title),
                            style = AegisTypography.headlineMedium,
                            color = colors.textPrimary,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.background,
                        ),
                )
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
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Protection Status
            Box(
                modifier =
                    Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isProtected) {
                                Brush.linearGradient(listOf(colors.success, colors.success.copy(alpha = 0.7f)))
                            } else {
                                Brush.linearGradient(listOf(colors.warning, colors.warning.copy(alpha = 0.7f)))
                            },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🛡️",
                    style = AegisTypography.displayMedium,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isProtected) stringResource(Res.string.profile_protected) else stringResource(Res.string.profile_at_risk),
                style = AegisTypography.headlineMedium,
                color = if (isProtected) colors.success else colors.warning,
            )

            Text(
                text =
                    if (isProtected) {
                        stringResource(
                            Res.string.profile_system_active,
                        )
                    } else {
                        stringResource(Res.string.profile_system_inactive)
                    },
                style = AegisTypography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Device ID Card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.profile_device_id),
                        style = AegisTypography.overline,
                        color = colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = deviceId,
                    style = AegisTypography.headlineSmall,
                    color = colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCopyId,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = Color.White,
                        ),
                ) {
                    Text(stringResource(Res.string.profile_copy_id), style = AegisTypography.button)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.profile_id_note),
                    style = AegisTypography.caption,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Privacy Report Card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        colors.primary.copy(alpha = 0.3f),
                                        colors.trustBadge.copy(alpha = 0.2f),
                                    ),
                            ),
                        ).padding(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.profile_privacy_report),
                    style = AegisTypography.titleLarge,
                    color = colors.textPrimary,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.background.copy(alpha = 0.5f))
                            .padding(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.profile_local_processing),
                        style = AegisTypography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            stringResource(
                                Res.string.profile_threats_blocked,
                                privacyStats.totalThreatsBlocked,
                                privacyStats.dataProcessedMb,
                            ),
                        style = AegisTypography.bodySmall,
                        color = colors.textSecondary,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(Res.string.profile_view_report),
                    style = AegisTypography.labelMedium,
                    color = colors.primary,
                    modifier = Modifier.clickable { onViewReport() },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // General Section
            Text(
                text = stringResource(Res.string.profile_general),
                style = AegisTypography.titleLarge,
                color = colors.textPrimary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileMenuItem(
                icon = Icons.Default.Star,
                title = stringResource(Res.string.profile_subscription),
                subtitle = subscriptionType ?: stringResource(Res.string.profile_plan_free),
                actionText = stringResource(Res.string.profile_upgrade),
                actionColor = colors.primary,
                onClick = onSubscriptionClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = stringResource(Res.string.profile_settings),
                onClick = onSettingsClick,
            )

            Spacer(modifier = Modifier.height(8.dp))

            ProfileMenuItem(
                icon = Icons.Default.Email,
                title = stringResource(Res.string.profile_support),
                subtitle = stringResource(Res.string.profile_encrypted_channel),
                onClick = onSupportClick,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Version
            Text(
                text = stringResource(Res.string.profile_version, "2.4.1", "8821"),
                style = AegisTypography.caption,
                color = colors.textTertiary,
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
    onClick: () -> Unit,
) {
    val colors = AegisTheme.colors

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .clickable { onClick() }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = AegisTypography.titleMedium,
                color = colors.textPrimary,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AegisTypography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        if (actionText != null) {
            Text(
                text = actionText,
                style = AegisTypography.labelMedium,
                color = actionColor,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textTertiary,
        )
    }
}
