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
    private var currentAnalysisHash = 0 // Track what content is currently being analyzed
    private var currentAnalysisText = ""
    private var currentAnalysisContact = ""

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

    // Add this class-level variable to prevent re-scanning static screens
    private var lastProcessedHash = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 1. GLOBAL CHECKS & BATTERY OPTIMIZATION 🔋
        if (event.packageName?.toString() !in SUPPORTED_PACKAGES) return
        if (System.currentTimeMillis() < globalPauseUntil) return

        // 🛑 OPTIMIZATION: Ignore Scroll Events to save CPU
        // We only care when the window content actually changes
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED) return

        val rootNode = rootInActiveWindow ?: return

        // 2. VIDEO CALL CHECK (Priority #1)
        // Checks for "Incoming Video Call" screens immediately
        if (detectVideoCallScam(rootNode)) return

        // 3. CHAT SCAM CHECK
        // Must be a real chat screen (have an input box) to proceed
        if (!isRealChatScreen(rootNode)) return

        // 4. IDENTIFY CONTACT (Fast Check)
        val contactName = extractTitle(rootNode)

        // 🛑 TRUST CHECK: If user manually trusted them, or we auto-trusted them before
        if (TrustRepository.isTrusted(contactName)) return

        // 5. EXTRACT & DEDUPLICATE CONTENT
        // Don't re-scan if we just analyzed this exact screen 100ms ago
        val capturedText = StringBuilder()
        traverseNode(rootNode, capturedText)
        val chatContent = capturedText.toString()

        if (chatContent.length < 10) return

        val contentHash = chatContent.hashCode()
        if (contentHash == lastProcessedHash) return // 🔋 CPU Saver: Static Screen
        if (isScreenSnoozed(contentHash)) return    // User dismissed this specific text

        // Update state
        lastProcessedHash = contentHash

        // Debounce only for meaningful new content updates
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < DEBOUNCE) return
        lastAnalysisTime = currentTime

        // 6. DECISION ENGINE (The "Smart" Logic)

        if (isLikelyPhoneNumber(contactName)) {
            // 🔴 CASE A: UNKNOWN NUMBER (Raw Digits) -> HIGH RISK

            // 🔋 GATEKEEPER: Don't waste money analyzing "Hi" or "Ok"
            val localRisk = SecurityTools.analyzeLocally(chatContent)

            if (localRisk == LocalRisk.SAFE && chatContent.length < 50) {
                Log.d("Aegis", "🔋 Unknown number sent safe/short text. Ignoring (Battery Saver).")
                return
            }

            // If text is LONG or SUSPICIOUS -> Analyze with Gemini
            Log.d("Aegis", "⚠️ Unknown Number + Complex Text. Analyzing...")
            runGeminiAnalysis(contactName, chatContent, contentHash)

        } else {
            // 🟡 CASE B: NAMED CONTACT ("Mom", "Rahul")
            val localVerdict = SecurityTools.analyzeLocally(chatContent)

            if (localVerdict != LocalRisk.SAFE) {
                // Suspicious keyword found -> Verify with Gemini
                runGeminiAnalysis(contactName, chatContent, contentHash)
            } else {
                // ✅ MESSAGE IS SAFE
                Log.d("Aegis", "Named contact '$contactName' passed local checks.")

                // 💡 NEW OPTIMIZATION: Build Trust
                // If this friend sends us a safe message, increase their "Trust Score".
                // After 3 safe messages, add them to the Ignore List permanently.
                TrustRepository.increaseTrustScore(contactName)
            }
        }
    }

    // --- HELPER TO AVOID DUPLICATE CODE ---
    private fun runGeminiAnalysis(contactName: String, chatContent: String, contentHash: Int) {
        
        // 1. Check if we are analyzing the SAME content (Exact Hash)
        if (analysisJob?.isActive == true && currentAnalysisHash == contentHash) {
             return
        }
        
        // 2. Check if we are analyzing SIMILAR content (Scrolling)
        if (analysisJob?.isActive == true) {
            // Must be same contact
            if (contactName == currentAnalysisContact) {
                 if (isSimilar(chatContent, currentAnalysisText)) {
                     Log.d("Aegis", "♻️ Skipping re-analysis: Content is similar (Scroll detected).")
                     return
                 }
            }
        }
        
        // Cancel any previous analysis (User scrolled to new message)
        analysisJob?.cancel()
        currentAnalysisHash = contentHash
        currentAnalysisText = chatContent
        currentAnalysisContact = contactName

        // Show UI feedback
        overlayManager.showWarning("Verifying conversation pattern...")

        analysisJob = serviceScope.launch {
            val verdict = geminiClient.analyze(chatContent)
            if (!isActive) return@launch

            if (verdict.riskLevel == RiskLevel.DANGER) {
                showBlockingShield(verdict.reason, contactName, contentHash, verdict.sources)
            } else {
                overlayManager.hideShield()
            }
        }
    }
    
    private fun isSimilar(text1: String, text2: String): Boolean {
        // Optimization: identical strings
        if (text1 == text2) return true
        
        // Optimization: Length diff > 30% means it changed a lot
        val len1 = text1.length
        val len2 = text2.length
        val maxLen = kotlin.math.max(len1, len2)
        if (maxLen == 0) return true
        
        val diff = kotlin.math.abs(len1 - len2)
        if (diff.toDouble() / maxLen.toDouble() > 0.3) return false // Length changed significantly

        // Jaccard Similarity on Words
        // We filter short words to avoid matching "is", "the", "at"
        // Use regex for spaces and whitespace
        val words1 = text1.split(Regex("\\s+")).filter { it.length > 3 }.toHashSet()
        val words2 = text2.split(Regex("\\s+")).filter { it.length > 3 }.toHashSet()
        
        if (words1.isEmpty() && words2.isEmpty()) return true
        if (words1.isEmpty() || words2.isEmpty()) return false
        
        val intersection = words1.count { words2.contains(it) }
        val union = words1.size + words2.size - intersection // |A U B| = |A| + |B| - |A n B|
        
        if (union == 0) return true
        
        val score = intersection.toDouble() / union.toDouble()
        // If 75% of unique words are shared, we assume it's the same conversation view
        return score > 0.75 
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