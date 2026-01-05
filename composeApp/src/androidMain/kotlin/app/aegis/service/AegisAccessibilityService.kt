package app.aegis.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.aegis.ai.gemini.GeminiClient
import app.aegis.ai.gemini.types.Source
import app.aegis.data.TrustRepository
import app.aegis.models.RiskLevel
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import kotlinx.coroutines.*

class AegisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var analysisJob: Job? = null // 🛑 STORE THE CURRENT JOB

    private val geminiClient = GeminiClient()
    private lateinit var overlayManager: OverlayManager

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

        // 2. IS CHAT SCREEN?
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
        if (TrustRepository.isTrusted(contactName)) return

        // 🛑 CRITICAL FIX: NEW EVENT = CANCEL OLD ANALYSIS
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

    override fun onInterrupt() {
        serviceScope.cancel()
    }
}