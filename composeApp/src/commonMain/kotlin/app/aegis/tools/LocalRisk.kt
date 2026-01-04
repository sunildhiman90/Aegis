package app.aegis.tools

enum class LocalRisk {
    SAFE, SUSPICIOUS, HIGH_RISK
}

object SecurityTools {
    
    // Keywords that trigger a "Yellow" Warning immediately
    private val SUSPICIOUS_KEYWORDS = listOf(
        "police", "cbi", "rbi", "narcotics", "customs", "seized", 
        "illegal", "arrest", "verification", "kyc", "block", 
        "expire", "otp", "pin", "anydesk", "teamviewer"
    )

    // Keywords that trigger an immediate "Red" Block (Optimistic Blocking)
    private val HIGH_RISK_KEYWORDS = listOf(
        "video call", "digital arrest", "money laundering"
    )

    fun analyzeLocally(text: String): LocalRisk {
        val lowerText = text.lowercase()

        // 1. Critical Check (Red)
        if (HIGH_RISK_KEYWORDS.any { lowerText.contains(it) }) {
            // Context check: If "Police" AND "Video Call" appear together -> RED
            if (lowerText.contains("police") && lowerText.contains("video")) {
                return LocalRisk.HIGH_RISK
            }
        }

        // 2. Suspicion Check (Yellow)
        if (SUSPICIOUS_KEYWORDS.any { lowerText.contains(it) }) {
            return LocalRisk.SUSPICIOUS
        }

        return LocalRisk.SAFE
    }
}