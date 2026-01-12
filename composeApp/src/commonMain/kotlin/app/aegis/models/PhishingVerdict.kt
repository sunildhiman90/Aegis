package app.aegis.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PhishingVerdict(
    val riskLevel: RiskLevel,
    val reason: String,
    val confidence: Int,
    // Reporting Details
    val recipient: String = "", // e.g., "abuse@godaddy.com"
    val subject: String = "",   // e.g., "Phishing Report: example.com"
    val body: String = ""       // e.g., "This URL is hosting a fake..."
)

fun parsePhishingVerdictJson(rawJson: String): PhishingVerdict {
    // 1. CLEANUP: Remove Markdown blocks often sent by Gemini
    val cleanJson = rawJson.trim()
        .replace("```json", "")
        .replace("```", "")
        .trim()

    return try {
        // 2. TRY PROPER PARSING
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        jsonParser.decodeFromString<PhishingVerdict>(cleanJson)

    } catch (e: Exception) {
        // 3. FALLBACK
        println("JSON Parse Failed: ${e.message}. Falling back to text check.")

        if (cleanJson.contains("DANGER", ignoreCase = true)) {
            PhishingVerdict(
                riskLevel = RiskLevel.DANGER,
                reason = "High risk link detected (AI Output was messy).",
                confidence = 80,
                recipient = "report@phishing.org", // generic fallback
                subject = "Suspicious Link Report",
                body = "I found a suspicious link. Please investigate."
            )
        } else if (cleanJson.contains("WARN", ignoreCase = true)) {
            PhishingVerdict(
                riskLevel = RiskLevel.WARN,
                reason = "Suspicious link.",
                confidence = 50
            )
        } else {
            PhishingVerdict(
                riskLevel = RiskLevel.SAFE,
                reason = "Safe.",
                confidence = 0
            )
        }
    }
}
