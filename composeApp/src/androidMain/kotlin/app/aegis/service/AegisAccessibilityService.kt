package app.aegis.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.aegis.ai.gemini.GeminiClient
import app.aegis.ai.gemini.types.Source
import app.aegis.data.settings.AppSettingsRepository
import app.aegis.helper.ScreenshotHelper
import app.aegis.models.NudityVerdict
import app.aegis.models.PhishingVerdict
import app.aegis.models.RiskLevel
import app.aegis.models.ScamVerdict
import app.aegis.models.SensitivityLevel
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import app.aegis.domain.model.Incident
import app.aegis.domain.model.IncidentSeverity
import app.aegis.domain.model.IncidentType
import app.aegis.domain.repository.IncidentRepository
import java.io.ByteArrayOutputStream
import java.util.UUID

class AegisAccessibilityService :
    AccessibilityService(),
    KoinComponent {
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var analysisJob: Job? = null // 🛑 STORE THE CURRENT JOB
    private var currentAnalysisHash = 0 // Track what content is currently being analyzed
    private var currentAnalysisText = ""
    private var currentAnalysisContact = ""
    private var lastInputText = "" // 🛑 Track user's last typed text for Smart Diff

    private val geminiClient: GeminiClient by inject()
    private val settingsRepository: AppSettingsRepository by inject()
    private val incidentRepository: IncidentRepository by inject()
    private val trustedContactRepository: app.aegis.domain.repository.TrustedContactRepository by inject() // 🟢 Inject Trusted Contact Repo
    private lateinit var overlayManager: OverlayManager
    
    // 🟢 Local Cache for synchronous checks (Performance)
    private var trustedCache = emptySet<String>()
    // 🟢 Local Session Scores (reset on restart, eventually persists to DB)
    private val sessionTrustScores = mutableMapOf<String, Int>()

    private lateinit var screenshotHelper: ScreenshotHelper // New Helper

    // Snooze Logic
    private val snoozedScreens = mutableMapOf<Int, Long>()
    private var globalPauseUntil = 0L

    private var lastAnalysisTime = 0L
    private val DEBOUNCE = 1000L // Reduced debounce slightly for responsiveness

    // 🛡️ SEXTORTION SHIELD - Continuous frame analysis
    private var sextortionAnalysisJob: Job? = null
    private var isInVideoCall = false
    private var lastCallActivityTime = 0L // Track when we last saw actual call evidence
    private var lastAudioWarningTime = 0L

    private val CALL_STATE_TIMEOUT =
        15_000L // 15 seconds - if no WhatsApp activity, assume call ended

    // 📱 VIDEO CALL DETECTION - Caller tracking
    private var pendingVideoCallCaller = "" // Store caller name from notification
    private var hasShownCameraWarning = false // Ensure warning shows only once per call
    private var cameraWarningShownTime: Long =
        0 // Debounce to prevent reset during notification->pickup transition

    private val SUPPORTED_PACKAGES =
        setOf(
            "com.whatsapp",
            "com.whatsapp.w4b",
            "com.google.android.apps.messaging",
            "org.telegram.messenger",
            "com.samsung.android.messaging",
        )

    override fun onServiceConnected() {
        super.onServiceConnected()
        overlayManager = OverlayManager(this)
        screenshotHelper =
            ScreenshotHelper(
                this,
                java.util.concurrent.Executors
                    .newSingleThreadExecutor(),
            )
        
        // 🟢 Start syncing trusted contacts from DB to Cache
        serviceScope.launch {
            trustedContactRepository.getAllContacts().collect { contacts ->
                // Cache both Names and Phone Numbers for fast lookup
                trustedCache = contacts.flatMap { listOf(it.name, it.phoneNumber) }
                    .filter { it.isNotBlank() }
                    .toSet()
                Log.d("Aegis", "✅ Trusted Cache Updated: ${trustedCache.size} entries")
            }
        }
        
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
                        val title =
                            extras
                                ?.getCharSequence(android.app.Notification.EXTRA_TITLE)
                                ?.toString() ?: ""
                        val text =
                            extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
                                ?: ""
                        val bigText =
                            extras
                                ?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)
                                ?.toString() ?: ""
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
                val callEndedIndicators =
                    listOf(
                        "call ended",
                        "ended",
                        "missed video call",
                        "missed call",
                        "declined",
                        "no answer",
                        "missed",
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
                val ongoingIndicators =
                    listOf(
                        "ongoing video call",
                        "ongoing call",
                        "video call in progress",
                        "tap to return",
                        "return to call",
                    )
                if (ongoingIndicators.any { notificationText.contains(it, ignoreCase = true) }) {
                    Log.d("Aegis", "📞 ONGOING VIDEO CALL DETECTED: $notificationText")

                    if (!isInVideoCall) {
                        val caller =
                            pendingVideoCallCaller.ifEmpty {
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
                    // Check for Missed/Ended/Declined notifications FIRST
                    if (notificationText.contains("missed", ignoreCase = true) ||
                        notificationText.contains("ended", ignoreCase = true) ||
                        notificationText.contains("declined", ignoreCase = true)
                    ) {
                        // Call ended/missed - stop everything and clear state
                        Log.d(
                            "Aegis",
                            "🛑 Call ended/missed notification: $notificationText. Clearing state.",
                        )
                        stopSextortionAnalysis()
                        overlayManager.hideShield() // Hide any showing warning
                        hasShownCameraWarning = false // Reset for next call
                        isInVideoCall = false
                        return
                    }

                    // Skip ongoing calls handled elsewhere
                    if (!notificationText.contains("ongoing", ignoreCase = true)) {
                        pendingVideoCallCaller = extractCallerFromNotification(notificationText)
                        Log.d(
                            "Aegis",
                            "🔔 INCOMING VIDEO CALL: $notificationText, caller=$pendingVideoCallCaller",
                        )

                        // 🛡️ STALE FLAG CHECK
                        // If the flag is TRUE, but it's been > 15 seconds since we showed it,
                        // this is likely a NEW call (or a re-dial). Force reset.
                        if (hasShownCameraWarning) {
                            val timeSinceShown = System.currentTimeMillis() - cameraWarningShownTime
                            if (timeSinceShown > 15000) {
                                Log.d(
                                    "Aegis",
                                    "♻️ Stale camera warning flag detected (>15s). Resetting for new incoming call.",
                                )
                                hasShownCameraWarning = false
                            }
                        }

                        // 🛡️ SHOW CAMERA WARNING ONLY FOR UNKNOWN NUMBERS
                        // If caller is a phone number (not saved contact name), show warning
                        // AND only if we haven't shown it already for this session
                        if (isPotentialRisk(pendingVideoCallCaller) && !hasShownCameraWarning) {
                            Log.d("Aegis", "🛡️ Unknown caller detected! Showing camera warning...")
                            hasShownCameraWarning = true // Mark as shown
                            cameraWarningShownTime =
                                System.currentTimeMillis() // Start debounce timer
                            overlayManager.showCameraWarning {
                                Log.d("Aegis", "🛡️ Camera warning acknowledged")
                            }
                        } else if (hasShownCameraWarning) {
                            Log.d("Aegis", "ℹ️ Camera warning already shown for this session.")
                        } else {
                            Log.d(
                                "Aegis",
                                "✅ Saved contact: $pendingVideoCallCaller - skipping camera warning",
                            )
                        }
                        return
                    }
                }
            }
        }
        
        // 🛑 OPTIMIZATION: Invalidate Cache on Screen/Window State Changes
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
             invalidateTitleCache()
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
                /*
                // REMOVED: Timeout logic was too aggressive for PiP / Multitasking.
                // We now trust isInVideoCall until explicitly cleared by detection logic.
                val timeSinceLastActivity = System.currentTimeMillis() - lastCallActivityTime
                if (timeSinceLastActivity > CALL_STATE_TIMEOUT) {
                    // Timeout: No WhatsApp activity for 30+ seconds, assume call ended
                    Log.d("Aegis", "⏰ Call state timeout: No activity for ${timeSinceLastActivity / 1000}s. Resetting.")
                    stopSextortionAnalysis()
                    return
                }
                 */
                // Still within call - continue PiP mode handling
                // Still within timeout - continue PiP mode handling
                Log.d(
                    "Aegis",
                    "📞 PiP mode detected: continuing video call analysis despite package=$currentPackage",
                )
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
        // 2. VIDEO CALL CHECK (Priority #1)
        // Checks for "Incoming Video Call" screens immediately
        if (detectVideoCallScam(rootNode)) {
            return
        }

        // 3. AUDIO CALL CHECK (Priority #2)
        // Checks for "Incoming Voice Call" screens
        if (detectAudioCallScam(rootNode)) {
            return
        }

        // REMOVED: Generic reset block. We now only reset on EXPLICIT exit conditions (see detectVideoCallScam)

        // 3. CHAT SCAM CHECK
        // Must be a real chat screen (have an input box) to proceed
        if (!isRealChatScreen(rootNode)) return

        // 4. IDENTIFY CONTACT (Fast Check)
        val contactName = extractTitle(rootNode)

        // 🛑 TRUST CHECK: If user manually trusted them, or we auto-trusted them before
        if (contactName.isNotBlank() && trustedCache.contains(contactName)) {
             // Log.d("Aegis", "✨ Trusted Contact detected (Cached): $contactName")
             return
        }

        // 5. EXTRACT & DEDUPLICATE CONTENT
        // Don't re-scan if we just analyzed this exact screen 100ms ago
        val capturedText = StringBuilder()
        traverseNode(rootNode, capturedText)
        val rawChatContent = capturedText.toString()

        // 🛑 NEW: Optimization for User-Initiated Conversations
        // If user is typing and there is almost no other content (User initiating), assume SAFE.
        val userInput = extractUserInput(rootNode)

        // SMART DIFF: If the ONLY change is what the user just typed (it got sent), SKIP.
        if (userInput.isBlank() && lastInputText.isNotBlank()) {
            // User cleared input -> valid chance they sent the message.
            // If the new content contains their last input, and we are stable otherwise, it's safe.
            // For now, simpler optimization: If input cleared, we reset lastInputText.
            lastInputText = ""
        }
        if (userInput.isNotBlank()) {
            lastInputText = userInput

            val historyStats =
                rawChatContent.length // Note: traverseNode now excludes EditText, so this is PURE history
            // Threshold 300 allows for Contact Name + Encryption Notice + Date headers
            if (historyStats < 300) {
                Log.d(
                    "Aegis",
                    "User typed: '$userInput' (History len: $historyStats). Starting conversation -> SAFE.",
                )
                return
            }
        }

        if (rawChatContent.length < 10) return

        // 🛑 STABLE HASHING (Ignore Timestamps & Status)
        val stableContent = getStableContent(rawChatContent)
        val contentHash = stableContent.hashCode()

        if (contentHash == lastProcessedHash) return // 🔋 CPU Saver: Static Screen
        if (isScreenSnoozed(contentHash)) return // User dismissed this specific text

        // Update state
        lastProcessedHash = contentHash

        // Debounce only for meaningful new content updates
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAnalysisTime < DEBOUNCE) return
        lastAnalysisTime = currentTime

        // 6. DECISION ENGINE (The "Smart" Logic)
        val sensitivity = settingsRepository.getSensitivity()

        // 🔗 PHISHING LINK CHECK (Priority: Links can be dangerous from ANYONE)
        val urls = extractUrls(stableContent)
        if (urls.isNotEmpty()) {
            Log.d("Aegis", "🔗 URLs detected: $urls")
            urls.forEach { url ->
                // Basic Whitelist Check (optimization)
                if (!isWhitelisted(url)) {
                    processSuspiciousUrl(url)
                }
            }
        }

        // 🛡️ SENSITIVITY GATEKEEPER LOGIC
        val isUnknown = isPotentialRisk(contactName) // True if not a saved contact
        val localRisk = SecurityTools.analyzeLocally(stableContent)

        // DECISION MATRIX
        val shouldAnalyze =
            when (sensitivity) {
                SensitivityLevel.LOW -> {
                    // Strict Battery Saver: Only analyze UNKNOWN contacts if they trigger key scam keywords
                    isUnknown && (localRisk == LocalRisk.HIGH_RISK || localRisk == LocalRisk.SUSPICIOUS)
                }

                SensitivityLevel.AGGRESSIVE -> {
                    // Paranoid: Analyze ALL Unknown contacts.
                    // PLUS: Analyze Saved Trusted Contacts if they say something VERY suspicious (Hacked Friend scenario)
                    isUnknown || (localRisk == LocalRisk.HIGH_RISK)
                }

                else -> { // BALANCED (Default)
                    // Analyze ALL Unknown contacts (standard behavior).
                    // Ignore Saved contacts unless explicitly suspicious (handled below for standard safety)
                    isUnknown
                }
            }

        if (shouldAnalyze) {
            // 🔋 GATEKEEPER: Don't waste money/battery analyzing "Hi" or "Ok" unless urgent
            if (stableContent.length < 15 && localRisk == LocalRisk.SAFE) {
                Log.d("Aegis", "🔋 Short safe text ignored (Battery Saver).")
                return
            }

            Log.d(
                "Aegis",
                "⚠️ Analysis Triggered [Sensitivity: $sensitivity, Risk: $localRisk]. Analyzing...",
            )
            runGeminiAnalysis(contactName, stableContent, contentHash, sensitivity)
        } else if (!isUnknown && localRisk != LocalRisk.SAFE) {
            // Special Case: A Saved Contact said something suspicious, but we didn't auto-trigger (Low/Balanced mode)
            // Log it, but maybe trust the user unless it's CRITICAL.
            // For now, Balanced mode trusts contacts implicitly unless listed otherwise.
            if (sensitivity == SensitivityLevel.BALANCED && localRisk == LocalRisk.HIGH_RISK) {
                // Even in Balanced, if "Mom" asks for OTP, we check.
                runGeminiAnalysis(contactName, stableContent, contentHash, sensitivity)
            } else {
                Log.d(
                    "Aegis",
                    "Safe contact '$contactName' passed local checks (Trust Score increased).",
                )
                increaseTrustScore(contactName)
            }
        } else {
            // Safe
            if (!isUnknown) increaseTrustScore(contactName)
        }
    }

    private fun increaseTrustScore(contactName: String) {
        if (contactName.isBlank() || trustedCache.contains(contactName)) return

        val currentScore = sessionTrustScores.getOrElse(contactName) { 0 } + 1
        sessionTrustScores[contactName] = currentScore
        
         if (currentScore >= 3) {
            Log.d("Aegis", "🏆 '$contactName' has earned our trust. Whitelisting via Repository.")
            // Persist to DB
             serviceScope.launch {
                val newContact = app.aegis.domain.model.TrustedContact(
                    id = UUID.randomUUID().toString(),
                    name = contactName,
                    phoneNumber = contactName, // Best guess if we only have title
                    relationship = "Auto-Added",
                    addedAt = System.currentTimeMillis()
                )
                 try {
                     trustedContactRepository.addContact(newContact)
                     sessionTrustScores.remove(contactName)
                 } catch (e: Exception) {
                     Log.e("Aegis", "Failed to auto-add trusted contact: ${e.message}")
                 }
             }
         }
    }

    // --- HELPER TO AVOID DUPLICATE CODE ---
    private fun runGeminiAnalysis(
        contactName: String,
        chatContent: String,
        contentHash: Int,
        sensitivity: SensitivityLevel,
    ) {
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

        analysisJob =
            serviceScope.launch {
                //TODO, just for testing
                //val verdict = geminiClient.analyze(chatContent, sensitivity)
                val verdicts = listOf(
                    ScamVerdict(
                        riskLevel = RiskLevel.DANGER,
                        reason = "test danger",
                        confidence = 99,
                        sources = listOf(Source("test", "https://google.com"))
                    ),
                    ScamVerdict(
                        riskLevel = RiskLevel.SAFE,
                        reason = "test safe",
                        confidence = 99,
                        sources = listOf(Source("test", "https://google.com"))
                    ),
                    ScamVerdict(
                        riskLevel = RiskLevel.WARN,
                        reason = "test Warn",
                        confidence = 99,
                        sources = listOf(Source("test", "https://google.com"))
                    )
                )
                val verdict = verdicts[0]
                if (!isActive) return@launch

                if (verdict.riskLevel == RiskLevel.DANGER) {
                    showBlockingShield(verdict.reason, contactName, contentHash, verdict.sources)
                    logIncident(
                        type = IncidentType.SCAM_TEXT,
                        riskLevel = verdict.riskLevel,
                        reason = verdict.reason,
                        contactName = contactName,
                        isBlocked = true
                    )
                } else {
                    overlayManager.hideShield()
                    if (verdict.riskLevel == RiskLevel.WARN) {
                         logIncident(
                            type = IncidentType.SCAM_TEXT,
                            riskLevel = verdict.riskLevel,
                            reason = verdict.reason,
                            contactName = contactName,
                            isBlocked = false
                        )
                    }
                }
            }
    }

    // --- HELPER TO LOG INCIDENTS ---
    private suspend fun logIncident(
        type: IncidentType,
        riskLevel: RiskLevel,
        reason: String,
        contactName: String,
        isBlocked: Boolean
    ) {
        val severity = when (riskLevel) {
            RiskLevel.DANGER -> IncidentSeverity.CRITICAL
            RiskLevel.WARN -> IncidentSeverity.MEDIUM
            RiskLevel.SAFE -> IncidentSeverity.LOW
        }

        // Only log actual threats or warnings
        if (riskLevel == RiskLevel.SAFE) return

        val incident = Incident(
            id = UUID.randomUUID().toString(),
            type = type,
            description = reason,
            timestamp = System.currentTimeMillis(),
            isBlocked = isBlocked,
            severity = severity
        )

        try {
            incidentRepository.addIncident(incident)
            Log.d("Aegis", "✅ Incident logged: ${incident.type} - ${incident.description}")
        } catch (e: Exception) {
            Log.e("Aegis", "❌ Failed to log incident: ${e.message}")
        }
    }

    private fun isSimilar(
        text1: String,
        text2: String,
    ): Boolean {
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
        sources: List<Source> = emptyList(),
    ) {
        overlayManager.showShield(
            reason = reason,
            contactName = contactName,
            onUnlock = {
                // Determine trust
                serviceScope.launch {
                    val newContact = app.aegis.domain.model.TrustedContact(
                        id = UUID.randomUUID().toString(),
                        name = contactName,
                        phoneNumber = contactName,
                        relationship = "Trusted via Shield",
                        addedAt = System.currentTimeMillis()
                    )
                    trustedContactRepository.addContact(newContact)
                }
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
            sources = sources,
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
        // IT IS A CHAT IF:
        // 1. It has an Input Box (Standard Chat)
        // 2. OR It has "Read Only" indicators (Short Codes, Bank Alerts, Business Accounts)
        return hasChatInput(node) || hasReadOnlyIndicator(node)
    }

    private fun hasChatInput(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.className == "android.widget.EditText") {
             val text = (node.text ?: "") .toString()
             val hint = (node.hintText ?: "").toString()
             
             // 🛑 EXCLUDE SEARCH BARS: If it's a search bar, it's a List Screen, NOT a Chat.
             if (hint.contains("Search", true) || text.contains("Search", true)) {
                 return false
             }

             // Check hint or content to see if it looks like a message input
             if (hint.contains("Type", true) || 
                 hint.contains("Message", true) || 
                 hint.contains("Chat", true) ||
                 text.contains("Type", true)) {
                 return true
             }
             // Fallback: If it's an EditText and NOT a search bar, assume it's chat input
             return true
        }
        val count = node.childCount
        for (i in 0 until count) {
            if (hasChatInput(node.getChild(i))) return true
        }
        return false
    }

    // 🛑 NEW: Detect "Read Only" chats (Short Codes, OTPs, Bank Alerts)
    // These screens replace the Input Box with text like "Can't reply to this short code".
    // WE MUST BE STRICT: Avoid generic terms like "Learn more" which might appear in Settings/Help.
    private fun hasReadOnlyIndicator(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        
        val text = (node.text ?: "").toString()
        val desc = (node.contentDescription ?: "").toString()
        
        // STRICT phrases found in Google Android / Samsung / WhatsApp read-only footers.
        val indicators = listOf(
            "Can't reply",
            "reply to this short code",
            "does not support replies", // Covers "Sender does not support replies"
            "sending to this short code" // Sometimes appears in error states
        )
        
        if (indicators.any { text.contains(it, ignoreCase = true) || desc.contains(it, ignoreCase = true) }) {
            return true
        }

        val count = node.childCount
        for (i in 0 until count) {
            if (hasReadOnlyIndicator(node.getChild(i))) return true
        }
        return false
    }

    private fun extractUserInput(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        if (node.className == "android.widget.EditText") {
            return (node.text ?: "").toString()
        }
        val count = node.childCount
        for (i in 0 until count) {
            val text = extractUserInput(node.getChild(i))
            if (text.isNotEmpty()) return text
        }
        return ""
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        sb: StringBuilder,
    ) {
        if (node == null) return

        // 🛑 EXCLUDE INPUT BOXES: We only analyze incoming messages, not what user is typing (Privacy + Optimization)
        if (node.className == "android.widget.EditText") return

        if (!node.text.isNullOrEmpty()) sb.append(node.text).append(" ")
        if (!node.contentDescription.isNullOrEmpty()) sb.append(node.contentDescription).append(" ")
        val count = node.childCount
        for (i in 0 until count) traverseNode(node.getChild(i), sb)
    }

    /**
     * Filters out volatile content (Time, Date, Status) to ensure stable hashing.
     */
    private fun getStableContent(text: String): String {
        var stable = text

        // 1. Remove Timestamps (10:00 AM, 10:00am, 22:00)
        stable = stable.replace(Regex("\\d{1,2}:\\d{2}\\s?([aA][pP][mM])?"), "")

        // 2. Remove Dates (Yesterday, Today, Oct 12, 12 Oct)
        stable = stable.replace(Regex("\\b(Yesterday|Today)\\b", RegexOption.IGNORE_CASE), "")
        stable = stable.replace(Regex("\\b[A-Za-z]{3}\\s\\d{1,2}\\b"), "") // Oct 12
        stable = stable.replace(Regex("\\b\\d{1,2}\\s[A-Za-z]{3}\\b"), "") // 12 Oct

        // 3. Remove Delivery Status
        stable =
            stable.replace(
                Regex("\\b(Read|Delivered|Sent|Sending)\\b", RegexOption.IGNORE_CASE),
                "",
            )
        stable = stable.replace(Regex("\\b(Sent via SMS|MMS)\\b", RegexOption.IGNORE_CASE), "")

        // 4. Remove Dynamic Contact Status (Top bar updates)
        stable =
            stable.replace(
                Regex(
                    "\\b(Online|Typing\\.\\.\\.|recording audio\\.\\.\\.|Last seen.*?)\\b",
                    RegexOption.IGNORE_CASE,
                ),
                "",
            )

        return stable
    }


    
    // Helper to find the "Back" button
    private fun findBackButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.contentDescription?.toString().equals("Navigate up", ignoreCase = true) ||
            node.contentDescription?.toString().equals("Back", ignoreCase = true)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
             val found = findBackButton(node.getChild(i) ?: continue)
             if (found != null) return found
        }
        return null
    }

    // Helper to extract Name from Action Bar container
    private fun findNameInActionBar(actionBar: AccessibilityNodeInfo): String? {
         val ignored = setOf("Navigate up", "Back", "Call", "Video", "More options", "Search")
         
         for (i in 0 until actionBar.childCount) {
             val child = actionBar.getChild(i) ?: continue
             
             // 🛑 CRITICAL: Do NOT pick up Editable text (like search bar hints)
             if (child.isEditable) continue
             
             // Check direct text
             val text = child.text?.toString()
             if (!text.isNullOrEmpty() && !ignored.contains(text) && text.length < 30) {
                 return text
             }
             
             // Check children (sometimes Name/Status are wrapped in a LinearLayout)
             if (child.childCount > 0) {
                 for (j in 0 until child.childCount) {
                     val grandChild = child.getChild(j) ?: continue
                     if (grandChild.isEditable) continue // Skip editable input fields
                     
                     val grandText = grandChild.text?.toString()
                      if (!grandText.isNullOrEmpty() && !ignored.contains(grandText) && 
                          !grandText.equals("Online", true) && !grandText.contains("last seen", true)) {
                         return grandText
                      }
                 }
             }
         }
         return null
    }

    // Helper to check if a node is inside a Scrollable Container (RecyclerView, ListView, ScrollView)
    // Contact Names (Titles) are usually in fixed headers, NOT content lists.
    private fun isInsideScrollable(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isScrollable || 
                current.className?.toString()?.contains("RecyclerView") == true ||
                current.className?.toString()?.contains("ListView") == true ||
                current.className?.toString()?.contains("ScrollView") == true) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun collectPossibleTitles(
        node: AccessibilityNodeInfo?,
        list: MutableList<String>,
    ) {
        // Collect text for fallback strategy
        // Increased limit to 20 to ensure we don't miss the title if the UI is complex
        if (node == null || list.size > 20) return
        
        // 🛑 CRITICAL: Ignore Editable Input Fields (e.g. "RCS message", "Type a message")
        // If a node is editable, its text is the input hint or content -> IGNORE IT.
        if (node.isEditable) return
        
        // 🛑 CRITICAL: Ignore Content inside Scrollable Lists (Messages)
        // If a node is part of a list, it's likely a message bubble, not the Title.
        // We only want static headers.
        if (isInsideScrollable(node)) return
        
        if (!node.text.isNullOrEmpty()) {
            val text = node.text.toString()
            // Assume contact names are short but not too short (e.g., > 1 char)
            if (text.length > 1) list.add(text)
        }
        
        // DFS traversal
        val count = node.childCount
        for (i in 0 until count) collectPossibleTitles(node.getChild(i), list)
    }

    // Helper to find Toolbar/ActionBar by class name
    private fun findToolbar(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val className = node.className?.toString() ?: ""
        if (className.contains("Toolbar", ignoreCase = true) || 
            className.contains("ActionBar", ignoreCase = true)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
             val found = findToolbar(node.getChild(i) ?: continue)
             if (found != null) return found
        }
        return null
    }

    // 🛑 OPTIMIZATION: Cache Strategy
    // We use a "Throttle" strategy:
    // 1. If Window ID changes -> Scan Immediately (New screen)
    // 2. If TYPE_WINDOW_STATE_CHANGED -> Scan Immediately (Major UI change)
    // 3. Otherwise -> Only scan every 300ms (Prevent Scroll Lag, but allowed rapid screen switches)
    private var lastWindowId = -1
    private var lastExtractionTime = 0L
    private var lastExtractedTitle = "Unknown"
    private val TITLE_CACHE_TTL = 300L // Reduced to 300ms for safety

    // Call this from onAccessibilityEvent to invalidate cache on specific major events
    private fun invalidateTitleCache() {
        lastExtractedTitle = "Unknown"
        lastExtractionTime = 0L
    }
    
    // 🛑 NEW: Clean dirty titles (e.g., "Save Name?", "Add Name")
    private fun cleanExtractedTitle(rawTitle: String): String {
        var title = rawTitle.trim()
        
        // Remove common prefixes (case insensitive)
        if (title.startsWith("Save ", ignoreCase = true)) {
            title = title.substring(5).trim()
        }
        if (title.startsWith("Add ", ignoreCase = true)) {
            title = title.substring(4).trim()
        }
        
        // Remove common suffixes
        if (title.endsWith("?")) {
            title = title.dropLast(1).trim()
        }
        
        return title
    }

    private fun extractTitle(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) return "Unknown"
        
        val currentWindowId = rootNode.windowId
        val currentTime = System.currentTimeMillis()
        
        // Check if we can use the cache
        val isSameWindow = (currentWindowId == lastWindowId)
        val isCacheValid = (currentTime - lastExtractionTime) < TITLE_CACHE_TTL
        
        if (isSameWindow && isCacheValid && lastExtractedTitle != "Unknown") {
            return lastExtractedTitle
        }
        
        // Update Cache State (before extraction, updated after success)
        lastWindowId = currentWindowId
        
        // 1. Precise ID Check (WhatsApp) - FASTEST
        val waList =
            rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/conversation_contact_name")
        if (!waList.isNullOrEmpty()) {
            val raw = waList[0].text?.toString() ?: "Unknown"
            val title = cleanExtractedTitle(raw)
            lastExtractedTitle = title
            lastExtractionTime = currentTime
            return title
        }

        // 2. Structural Action Bar Check (Universal Fallback)
        var title: String? = null
        
        val toolbar = findToolbar(rootNode)
        if (toolbar != null) {
             title = findNameInActionBar(toolbar)
        } else {
            // If no explicit Toolbar found, try the Back Button heuristic
             val backButton = findBackButton(rootNode)
             if (backButton != null) {
                 val actionBar = backButton.parent
                 if (actionBar != null) {
                     title = findNameInActionBar(actionBar)
                 }
             }
        }
        
        if (title != null) {
            val cleanTitle = cleanExtractedTitle(title)
            lastExtractedTitle = cleanTitle
            lastExtractionTime = currentTime
            return cleanTitle
        }

        // 3. Last Resort: Deep Scan
        val possibleTitles = mutableListOf<String>()
        collectPossibleTitles(rootNode, possibleTitles)
        
        // Filter out common UI elements/buttons
        val ignoredTitles = setOf(
            "Apply Now", "Call", "Video", "Add", "Block", "Report", "Search",
            "Type a message", "Message", "Online", "Typing...", "last seen",
            "RCS message", "Text message", "Sending with"
        )
        
        val found = possibleTitles.firstOrNull { t ->
             val clean = t.trim()
             clean.length < 25 && 
             !clean.contains(":") && 
             !clean.contains("Type") &&
             !t.contains("RCS message", true) &&
             !t.contains("Sending with", true) &&
             !ignoredTitles.any { clean.equals(it, ignoreCase = true) }
        } ?: "Unknown"
        
        // Even if unknown, we cache it to prevent retrying deeply every ms
        val finalTitle = cleanExtractedTitle(found)
        lastExtractedTitle = finalTitle
        lastExtractionTime = currentTime
        
        return finalTitle
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
        // EXIT CONDITION #1: User returned to chat screen (call definitely ended)
        if (hasChatInput(rootNode)) {
            if (isInVideoCall) {
                Log.d("Aegis", "📞 Call ended: User returned to chat screen")
                stopSextortionAnalysis()
            }
            if (hasShownCameraWarning) {
                Log.d("Aegis", "Resetting camera warning flag (User returned to Chat Screen)")
                hasShownCameraWarning = false // RESET: User is in chat, call is over.
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
        val isIncomingVideoCall =
            screenText.any {
                it.contains("Incoming video call", ignoreCase = true) ||
                    (
                        it.contains("WhatsApp video call", ignoreCase = true) &&
                            it.contains(
                                "Decline",
                                ignoreCase = true,
                            )
                    )
            }

        // ═══════════════════════════════════════════════════════════════════
        // STEP 3: Detect ACTIVE video call (already connected OR connecting)
        // ═══════════════════════════════════════════════════════════════════
        // Multiple ways to detect an active video call:
        // 1. Timer + Controls (established call)
        // 2. "Connecting" + Controls (call in progress of connecting)
        // 3. "encrypted video call" text (WhatsApp specific)
        // 4. "Leave call" button (definitive indicator)
        val hasCallTimer =
            screenText.any { it.matches(Regex("^\\d{1,2}:\\d{2}(:\\d{2})?$")) } // "0:06" or "1:23:45"
        val hasCallControls =
            screenText.any {
                it.contains("mute", ignoreCase = true) ||
                    it.contains("camera", ignoreCase = true) ||
                    it.contains("end call", ignoreCase = true) ||
                    it.contains("speaker", ignoreCase = true) ||
                    it.contains("switch camera", ignoreCase = true) ||
                    it.contains("video off", ignoreCase = true) ||
                    it.contains("microphone", ignoreCase = true) ||
                    it.contains("Leave call", ignoreCase = true) // Definitive indicator
            }
        val isConnecting = screenText.any { it.equals("Connecting", ignoreCase = true) }
        val isEncryptedVideoCall =
            screenText.any { it.contains("encrypted video call", ignoreCase = true) }
        val hasLeaveCall = screenText.any { it.contains("Leave call", ignoreCase = true) }

        // Negative indicator: Screen has "Accept" or "Decline" buttons (Incoming call screen)
        // Even if it says "encrypted video call", if these buttons are here, it's NOT active yet.
        val hasIncomingCallButtons =
            screenText.any {
                it.contains("Accept call", ignoreCase = true) ||
                    it.contains("Decline call", ignoreCase = true) ||
                    it.contains("slide to answer", ignoreCase = true)
            }

        // Negative Indicator: Chat Screen (has text input)
        val hasMessageInput =
            screenText.any {
                it.contains("Type a message", ignoreCase = true) ||
                    it.equals("Message", ignoreCase = true)
            }

        // Negative Indicator: Main List (has tabs)
        val hasMainTabs =
            screenText.any { it.equals("Chats", ignoreCase = true) } &&
                screenText.any { it.equals("Calls", ignoreCase = true) }

        // EXIT CONDITION #2: Main App List (Chats/Calls tabs)
        if (hasMainTabs) {
            if (isInVideoCall) stopSextortionAnalysis()
            if (hasShownCameraWarning) {
                Log.d("Aegis", "Resetting camera warning flag (User returned to Main List)")
                hasShownCameraWarning = false // RESET: User is in list, call is over.
            }
            return false
        }

        // Active call if ANY of these combinations AND distinct lack of incoming call buttons:
        val isActiveVideoCall =
            (
                (hasCallTimer && hasCallControls) || // Established call
                    (isConnecting && hasCallControls) || // Connecting phase
                    isEncryptedVideoCall || // WhatsApp video call text
                    hasLeaveCall
            ) && // Definitive "Leave call" button
                !hasIncomingCallButtons && // EXCLUDE incoming call screens
                !hasMessageInput && // EXCLUDE chat thread
                !hasMainTabs // EXCLUDE chat list

        Log.d(
            "Aegis",
            "📞 Video call check: isActive=$isActiveVideoCall, hasIncomingBtns=$hasIncomingCallButtons, hasTimer=$hasCallTimer, hasControls=$hasCallControls, isEncrypted=$isEncryptedVideoCall, screenText=${
                screenText.take(10)
            }",
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

        val callerId =
            screenText.firstOrNull { text ->
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
        if (isPotentialRisk(callerId)) {
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
    // 📞 AUDIO CALL IMPLEMENTATION
    // ---------------------------------------------------------
    private fun detectAudioCallScam(rootNode: AccessibilityNodeInfo): Boolean {
        val screenText = mutableListOf<String>()
        collectAllText(rootNode, screenText)

        val isIncomingAudioCall =
            screenText.any {
                it.contains("Incoming voice call", ignoreCase = true) ||
                    (
                        it.contains("WhatsApp voice call", ignoreCase = true) &&
                            it.contains(
                                "Decline",
                                ignoreCase = true,
                            )
                    )
            }

        if (!isIncomingAudioCall) return false

        val callerId =
            screenText.firstOrNull { text ->
                val cleanText = text.trim()
                !cleanText.equals("incoming voice call", ignoreCase = true) &&
                    !cleanText.equals("whatsapp voice call", ignoreCase = true) &&
                    !cleanText.equals("decline", ignoreCase = true) &&
                    !cleanText.equals("answer", ignoreCase = true) &&
                    !cleanText.equals("swipe up to accept", ignoreCase = true) &&
                    cleanText.length > 2
            } ?: "Unknown"

        if (isPotentialRisk(callerId)) {
            Log.d("Aegis", "📞 Unknown Audio Call Detected: $callerId")
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastAudioWarningTime > 10000) {
                lastAudioWarningTime = currentTime
                overlayManager.showWarning(
                    text = """⚠️ Unknown Caller: $callerId
DO NOT share OTPs. Real Police/Banks never ask for PINs on call.""",
                    onDismiss = {},
                )
            }
            return true
        }
        return false
    }

    // ---------------------------------------------------------
    // 🛑 CRITICAL HELPER: PHONE NUMBER DETECTION
    // ---------------------------------------------------------

    /**
     * Determines if a string is likely a raw phone number (Unknown)
     * or a saved contact name (Trusted).
     */

    /**
     * Determines if a contact is POTENTIALLY RISKY.
     * Returns TRUE if:
     * 1. It's a raw phone number (Unknown).
     * 2. It's a "Fake Official" name (e.g., "Bank Support", "CBI Officer") even if saved.
     */
    private fun isPotentialRisk(contactName: String): Boolean {
        // 1. Check for "Official" Keywords (Scammers use these in saved names)
        val suspiciousKeywords =
            listOf(
                "Bank",
                "Support",
                "Service",
                "Customer",
                "Care", // Banking
                "Police",
                "CBI",
                "Officer",
                "Inspector",
                "Cyber",
                "Crime", // Digital Arrest
                "FedEx",
                "DHL",
                "Customs", // Courier Scams
                "Lottery",
                "Winner",
                "Prize", // Lottery Scams
                "Investment",
                "Crypto",
                "Stock",
                "Trading", // Investment Scams
            )

        if (suspiciousKeywords.any { contactName.contains(it, ignoreCase = true) }) {
            Log.d("Aegis", "🚨 DETECTED SUSPICIOUS NAME: '$contactName'. Treating as High Risk.")
            return true
        }

        // 2. Local Trust Check (If explicitly trusted, return false)
        if (trustedCache.contains(contactName)) return false

        // 3. Raw Phone Number Check
        val clean = contactName.trim()

        // Explicit Indicators
        if (clean.contains("Unknown", true) ||
            clean.contains(
                "Private",
                true,
            ) || clean.contains("Spam", true)
        ) {
            return true
        }

        // Saved contacts usually have letters. If NO letters, it's a number.
        if (clean.any { it.isLetter() }) {
            return false // It's a saved name (and didn't trigger the keyword check above)
        }

        // Count digits
        val digitCount = clean.count { it.isDigit() }
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
            Regex("(.+?)\\s+(?:WhatsApp\\s+)?video\\s+call", RegexOption.IGNORE_CASE).find(
                notificationText,
            )
        if (beforeMatch != null) {
            val name = beforeMatch.groupValues[1].trim()
            if (name.isNotEmpty() && !name.equals("Incoming", ignoreCase = true)) {
                return name
            }
        }

        return "Unknown Caller"
    }

    private fun collectAllText(
        node: AccessibilityNodeInfo?,
        list: MutableList<String>,
    ) {
        if (node == null) return
        if (!node.isVisibleToUser) return // Skip hidden elements (like buttons behind the call screen)

        // 🛑 NEW: Video Call Logic shouldn't read Input Box either (privacy)
        if (node.className == "android.widget.EditText") return

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
    private suspend fun encodeBitmapToBase64(bitmap: Bitmap): String =
        withContext(Dispatchers.Default) {
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
        val cameraButtonIds =
            listOf(
                "com.whatsapp:id/btn_camera_off",
                "com.whatsapp:id/camera_btn",
                "com.whatsapp:id/call_camera",
            )

        for (buttonId in cameraButtonIds) {
            val cameraBtn = rootNode.findAccessibilityNodeInfosByViewId(buttonId)
            if (cameraBtn
                    ?.firstOrNull()
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true
            ) {
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
        lastCallActivityTime = System.currentTimeMillis() // Initialize timeout tracking

        // 🛡️ CAMERA WARNING - Show only if not already shown AND unknown caller
        if (!hasShownCameraWarning && isPotentialRisk(callerId)) {
            Log.d("Aegis", "🛡️ Showing camera warning for: $callerId")
            hasShownCameraWarning = true // Mark as shown
            cameraWarningShownTime = System.currentTimeMillis() // Start debounce timer
            overlayManager.showCameraWarning {
                Log.d("Aegis", "🛡️ Camera warning acknowledged. Starting analysis...")
            }
        } else {
            Log.d(
                "Aegis",
                "ℹ️ Camera warning skipped (shown=$hasShownCameraWarning, unknown=${
                    isPotentialRisk(
                        callerId,
                    )
                })",
            )
        }

        Log.d("Aegis", "🛡️ Starting Video Call Protection (Sextortion + Digital Arrest)")

        sextortionAnalysisJob =
            serviceScope.launch {
                var consecutiveSafeFrames = 0 // Counter for early termination
                // Stop after 3 consecutive safe frames (approx 3 seconds of non-suspicious content)
                // This prevents the analysis from stopping just because of a brief moment of safety.
                val safeFramesThreshold = 3

                while (isActive && isInVideoCall) {
                    try {
                        // ═══════════════════════════════════════════════════════════
                        // CHECK FOR ACTIVE OVERLAY (Camera Warning / Shield)
                        // ═══════════════════════════════════════════════════════════
                        // If our own warning/shield is showing, don't analyze it!
                        if (overlayManager.isShowing) {
                            Log.d("Aegis", "🛡️ Overlay visible, skipping analysis frame...")
                            delay(1000)
                            continue
                        }

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
                            val lockScreenIndicators =
                                listOf(
                                    "enter pin",
                                    "enter your pin",
                                    "enter password",
                                    "enter your password",
                                    "fingerprint",
                                    "face unlock",
                                    "unlock",
                                    "swipe to unlock",
                                    "pattern",
                                    "draw pattern",
                                    "enter passcode",
                                )
                            if (lockScreenIndicators.any { screenText.contains(it) }) {
                                Log.d("Aegis", "🔒 Lock screen detected, skipping analysis frame...")
                                // Update activity time - lock screen counts as "still in call"
                                // This prevents the timeout from triggering during PIN entry
                                lastCallActivityTime = System.currentTimeMillis()
                                delay(1000) // Wait a bit before checking again
                                continue // Don't count, don't analyze
                            }
                        }

                        val bitmap = screenshotHelper.captureScreen()
                        if (bitmap != null) {
                            val base64 = encodeBitmapToBase64(bitmap)

                            val sensitivity = settingsRepository.getSensitivity()

                            // 1. Check for NUDITY (Sextortion)
                            //TODO, just for testing
                            //val nudityVerdict = geminiClient.analyzeForNudity(base64, sensitivity)
                            val nudityVerdict = NudityVerdict(
                                nudity = true,
                            )
                            Log.d(
                                "Aegis",
                                "📸 Nudity check: ${nudityVerdict.nudity}, fake=${nudityVerdict.fakeFeed}, confidence=${nudityVerdict.confidence}",
                            )

                            if (nudityVerdict.nudity) {
                                Log.d("Aegis", "🚨 NUDITY DETECTED! Ending call immediately.")
                                autoEndCall("Sextortion attempt - Explicit content detected")
                                logIncident(
                                    type = IncidentType.SEXTORTION,
                                    riskLevel = RiskLevel.DANGER,
                                    reason = "Nudity detected in video call",
                                    contactName = callerId,
                                    isBlocked = true
                                )
                                break
                            }

                            // 2. Check for POLICE IMPERSONATION (Digital Arrest)
                            // Uses existing analyzeImage which looks for "Police Uniforms"
                            //TODO, just for testing
                            //val scamVerdict = geminiClient.analyzeImage(base64, sensitivity)
                            val scamVerdict = ScamVerdict(
                                riskLevel = RiskLevel.DANGER,
                                reason = "test danger image",
                                confidence = 99,
                                sources = listOf(Source("test", "https://google.com"))
                            )
                            Log.d(
                                "Aegis",
                                "👮 Police check: ${scamVerdict.riskLevel}, reason=${scamVerdict.reason}",
                            )

                            if (scamVerdict.riskLevel == RiskLevel.DANGER) {
                                Log.d("Aegis", "🚨 DIGITAL ARREST DETECTED! ${scamVerdict.reason}")
                                consecutiveSafeFrames = 0 // Reset counter on threat detection
                                overlayManager.showShield(
                                    reason = "🚨 DIGITAL ARREST SCAM\n${scamVerdict.reason}",
                                    contactName = callerId,
                                    sources = scamVerdict.sources,
                                    onDismiss = { overlayManager.hideShield() },
                                    onUnlock = { overlayManager.hideShield() },
                                )
                                // Don't auto-end for digital arrest, let user decide
                                logIncident(
                                    type = IncidentType.POLICE_IMPERSONATION,
                                    riskLevel = RiskLevel.DANGER,
                                    reason = scamVerdict.reason,
                                    contactName = callerId,
                                    isBlocked = true
                                )
                            } else if (scamVerdict.riskLevel == RiskLevel.SAFE && !nudityVerdict.fakeFeed) {
                                // ═══════════════════════════════════════════════════════════════
                                // SMART EARLY TERMINATION
                                // ═══════════════════════════════════════════════════════════════
                                // If we get consecutive SAFE verdicts (no nudity, no scam, no fake feed),
                                // the user likely ended the call or is on a non-video screen.
                                // Lock screen frames are skipped above, so 3 safe frames = call ended.
                                consecutiveSafeFrames++
                                Log.d(
                                    "Aegis",
                                    "✅ Safe frame detected ($consecutiveSafeFrames/$safeFramesThreshold)",
                                )

                                if (consecutiveSafeFrames >= safeFramesThreshold) {
                                    Log.d(
                                        "Aegis",
                                        "🛑 $safeFramesThreshold consecutive safe frames. Assuming call ended, stopping analysis.",
                                    )
                                    stopSextortionAnalysis()
                                    break
                                }
                            } else {
                                consecutiveSafeFrames = 0 // Reset on any non-safe verdict
                            }

                            // 3. Warn about fake/recorded video
                            if (nudityVerdict.fakeFeed) {
                                Log.d("Aegis", "⚠️ Possible fake/recorded video feed detected")
                                consecutiveSafeFrames = 0 // Reset counter - still suspicious
                                overlayManager.showWarning(
                                    "⚠️ Video may be pre-recorded",
                                    onDismiss = {},
                                )
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
        // hasShownCameraWarning = false // DO NOT RESET HERE! Only reset when detectVideoCallScam confirms no call.
        sextortionAnalysisJob?.cancel()
        sextortionAnalysisJob = null
        Log.d("Aegis", "🛡️ Sextortion Shield analysis stopped")
    }

    /**
     * Emergency: Auto-end the video call and show blocking shield.
     */
    private fun autoEndCall(reason: String) {
        stopSextortionAnalysis()

        // 1. Reveal Controls: Tap the center of the screen
        // Video call controls often fade out. We need to wake them up.
        tapToRevealControls()

        // Wait a small bit for UI to respond to the tap
        GlobalScope.launch {
            delay(200) // 200ms delay for UI animation

            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                // 2. Smart Search for End Button
                // We look for text like "End", "Decline", "Hang up" or standard IDs
                val ended = findAndClickEndButton(rootNode)

                if (!ended) {
                    Log.d("Aegis", "⚠️ End button not found via search. Attempting fallback...")
                    // 3. Fallback: Global Back Actions
                    // Often pressing 'Back' during a call will ask "End call?", pressing again confirms.
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    delay(300)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d("Aegis", "🔙 Performed double BACK action as fallback")
                }
            } else {
                Log.d("Aegis", "❌ Root node null, forcing Global Back")
                performGlobalAction(GLOBAL_ACTION_BACK)
            }

            delay(500)
            // 4. Safety Net: Home Screen
            // Even if call didn't end, strict reset logic will handle the flag eventually,
            // but we must get the user out of the scam interface.
            performGlobalAction(GLOBAL_ACTION_HOME)
            Log.d("Aegis", "🏠 Performed GLOBAL_ACTION_HOME safety net")
        }

        // Show blocking shield regardless
        overlayManager.showShield(
            reason = "🚨 SEXTORTION ATTEMPT BLOCKED\n$reason",
            contactName = "Unknown Caller",
            onDismiss = { overlayManager.hideShield() },
            onUnlock = { overlayManager.hideShield() },
        )
    }

    /**
     * Simulates a tap in the center of the screen to reveal hidden call controls.
     */
    private fun tapToRevealControls() {
        try {
            val metrics = resources.displayMetrics
            val centerX = metrics.widthPixels / 2f
            val centerY = metrics.heightPixels / 2f

            val path = Path()
            path.moveTo(centerX, centerY)

            val gesture =
                GestureDescription
                    .Builder()
                    .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                    .build()

            dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        Log.d("Aegis", "👆 Tap dispatched to reveal controls")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        Log.d("Aegis", "❌ Tap gesture cancelled")
                    }
                },
                null,
            )
        } catch (e: Exception) {
            Log.e("Aegis", "Failed to dispatch tap: ${e.message}")
        }
    }

    /**
     * Recursively searches for and clicks any button that looks like it would end a call.
     */
    private fun findAndClickEndButton(node: AccessibilityNodeInfo): Boolean {
        // Check if this node is a candidate
        if (node.isClickable) {
            val text = (node.text ?: "").toString().lowercase()
            val desc = (node.contentDescription ?: "").toString().lowercase()
            val viewId = (node.viewIdResourceName ?: "").lowercase()

            // Criteria for "End Call" button
            val isEndButton =
                text.contains("end call") || text.contains("hang up") || text.contains("decline") ||
                    desc.contains("end call") || desc.contains("hang up") || desc.contains("decline") ||
                    desc.contains("end") || // Simple "End" often works
                    viewId.contains("end_call") || viewId.contains("hangup") ||
                    viewId.contains(
                        "reject",
                    )

            if (isEndButton) {
                val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (clicked) {
                    Log.d("Aegis", "✅ End button CLICKED: text='$text', desc='$desc', id='$viewId'")
                    return true
                }
            }
        }

        // Recursive search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findAndClickEndButton(child)) {
                return true
            }
        }
        return false
    }

    // ---------------------------------------------------------
    // 🎣 PHISHING SCAM IMPLEMENTATION
    // ---------------------------------------------------------
    private fun extractUrls(text: String): List<String> {
        val urlRegex =
            Regex(
                "(https?://[\\w\\-\\.]+\\.[a-z]{2,}(/[\\w\\- ./?%&=]*)?)",
                RegexOption.IGNORE_CASE,
            )
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private fun isWhitelisted(url: String): Boolean {
        return try {
            val host =
                android.net.Uri
                    .parse(url)
                    .host ?: return false
            val safeDomains =
                listOf(
                    "google.com",
                    "youtube.com",
                    "facebook.com",
                    "instagram.com",
                    "whatsapp.com",
                    "wikipedia.org",
                    "amazon.in",
                    "flipkart.com",
                    "apple.com",
                    "microsoft.com",
                )
            safeDomains.any {
                host.equals(it, ignoreCase = true) ||
                    host.endsWith(
                        ".$it",
                        ignoreCase = true,
                    )
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun processSuspiciousUrl(url: String) {
        if (analysisJob?.isActive == true && currentAnalysisText == url) return // Dedup

        Log.d("Aegis", "🎣 Analyzing Suspicious URL: $url")

        // ⚡️ IMMEDIATE FEEDBACK: Warn the user effectively "Wait, I'm checking"
        overlayManager.showWarning(
            "🔍 Wait, I'm checking this link for scams/phishing...",
            onDismiss = {},
        )

        analysisJob =
            serviceScope.launch {
                val sensitivity = settingsRepository.getSensitivity()
                //TODO, just for testing
                //val verdict = geminiClient.analyzeUrl(url, sensitivity)
                val verdict = PhishingVerdict(
                    riskLevel = RiskLevel.DANGER,
                    reason = "test danger url",
                    confidence = 99,
                )
                if (!isActive) return@launch

                withContext(Dispatchers.Main) {
                    if (verdict.riskLevel == app.aegis.models.RiskLevel.DANGER) {
                        overlayManager.showPhishingWarning(
                             reason = verdict.reason,
                             url = url,
                             onReport = { launchReportIntent(verdict) },
                             onDismiss = { overlayManager.hideShield() },
                             onTrust = {
                                 // Add to whitelist session
                                 overlayManager.hideShield()
                             },
                         )
                        logIncident(
                            type = IncidentType.PHISHING_LINK,
                            riskLevel = RiskLevel.DANGER,
                            reason = verdict.reason,
                            contactName = "Unknown Link",
                            isBlocked = true
                        )
                    } else {
                        // ✅ Safe: Remove the yellow warning
                        overlayManager.hideShield()
                    }
                }
            }
    }

    private fun launchReportIntent(verdict: app.aegis.models.PhishingVerdict) {
        try {
            val intent =
                android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                    data = android.net.Uri.parse("mailto:") // only email apps should handle this
                    putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf(verdict.recipient))
                    putExtra(android.content.Intent.EXTRA_SUBJECT, verdict.subject)
                    putExtra(android.content.Intent.EXTRA_TEXT, verdict.body)
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            startActivity(intent)
            Log.d("Aegis", "🚀 Report Intent Launched to ${verdict.recipient}")
        } catch (e: Exception) {
            Log.e("Aegis", "Failed to launch report intent: ${e.message}")
        }
    }

    override fun onInterrupt() {
        stopSextortionAnalysis()
        serviceScope.cancel()
    }
}
