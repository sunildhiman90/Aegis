package app.aegis.ui

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.GppBad
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phishing
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.SmartToy
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

/**
 * Phishing warning overlay.
 * Adapted from 'critical_danger_warning' design (originally for video calls) to maintain consistent Danger UI.
 */
@Composable
fun PhishingShield(
    reason: String,
    url: String,
    onReport: () -> Unit,
    onDismiss: () -> Unit,
    onTrust: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Pulsing animation
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
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

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
                        text = "PHISHING ALERT",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🛑 Animated Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
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
                    imageVector = Icons.Default.Phishing,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(100.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Headlines
            Text(
                text = "MALICIOUS\nLINK",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aegis AI identified this link as malicious.",
                color = Color(0xFFFECACA), // red-200
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 🤖 AI Analysis Report Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0E0E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // Take available space
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
                                "AI ANALYSIS REPORT",
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
                            Text("THREAT DETECTED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        // URL Display
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF000000).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = url,
                                    color = Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reason
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), CircleShape)
                                    .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Dangerous,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    "Detection Reason",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = reason,
                                    color = Color(0xFFFECACA), // red-200
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🛑 Action Button (Report)
            Button(
                onClick = onReport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)), // red-600
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.Report,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        "REPORT & TAKEDOWN",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                    Text(
                        "Help protect others from this scam",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Button (Dismiss)
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Dismiss (Don't open)", color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // False Alarm Link
            Text(
                text = "False Alarm (Proceed to site)",
                color = Color(0xFF9CA3AF), // gray-400
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onTrust() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

