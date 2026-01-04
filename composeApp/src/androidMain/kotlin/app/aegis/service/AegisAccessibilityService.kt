package app.aegis.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.aegis.ai.GeminiClient
import app.aegis.data.TrustRepository
import app.aegis.models.RiskLevel
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import kotlinx.coroutines.*


class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val geminiClient = GeminiClient()
    private lateinit var overlayManager: OverlayManager

    // Store snippets we want to ignore for 20 seconds
    private val snoozedSnippets = mutableMapOf<String, Long>()

    private var lastAnalysisTime = 0L
    private val DEBOUNCE = 1500L

    // 1. DEFINE SUPPORTED APPS (The Whitelist)
    // Why? Many apps have an EditText (Input Box):
    //Google Chrome: The URL bar is an EditText.
    //Notes App: The note body is an EditText.
    //Login Screens: The username/password fields are EditTexts.
    //If you don't filter the apps, Aegis will try to "scan" your password field or your Google Search for scams, which causes that annoying "Verifying..." banner everywhere.
    private val SUPPORTED_PACKAGES = setOf(
        "com.whatsapp",                       // WhatsApp
        "com.whatsapp.w4b",                   // WhatsApp Business
        "com.google.android.apps.messaging",  // Google Messages (SMS)
        "org.telegram.messenger",             // Telegram
        "com.samsung.android.messaging"       // Samsung Messages
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        Log.d("Aegis", "✅ Service Online")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 🛑 STEP 0: IGNORE UNSUPPORTED APPS IMMEDIATELY
        // If the event comes from Chrome, Settings, or Calculator -> STOP.
        if (event.packageName?.toString() !in SUPPORTED_PACKAGES) {
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < DEBOUNCE) return

        val rootNode = rootInActiveWindow ?: return

        // 🛑 STEP 1: ARE WE IN A CHAT?
        // If we cannot find the "Type a message" box, we are likely in the List View.
        // In that case, DO NOT SCAN. It's too risky/noisy.
        if (!isChatScreen(rootNode)) {
            // Log.d("Aegis", "Not in a chat screen. Skipping.")
            return
        }

        // 🛑 STEP 2: NOW WE KNOW WE ARE IN A CHAT
        // 1. EXTRACT TITLE (Who are we talking to?)
        val contactName = extractTitle(rootNode)

        // 2. CHECK WHITELIST (Memory)
        if (TrustRepository.isTrusted(contactName)) {
            // Log.d("Aegis", "Trusted Contact ($contactName) - Skipping Analysis")
            return
        }

        // 3. EXTRACT CONTENT (Using your ROBUST traverseNode logic)
        val capturedText = StringBuilder()
        traverseNode(rootNode, capturedText)
        val chatContent = capturedText.toString()

        // 🛑 CHECK: Is this screen showing text we just dismissed?
        val now = System.currentTimeMillis()
        val isKnownThreat = snoozedSnippets.any { (snippet, expiry) ->
            now < expiry && chatContent.contains(snippet, ignoreCase = true)
        }

        if (isKnownThreat) {
            // Log.d("Aegis", "💤 Ignoring screen because it contains dismissed text.")
            return
        }

        if (chatContent.length < 10) return

        // Log.d("Aegis", "Analyzing Content: ${chatContent.take(50)}...")
        lastAnalysisTime = currentTime

        // 4. RUN LOCAL TOOLS (Instant Response)
        val localVerdict = SecurityTools.analyzeLocally(chatContent)

        when (localVerdict) {
            LocalRisk.HIGH_RISK -> {
                Log.d("Aegis", "🔴 Local Tool Triggered Block!")
                overlayManager.showShield(
                    reason = "Critical keywords detected locally.",
                    contactName = contactName,
                    onDismiss = {

                        // ✅ SNOOZE LOGIC
                        // Capture the first 50 chars of the message (or the trigger keyword context)
                        // This identifies THIS specific message, not the generic keyword.
                        val snippet = if (chatContent.length > 50) chatContent.take(50) else chatContent

                        // Ignore any screen containing this specific message for 20 seconds
                        snoozedSnippets[snippet] = System.currentTimeMillis() + 20000L
                    },
                    onUnlock = {
                        TrustRepository.trustContact(contactName)
                    })
            }

            LocalRisk.SUSPICIOUS -> {
                Log.d("Aegis", "🟡 Suspicious - Verifying...")
                overlayManager.showWarning("Verifying conversation pattern...")
                analyzeWithGemini(chatContent, contactName)
            }

            LocalRisk.SAFE -> {
                // Safe for now
            }
        }
    }

    private fun analyzeWithGemini(chatContent: String, contactName: String) {
        serviceScope.launch {
            val verdict = geminiClient.analyze(chatContent)

            if (verdict.riskLevel == RiskLevel.DANGER) {
                overlayManager.showShield(
                    reason = verdict.reason,
                    contactName = contactName,
                    onUnlock = {
                        TrustRepository.trustContact(contactName)
                    },
                    onDismiss = {

                        // ✅ SNOOZE LOGIC
                        // Capture the first 50 chars of the message (or the trigger keyword context)
                        // This identifies THIS specific message, not the generic keyword.
                        val snippet = if (chatContent.length > 50) chatContent.take(50) else chatContent

                        // Ignore any screen containing this specific message for 20 seconds
                        snoozedSnippets[snippet] = System.currentTimeMillis() + 20000L
                    }
                )
            } else {
                overlayManager.hideShield() // Dismiss Yellow Banner
            }
        }
    }

    // --- YOUR RESTORED HELPER FUNCTION ---
    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return

        // 1. Grab Text
        if (!node.text.isNullOrEmpty()) {
            sb.append(node.text).append(" ")
        }

        // 2. Grab Content Description (CRITICAL for WhatsApp)
        if (!node.contentDescription.isNullOrEmpty()) {
            sb.append(node.contentDescription).append(" ")
        }

        // 3. Go Deeper
        val count = node.childCount
        for (i in 0 until count) {
            traverseNode(node.getChild(i), sb)
        }
    }

    private fun extractTitle(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "Unknown"

        // 1. Try WhatsApp Specific ID (Most accurate for your demo)
        val waList = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (!waList.isNullOrEmpty()) return waList[0].text?.toString() ?: "Unknown"

        // 2. GENERIC FALLBACK (For SMS / Telegram)
        // The Contact Name is usually the text in the "Action Bar" (Top of screen).
        // We can iterate the top-level children and look for the first non-empty text.

        // Quick Hackathon Heuristic:
        // Scan the first 5 text nodes. The one with the largest text size or simply the first one is usually the Title.
        // Since we can't easily check text size here, we just grab the first text that ISN'T a standard system label.

        val possibleTitles = mutableListOf<String>()
        collectPossibleTitles(rootNode, possibleTitles)

        // Return the first valid-looking string (not a time, not "Type a message")
        return possibleTitles.firstOrNull {
            it.length < 25 && !it.contains(":") && !it.contains("Type")
        } ?: "Unknown"
    }

    // Helper to gather top-level text
    private fun collectPossibleTitles(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null || list.size > 3) return // specific optimization: only look at top 3 items

        if (!node.text.isNullOrEmpty()) {
            val text = node.text.toString()
            // Filter out obvious noise (Time, Battery, etc)
            if (text.length > 2) list.add(text)
        }

        val count = node.childCount
        for (i in 0 until count) {
            collectPossibleTitles(node.getChild(i), list)
        }
    }

    //    private fun extractText(node: AccessibilityNodeInfo?): String {
