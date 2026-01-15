package app.aegis

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.compose.KoinContext

fun MainViewController() =
    ComposeUIViewController {
        KoinContext {
            App()
        }
    }
