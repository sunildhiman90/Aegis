package app.aegis.models

import kotlinx.serialization.Serializable

@Serializable
enum class RiskLevel {
    SAFE, WARN, DANGER
}

@Serializable
data class ScamVerdict(
    val riskLevel: RiskLevel,
    val reason: String,
    val confidence: Int
)

// Helper to parse the raw JSON string from Gemini (which can be messy)
fun parseGeminiJson(rawJson: String): ScamVerdict {
    // In a hackathon, fallback to SAFE if JSON breaks
    return try {
        // Simple manual parsing or use Json.decodeFromString if structure is perfect
        if (rawJson.contains("DANGER")) {
            ScamVerdict(RiskLevel.DANGER, "High risk scam pattern detected.", 90)
        } else if (rawJson.contains("WARN")) {
            ScamVerdict(RiskLevel.WARN, "Suspicious activity detected.", 50)
        } else {
            ScamVerdict(RiskLevel.SAFE, "Safe.", 0)
        }
    } catch (e: Exception) {
        ScamVerdict(RiskLevel.SAFE, "Error parsing AI response.", 0)
    }
}