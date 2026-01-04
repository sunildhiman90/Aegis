package app.aegis

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Aegis Setup")

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Overlay Permission
                Button(onClick = {
                    // Check if permission is already granted
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        try {
                            // Method 1: Open specific page for THIS app
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                "package:$packageName".toUri()
                            )
                            startActivity(intent)
                        } catch (e: Exception) {
                            // Method 2: Fallback to the general list if the device ignores the package URI
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            startActivity(intent)
                        }
                    }
                }) {
                    Text("1. Grant Overlay Permission")
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. Accessibility Permission
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }) {
                    Text("2. Enable Accessibility Service")
                }
            }

        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}