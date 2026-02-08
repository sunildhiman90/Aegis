package app.aegis.ai.gemini

import app.aegis.AegisConfig
import app.aegis.ai.gemini.types.Content
import app.aegis.ai.gemini.types.GenerateContentConfig
import app.aegis.ai.gemini.types.GenerateContentRequest
import app.aegis.ai.gemini.types.GenerateContentResponse
import app.aegis.ai.gemini.types.GoogleSearchRetrieval
import app.aegis.ai.gemini.types.InlineData
import app.aegis.ai.gemini.types.Part
import app.aegis.ai.gemini.types.Source
import app.aegis.ai.gemini.types.Tool
import app.aegis.models.NudityVerdict
import app.aegis.models.RiskLevel
import app.aegis.models.ScamVerdict
import app.aegis.models.SensitivityLevel
import app.aegis.models.parseGeminiJson
import app.aegis.models.parseNudityVerdictJson
import app.aegis.tools.LocalRisk
import app.aegis.tools.SecurityTools
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class GeminiClient(
    private val client: HttpClient,
) {
    //    private val defaultModel = "gemini-3.0-pro"
//    private val defaultModelVision = "gemini-3.0-flash"

    // TODO, just for testing, need to remove later
    private val defaultModel = "gemini-2.5-pro"
    private val defaultModelVision = "gemini-2.5-flash"

    // Using Gemini 3 Pro (via AI Studio Free Tier)
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/"

    /**
     * 1. TEXT MODE (Chat Scams)
     * Restored the "Mental Toolkit" for maximum security accuracy.
     */
    suspend fun analyze(
        screenText: String,
        sensitivity: SensitivityLevel = SensitivityLevel.BALANCED,
        model: String = defaultModel,
    ): ScamVerdict {
        val apiKey = AegisConfig.GEMINI_API_KEY

        // Local Tools Pre-check
        val localAnalysis = SecurityTools.analyzeLocally(screenText)
        val toolOutput =
            when (localAnalysis) {
                LocalRisk.HIGH_RISK -> "Local Regex Tool detected CRITICAL keywords."
                LocalRisk.SUSPICIOUS -> "Local Regex Tool detected SUSPICIOUS keywords."
                LocalRisk.SAFE -> "Local Regex Tool found no pattern."
            }

        // Sensitivity Instructions
        val sensitivityInstruction =
            when (sensitivity) {
                SensitivityLevel.LOW -> "SENSITIVITY: LOW. You are a conservative investigator. Only flag this as DANGER if you have 90%+ certainty and concrete evidence (like a known bad link or typical fraud pattern). If unsure, mark SAFE."
                SensitivityLevel.AGGRESSIVE -> "SENSITIVITY: HIGH. You are a paranoid security guard. If there is ANY typical scam pattern (urgency, secrecy, authority, unusual request), flag it immediately. Err on the side of caution."
                else -> "SENSITIVITY: BALANCED. Use your best judgment. If you see psychological manipulation or scam indicators, flag it."
            }

        // 🛑 AI Prompt
        val groundingInstruction =
            """
            **MANDATORY INVESTIGATION STEP:**
            **USE GOOGLE SEARCH** to verify the specific claims, phone numbers, or links found in the text.
            - If it mentions an organization (e.g., FedEx, Mumbai Police, RBI), search if they typically use this communication method.
            - Search for the specific phrasing (e.g., "package seized", "video call verification") to see if it matches recent fraud alerts.
            """.trimIndent()

        val prompt =
            """
            You are Aegis, an Autonomous Security Investigator.
            
            [CASE FILE]
            - Raw Text: "$screenText"
            - Tool Report: $toolOutput
            - Operation Mode: $sensitivityInstruction
            
            [YOUR MISSION]
            Investigate the "Raw Text" for potential fraud, coercion, or scams.
            
            $groundingInstruction
            
            [MENTAL TOOLKIT - Combine Search with Logic]
            1. Official Protocol: Do government agencies or banks ever use personal numbers or video calls? (Verify via Search if enabled).
            2. Urgency Check: Does the text threaten "arrest", "blocking", or "expiry" within a short time?
            3. Link Analysis: Are they using "bit.ly", "ngrok", or unofficial domains for banking?
            
            [OUTPUT REQUIREMENTS]
            Return strictly JSON with this schema:
            {
              "riskLevel": "DANGER" | "WARN" | "SAFE",
              "reason": "Briefly state the verdict. MUST cite specific evidence found via Search.",
              "confidence": 0-100
            }
            """.trimIndent()

        return executeRequest(model, apiKey, listOf(Part(text = prompt)), useGoogleSearch = true)
    }

    /**
     * 2. VISION MODE (Video Call Scams)
     * * accepts 'base64Image' string instead of Bitmap.
     * The Platform code (Android) is responsible for converting the image.
     */
    suspend fun analyzeImage(
        base64Image: String,
        sensitivity: SensitivityLevel = SensitivityLevel.BALANCED,
        model: String = defaultModelVision,
    ): ScamVerdict {
        val apiKey = AegisConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return ScamVerdict(RiskLevel.SAFE, "No API Key", 0)

        val sensitivityInstruction =
            when (sensitivity) {
                SensitivityLevel.LOW -> "SENSITIVITY: LOW. Only flag DANGER if you see a clear, undeniable Police Uniform/Badge. If ambiguous, assume SAFE."
                SensitivityLevel.AGGRESSIVE -> "SENSITIVITY: HIGH. Use strict scrutiny. If you see ANY element that looks like a police uniform, cap, or official seal, flag it as DANGER. Better safe than sorry."
                else -> "SENSITIVITY: BALANCED. Standard check. Flag if you identify a police uniform or ongoing digital arrest setup."
            }

        val promptText =
            """
            You are an Indian Cyber-Security Agent. Analyze this video frame for "Digital Arrest" video call scams.
            
            [OPERATION MODE]
            $sensitivityInstruction
            
            Look for:
            1. People wearing Khaki uniforms or Police caps.
            2. Backgrounds that look like Police Stations, Flags, or Official Seals.
            3. Text overlays saying "CBI", "RBI", "Supreme Court".

            CRITICAL RULE: Real police NEVER video call. If you see a uniform, it is a SCAM.

            Output strictly JSON:
            {
              "riskLevel": "DANGER" | "SAFE",
              "reason": "Briefly state what was detected (e.g., 'Fake CBI Officer detected').",
              "confidence": 0-100
            }
            """.trimIndent()

        // Multipart Request (Text + Image)
        return executeRequest(
            model,
            apiKey,
            listOf(
                Part(text = promptText),
                Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image)),
            ),
            useGoogleSearch = false,
        )
    }

    /**
     * 3. SEXTORTION MODE (Video Call Frame Analysis)
     * Analyzes video frames for nudity and fake feed detection.
     * Returns NudityVerdict for quick boolean checks.
     */
    suspend fun analyzeForNudity(
        base64Image: String,
        sensitivity: SensitivityLevel = SensitivityLevel.BALANCED,
        model: String = defaultModelVision,
    ): NudityVerdict {
        val apiKey = AegisConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return NudityVerdict(nudity = false, fakeFeed = false, confidence = 0)

        val sensitivityInstruction =
            when (sensitivity) {
                SensitivityLevel.LOW -> "SENSITIVITY: LOW. Only flag explicit nudity. Ignore static artifacts unless obvious."
                SensitivityLevel.AGGRESSIVE -> "SENSITIVITY: HIGH. Flag any suggestive content or partial exposure. Be highly suspicious of any video artifacts indicating a fake feed."
                else -> "SENSITIVITY: BALANCED. Standard detection for nudity and deepfake artifacts."
            }

        val promptText =
            """
            URGENT SAFETY ANALYSIS: Analyze this video call screenshot for sextortion/blackmail threats.
            
            [OPERATION MODE]
            $sensitivityInstruction
            
            CHECK FOR NUDITY:
            - Is there ANY exposed human body parts visible? (breasts, genitals, buttocks)
            - Is there revealing/explicit clothing or poses?
            - Are there private body parts even partially visible?
            
            CHECK FOR FAKE VIDEO:
            - Does the video appear pre-recorded or looped? (look for compression artifacts, static elements, unnatural movements)
            
            CRITICAL: If there is ANY nudity or explicit content, set nudity to TRUE.
            
            Return strictly JSON: { "nudity": true/false, "fake_feed": true/false, "confidence": 0-100 }
            """.trimIndent()

        return try {
            val response: GenerateContentResponse =
                client
                    .post("${baseUrl}$model:generateContent") {
                        contentType(ContentType.Application.Json)
                        headers { append("x-goog-api-key", apiKey) }
                        setBody(
                            GenerateContentRequest(
                                contents =
                                    listOf(
                                        Content(
                                            listOf(
                                                Part(text = promptText),
                                                Part(
                                                    inlineData = InlineData(
                                                        mimeType = "image/jpeg",
                                                        data = base64Image
                                                    )
                                                ),
                                            ),
                                        ),
                                    ),
                                generationConfig = GenerateContentConfig(responseMimeType = "application/json"),
                            ),
                        )
                    }.body()

            // Check for safety block (likely nudity)
            val candidate = response.candidates?.firstOrNull()
            if (candidate?.finishReason == "SAFETY") {
                return NudityVerdict(nudity = true, fakeFeed = false, confidence = 100)
            }

            val rawText =
                candidate
                    ?.content
                    ?.parts
                    ?.firstOrNull()
                    ?.text ?: ""
            parseNudityVerdictJson(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            NudityVerdict(nudity = false, fakeFeed = false, confidence = 0)
        }
    }

    // ... (executeRequest remains the same) ...
    private suspend fun executeRequest(
        model: String,
        apiKey: String,
        parts: List<Part>,
        useGoogleSearch: Boolean,
    ): ScamVerdict {
        // ... (Same logic as before)
        return try {
            val toolsList = if (useGoogleSearch) listOf(Tool(googleSearch = GoogleSearchRetrieval())) else emptyList()

            val response: GenerateContentResponse =
                client
                    .post("$baseUrl$model:generateContent") {
                        contentType(ContentType.Application.Json)
                        headers { append("x-goog-api-key", apiKey) }
                        setBody(
                            GenerateContentRequest(
                                contents = listOf(Content(parts)),
                                generationConfig = GenerateContentConfig(responseMimeType = "application/json"),
                                tools = toolsList,
                            ),
                        )
                    }.body()

            // CHECK 1: Did we get a valid candidate?
            val candidate = response.candidates?.firstOrNull()

            // CHECK 2: Did it finish because of Safety?
            // If the model saw nudity and refused to generate text, "finishReason" will be "SAFETY"
            if (candidate?.finishReason == "SAFETY") {
                return ScamVerdict(
                    riskLevel = RiskLevel.DANGER,
                    reason = "🚨 Content blocked by AI Safety filters (Likely Nudity/Violence)",
                    confidence = 100,
                )
            }

            // FIND THE JSON PART:
            // The model might return "Thinking..." text in one part, and JSON in another.
            // We search for the part that contains a JSON object.
            val partWithJson =
                candidate?.content?.parts?.find {
                    val text = it.text?.trim() ?: ""
                    text.startsWith("{") || text.contains("```json")
                } ?: candidate?.content?.parts?.firstOrNull()

            val rawText = partWithJson?.text ?: ""

            // 1. Parse the Text Verdict
            val baseVerdict = parseGeminiJson(rawText)

            // 2. Extract Sources from Metadata
            val extractedSources =
                candidate
                    ?.groundingMetadata
                    ?.groundingChunks
                    ?.mapNotNull { it.web }
                    ?.map { Source(title = it.title ?: "Source", url = it.uri ?: "") }
                    ?.distinctBy { it.url } // Remove duplicates
                    ?: emptyList()

            // 3. Combine
            baseVerdict.copy(sources = extractedSources)
        } catch (e: Exception) {
            e.printStackTrace()
            ScamVerdict(RiskLevel.SAFE, "Analysis Failed", 0)
        }
    }

    /**
     * 4. PHISHING LINK MODE (The Marathon Agent)
     * Analyzes URLs for phishing and generates a takedown report.
     * If the provider is unknown, it falls back to report_phishing@google.com (Google Safe Browsing).
     *
     * @param url The URL to analyze
     * @param model The model to use for analysis
     * @return The verdict of the analysis
     */
    suspend fun analyzeUrl(
        url: String,
        contextText: String,
        sensitivity: SensitivityLevel = SensitivityLevel.BALANCED,
        model: String = defaultModelVision,
    ): app.aegis.models.PhishingVerdict {
        val apiKey = AegisConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) return app.aegis.models.PhishingVerdict(app.aegis.models.RiskLevel.SAFE, "No API Key", 0)

        val sensitivityInstruction =
            when (sensitivity) {
                SensitivityLevel.LOW -> "SENSITIVITY: LOW. Only flag if it is a KNOWN malicious pattern or unmistakable homograph attack. If unsure, assume SAFE."
                SensitivityLevel.AGGRESSIVE -> "SENSITIVITY: HIGH. Flag ANY suspicious TLD (.xyz, .cc), misuse of free hosting (ngrok, vercel), or mismatch between content and domain. Err on the side of caution."
                else -> "SENSITIVITY: BALANCED. Standard phishing detection."
            }

        val groundingInstruction =
            "Use GOOGLE SEARCH to verify if this domain is known for phishing or if the brand typically uses this TLD."

        // 🛑 CRITICAL PROMPT: Detects Phishing & Scams in context
        val promptText =
            """
            You are a Cyber-Security Analyst. Investigate this URL for phishing/scam activity, specifically in the context of the provided message.
            
            [OPERATION MODE]
            $sensitivityInstruction
            
            [CASE DATA]
            URL: "$url"
            MESSAGE CONTEXT: "$contextText"
            
            $groundingInstruction
            
            Check for:
            1. Phishing: Homograph attacks, suspicious TLDs (.top, .xyz), or brand impersonation.
            2. Scams: Even if the link is safe (e.g., bit.ly or whatsapp.com), is the message context a scam? 
               Look for urgency, "digital arrest", payment requests, or coercion.
            
            MISSION: 
            If the link is dangerous OR the message context is a scam (even with a safe link), set riskLevel to DANGER.
            
            Return strictly JSON:
            {
              "riskLevel": "DANGER" | "WARN" | "SAFE",
              "reason": "Brief explanation. (e.g., 'Safe link but message is a sextortion attempt' or 'Phishing link for FedEx').",
              "confidence": 0-100,
              "sources": [ { "title": "Evidence Source", "url": "https://..." } ],
              "recipient": "abuse@<hosting_provider>.com", 
              "subject": "Security Threat Report: $url",
              "body": "To whom it may concern,\n\nThe following URL/Context was flagged as a threat:\nURL: $url\nContext: $contextText\n\nPlease investigate.\n\nSent via Aegis Security Agent."
            }
            """.trimIndent()

        val toolsList = listOf(Tool(googleSearch = GoogleSearchRetrieval()))

        return try {
            val response: GenerateContentResponse =
                client
                    .post("$baseUrl$model:generateContent") {
                        contentType(ContentType.Application.Json)
                        headers { append("x-goog-api-key", apiKey) }
                        setBody(
                            GenerateContentRequest(
                                contents = listOf(Content(listOf(Part(text = promptText)))),
                                generationConfig = GenerateContentConfig(responseMimeType = "application/json"),
                                tools = toolsList,
                            ),
                        )
                    }.body()

            // FIND THE JSON PART:
            // The model might return "Thinking..." text in one part, and JSON in another.
            // We search for the part that contains a JSON object.
            val partWithJson =
                response.candidates?.firstOrNull()?.content?.parts?.find {
                    val text = it.text?.trim() ?: ""
                    text.startsWith("{") || text.contains("```json")
                } ?: response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()

            val rawText = partWithJson?.text ?: ""
            app.aegis.models.parsePhishingVerdictJson(rawText)
        } catch (e: Exception) {
            e.printStackTrace()
            app.aegis.models.PhishingVerdict(app.aegis.models.RiskLevel.SAFE, "Analysis Failed", 0)
        }
    }
}
