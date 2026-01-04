package app.aegis.ai.models

import kotlinx.serialization.Serializable

// --- JSON Models for Gemini API ---
@Serializable
data class GenerationRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(val parts: List<Part>, val role: String = "user")

@Serializable
data class Part(val text: String? = null)

@Serializable
data class GenerationConfig(
    val responseMimeType: String = "application/json",
    // 🏆 CRITICAL: This is what makes it a "Gemini 3" project
    val thinkingLevel: String = "HIGH",
    // 🧠 CRITICAL: Include thoughts so you can SHOW them in the UI (The "Wow" factor)
    val includeThoughts: Boolean = true
)

@Serializable
data class GeminiResponse(val candidates: List<Candidate>? = null)

@Serializable
data class Candidate(val content: Content)
