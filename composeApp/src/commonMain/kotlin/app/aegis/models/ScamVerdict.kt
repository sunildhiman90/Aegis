package app.aegis.models

import app.aegis.ai.gemini.types.Source
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class RiskLevel {
    SAFE, WARN, DANGER
}

@Serializable
data class ScamVerdict(
    val riskLevel: RiskLevel,
    val reason: String,
    val confidence: Int,
    val sources: List<Source> = emptyList() // Default to empty
)

// Helper to parse the raw JSON string from Gemini (which can be messy)
//fun parseGeminiJson(rawJson: String): ScamVerdict {
//    // In a hackathon, fallback to SAFE if JSON breaks
//    return try {
//        // Simple manual parsing or use Json.decodeFromString if structure is perfect
//        if (rawJson.contains("DANGER")) {
//            ScamVerdict(RiskLevel.DANGER, "High risk scam pattern detected.", 90)
//        } else if (rawJson.contains("WARN")) {
//            ScamVerdict(RiskLevel.WARN, "Suspicious activity detected.", 50)
//        } else {
//            ScamVerdict(RiskLevel.SAFE, "Safe.", 0)
//        }
//    } catch (e: Exception) {
//        ScamVerdict(RiskLevel.SAFE, "Error parsing AI response.", 0)
//    }
//}

fun parseGeminiJson(rawJson: String): ScamVerdict {
    // 1. CLEANUP: Remove Markdown blocks often sent by Gemini
    val cleanJson = rawJson.trim()
        .replace("```json", "")
        .replace("```", "")
        .trim()

    return try {
        // 2. TRY PROPER PARSING (To get Risk + Reason + Sources)
        val jsonParser = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        jsonParser.decodeFromString<ScamVerdict>(cleanJson)

    } catch (e: Exception) {
        // 3. FALLBACK (If JSON is broken, at least save the Verdict)
        println("JSON Parse Failed: ${e.message}. Falling back to text check.")

        if (cleanJson.contains("DANGER", ignoreCase = true)) {
            ScamVerdict(
                riskLevel = RiskLevel.DANGER,
                reason = "High risk pattern detected (AI Output was messy).",
                confidence = 80,
                sources = emptyList() // We lose sources, but app keeps running
            )
        } else if (cleanJson.contains("WARN", ignoreCase = true)) {
            ScamVerdict(
                riskLevel = RiskLevel.WARN,
                reason = "Suspicious activity.",
                confidence = 50
            )
        } else {
            ScamVerdict(
                riskLevel = RiskLevel.SAFE,
                reason = "Safe.",
                confidence = 0
            )
        }
    }
}