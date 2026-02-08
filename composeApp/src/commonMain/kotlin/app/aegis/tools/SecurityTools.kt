package app.aegis.tools


enum class LocalRisk {
    SAFE, SUSPICIOUS, HIGH_RISK
}

object SecurityTools {

    // 🟢 SAFE LIST: Ignore links from these domains (Music, Maps, Socials)
    // This prevents Gemini calls when friends share reels or songs.
    private val SAFE_DOMAINS = listOf(
        "youtube.com", "youtu.be", "instagram.com", "facebook.com", "twitter.com", "x.com",
        "spotify.com", "music.apple.com", "maps.google.com", "goo.gl/maps",
        "amazon.in", "flipkart.com", "zomato.com", "swiggy.com" // Common Indian safe apps
    )


    // 💰 MONEY PATTERN: Kotlin Regex (KMP Compatible)
    private val MONEY_PATTERN = Regex(
        "(rs\\.?|inr|\\$|usd|euro|eur|₹)\\s?\\d{3,}",
        RegexOption.IGNORE_CASE
    )

    // 🚨 HIGH RISK KEYWORDS (Scams/Threats)
    private val HIGH_RISK_KEYWORDS = listOf(
        "digital arrest", "narcotics", "cbi", "money laundering",
        "seized", "illegal package", "police verification",
        "cyber crime", "arrest warrant"
    )

    // ⚠️ SUSPICIOUS KEYWORDS (Banking/Access)
    private val SUSPICIOUS_KEYWORDS = listOf(
        "kyc", "pan card", "update", "expire", "block",
        "anydesk", "teamviewer", "screen share",
        "lottery", "winner", "prize", "investment", "part time job"
    )


    fun analyzeLocally(text: String): LocalRisk {
        val lower = text.lowercase()

        // 1. 🚨 CRITICAL TRIGGERS (Immediate High Risk)
        // If any of these exist, it is 99% a scam.
        if (HIGH_RISK_KEYWORDS.any { lower.contains(it) }) {
            return LocalRisk.HIGH_RISK
        }

        // Special Case: "Police" + "Video" together is a major red flag for Digital Arrest
        if (lower.contains("police") && (lower.contains("video") || lower.contains("call"))) {
            return LocalRisk.HIGH_RISK
        }

        // 2. ⚠️ CONTEXTUAL TRIGGERS (Smart Checks)

        // A) LINKS: Check if it's a link, but ignore Safe Domains
        if (lower.contains("http") || lower.contains("www.")) {
            val isSafeLink = SAFE_DOMAINS.any { lower.contains(it) }
            if (!isSafeLink) {
                // Unknown link (bit.ly, ngrok, weird-bank.com) -> SUSPICIOUS
                return LocalRisk.SUSPICIOUS
            }
        }

        // B) URGENCY: Only flag if paired with "Money" or "Action"
        // Friend: "Come urgent" -> SAFE
        // Scam: "Send money urgent" -> SUSPICIOUS
        if (lower.contains("urgent") || lower.contains("immediately") || lower.contains("fast")) {
            if (lower.contains("pay") || lower.contains("send") || lower.contains("transfer") || lower.contains("link")) {
                return LocalRisk.SUSPICIOUS
            }
        }

        // C) MONEY DEMANDS: Only flag explicit demands
        // Friend: "I have no money" -> SAFE
        // Scam: "Transfer 5000 rs" -> SUSPICIOUS
        if (lower.contains("send") || lower.contains("transfer") || lower.contains("pay")) {
            if (MONEY_PATTERN.containsMatchIn(text)) {
                return LocalRisk.SUSPICIOUS
            }
        }

        // 3. ⚠️ KEYWORD FALLBACK
        // Check standard banking/scam keywords (KYC, AnyDesk, etc.)
        if (SUSPICIOUS_KEYWORDS.any { lower.contains(it) }) {
            return LocalRisk.SUSPICIOUS
        }

        // If none of the above, it's safe!
        return LocalRisk.SAFE
    }
}