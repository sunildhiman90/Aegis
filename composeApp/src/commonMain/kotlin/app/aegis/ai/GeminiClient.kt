package app.aegis.ai

import app.aegis.AegisConfig
import app.aegis.ai.models.Content
import app.aegis.ai.models.GeminiResponse
import app.aegis.ai.models.GenerationConfig
import app.aegis.ai.models.GenerationRequest
import app.aegis.ai.models.Part
import app.aegis.models.RiskLevel
import app.aegis.models.ScamVerdict
import app.aegis.models.parseGeminiJson
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.logging.KtorSimpleLogger
import io.ktor.util.logging.Logger
import kotlinx.serialization.json.Json

class GeminiClient {

    //TODO, inject via KOIN DI
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : io.ktor.client.plugins.logging.Logger {
                override fun log(message: String) {
                    println("HTTP Client:$message")
                }
            }
            level = LogLevel.ALL
        }
    }

    // Using Gemini 3 Pro (via AI Studio Free Tier)
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/"

    suspend fun analyze_old(screenText: String, model: String = "gemini-3-flash-preview"): ScamVerdict {
         val apiKey = AegisConfig.GEMINI_API_KEY

        if (apiKey.isBlank()) return ScamVerdict(RiskLevel.SAFE, "No API Key", 0)

        val prompt = """
            You are Aegis, a scam detection kernel. Analyze this screen text:
            "$screenText"
            
            RULES:
            1. If text implies 'Digital Arrest', 'Police Video Call', or 'CBI Investigation' -> RETURN DANGER.
            2. If text implies 'Enter PIN to receive money' -> RETURN DANGER.
            3. If text is normal conversation -> RETURN SAFE.
            
            Output strictly one word: DANGER, WARN, or SAFE.
        """.trimIndent()

        val requestBody = GenerationRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                thinkingLevel = "HIGH",
                includeThoughts = true
            )
        )

        return try {
            val response: GeminiResponse = client.post("$baseUrl$model:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body()

            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseGeminiJson(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            ScamVerdict(RiskLevel.SAFE, "Network Error", 0)
        }
    }

    suspend fun analyze(screenText: String, model: String = "gemini-3-flash-preview"): ScamVerdict {
        val apiKey = AegisConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return ScamVerdict(RiskLevel.SAFE, "No API Key", 0)

        // 1. GATHER EVIDENCE (The "Agent" Step)
        // We re-run local tools here to include their output in the prompt context
        val localAnalysis = SecurityTools.analyzeLocally(screenText)
        val toolOutput = when(localAnalysis) {
            LocalRisk.HIGH_RISK -> "Local Regex Tool detected CRITICAL keywords (Video Call/Police)."
            LocalRisk.SUSPICIOUS -> "Local Regex Tool detected SUSPICIOUS keywords."
            LocalRisk.SAFE -> "Local Regex Tool found no pattern."
        }

        // 2. CONSTRUCT THE INVESTIGATOR PROMPT
        val prompt = """
            You are Aegis, an Autonomous Security Investigator.
            
            [CASE FILE]
            - Raw Text: "$screenText"
            - Tool Report: $toolOutput
            
            [YOUR MISSION]
            Do not just classify. INVESTIGATE the text for "Digital Arrest", "FedEx", or "UPI" scams.
            
            [TOOLS TO USE MENTALLY]
            1. Context Check: Police/CBI never make video calls. If found -> DANGER.
            2. Urgency Check: "Expires soon", "Immediate payment" -> COERCION.
            3. Link Check: "bit.ly" or "ngrok" in banking messages -> DANGER.
            
            [OUTPUT REQUIREMENTS]
            Return strictly JSON with this schema:
            {
              "riskLevel": "DANGER" | "WARN" | "SAFE",
              "reason": "Detailed evidence list (e.g., '1. Sender is +92. 2. Police do not video call.')",
              "confidence": 0-100
            }
        """.trimIndent()

        // 3. SEND REQUEST
        return try {
            val response: GeminiResponse = client.post("$baseUrl$model:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(GenerationRequest(
                    contents = listOf(Content(listOf(Part(prompt)))),
                    generationConfig = GenerationConfig(
                        responseMimeType = "application/json",
                        // So our prompt above enforces the logic.
                        includeThoughts = true
                    )
                ))
            }.body()

            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseGeminiJson(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            ScamVerdict(RiskLevel.SAFE, "Agent Offline", 0)
        }
    }
}