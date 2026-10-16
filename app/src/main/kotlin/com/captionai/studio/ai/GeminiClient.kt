package com.captionai.studio.ai

import android.util.Log
import com.captionai.studio.storage.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

// ─────────────────────────────────────────────────────────────────────────────
// Data models
// ─────────────────────────────────────────────────────────────────────────────

data class CaptionRequest(
    val platform: String,
    val contentType: String,
    val tone: String,
    val length: String,
    val userContext: String,        // Can be in ANY language
    val language: String = "English" // Output language preference
)

data class CaptionResult(
    val captions: List<String>,
    val hashtags: List<String>,
    val description: String,
    val bio: String,
    val platform: String
)

object RateLimiter {
    private val requestTimestamps = mutableListOf<Long>()
    private const val MAX_PER_MINUTE = 14
    private const val WINDOW_MS = 60_000L

    fun canMakeRequest(): Boolean {
        val now = System.currentTimeMillis()
        requestTimestamps.removeAll { now - it > WINDOW_MS }
        return requestTimestamps.size < MAX_PER_MINUTE
    }

    fun recordRequest() {
        requestTimestamps.add(System.currentTimeMillis())
    }

    fun msUntilNextSlot(): Long {
        if (requestTimestamps.isEmpty()) return 0L
        val oldest = requestTimestamps.min()
        val wait = WINDOW_MS - (System.currentTimeMillis() - oldest)
        return if (wait > 0) wait else 0L
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gemini API client
// ─────────────────────────────────────────────────────────────────────────────

object GeminiClient {

    private const val MODEL_ID = "gemini-2.5-flash" // Stable model as per user requirement
    private const val TAG = "GeminiClient"

    // API Key Rotation - Failover support for 3 credentials
    // ⚠️ IMPORTANT: DO NOT hardcode real API keys here.
    // Store your 3 Gemini API keys in local.properties (which is git-ignored):
    //   GEMINI_KEY_1=AIzaSy...
    //   GEMINI_KEY_2=AIzaSy...
    //   GEMINI_KEY_3=AIzaSy...
    // Then reference via BuildConfig (configure in app/build.gradle with buildConfigField)
    private val apiKeys = listOf(
        BuildConfig.GEMINI_KEY_1,
        BuildConfig.GEMINI_KEY_2,
        BuildConfig.GEMINI_KEY_3
    ).filter { it.isNotBlank() }
    private var currentKeyIndex = 0

    var lastResult: CaptionResult? = null
    var lastRequest: CaptionRequest? = null
    var pendingImageBase64: String? = null

    /**
     * Requirement 1: Stable Caption Generation API implementation.
     * Logic remains unchanged, but wrapped with key rotation failover.
     */
    suspend fun generateCaptions(request: CaptionRequest, imageBase64: String? = null): Result<CaptionResult> =
        withContext(Dispatchers.IO) {
            try {
                if (request == lastRequest && lastResult != null) {
                    return@withContext Result.success(lastResult!!)
                }

                if (!RateLimiter.canMakeRequest()) {
                    val waitMs = RateLimiter.msUntilNextSlot()
                    return@withContext Result.failure(Exception("Rate limit reached. Please wait ${waitMs / 1000}s"))
                }

                val prompt = buildCaptionPrompt(request)
                // Use rotation-enabled executor
                val responseText = executeWithRotation(prompt, imageBase64, isTrendingSync = false)
                RateLimiter.recordRequest()

                val parsed = parseCaptionResponse(responseText, request.platform)
                lastRequest = request
                lastResult = parsed
                Result.success(parsed)
            } catch (e: Exception) {
                Log.e(TAG, "Caption generation error", e)
                Result.failure(e)
            }
        }

    /**
     * Requirement 2: Trending Sync investigation and update.
     * Uses current API structure (v1beta fallback) to ensure connectivity.
     */
    suspend fun syncTrendingCaptions(language: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                if (!RateLimiter.canMakeRequest()) {
                    return@withContext Result.failure(Exception("Rate limit reached"))
                }

                val prompt = buildTrendingPrompt(language)
                // Updated to use the same robust rotation and latest endpoint logic
                val responseText = executeWithRotation(prompt, null, isTrendingSync = true)
                RateLimiter.recordRequest()

                val captions = parseTrendingResponse(responseText)
                Result.success(captions)
            } catch (e: Exception) {
                Log.e(TAG, "Trending sync error", e)
                Result.failure(e)
            }
        }

    /**
     * Requirement 3: API Key failover logic.
     */
    private fun executeWithRotation(prompt: String, imageBase64: String?, isTrendingSync: Boolean): String {
        val userKey = PrefsManager.getInstance().getApiKey()
        if (!userKey.isNullOrBlank()) {
            Log.d(TAG, "Using custom user API key")
            return callGeminiInternal(prompt, userKey.trim(), imageBase64, isTrendingSync)
        }

        var lastException: Exception? = null
        val startingIndex = currentKeyIndex

        for (i in apiKeys.indices) {
            val keyIndex = (startingIndex + i) % apiKeys.size
            val key = apiKeys[keyIndex]

            try {
                Log.i(TAG, "Executing API call with Key #${keyIndex + 1}")
                val result = callGeminiInternal(prompt, key, imageBase64, isTrendingSync)
                
                // Update current working key index
                currentKeyIndex = keyIndex
                return result
            } catch (e: Exception) {
                val errorMsg = e.message ?: ""
                // Rotate only on quota (429) or certain permission (403) errors
                if (errorMsg.contains("429") || errorMsg.contains("quota", true) || errorMsg.contains("403")) {
                    Log.w(TAG, "Key #${keyIndex + 1} exhausted. Rotating to next...")
                    lastException = e
                    continue
                } else {
                    throw e // Permanent error (e.g. 400 Bad Request), don't rotate
                }
            }
        }
        throw lastException ?: Exception("Failover failed: No available keys.")
    }

    private fun callGeminiInternal(prompt: String, apiKey: String, imageBase64: String?, isTrendingSync: Boolean): String {
        // Trending sync uses v1beta for better batch handling; Caption uses v1 for stability
        // We will try multiple combinations for TrendingSync specifically if it fails
        val version = if (isTrendingSync) "v1beta" else "v1"
        val urlStr = "https://generativelanguage.googleapis.com/$version/models/$MODEL_ID:generateContent?key=${apiKey.trim()}"
        
        Log.d(TAG, "Calling Gemini $version with $MODEL_ID (isSync: $isTrendingSync)")
        
        try {
            val url = URL(urlStr)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 30_000
            conn.readTimeout = 30_000

            val body = buildRequestBody(prompt, imageBase64)
            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                return conn.inputStream.bufferedReader().readText()
            } else {
                val errorText = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
                Log.e(TAG, "API Error $responseCode: $errorText")
                
                // If 404 and it's trending sync, try standard flash model
                if (isTrendingSync && responseCode == 404) {
                    return retryWithModel(prompt, apiKey, "gemini-1.5-flash", "v1beta")
                }
                
                // Extract clean error message
                val cleanMessage = try {
                    val json = JSONObject(errorText)
                    if (json.has("error")) json.getJSONObject("error").getString("message") else errorText
                } catch (_: Exception) { errorText }
                
                throw Exception("$responseCode: $cleanMessage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Request failed", e)
            throw e
        }
    }

    private fun retryWithModel(prompt: String, apiKey: String, model: String, version: String): String {
        val urlStr = "https://generativelanguage.googleapis.com/$version/models/$model:generateContent?key=${apiKey.trim()}"
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.doOutput = true
        val body = buildRequestBody(prompt, null)
        conn.outputStream.use { it.write(body.toString().toByteArray()) }
        if (conn.responseCode == 200) return conn.inputStream.bufferedReader().readText()
        throw Exception("Failed to sync trends on all endpoints.")
    }

    private fun buildCaptionPrompt(request: CaptionRequest): String {
        val outputLangInstruction = when (request.language) {
            "Urdu"       -> "Write all captions and bio in Urdu script (اردو)."
            "Roman Urdu" -> "Write all captions and bio in Roman Urdu (Urdu written in English letters)."
            else         -> "Write all captions and bio in English."
        }

        val lengthGuide = when (request.length) {
            "Short"  -> "1–2 lines each"
            "Long"   -> "4–6 lines each"
            else     -> "2–3 lines each"
        }

        return """
You are a viral social media caption writer. $outputLangInstruction
The user wants captions for: ${request.platform}
Content type: ${request.contentType}
Tone: ${request.tone}
Length: $lengthGuide

User context: "${request.userContext}"

Respond ONLY with a valid JSON object:
{
  "captions": ["Caption 1", "Caption 2", "Caption 3"],
  "hashtags": ["tag1", "tag2"],
  "description": "Post description",
  "bio": "Bio suggestion"
}
        """.trimIndent()
    }

    private fun buildTrendingPrompt(language: String): String {
        val lang = if (language == "Urdu") "Urdu script" else if (language == "Roman Urdu") "Roman Urdu" else "English"
        return """
You are a social media trend expert. Write in $lang.
Generate 25 viral social media captions that are currently trending.
Respond ONLY with a JSON array of strings: ["Caption 1", "Caption 2", ..., "Caption 25"]
        """.trimIndent()
    }

    private fun buildRequestBody(prompt: String, imageBase64: String?): JSONObject {
        return JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                        imageBase64?.let { base64 ->
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64)
                                })
                            })
                        }
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
                put("maxOutputTokens", 2048)
            })
        }
    }

    private fun parseCaptionResponse(raw: String, platform: String): CaptionResult {
        val json = JSONObject(raw).getJSONArray("candidates").getJSONObject(0)
            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
        val parsed = JSONObject(json.replace("```json", "").replace("```", "").trim())
        
        val captions = mutableListOf<String>()
        val capsArr = parsed.getJSONArray("captions")
        for (i in 0 until capsArr.length()) captions.add(capsArr.getString(i))

        val hashtags = mutableListOf<String>()
        val tagsArr = parsed.getJSONArray("hashtags")
        for (i in 0 until tagsArr.length()) hashtags.add("#${tagsArr.getString(i).trim().removePrefix("#")}")

        return CaptionResult(captions, hashtags, parsed.optString("description"), parsed.optString("bio"), platform)
    }

    private fun parseTrendingResponse(raw: String): List<String> {
        val candidates = JSONObject(raw).getJSONArray("candidates")
        if (candidates.length() == 0) throw Exception("No candidates in response")
        
        val content = candidates.getJSONObject(0).getJSONObject("content")
        val parts = content.getJSONArray("parts")
        if (parts.length() == 0) throw Exception("No parts in response")
        
        val text = parts.getJSONObject(0).getString("text")
        
        // More robust JSON extraction for array
        val jsonStart = text.indexOf("[")
        val jsonEnd = text.lastIndexOf("]")
        if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
            // Fallback: maybe it's a newline-separated list? 
            // Try to split by common list markers if JSON fails
            val lines = text.lines()
                .map { it.trim().removePrefix("-").removePrefix("•").trim() }
                .filter { it.length > 5 && !it.startsWith("[") && !it.startsWith("]") }
            if (lines.isNotEmpty()) return lines
            throw Exception("Could not find JSON array in response: $text")
        }
        
        val jsonArrayPart = text.substring(jsonStart, jsonEnd + 1)
        val arr = JSONArray(jsonArrayPart)
        return (0 until arr.length()).map { arr.getString(it) }
    }
}