//        if (node == null) return ""
//        val sb = StringBuilder()
//
//        if (node.text != null && node.text.isNotEmpty()) {
//            sb.append(node.text).append("\n")
//        }
//
//        for (i in 0 until node.childCount) {
//            sb.append(extractText(node.getChild(i)))
//        }
//        return sb.toString()
//    }

    override fun onInterrupt() {
        serviceScope.cancel()
    }

    // --- HELPER TO DETECT CHAT SCREEN ---
    private fun isChatScreen(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Strategy: Look for the Input Field ("Type a message")
        // This class name is standard across Android (EditText)
        // WhatsApp/Telegram both use standard EditText for the input bar.

        // Method A: Check for specific IDs (More precise but can break if app updates)
        // val waInput = node.findAccessibilityNodeInfosByViewId("com.whatsapp:id/entry")
        // if (!waInput.isNullOrEmpty()) return true

        // Method B: Generic Class Search (Robust for Hackathon)
        // We traverse down to find any "EditText".
        return hasEditText(node)
    }

    private fun hasEditText(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        // Check if this specific node is an editable text field
        if (node.className == "android.widget.EditText") {
            return true
        }

        // Check children
        val count = node.childCount
        for (i in 0 until count) {
            if (hasEditText(node.getChild(i))) return true
        }
        return false
    }
}