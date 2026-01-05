package app.aegis.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import app.aegis.ai.gemini.types.Source
import app.aegis.ui.ScamShield
import androidx.compose.ui.platform.ViewCompositionStrategy
import app.aegis.ui.ScamWarning

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    // Hold reference to our dummy lifecycle
    private var lifecycleOwner: OverlayLifecycleOwner? = null


    // 🟡 YELLOW WARNING (Non-Blocking)
    fun showWarning(text: String) {
        if (overlayView != null) return // Don't spam if already showing

        setupComposeView {
            ScamWarning(text = text)
        }

        // Params for Banner: Top only, Click-through
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_NOT_FOCUSABLE = User can click chat while banner is there
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 🔴 RED SHIELD (Blocking)
    fun showShield(
        reason: String,
        contactName: String,
        sources: List<Source> = emptyList(),
        onUnlock: () -> Unit,
        onDismiss: () -> Unit
    ) {
        hideShield() // Reset previous view

        setupComposeView {
            ScamShield(
                reason = reason,
                contactName = contactName,
                sources = sources,
                onDismiss = {
                    hideShield()
                    onDismiss()
                },
                onUnlock = {
                    hideShield()
                    onUnlock()
                }
            )
        }

        // Params for Shield: Full Screen, Blocking
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_WATCH_OUTSIDE_TOUCH is good for Compose gestures
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- HELPER TO REDUCE DUPLICATE CODE ---
    private fun setupComposeView(content: @androidx.compose.runtime.Composable () -> Unit) {
        // 1. Lifecycle
        lifecycleOwner = OverlayLifecycleOwner()
        lifecycleOwner?.onCreate()
        lifecycleOwner?.onResume()

        // 2. View
        overlayView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            lifecycleOwner?.attachToView(this)
            setContent(content)
        }
    }

    fun hideShield() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
                lifecycleOwner?.onDestroy()
                lifecycleOwner = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
        }
    }


}