package app.aegis.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aegis.ai.gemini.types.Source
import aegis.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import app.aegis.ui.theme.AegisTheme

@Composable
fun ScamShield(
    reason: String,
    contactName: String,
    sources: List<Source>,
    isCall: Boolean = false,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    onEndCall: (() -> Unit)? = null,
    onSourceClick: (Source) -> Unit
) {
    val scrollState = rememberScrollState()

    // Pulsing animation for the main icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // 🔴 MAIN CONTAINER - Gradient Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF450a0a), // Darker red top
                        Color(0xFF220606),
                        Color(0xFF110505)  // Almost black bottom
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ⚠️ Critical Alert Badge
            Box(
                modifier = Modifier
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape)
                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFEF4444), // danger-bright
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.scam_shield_critical_alert),
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 🛑 Animated Icon
            Box(
                modifier = Modifier
                    .size(80.dp) // Reduced from 120dp
                    .scale(scale),
                contentAlignment = Alignment.Center
            ) {
                // Glow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFDC2626).copy(alpha = 0.2f), CircleShape)
                        .clip(CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.GppBad,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(64.dp) // Reduced from 100dp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Headlines
            Text(
                text = stringResource(Res.string.scam_shield_danger_detected),
                color = Color.White,
                fontSize = 24.sp, // Reduced from 40sp
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(if (isCall) Res.string.scam_shield_flagged_call else Res.string.scam_shield_flagged_chat),
                color = Color(0xFFFECACA), // red-200
                fontSize = 16.sp, // Reduced from 18sp
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp)) // Reduced from 32dp

            // 🤖 AI Analysis Report Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0E0E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(15.dp, RoundedCornerShape(12.dp))
            ) {
                Column {
                    // Card Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF450A0A).copy(alpha = 0.5f))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.SmartToy, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(Res.string.scam_shield_ai_report),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDC2626), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                stringResource(Res.string.scam_shield_match_found),
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                    )

                    // Card Content
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Threat Visual (Placeholder)
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Scam Risk",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "99% CONFIDENCE",
                                        color = Color(0xFFF87171),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(1.dp)
                                        .background(Color(0xFF7F1D1D).copy(alpha = 0.5f))
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = reason,
                                    color = Color(0xFFFECACA), // red-200
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )

                                if (sources.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "DETECTION SOURCES",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    sources.forEach { source ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onSourceClick(source) }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Link,
                                                contentDescription = null,
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = source.title,
                                                color = Color(0xFFEF4444),
                                                fontSize = 12.sp,
                                                textDecoration = TextDecoration.Underline
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (isCall) {
                // Call Specific: END CALL + DISMISS
                /*
                Button(
                    onClick = { onEndCall?.invoke() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), // Red
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CallEnd, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(Res.string.overlay_block_end_call),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                */

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(Res.string.scam_shield_dismiss), color = Color.White)
                }
            } else {
                // Chat Specific: DISMISS ALERT (Primary)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(Res.string.scam_shield_dismiss), color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // False Alarm Link
            Text(
                text = stringResource(Res.string.scam_shield_false_alarm),
                color = Color(0xFF9CA3AF), // gray-400
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onUnlock() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
@Preview
fun ScamShieldPreview() {
    AegisTheme {
        ScamShield(
            reason = "Aegis detected patterns typical of a 'Sextortion' attempt. The caller matches high-risk heuristics.",
            contactName = "+91 98765 43210",
            sources = listOf(
                Source("Cyber Crime Awareness", "https://cybercrime.gov.in"),
                Source("Known Scam Patterns", "https://example.com/scams"),
            ),
            isCall = true,
            onDismiss = {},
            onUnlock = {},
            onSourceClick = {}
        )
    }
}