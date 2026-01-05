package app.aegis.ai.gemini

import app.aegis.AegisConfig
import app.aegis.ai.gemini.types.Content
import app.aegis.ai.gemini.types.GenerateContentConfig
import app.aegis.ai.gemini.types.GenerateContentRequest
import app.aegis.ai.gemini.types.GenerateContentResponse
import app.aegis.ai.gemini.types.GoogleSearchRetrieval
import app.aegis.ai.gemini.types.Part
import app.aegis.ai.gemini.types.Source
import app.aegis.ai.gemini.types.Tool
import app.aegis.models.RiskLevel
import app.aegis.models.ScamVerdict
import app.aegis.models.parseGeminiJson
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
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
            logger = object : Logger {
                override fun log(message: String) {
                    println("HTTP Client:$message")
                }
            }
            //TODO, enable only in debug build
            level = LogLevel.ALL
        }
    }

    // Using Gemini 3 Pro (via AI Studio Free Tier)
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/"

    suspend fun analyze(screenText: String, model: String = "gemini-2.5-flash"): ScamVerdict {
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
        // If the internet is down or Google Search fails, the "Mental Toolkit" section still gives Gemini the logic to spot the scam ("Urgency Check", "Link Analysis"). It falls back to being smart, but tries to be a researcher first.
        val prompt = """
            You are Aegis, an Autonomous Security Investigator.
            
            [CASE FILE]
            - Raw Text: "$screenText"
            - Tool Report: $toolOutput
            
            [YOUR MISSION]
            Investigate the "Raw Text" for potential fraud, coercion, or scams.
            
            **MANDATORY INVESTIGATION STEP:**
            **USE GOOGLE SEARCH** to verify the specific claims, phone numbers, or links found in the text.
            - If it mentions an organization (e.g., FedEx, Mumbai Police, RBI), search if they typically use this communication method.
            - Search for the specific phrasing (e.g., "package seized", "video call verification") to see if it matches recent fraud alerts.
            
            [MENTAL TOOLKIT - Combine Search with Logic]
            1. Official Protocol: Do government agencies or banks ever use personal numbers or video calls? (Verify via Search).
            2. Urgency Check: Does the text threaten "arrest", "blocking", or "expiry" within a short time?
            3. Link Analysis: Are they using "bit.ly", "ngrok", or unofficial domains for banking?
            
            [OUTPUT REQUIREMENTS]
            Return strictly JSON with this schema:
            {
              "riskLevel": "DANGER" | "WARN" | "SAFE",
              "reason": "Briefly state the verdict. MUST cite the specific evidence found via Search (e.g., 'Google Search confirms Mumbai Police issued a warning about video call scams on Jan 2026').",
              "confidence": 0-100
            }
        """.trimIndent()

        // 3. SEND REQUEST
        return try {
            val response: GenerateContentResponse = client.post("$baseUrl$model:generateContent") {
                contentType(ContentType.Application.Json)
                setBody(
                    GenerateContentRequest(
                        contents = listOf(Content(listOf(Part(prompt)))),
                        generationConfig = GenerateContentConfig(
                            responseMimeType = "application/json",
                            // So our prompt above enforces the logic.
                            includeThoughts = true
                        ),
                        tools = listOf(
                            Tool(
                                googleSearch = GoogleSearchRetrieval()
                            )
                        )
                    )
                )
                headers {
                    append("x-goog-api-key", apiKey)
                }
            }.body()

            /*val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            parseGeminiJson(rawText)*/

            val candidate = response.candidates?.firstOrNull()
            val rawText = candidate?.content?.parts?.firstOrNull()?.text ?: ""

            // 1. Parse the Text Verdict
            val baseVerdict = parseGeminiJson(rawText)

            // 2. Extract Sources from Metadata
            val extractedSources = candidate?.groundingMetadata?.groundingChunks
                ?.mapNotNull { it.web }
                ?.map { Source(title = it.title ?: "Source", url = it.uri ?: "") }
                ?.distinctBy { it.url } // Remove duplicates
                ?: emptyList()

            // 3. Combine
            baseVerdict.copy(sources = extractedSources)
        } catch (e: Exception) {
            e.printStackTrace()
            ScamVerdict(RiskLevel.SAFE, "Agent Offline", 0)
        }
    }
}