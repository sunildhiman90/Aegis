package app.aegis.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Result from Gemini's nudity/sextortion analysis of a video frame.
 */
@Serializable
data class NudityVerdict(
    @SerialName("nudity")
    val nudity: Boolean = false,
    
    @SerialName("fake_feed")
    val fakeFeed: Boolean = false,
    
    val confidence: Int = 0
)

/**
 * Parse Gemini's JSON response into NudityVerdict.
 * Handles messy responses with markdown blocks.
 */
fun parseNudityVerdictJson(rawJson: String): NudityVerdict {
    return try {
        // Clean up markdown code blocks if present
        val cleaned = rawJson
            .replace("```json", "")
            .replace("```", "")
            .trim()
        
        kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            isLenient = true
        }.decodeFromString<NudityVerdict>(cleaned)
    } catch (e: Exception) {
        // Default to safe if parsing fails
        NudityVerdict(nudity = false, fakeFeed = false, confidence = 0)
    }
}
