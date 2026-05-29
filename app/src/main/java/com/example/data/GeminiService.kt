package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun getMatchAnalysis(
        team1: String,
        team2: String,
        group: String,
        venue: String,
        status: String,
        score: String,
        customApiKey: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = if (customApiKey.isNotBlank()) {
            customApiKey.trim()
        } else {
            try {
                BuildConfig.GEMINI_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Google AI Studio API Key not configured. If the global key has issues, you can paste your custom Gemini API Key below to run real-time analysis!"
        }

        val prompt = if (status == "UPCOMING") {
            "You are a professional World Cup soccer analyst. Provide a brief, highly engaging, 3-sentence prediction and tactical battleground for the upcoming match: $team1 vs $team2 (Group $group) to be held at $venue. Be direct, exciting, and professional."
        } else {
            "You are a professional World Cup soccer analyst. Provide a brief, highly engaging, 3-sentence recap/tactical analysis for this match: $team1 vs $team2. Current status is $status with score: $score. Discuss key match momentum and implications on their World Cup journey in Group $group."
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }
        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())

        val modelCandidates = listOf("gemini-2.5-flash", "gemini-1.5-flash", "gemini-flash-latest", "gemini-3.5-flash")
        var lastErrorMsg = ""

        for (modelName in modelCandidates) {
            try {
                Log.d(TAG, "Attempting Gemini analysis with model: $modelName")
                val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(endpoint)
                    .post(requestBody)
                    .build()

                val result = client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val jsonResponse = JSONObject(bodyStr)
                        val candidatesArray = jsonResponse.optJSONArray("candidates")
                        val firstCandidate = candidatesArray?.optJSONObject(0)
                        val content = firstCandidate?.optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        val firstPart = parts?.optJSONObject(0)
                        val text = firstPart?.optString("text")

                        if (!text.isNullOrBlank()) {
                            text.trim()
                        } else {
                            null
                        }
                    } else {
                        val errorMsg = response.body?.string() ?: ""
                        Log.e(TAG, "Request failed for model $modelName: $errorMsg, code ${response.code}")
                        
                        val parsedMsg = try {
                            val obj = JSONObject(errorMsg)
                            obj.getJSONObject("error").getString("message")
                        } catch (e: Exception) {
                            null
                        }

                        lastErrorMsg = if (!parsedMsg.isNullOrBlank()) {
                            "AI Studio Error ($modelName): $parsedMsg (HTTP ${response.code})"
                        } else {
                            "Unable to query ($modelName). (HTTP Error Code ${response.code})"
                        }
                        null
                    }
                }

                if (result != null) {
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception calling Gemini model $modelName", e)
                lastErrorMsg = "Connection error for model $modelName: ${e.localizedMessage ?: ""}"
            }
        }

        return@withContext "Google AI Studio Request Failed.\n\nDetails of the final attempt:\n$lastErrorMsg\n\nNote: If this 403 persists across all models, please double-check that your Gemini API key in the AI Studio Secrets panel is valid and has not been restricted or expired."
    }
}
