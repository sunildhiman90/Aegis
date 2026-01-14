package app.aegis.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Yellow caution banner for non-blocking warnings.
 * Matches design: cautionary_text_banner
 */
@Composable
fun ScamWarning(
    text: String,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Top Padding to avoid status bar overlap if needed, usually handled by OverlayManager logic 
    // but adding some safe padding inside the container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp) // Margin from screen edges
    ) {
        // Warning Container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(Color(0xFFFFC107), RoundedCornerShape(12.dp)) // warning-bg (Amber)
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = Color.Black, // warning-text
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Suspicious Pattern Detected",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = text, // Dynamic text from service
                    color = Color.Black.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Learn more",
                    color = Color.Black.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { /* TODO: Open help */ }
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Close Button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}