package app.aegis.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.aegis.ui.components.AegisPrimaryButton
import app.aegis.ui.theme.AegisTheme
import app.aegis.ui.theme.AegisTypography
import kotlinx.coroutines.launch

/**
 * Onboarding Screen with horizontal pager
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onSkip: () -> Unit = {},
    onComplete: () -> Unit = {}
) {
    val colors = AegisTheme.colors

    val pages = listOf(
        OnboardingPage(
            emoji = "🔍",
            title = "Identify Scammers",
            description = "Aegis analyzes chat patterns to warn you about impersonators and fake agents in real-time.",
            showMockChat = true
        ),
        OnboardingPage(
            emoji = "🛡️",
            title = "Safe Browsing",
            description = "We scan links in your messages to stop you from visiting malicious phishing sites before they steal your data.",
            showMockChat = false
        ),
        OnboardingPage(
            emoji = "📵",
            title = "Stop Blackmail",
            description = "We automatically hide your camera during calls from unknown numbers to prevent sextortion or fake police video call scams.",
            showMockChat = false,
            showCameraBlocked = true
        ),
        OnboardingPage(
            emoji = "✨",
            title = "You're All Set",
            description = "Aegis is ready to run in the background. Grant permissions on the next screen to activate full protection.",
            showMockChat = false,
            showCameraBlocked = false // Will default to ShieldHandUI (Big Shield) which fits nicely
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top bar with Skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (pagerState.currentPage > 0) {
                TextButton(onClick = {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                }) {
                    Text("←", color = colors.textSecondary)
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            TextButton(onClick = onSkip) {
                Text(
                    text = "Skip",
                    style = AegisTypography.labelMedium,
                    color = colors.textSecondary
                )
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = pages[page],
                modifier = Modifier.fillMaxSize()
            )
        }

        // Bottom section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 24.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == pagerState.currentPage)
                                    colors.primary
                                else
                                    colors.textTertiary
                            )
                    )
                }
            }

            // Button
            val isLastPage = pagerState.currentPage == pages.size - 1

            AegisPrimaryButton(
                text = if (isLastPage) "Enable Protection" else if (pagerState.currentPage == 0) "Continue" else "Next",
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                leadingIcon = if (isLastPage) Icons.Default.Check else null
            )


        }
    }
}

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val showMockChat: Boolean = false,
    val showCameraBlocked: Boolean = false
)

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    modifier: Modifier = Modifier
) {
    val colors = AegisTheme.colors

    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual area
        if (page.showMockChat) {
            MockChatUI()
        } else if (page.showCameraBlocked) {
            CameraBlockedUI()
        } else {
            ShieldHandUI()
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title with colored word
        val titleParts = page.title.split(" ")
        if (titleParts.size > 1) {
            Row {
                Text(
                    text = titleParts.dropLast(1).joinToString(" ") + " ",
                    style = AegisTypography.displayMedium,
                    color = colors.textPrimary
                )
                Text(
                    text = titleParts.last(),
                    style = AegisTypography.displayMedium,
                    color = colors.primary
                )
            }
        } else {
            Text(
                text = page.title,
                style = AegisTypography.displayMedium,
                color = colors.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            style = AegisTypography.bodyLarge,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MockChatUI() {
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        // Sender info
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.textTertiary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Unknown Sender", style = AegisTypography.titleSmall, color = colors.textPrimary)
                Text("Today, 2:41 PM", style = AegisTypography.caption, color = colors.textTertiary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message bubble with SCAM badge
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .padding(12.dp)
            ) {
                Text(
                    text = "URGENT: Your account has been flagged. Click here to verify immediately.",
                    style = AegisTypography.bodyMedium,
                    color = colors.textPrimary
                )
            }

            // SCAM Badge
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.error)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "SCAM",
                    style = AegisTypography.labelSmall,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Analyzing indicator
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.primary.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Analyzing patterns...",
                style = AegisTypography.labelSmall,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun ShieldHandUI() {
    val colors = AegisTheme.colors

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        colors.primary.copy(alpha = 0.3f),
                        colors.primary.copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🛡️",
            style = AegisTypography.displayLarge.copy(fontSize = AegisTypography.displayLarge.fontSize * 2)
        )
    }
}

@Composable
private fun CameraBlockedUI() {
    val colors = AegisTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Camera blocked icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(colors.primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📵",
                style = AegisTypography.displayMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Camera blocked badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(colors.primary.copy(alpha = 0.2f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "CAMERA BLOCKED",
                style = AegisTypography.labelSmall,
                color = colors.primary
            )
        }
    }
}
