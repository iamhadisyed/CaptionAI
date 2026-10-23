package com.captionai.studio.ai

import android.util.Log
import com.captionai.studio.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GroqClient — High-speed inference using Groq Cloud API (LPU hardware).
 * Acts as the PRIMARY (1st Priority) AI engine for Caption AI.
 *
 * Supported models:
 *  - Text & Trending Captions: GPT OSS 120B (openai/gpt-oss-120b) with fallback to 20B (openai/gpt-oss-20b)
 *  - Multimodal Photo Analysis: Qwen 3.8 27B Vision (qwen/qwen3.8-27b)
 *
 * Automatically falls over to Google Gemini if Groq rate limits are exceeded (429)
 * or if Groq is unconfigured / encounters network failure.
 */
object GroqClient {

    private const val TAG = "GroqClient"
    private const val BASE_URL = "https://api.groq.com/openai/v1/chat/completions"
    
    // Active production models hosted on Groq Cloud
    private const val TEXT_MODEL = "openai/gpt-oss-120b"
    private const val FAST_TEXT_MODEL = "openai/gpt-oss-20b"
    private const val VISION_MODEL = "qwen/qwen3.8-27b"

    /**
     * Checks if a Groq API key is available via custom user key or BuildConfig.
     */
    fun hasConfiguredKey(customKey: String? = null): Boolean {
        return getActiveKey(customKey).isNotBlank()
    }

    /**
     * Retrieves the active Groq key.
     * Prioritizes custom key override if it's a valid Groq key (starts with gsk_),
     * otherwise falls back to BuildConfig.GROQ_API_KEY.
     */
    fun getActiveKey(customKey: String? = null): String {
        if (!customKey.isNullOrBlank()) {
            val trimmed = customKey.trim()
            if (trimmed.startsWith("gsk_")) {
                return trimmed
            }
        }
        return BuildConfig.GROQ_API_KEY.trim()
    }

    /**
     * Generates captions via Groq Cloud API.
     */
    fun generateCaptions(prompt: String, apiKey: String, imageBase64: String?, platform: String): CaptionResult {
        val model = if (imageBase64 != null) VISION_MODEL else TEXT_MODEL
        Log.i(TAG, "Calling Groq Cloud ($model) for captions on platform: $platform")

        return try {
            val rawResponse = callGroqChat(
                prompt = prompt,
                apiKey = apiKey,
                imageBase64 = imageBase64,
                model = model,
                useJsonObjectMode = true
            )
            parseCaptionResponse(rawResponse, platform)
        } catch (e: Exception) {
            // If primary 120b text model is throttled/rate-limited, try fast 20b model before failover
            if (imageBase64 == null && (e.message?.contains("429") == true || e.message?.contains("rate_limit", true) == true)) {
                Log.w(TAG, "120b throttled, trying fast 20b model on Groq: ${e.message}")
                val rawResponse = callGroqChat(
                    prompt = prompt,
                    apiKey = apiKey,
                    imageBase64 = null,
                    model = FAST_TEXT_MODEL,
                    useJsonObjectMode = true
                )
                parseCaptionResponse(rawResponse, platform)
            } else {
                throw e
            }
        }
    }

    /**
     * Syncs trending captions via Groq Cloud API.
     */
    fun syncTrendingCaptions(prompt: String, apiKey: String): List<String> {
        Log.i(TAG, "Calling Groq Cloud ($TEXT_MODEL) for trending sync")
        val rawResponse = try {
            callGroqChat(
                prompt = prompt,
                apiKey = apiKey,
                imageBase64 = null,
                model = TEXT_MODEL,
                useJsonObjectMode = false
            )
        } catch (e: Exception) {
            if (e.message?.contains("429") == true || e.message?.contains("rate_limit", true) == true) {
                Log.w(TAG, "120b throttled for trends, trying 20b model on Groq")
                callGroqChat(
                    prompt = prompt,
                    apiKey = apiKey,
                    imageBase64 = null,
                    model = FAST_TEXT_MODEL,
                    useJsonObjectMode = false
                )
            } else throw e
        }
        return parseTrendingResponse(rawResponse)
    }

