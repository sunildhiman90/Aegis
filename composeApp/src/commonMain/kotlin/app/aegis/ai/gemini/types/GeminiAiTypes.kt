package app.aegis.ai.gemini.types

import app.aegis.models.RiskLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// --- JSON Models for Gemini API ---
@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerateContentConfig? = null,
    val tools: List<Tool>? = null
)

@Serializable
data class Content(val parts: List<Part>, val role: String = "user")

@Serializable
data class Part(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: InlineData? = null
)

@Serializable
data class GenerateContentConfig(
    val responseMimeType: String = "application/json",
    // This is what makes it a "Gemini 3" project
    val thinkingLevel: String = "HIGH",
    // 🧠 CRITICAL: Include thoughts so you can SHOW them in the UI (The "Wow" factor)
    val includeThoughts: Boolean = true,
    //val thinkingBudget: Int? = 0,
)

@Serializable
data class GenerateContentResponseUsageMetadata(
    val promptTokenCount: Int? = null,
    val promptTokenDetails: List<ModalityTokenCount>? = null,
    val cachedContentTokenCount: Int? = null,
    val candidatesTokenCount: Int? = null,
    val thoughtsTokenCount: Int? = null,
    val toolUsePromptTokenCount: Int? = null,
    val totalTokenCount: Int? = null,
)

@Serializable
data class ModalityTokenCount(
    val modality: String? = null,
    val tokenCount: Int? = null
)

enum class MediaModalityEnum {
    MODALITY_UNSPECIFIED,
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    DOCUMENT
}

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null,
    val modelVersion: String? = null,
    val responseId: String? = null,
    val usageMetadata: GenerateContentResponseUsageMetadata? = null
)

@Serializable
data class Candidate(
    val content: Content,
    val groundingMetadata: GroundingMetadata? = null,
    val finishReason: String? = null
)

@Serializable
data class Tool(
    // 🟢 For Gemini 1.5 (Standard)
    // Maps kotlin variable 'googleSearchRetrieval' -> JSON key "googleSearchRetrieval"
    val googleSearchRetrieval: GoogleSearchRetrieval? = null,

    // 🔵 For Gemini 2.0 / 3.0 (Newer/Experimental)
    // Maps kotlin variable 'googleSearch' -> JSON key "google_search"
    @SerialName("google_search")
    val googleSearch: GoogleSearchRetrieval? = null
)

@Serializable
class GoogleSearchRetrieval // Keep empty

// 1. ADD SOURCE CLASS
@Serializable
data class Source(
    val title: String,
    val url: String
)
@Serializable
data class GroundingMetadata(
    val groundingChunks: List<GroundingChunk>? = null
)

@Serializable
data class GroundingChunk(
    val web: Web? = null
)

@Serializable
data class Web(
    val uri: String? = null,
    val title: String? = null
)


@Serializable
data class InlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String // Base64 encoded string
)