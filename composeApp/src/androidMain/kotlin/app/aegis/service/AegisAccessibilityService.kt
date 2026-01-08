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

    // 🛡️ SEXTORTION SHIELD - Continuous frame analysis
    private var sextortionAnalysisJob: Job? = null
    private var isInVideoCall = false
    private var lastCallActivityTime = 0L  // Track when we last saw actual call evidence
    private val CALL_STATE_TIMEOUT = 15_000L  // 15 seconds - if no WhatsApp activity, assume call ended

    // 📱 ONGOING VIDEO CALL DETECTION
    // When user answers video call (from notification or app):
    // 1. "Incoming video call" notification → store caller name
    // 2. "Ongoing video call" notification → call connected → start analysis
    // 3. "Missed/ended" notification → call ended → stop analysis
    private var pendingVideoCallCaller = ""  // Caller name from incoming notification

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
        screenshotHelper = ScreenshotHelper(this, java.util.concurrent.Executors.newSingleThreadExecutor())
        Log.d("Aegis", "✅ Service Online")
    }

    // Add this class-level variable to prevent re-scanning static screens
    private var lastProcessedHash = 0

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // ═══════════════════════════════════════════════════════════════════
        // 🔔 PRIORITY CHECK: Detect video calls from NOTIFICATIONS
        // ═══════════════════════════════════════════════════════════════════
        // When user receives a call while outside WhatsApp and answers from
        // the notification banner, we never see the WhatsApp UI. We must catch
        // the notification itself to start protection BEFORE user answers.
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val eventPackage = event.packageName?.toString() ?: ""

            // Try to get notification content from multiple sources:
            // 1. event.text (sometimes empty on Samsung)
            // 2. Notification object from parcelableData
            var notificationText = event.text?.joinToString(" ") ?: ""

            // If event.text is empty, try to extract from Notification object
            if (notificationText.isBlank()) {
                try {
                    val notification = event.parcelableData as? android.app.Notification
                    if (notification != null) {
                        val extras = notification.extras
                        val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
                        val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
                        val bigText = extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
                        notificationText = "$title $text $bigText".trim()
                    }
                } catch (e: Exception) {
                    Log.e("Aegis", "Error extracting notification content: ${e.message}")
                }
            }

            // DEBUG: Log ALL notifications to understand what's coming through
            Log.d("Aegis", "🔔 NOTIFICATION EVENT: package=$eventPackage, text=$notificationText")

            if (eventPackage in SUPPORTED_PACKAGES) {
                Log.d("Aegis", "🔔 WhatsApp notification: $notificationText")

                // ═══════════════════════════════════════════════════════════
                // CALL ENDED/MISSED/DECLINED DETECTION → Stop Analysis
                // ═══════════════════════════════════════════════════════════
                val callEndedIndicators = listOf(
                    "call ended", "ended", "missed video call",
                    "missed call", "declined", "no answer", "missed"
                )
                if (callEndedIndicators.any { notificationText.contains(it, ignoreCase = true) }) {
                    Log.d("Aegis", "📴 CALL ENDED/MISSED: $notificationText")
                    if (isInVideoCall) {
                        stopSextortionAnalysis()
                    }
                    pendingVideoCallCaller = ""
                    return
                }

                // ═══════════════════════════════════════════════════════════
                // ONGOING VIDEO CALL DETECTED → Show Warning + Start Analysis
                // ═══════════════════════════════════════════════════════════
                // WhatsApp shows "Ongoing video call" notification when call is active.
                // This is the RELIABLE signal that user answered the call.
                // IMPORTANT: Show camera warning IMMEDIATELY to protect user!
                val ongoingIndicators = listOf(
                    "ongoing video call", "ongoing call", "video call in progress",
                    "tap to return", "return to call"
                )
                if (ongoingIndicators.any { notificationText.contains(it, ignoreCase = true) }) {
                    Log.d("Aegis", "📞 ONGOING VIDEO CALL DETECTED: $notificationText")

                    if (!isInVideoCall) {
                        val caller = pendingVideoCallCaller.ifEmpty {
                            extractCallerFromNotification(notificationText).ifEmpty { "Unknown Caller" }
                        }

                        // Show camera warning IMMEDIATELY - user's face may already be exposed!
                        Log.d("Aegis", "🛡️ Showing camera warning for: $caller")
                        overlayManager.showCameraWarning {
                            // After user acknowledges, start analysis
                            Log.d("Aegis", "🛡️ Camera warning acknowledged. Starting analysis...")
                            startVideoCallAnalysisLoop(caller)
                        }
                    }
                    return
                }

                // ═══════════════════════════════════════════════════════════
                // INCOMING VIDEO CALL NOTIFICATION → Show warning IMMEDIATELY
                // ═══════════════════════════════════════════════════════════
                // Show camera warning NOW - before user even answers!
                // User's face may be exposed as soon as they answer.
                // ONLY for unknown callers (phone numbers) - not saved contacts.
                if (notificationText.contains("video call", ignoreCase = true) ||
                    notificationText.contains("Video call", ignoreCase = true)
                ) {
                    // Skip already-handled notifications
                    if (!notificationText.contains("missed", ignoreCase = true) &&
                        !notificationText.contains("ended", ignoreCase = true) &&
                        !notificationText.contains("ongoing", ignoreCase = true)
                    ) {
                        pendingVideoCallCaller = extractCallerFromNotification(notificationText)
                        Log.d("Aegis", "🔔 INCOMING VIDEO CALL: $notificationText, caller=$pendingVideoCallCaller")

                        // 🛡️ SHOW CAMERA WARNING ONLY FOR UNKNOWN NUMBERS
                        // If caller is a phone number (not saved contact name), show warning
                        if (isLikelyPhoneNumber(pendingVideoCallCaller)) {
                            Log.d("Aegis", "🛡️ Unknown caller detected! Showing camera warning...")
                            overlayManager.showCameraWarning {
                                Log.d("Aegis", "🛡️ Camera warning acknowledged")
                            }
                        } else {
                            Log.d("Aegis", "✅ Saved contact: $pendingVideoCallCaller - skipping camera warning")
                        }
                        return
                    }
                }
            }
        }

        // 1. GLOBAL CHECKS & BATTERY OPTIMIZATION 🔋
        // 
        // SPECIAL CASE: PiP (Picture-in-Picture) Mode
        // When WhatsApp video call is minimized to a small floating window,
        // accessibility events come from the home screen/launcher, not WhatsApp.
        // If we're already tracking a video call, we MUST skip the package check
        // to keep the screenshot analysis loop running.
        val currentPackage = event.packageName?.toString()
        if (currentPackage !in SUPPORTED_PACKAGES) {
            // If in an active video call, check for timeout
            if (isInVideoCall) {
                val timeSinceLastActivity = System.currentTimeMillis() - lastCallActivityTime
                if (timeSinceLastActivity > CALL_STATE_TIMEOUT) {
                    // Timeout: No WhatsApp activity for 30+ seconds, assume call ended
                    Log.d("Aegis", "⏰ Call state timeout: No activity for ${timeSinceLastActivity / 1000}s. Resetting.")
                    stopSextortionAnalysis()
                    return
                }
                // Still within timeout - continue PiP mode handling
                Log.d("Aegis", "📞 PiP mode detected: continuing video call analysis despite package=$currentPackage")
            } else {
                return // Not in a call - ignore events from other apps
            }
        } else {
            // We received an event from WhatsApp - update last activity time if in call
            if (isInVideoCall) {
                lastCallActivityTime = System.currentTimeMillis()
            }
        }
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
            // TODO: Revert after testing
            //if (true) { // Was: isLikelyPhoneNumber(callerId)
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
        overlayManager.showWarning("Verifying conversation pattern...", onDismiss = {
            // User manually stopped the analysis
            analysisJob?.cancel()
            Log.d("Aegis", "Analysis manually stopped by user.")
        })

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
    private fun showBlockingShield(
        reason: String,
        contactName: String,
        contentHash: Int,
        sources: List<Source> = emptyList()
    ) {
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
    // 🛑 VIDEO CALL IMPLEMENTATION (Sextortion + Digital Arrest Protection)
    // ---------------------------------------------------------
    //
    // VIDEO CALL PERSISTENCE STRATEGY:
    // ================================
    // Once a video call is detected and analysis starts, we STAY in call mode until
    // we explicitly detect the call has ended. This is necessary because:
    //
    // PROBLEM: WhatsApp's video call UI hides controls (mute, camera, end call buttons)
    // after ~5 seconds of inactivity to show the video feed full-screen. If we relied
    // on detecting these controls, we'd falsely think the call ended when they fade.
    //
    // SOLUTION: Use a state machine approach:
    // 1. ENTER call mode when we detect incoming call OR active call indicators
    // 2. STAY in call mode regardless of UI control visibility
    // 3. EXIT call mode ONLY when one of these definitive signals occurs:
    //    - User returns to chat screen (hasChatInput returns true)
    //    - User leaves WhatsApp app (package check in onAccessibilityEvent)
    //
    // The `isInVideoCall` flag tracks this state across accessibility events.
    // ---------------------------------------------------------
    private fun detectVideoCallScam(rootNode: AccessibilityNodeInfo): Boolean {

        // ═══════════════════════════════════════════════════════════════════
        // EXIT CONDITION #1: User returned to chat screen (call definitely ended)
        // ═══════════════════════════════════════════════════════════════════
        // If the screen has a "Type a message" input box, it's a CHAT screen.
        // This is a definitive signal that any video call has ended.
        // This also prevents false positives from the "Video Call" button in chat headers.
        if (hasChatInput(rootNode)) {
            if (isInVideoCall) {
                Log.d("Aegis", "📞 Call ended: User returned to chat screen")
                stopSextortionAnalysis()
            }
            return false
        }
        // NOTE: EXIT CONDITION #2 (leaving WhatsApp) is handled in onAccessibilityEvent
        // via the SUPPORTED_PACKAGES check - we simply won't receive events from other apps.

        // ═══════════════════════════════════════════════════════════════════
        // STEP 1: Collect all visible text + content descriptions from screen
        // ═══════════════════════════════════════════════════════════════════
        val screenText = mutableListOf<String>()
        collectAllText(rootNode, screenText)

        // ═══════════════════════════════════════════════════════════════════
        // STEP 2: Detect INCOMING video call (ringing, not yet answered)
        // ═══════════════════════════════════════════════════════════════════
        val isIncomingVideoCall = screenText.any {
            it.contains("Incoming video call", ignoreCase = true) ||
                    (it.contains("WhatsApp video call", ignoreCase = true) && it.contains("Decline", ignoreCase = true))
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Detect ACTIVE video call (already connected OR connecting)
        // ═══════════════════════════════════════════════════════════════════
        // Multiple ways to detect an active video call:
        // 1. Timer + Controls (established call)
        // 2. "Connecting" + Controls (call in progress of connecting)
        // 3. "encrypted video call" text (WhatsApp specific)
        // 4. "Leave call" button (definitive indicator)
        val hasCallTimer = screenText.any { it.matches(Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")) } // "0:06" or "1:23:45"
        val hasCallControls = screenText.any {
            it.contains("mute", ignoreCase = true) ||
                    it.contains("camera", ignoreCase = true) ||
                    it.contains("end call", ignoreCase = true) ||
                    it.contains("speaker", ignoreCase = true) ||
                    it.contains("switch camera", ignoreCase = true) ||
                    it.contains("video off", ignoreCase = true) ||
                    it.contains("microphone", ignoreCase = true) ||
                    it.contains("Leave call", ignoreCase = true)  // Definitive indicator
        }
        val isConnecting = screenText.any { it.equals("Connecting", ignoreCase = true) }
        val isEncryptedVideoCall = screenText.any { it.contains("encrypted video call", ignoreCase = true) }
        val hasLeaveCall = screenText.any { it.contains("Leave call", ignoreCase = true) }

        // Active call if ANY of these combinations:
        val isActiveVideoCall = (hasCallTimer && hasCallControls) ||  // Established call
                (isConnecting && hasCallControls) ||   // Connecting phase
                isEncryptedVideoCall ||                // WhatsApp video call text
                hasLeaveCall                           // Definitive "Leave call" button

        Log.d(
            "Aegis",
            "📞 Video call check: hasCallTimer=$hasCallTimer, hasCallControls=$hasCallControls, isInVideoCall=$isInVideoCall, screenText=${
                screenText.take(10)
            }"
        )

        // ═══════════════════════════════════════════════════════════════════
        // STEP 4: STATE PERSISTENCE - Stay in call mode once entered
        // ═══════════════════════════════════════════════════════════════════
        // If we're already tracking a video call, CONTINUE tracking it even if
        // the UI controls have faded away. The analysis loop keeps running.
        // We only exit via the definitive conditions at the top of this function.
        if (isInVideoCall) {
            Log.d("Aegis", "📞 Still in active video call (persisting state despite UI changes)")
            return true // Continue blocking chat logic, keep analysis running
        }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 5: Not in a call yet - check if we should START tracking one
        // ═══════════════════════════════════════════════════════════════════
        if (!isIncomingVideoCall && !isActiveVideoCall) {
            Log.d("Aegis", "🚨 NOT AN INCOMING OR ACTIVE VIDEO CALL")
            return false
        }

        val callerId = screenText.firstOrNull { text ->
            val cleanText = text.trim()
            !cleanText.equals("incoming video call", ignoreCase = true) &&
                    !cleanText.equals("whatsapp video call", ignoreCase = true) &&
                    !cleanText.equals("decline", ignoreCase = true) &&
                    !cleanText.equals("answer", ignoreCase = true) &&
                    !cleanText.contains("mute", ignoreCase = true) &&
                    !cleanText.contains("camera", ignoreCase = true) &&
                    cleanText.length > 2
        } ?: "Unknown"

        // 3. Is it an Unknown Number? (High Risk) - Apply Zero-Trust for BOTH threats
        if (isLikelyPhoneNumber(callerId)) {
            Log.d("Aegis", "🚨 UNKNOWN VIDEO CALL: $callerId")

            // 🛡️ ZERO-TRUST DEFENSE: Try to disable camera immediately
            if (isIncomingVideoCall && !isInVideoCall) {
                // First attempt: Auto-click camera off button
                val cameraDisabled = attemptCameraOff(rootNode)

                if (!cameraDisabled) {
                    // Fallback: Show full-screen camera warning
                    overlayManager.showCameraWarning {
                        // User acknowledged - start protection loop
                        Log.d("Aegis", "✅ User acknowledged camera warning")
                        startVideoCallAnalysisLoop(callerId)
                    }
                } else {
                    // Camera was disabled, start analysis loop
                    startVideoCallAnalysisLoop(callerId)
                }
            } else if (isActiveVideoCall && sextortionAnalysisJob?.isActive != true) {
                // Already in call, ensure analysis is running
                startVideoCallAnalysisLoop(callerId)
            }

            return true
        }

        return true // Block "Chat" logic from running on Call screen
    }

    // ---------------------------------------------------------
    // 🛑 CRITICAL HELPER: PHONE NUMBER DETECTION
    // ---------------------------------------------------------
    /**
     * Determines if a string is likely a raw phone number (Unknown)
     * or a saved contact name (Trusted).
     */
    private fun isLikelyPhoneNumber(text: String): Boolean {

        //TODO. just for testing, returning true, need to change later
        return true

        val clean = text.trim()

        // 1. Explicit Indicators of Risk
        if (clean.contains("Unknown", true) ||
            clean.contains("Private", true) ||
            clean.contains("Spam", true)
        ) {
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

    /**
     * Extracts caller name from WhatsApp notification text.
     * Examples:
     * - "Incoming video call from Shivani" → "Shivani"
     * - "Shivani WhatsApp video call" → "Shivani"
     */
    private fun extractCallerFromNotification(notificationText: String): String {
        // Try pattern: "Incoming video call from [Name]"
        val fromMatch = Regex("from\\s+(.+)", RegexOption.IGNORE_CASE).find(notificationText)
        if (fromMatch != null) {
            return fromMatch.groupValues[1].trim()
        }

        // Try pattern: "[Name] WhatsApp video call"
        val beforeMatch =
            Regex("(.+?)\\s+(?:WhatsApp\\s+)?video\\s+call", RegexOption.IGNORE_CASE).find(notificationText)
        if (beforeMatch != null) {
            val name = beforeMatch.groupValues[1].trim()
            if (name.isNotEmpty() && !name.equals("Incoming", ignoreCase = true)) {
                return name
            }
        }

        return "Unknown Caller"
    }

    private fun collectAllText(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        if (!node.text.isNullOrEmpty()) list.add(node.text.toString())
        // Also collect contentDescription for icon buttons (Mute, Camera, End call, etc.)
        if (!node.contentDescription.isNullOrEmpty()) list.add(node.contentDescription.toString())
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

    // ---------------------------------------------------------
    // 🛡️ SEXTORTION SHIELD IMPLEMENTATION
    // ---------------------------------------------------------

    /**
     * Zero-Trust Defense: Attempt to disable camera immediately.
     * If auto-click fails, show full-screen warning overlay.
     */
    private fun attemptCameraOff(rootNode: AccessibilityNodeInfo): Boolean {
        // Try WhatsApp camera toggle button (multiple possible IDs)
        val cameraButtonIds = listOf(
            "com.whatsapp:id/btn_camera_off",
            "com.whatsapp:id/camera_btn",
            "com.whatsapp:id/call_camera"
        )

        for (buttonId in cameraButtonIds) {
            val cameraBtn = rootNode.findAccessibilityNodeInfosByViewId(buttonId)
            if (cameraBtn?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                Log.d("Aegis", "✅ Camera disabled via $buttonId")
                return true
            }
        }

        // Fallback: Show Camera Warning Overlay
        Log.d("Aegis", "⚠️ Could not auto-disable camera. Showing warning overlay.")
        return false
    }

    /**
     * Start continuous frame analysis at ~1 FPS.
     * Analyzes for BOTH:
     * 1. Sextortion (nudity detection via analyzeForNudity)
     * 2. Digital Arrest (police impersonation via analyzeImage)
     */
    private fun startVideoCallAnalysisLoop(callerId: String) {
        if (sextortionAnalysisJob?.isActive == true) return

        isInVideoCall = true
        lastCallActivityTime = System.currentTimeMillis()  // Initialize timeout tracking

        // 🛡️ CAMERA WARNING - Show immediately to protect user!
        // User's face may already be exposed, warn them to cover camera
        Log.d("Aegis", "🛡️ Showing camera warning for: $callerId")
        overlayManager.showCameraWarning {
            Log.d("Aegis", "🛡️ Camera warning acknowledged. Starting analysis...")
        }

        Log.d("Aegis", "🛡️ Starting Video Call Protection (Sextortion + Digital Arrest)")

        sextortionAnalysisJob = serviceScope.launch {
            var consecutiveSafeFrames = 0  // Counter for early termination
            val safeFramesThreshold =
                1 // TODO using 1 for tersting, else we can set it 2 or 3  // Stop after 3 consecutive safe frames

            while (isActive && isInVideoCall) {
                try {
                    // ═══════════════════════════════════════════════════════════
                    // SKIP ANALYSIS IF LOCK SCREEN DETECTED
                    // ═══════════════════════════════════════════════════════════
                    // When user is on lock screen entering PIN/password/fingerprint,
                    // don't waste API calls or count safe frames. Wait for unlock.
                    val rootNode = rootInActiveWindow
                    if (rootNode != null) {
                        val textList = mutableListOf<String>()
                        collectAllText(rootNode, textList)
                        val screenText = textList.joinToString(" ").lowercase()
                        val lockScreenIndicators = listOf(
                            "enter pin", "enter your pin", "enter password", "enter your password",
                            "fingerprint", "face unlock", "unlock", "swipe to unlock",
                            "pattern", "draw pattern", "enter passcode"
                        )
                        if (lockScreenIndicators.any { screenText.contains(it) }) {
                            Log.d("Aegis", "🔒 Lock screen detected, skipping analysis frame...")
                            // Update activity time - lock screen counts as "still in call"
                            // This prevents the timeout from triggering during PIN entry
                            lastCallActivityTime = System.currentTimeMillis()
                            delay(1000)  // Wait a bit before checking again
                            continue  // Don't count, don't analyze
                        }
                    }

                    val bitmap = screenshotHelper.captureScreen()
                    if (bitmap != null) {
                        val base64 = encodeBitmapToBase64(bitmap)

                        // 1. Check for NUDITY (Sextortion)
                        val nudityVerdict = geminiClient.analyzeForNudity(base64)
                        Log.d(
                            "Aegis",
                            "📸 Nudity check: ${nudityVerdict.nudity}, fake=${nudityVerdict.fakeFeed}, confidence=${nudityVerdict.confidence}"
                        )

                        if (nudityVerdict.nudity) {
                            Log.d("Aegis", "🚨 NUDITY DETECTED! Ending call immediately.")
                            autoEndCall("Sextortion attempt - Explicit content detected")
                            break
                        }

                        // 2. Check for POLICE IMPERSONATION (Digital Arrest) 
                        // Uses existing analyzeImage which looks for "Police Uniforms"
                        val scamVerdict = geminiClient.analyzeImage(base64)
                        Log.d("Aegis", "👮 Police check: ${scamVerdict.riskLevel}, reason=${scamVerdict.reason}")

                        if (scamVerdict.riskLevel == RiskLevel.DANGER) {
                            Log.d("Aegis", "🚨 DIGITAL ARREST DETECTED! ${scamVerdict.reason}")
                            consecutiveSafeFrames = 0  // Reset counter on threat detection
                            overlayManager.showShield(
                                reason = "🚨 DIGITAL ARREST SCAM\n${scamVerdict.reason}",
                                contactName = callerId,
                                sources = scamVerdict.sources,
                                onDismiss = { overlayManager.hideShield() },
                                onUnlock = { overlayManager.hideShield() }
                            )
                            // Don't auto-end for digital arrest, let user decide
                        } else if (scamVerdict.riskLevel == RiskLevel.SAFE && !nudityVerdict.fakeFeed) {
                            // ═══════════════════════════════════════════════════════════════
                            // SMART EARLY TERMINATION
                            // ═══════════════════════════════════════════════════════════════
                            // If we get consecutive SAFE verdicts (no nudity, no scam, no fake feed),
                            // the user likely ended the call or is on a non-video screen.
                            // Lock screen frames are skipped above, so 3 safe frames = call ended.
                            consecutiveSafeFrames++
                            Log.d("Aegis", "✅ Safe frame detected ($consecutiveSafeFrames/$safeFramesThreshold)")

                            if (consecutiveSafeFrames >= safeFramesThreshold) {
                                Log.d(
                                    "Aegis",
                                    "🛑 $safeFramesThreshold consecutive safe frames. Assuming call ended, stopping analysis."
                                )
                                stopSextortionAnalysis()
                                break
                            }
                        } else {
                            consecutiveSafeFrames = 0  // Reset on any non-safe verdict
                        }

                        // 3. Warn about fake/recorded video
                        if (nudityVerdict.fakeFeed) {
                            Log.d("Aegis", "⚠️ Possible fake/recorded video feed detected")
                            consecutiveSafeFrames = 0  // Reset counter - still suspicious
                            overlayManager.showWarning("⚠️ Video may be pre-recorded", onDismiss = {})
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Aegis", "Frame analysis error: ${e.message}")
                }

                delay(1000) // 1 FPS
            }
        }
    }

    /**
     * Stop the analysis loop when call ends.
     */
    private fun stopSextortionAnalysis() {
        isInVideoCall = false
        sextortionAnalysisJob?.cancel()
        sextortionAnalysisJob = null
        Log.d("Aegis", "🛡️ Sextortion Shield analysis stopped")
    }

    /**
     * Emergency: Auto-end the video call and show blocking shield.
     */
    private fun autoEndCall(reason: String) {
        stopSextortionAnalysis()

        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            // Try to find and click End Call button
            val endButtonIds = listOf(
                "com.whatsapp:id/end_call_btn",
                "com.whatsapp:id/call_end",
                "com.whatsapp:id/hangup_btn"
            )

            for (buttonId in endButtonIds) {
                val endBtn = rootNode.findAccessibilityNodeInfosByViewId(buttonId)
                if (endBtn?.firstOrNull()?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) {
                    Log.d("Aegis", "✅ Call ended automatically via $buttonId")
                    break
                }
            }
        }

        // Show blocking shield regardless
        overlayManager.showShield(
            reason = "🚨 SEXTORTION ATTEMPT BLOCKED\n$reason",
            contactName = "Unknown Caller",
            onDismiss = { overlayManager.hideShield() },
            onUnlock = { overlayManager.hideShield() }
        )
    }

    override fun onInterrupt() {
        stopSextortionAnalysis()
        serviceScope.cancel()
    }

}