    /**
     * Low-level HTTP POST to Groq OpenAI-compatible Chat Completions endpoint.
     */
    private fun callGroqChat(
        prompt: String,
        apiKey: String,
        imageBase64: String?,
        model: String,
        useJsonObjectMode: Boolean
    ): String {
        val url = URL(BASE_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
        conn.setRequestProperty("User-Agent", "CaptionAI-Android/${BuildConfig.VERSION_NAME}")
        conn.doOutput = true
        conn.connectTimeout = 25_000
        conn.readTimeout = 25_000

        val requestBody = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put("max_completion_tokens", 1000)
            if (useJsonObjectMode) {
                put("response_format", JSONObject().put("type", "json_object"))
            }
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are an expert social media caption writer and trend researcher. Respond strictly with raw valid JSON.")
                })
                put(JSONObject().apply {
                    put("role", "user")
                    if (imageBase64 != null) {
                        put("content", JSONArray().apply {
                            put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                put("type", "image_url")
                                put("image_url", JSONObject().apply {
                                    put("url", "data:image/jpeg;base64,$imageBase64")
                                })
                            })
                        })
                    } else {
                        put("content", prompt)
                    }
                })
            })
        }

        conn.outputStream.use { it.write(requestBody.toString().toByteArray()) }

        val responseCode = conn.responseCode
        if (responseCode == 200) {
            return conn.inputStream.bufferedReader().readText()
        } else {
            val errorText = conn.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            Log.e(TAG, "Groq Cloud API Error $responseCode: $errorText")
            val cleanMessage = try {
                val json = JSONObject(errorText)
                if (json.has("error")) {
                    val err = json.get("error")
                    if (err is JSONObject && err.has("message")) err.getString("message") else err.toString()
                } else errorText
            } catch (_: Exception) {
                errorText
            }

            if (responseCode == 429) {
                throw Exception("Groq Rate Limit Exceeded (429): $cleanMessage")
            } else {
                throw Exception("Groq API $responseCode: $cleanMessage")
            }
        }
    }

    private fun parseCaptionResponse(raw: String, platform: String): CaptionResult {
        val choices = JSONObject(raw).getJSONArray("choices")
        if (choices.length() == 0) throw Exception("No choices in Groq response")
        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")

        val cleanJson = extractJsonBlock(content)
        val parsed = JSONObject(cleanJson)

        val captions = mutableListOf<String>()
        val capsArr = parsed.optJSONArray("captions") ?: parsed.optJSONArray("caption") ?: JSONArray()
        for (i in 0 until capsArr.length()) {
            captions.add(capsArr.getString(i))
        }

        if (captions.isEmpty()) {
            throw Exception("Groq returned empty captions list")
        }

        val hashtags = mutableListOf<String>()
        val tagsArr = parsed.optJSONArray("hashtags") ?: parsed.optJSONArray("tags") ?: JSONArray()
        for (i in 0 until tagsArr.length()) {
            val tag = tagsArr.getString(i).trim()
            hashtags.add("#${tag.removePrefix("#")}")
        }

        return CaptionResult(
            captions = captions,
            hashtags = hashtags,
            description = parsed.optString("description", ""),
            bio = parsed.optString("bio", ""),
            platform = platform
        )
    }

    private fun parseTrendingResponse(raw: String): List<String> {
        val choices = JSONObject(raw).getJSONArray("choices")
        if (choices.length() == 0) throw Exception("No choices in Groq response")
        val text = choices.getJSONObject(0).getJSONObject("message").getString("content")

        // 1. Direct JSON array search
        val jsonStart = text.indexOf("[")
        val jsonEnd = text.lastIndexOf("]")
        if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
            val jsonArrayPart = text.substring(jsonStart, jsonEnd + 1)
            val arr = JSONArray(jsonArrayPart)
            return (0 until arr.length()).map { arr.getString(it) }
        }

        // 2. Check if wrapped in an object like {"trends": [...] or "captions": [...]}
        try {
            val obj = JSONObject(extractJsonBlock(text))
            val arr = obj.optJSONArray("trends") ?: obj.optJSONArray("captions") ?: obj.optJSONArray("items")
            if (arr != null && arr.length() > 0) {
                return (0 until arr.length()).map { arr.getString(it) }
            }
        } catch (_: Exception) {}

        // 3. Fallback for line-separated items
        val lines = text.lines()
            .map { it.trim().removePrefix("-").removePrefix("•").removePrefix("\"").removeSuffix("\"").trim() }
            .filter { it.length > 5 && !it.startsWith("[") && !it.startsWith("]") && !it.startsWith("{") }
        if (lines.isNotEmpty()) return lines

        throw Exception("Could not parse trending captions from Groq response: $text")
    }

    private fun extractJsonBlock(text: String): String {
        val trimmed = text.replace("```json", "").replace("```", "").trim()
        val start = trimmed.indexOf("{")
        val end = trimmed.lastIndexOf("}")
        return if (start != -1 && end != -1 && end > start) {
            trimmed.substring(start, end + 1)
        } else {
            trimmed
        }
    }
}
