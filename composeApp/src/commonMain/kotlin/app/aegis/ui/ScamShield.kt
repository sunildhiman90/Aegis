package app.aegis.ui


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.aegis.ai.gemini.types.Source

@Composable
fun ScamShield(
    reason: String,
    contactName: String,
    sources: List<Source>,
    onDismiss: () -> Unit,
    onUnlock: () -> Unit,
    onSourceClick: (Source) -> Unit
) {
    val scrollState = rememberScrollState()

    // 🔴 MAIN CONTAINER
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFB71C1C).copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxHeight()
        ) {
            // 🛡️ HEADER (Fixed at Top)
            Spacer(modifier = Modifier.height(20.dp))
            Text("🛡️", fontSize = 60.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "SCAM DETECTED",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 🕵️ EVIDENCE CARD (Flexible Height)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // Take available space, but allow buttons to push up
                    .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FIXED HEADER Inside Card
                Text(
                    text = "🔎 AI INVESTIGATION REPORT",
                    color = Color(0xFFFFD700),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // SCROLLABLE CONTENT (Reason)
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false) // Push footer down, but shrink if needed
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = reason,
                        color = Color.White,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // FIXED FOOTER (Resources)
                if (sources.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.DarkGray))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("VERIFIED SOURCES:", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        sources.take(3).forEach { source ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable { onSourceClick(source) }
                            ) {
                                Text("🔗", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(source.title, color = Color(0xFF64B5F6), fontSize = 14.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 🛑 FOOTER (Fixed at Bottom)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK, I WILL BLOCK THEM", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "False Alarm? I trust '$contactName'",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onUnlock() }.padding(8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}