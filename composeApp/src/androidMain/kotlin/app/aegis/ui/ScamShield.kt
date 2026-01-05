package app.aegis.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aegis.ai.gemini.types.Source

//TODO, migrate to compose commonMain
@Composable
fun ScamShield(
    reason: String,
    contactName: String,
    sources: List<Source>,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    
    // 🔴 1. MAIN CONTAINER (Deep Red Background)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB71C1C).copy(alpha = 0.95f)), // Deep Red
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            // 🛡️ ICON & TITLE
            Text("🛡️", fontSize = 60.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SCAM BLOCKED",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 🕵️ 2. EVIDENCE CARD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp)) // Gold Border
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Label
                Text(
                    text = "🔎 AI INVESTIGATION REPORT",
                    color = Color(0xFFFFD700), // Gold Text
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Reason Text
                Text(
                    text = reason,
                    color = Color.White,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                // 🔗 3. SOURCES SECTION (Only if sources exist)
                if (sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Divider line
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "VERIFIED SOURCES:",
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Render Links
                    sources.take(3).forEach { source ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .clickable {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        context.startActivity(intent)
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                        ) {
                            Text("🔗", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = source.title,
                                color = Color(0xFF64B5F6), // Link Blue
                                fontSize = 14.sp,
                                maxLines = 1,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 🛑 4. PRIMARY BUTTON (Block)
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "OK, I WILL BLOCK THEM",
                    color = Color(0xFFB71C1C),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ✅ 5. SECONDARY LINK (Trust)
            Text(
                text = "False Alarm? I trust '$contactName'",
                color = Color.LightGray,
                fontSize = 14.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable { onUnlock() }
                    .padding(8.dp)
            )
        }
    }
}