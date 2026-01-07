package app.aegis.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.aegis.ai.gemini.GeminiClient
import app.aegis.ai.gemini.types.Source
import app.aegis.data.TrustRepository
import app.aegis.helper.ScreenshotHelper
import app.aegis.models.RiskLevel
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream

class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var analysisJob: Job? = null // 🛑 STORE THE CURRENT JOB

    private val geminiClient = GeminiClient()
    private lateinit var overlayManager: OverlayManager

    private lateinit var screenshotHelper: ScreenshotHelper // New Helper

    // Snooze Logic
    private val snoozedScreens = mutableMapOf<Int, Long>()
    private var globalPauseUntil = 0L

    private var lastAnalysisTime = 0L
    private val DEBOUNCE = 1000L // Reduced debounce slightly for responsiveness

    private val SUPPORTED_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.google.android.apps.messaging",
        "org.telegram.messenger",
        "com.samsung.android.messaging"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        Log.d("Aegis", "✅ Service Online")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. GLOBAL CHECKS
        if (event.packageName?.toString() !in SUPPORTED_PACKAGES) return
        if (System.currentTimeMillis() < globalPauseUntil) return

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < DEBOUNCE) return

        val rootNode = rootInActiveWindow ?: return

        // 1. VIDEO CALL CHECK (Priority)
        if (detectVideoCallScam(rootNode)) return

        // 2. CHAT SCAM CHECK
        if (!isRealChatScreen(rootNode)) return


        // 3. EXTRACT CONTENT
        val capturedText = StringBuilder()
        traverseNode(rootNode, capturedText)
        val chatContent = capturedText.toString()

        if (chatContent.length < 10) return

        // 4. CHECK SNOOZE
        val contentHash = chatContent.hashCode()
        if (isScreenSnoozed(contentHash)) return

        val contactName = extractTitle(rootNode)

        // NEW CHECK: IGNORE BUSINESS & SAVED CONTACTS
        // If the name has letters (e.g. "Flipkart", "Mom"), it's not a raw unknown number.
        // We assume Saved Contacts and Business Accounts are safe to avoid false positives.
        if (!isLikelyPhoneNumber(contactName)) {
            Log.d("Aegis", "Skipping analysis: '$contactName' is a known name/business.")
            return
        }

        if (TrustRepository.isTrusted(contactName)) return

        // CRITICAL FIX: NEW EVENT = CANCEL OLD ANALYSIS
        // This stops the "Ghost Dismiss" bug.
        // If we are analyzing a new screen, the old Gemini result is irrelevant.
        analysisJob?.cancel()

        lastAnalysisTime = currentTime

        // 5. RUN ANALYSIS
        val localVerdict = SecurityTools.analyzeLocally(chatContent)

        when (localVerdict) {
            LocalRisk.HIGH_RISK -> {
                Log.d("Aegis", "🔴 LOCAL BLOCK (High Risk)")
                // Stop any pending Gemini loading indicators
                analysisJob?.cancel()

                showBlockingShield(
                    reason = "Critical keywords detected locally.",
                    contactName = contactName,
                    contentHash = contentHash,
                    sources = emptyList()
                )
            }
            LocalRisk.SUSPICIOUS -> {
                Log.d("Aegis", "🟡 SUSPICIOUS - Starting Gemini...")
                overlayManager.showWarning("Verifying conversation pattern...")

                // Start Gemini in a tracked Job
                analysisJob = serviceScope.launch {
                    val verdict = geminiClient.analyze(chatContent)

                    if (!isActive) return@launch // Don't update UI if job was cancelled

                    if (verdict.riskLevel == RiskLevel.DANGER) {
                        showBlockingShield(verdict.reason, contactName, contentHash, verdict.sources)
                    } else {
                        // Only hide if we haven't been cancelled/replaced
                        overlayManager.hideShield()
                    }
                }
            }
            LocalRisk.SAFE -> {
                // Do nothing
            }
        }
    }

    // --- HELPER TO UNIFY SHIELD LOGIC ---
    private fun showBlockingShield(reason: String, contactName: String, contentHash: Int, sources: List<Source> = emptyList()) {
        overlayManager.showShield(
            reason = reason,
            contactName = contactName,
            onUnlock = {
                TrustRepository.trustContact(contactName)
                overlayManager.hideShield()
            },
            onDismiss = {
                // 🛑 TRIGGER ALL DEFENSES
                // 1. Pause everything for 3 seconds
                globalPauseUntil = System.currentTimeMillis() + 3000L

                // 2. Snooze this specific text for 30s
                snoozedScreens[contentHash] = System.currentTimeMillis() + 30000L

                // 3. Hide UI
                overlayManager.hideShield()
            },
            sources = sources
        )
    }

    private fun isScreenSnoozed(hash: Int): Boolean {
        val expiry = snoozedScreens[hash] ?: return false
        if (System.currentTimeMillis() > expiry) {
            snoozedScreens.remove(hash)
            return false
        }
        return true
    }

    // --- NODE TRAVERSAL (Keep existing) ---
    private fun isRealChatScreen(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        return hasChatInput(node)
    }

    private fun hasChatInput(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.className == "android.widget.EditText") {
            val text = (node.text ?: "").toString().lowercase()
            val hint = (node.hintText ?: "").toString().lowercase()
            if (text.contains("search") || hint.contains("search")) return false
            return true
        }
        val count = node.childCount
        for (i in 0 until count) {
            if (hasChatInput(node.getChild(i))) return true
        }
        return false
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return
        if (!node.text.isNullOrEmpty()) sb.append(node.text).append(" ")
        if (!node.contentDescription.isNullOrEmpty()) sb.append(node.contentDescription).append(" ")
        val count = node.childCount
        for (i in 0 until count) traverseNode(node.getChild(i), sb)
    }

    private fun extractTitle(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "Unknown"
        val waList = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (!waList.isNullOrEmpty()) return waList[0].text?.toString() ?: "Unknown"

        val possibleTitles = mutableListOf<String>()
        collectPossibleTitles(rootNode, possibleTitles)
        return possibleTitles.firstOrNull {
            it.length < 25 && !it.contains(":") && !it.contains("Type")
        } ?: "Unknown"
    }

    private fun collectPossibleTitles(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null || list.size > 3) return
        if (!node.text.isNullOrEmpty()) {
            val text = node.text.toString()
            if (text.length > 2) list.add(text)
        }
        val count = node.childCount
        for (i in 0 until count) collectPossibleTitles(node.getChild(i), list)
    }

    // ---------------------------------------------------------
    // 🛑 VIDEO CALL IMPLEMENTATION
    // ---------------------------------------------------------
    private fun detectVideoCallScam(rootNode: AccessibilityNodeInfo): Boolean {

        // If the screen has a "Type a message" box, it is a CHAT screen, not an incoming call.
        // This prevents the AI from reading the top-right "Video Call" button and panicking.
        if (hasChatInput(rootNode)) return false

        // 1. Fast scrape of text
        val screenText = mutableListOf<String>()
        collectAllText(rootNode, screenText)

        // 2. Strict Keyword Check
        // We only care if it explicitly says "Incoming" or calls out the specific state.
        // Just "Video call" is too common (it appears on buttons).
        val isVideoCall = screenText.any {
            it.contains("Incoming video call", ignoreCase = true) ||
                    it.contains("Incoming voice call", ignoreCase = true) ||
                    (it.contains("WhatsApp video call", ignoreCase = true) && it.contains("Decline", ignoreCase = true))
        }

        if (!isVideoCall) return false

        val callerId = screenText.firstOrNull { text ->
            val cleanText = text.trim()
            !cleanText.equals("incoming video call", ignoreCase = true) &&
                    !cleanText.equals("whatsapp video call", ignoreCase = true) &&
                    !cleanText.equals("decline", ignoreCase = true) &&
                    !cleanText.equals("answer", ignoreCase = true) &&
                    cleanText.length > 2
        } ?: "Unknown"

        // 3. Is it an Unknown Number? (High Risk)
        if (isLikelyPhoneNumber(callerId)) {
            Log.d("Aegis", "🚨 UNKNOWN VIDEO CALL: $callerId")
            overlayManager.showWarning("⚠️ Unknown Video Call. Scanning for threats...")

            serviceScope.launch {
                // 1. Capture (Android Specific)
                val bitmap = screenshotHelper.captureScreen()

                if (bitmap != null) {
                    // 2. Convert (Android Specific)
                    val base64String = encodeBitmapToBase64(bitmap)

                    // 3. Analyze (Shared KMP Logic)
                    // We pass the string, so GeminiClient doesn't need to know about Bitmaps
                    val verdict = geminiClient.analyzeImage(base64String)

                    if (verdict.riskLevel == RiskLevel.DANGER) {
                        overlayManager.showShield(
                            reason = "🚨 VISUAL THREAT: ${verdict.reason}",
                            contactName = callerId,
                            onDismiss = { overlayManager.hideShield() },
                            onUnlock = { overlayManager.hideShield() }
                        )
                    }
                }
            }
            return true
        }
        return true // Still return true to block "Chat" logic from running on Call screen
    }

    // ---------------------------------------------------------
    // 🛑 CRITICAL HELPER: PHONE NUMBER DETECTION
    // ---------------------------------------------------------
    /**
     * Determines if a string is likely a raw phone number (Unknown)
     * or a saved contact name (Trusted).
     */
    private fun isLikelyPhoneNumber(text: String): Boolean {
        val clean = text.trim()

        // 1. Explicit Indicators of Risk
        if (clean.contains("Unknown", true) ||
            clean.contains("Private", true) ||
            clean.contains("Spam", true)) {
            return true
        }

        // 2. Saved Contacts usually contain Letters (e.g., "Mom", "Rahul JLL")
        // If it has letters, it is NOT a raw phone number.
        if (clean.any { it.isLetter() }) {
            return false
        }

        // 3. Raw Numbers contain mostly digits (e.g., "+91 999...", "0987...")
        // We strip special chars (+, -, spaces) and count digits.
        val digitCount = clean.count { it.isDigit() }

        // If it has > 6 digits and NO letters, it's a Phone Number.
        return digitCount > 6
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        if (!node.text.isNullOrEmpty()) list.add(node.text.toString())
        val count = node.childCount
        for (i in 0 until count) collectAllText(node.getChild(i), list)
    }

    /**
     * Helper to convert Bitmap to Base64 String
     * This runs on the IO dispatcher to avoid blocking the UI thread.
     */
    private suspend fun encodeBitmapToBase64(bitmap: Bitmap): String = withContext(Dispatchers.Default) {
        val outputStream = ByteArrayOutputStream()
        // Compress to JPEG, Quality 60 (Good balance for AI analysis vs Size)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
        val byteArray = outputStream.toByteArray()
        // NO_WRAP omits newlines, which is safer for JSON payloads
        Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
    
    override fun onInterrupt() {
        serviceScope.cancel()
    }

}