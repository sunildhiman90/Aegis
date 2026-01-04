package app.aegis.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt

class OverlayManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: FrameLayout? = null

    // 🟡 LEVEL 2: WARNING (Non-Blocking)
    fun showWarning(text: String) {
        if (overlayView != null) return // Already showing something

        overlayView = FrameLayout(context).apply {
            setBackgroundColor("#B3FFC107".toColorInt()) // Semi-transparent YELLOW

            // Small Banner at the TOP
            val banner = TextView(context).apply {
                this.text = "⚠️ Aegis is verifying this chat... ($text)"
                setTextColor(Color.BLACK)
                textSize = 16f
                gravity = Gravity.CENTER
                setBackgroundColor(Color.YELLOW)
                setPadding(20, 20, 20, 20)

                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP
                )
            }
            addView(banner)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT, // Only take top space
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            // FLAG_NOT_FOCUSABLE + FLAG_NOT_TOUCH_MODAL = User can click THROUGH it to the app below
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        try { windowManager.addView(overlayView, params) } catch (e: Exception) {}
    }


    // 🔴 LEVEL 3: DANGER (Blocking) - The existing function
//    fun showShield(reason: String, onUnlock: () -> Unit) {
//        hideShield() // Remove yellow banner if exists
//
//        if (overlayView != null) return // Already showing
//
//        overlayView = FrameLayout(context).apply {
//            setBackgroundColor(Color.parseColor("#E6FF0000")) // Semi-transparent RED
//
//            val message = TextView(context).apply {
//                text = "⚠️ SCAM DETECTED ⚠️\n\n$reason\n\nTouch has been disabled."
//                setTextColor(Color.WHITE)
//                textSize = 24f
//                gravity = Gravity.CENTER
//                layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    Gravity.CENTER
//                )
//            }
//
//            val unlockBtn = Button(context).apply {
//                text = "Hold to Unlock (False Positive)"
//                layoutParams = FrameLayout.LayoutParams(
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    FrameLayout.LayoutParams.WRAP_CONTENT,
//                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//                ).apply { bottomMargin = 100 }
//
//                // Simple logic: In a real app, use onLongClickListener
//                setOnClickListener {
//                    hideShield()
//                    onUnlock()
//                }
//            }
//
//            addView(message)
//            addView(unlockBtn)
//        }
//
//        val params = WindowManager.LayoutParams(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.MATCH_PARENT,
//            // TYPE_ACCESSIBILITY_OVERLAY is crucial - it sits above almost everything
//            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
//            // FLAG_NOT_FOCUSABLE = allows simple clicks, but we want to consume them.
//            // Removing FLAG_NOT_FOCUSABLE makes this view consume ALL input keys/touches.
//            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_FULLSCREEN,
//            PixelFormat.TRANSLUCENT
//        )
//
//        try {
//            windowManager.addView(overlayView, params)
//        } catch (e: Exception) {
//            e.printStackTrace() // Permission might be missing
//        }
//    }

    // 🔴 Red Shield (Blocking) - UPDATED
    fun showShield(reason: String, contactName: String, onUnlock: () -> Unit, onDismiss: () -> Unit) {
        hideShield()

        overlayView = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#F2B71C1C")) // Solid Deep Red (High Alert)
            isClickable = true
            isFocusable = true

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                setPadding(60, 60, 60, 60)
            }

            // ICON
            val icon = TextView(context).apply {
                text = "🛡️"
                textSize = 60f
                gravity = Gravity.CENTER
            }

            // TITLE
            val title = TextView(context).apply {
                text = "SCAM DETECTED"
                textSize = 28f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 20, 0, 20)
                paint.isFakeBoldText = true
            }

            // REASON (The Agent Evidence)
            val desc = TextView(context).apply {
                text = "Gemini Agent found evidence of fraud:\n\n$reason"
                textSize = 16f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 60)
            }

            // BUTTON 1: "I will block them" (The Safe Exit)
            val dismissBtn = Button(context).apply {
                text = "OK, I WILL BLOCK THEM"
                setBackgroundColor(Color.WHITE)
                setTextColor(Color.RED)
                textSize = 16f
                setPadding(30, 20, 30, 20)
                setOnClickListener {
                    hideShield()
                    onDismiss() // Callback to snooze alerts
                }
            }

            // BUTTON 2: "False Alarm" (The Whitelist)
            val trustBtn = TextView(context).apply {
                text = "False Alarm? I trust '$contactName'"
                setTextColor(Color.LTGRAY)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(0, 40, 0, 0)
                setOnClickListener {
                    hideShield()
                    onUnlock() // Whitelist callback
                }
            }

            container.addView(icon)
            container.addView(title)
            container.addView(desc)
            container.addView(dismissBtn) // Primary Action
            container.addView(trustBtn)   // Secondary Action
            addView(container)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try { windowManager.addView(overlayView, params) } catch (e: Exception) {}
    }

    fun hideShield() {
        overlayView?.let {
            windowManager.removeView(it)
            overlayView = null
        }
    }
}