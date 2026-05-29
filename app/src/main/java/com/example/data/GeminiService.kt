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
        score: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Google AI Studio API Key not configured. Open the AI Studio Secrets panel on the left sidebar, add 'GEMINI_API_KEY' with your Google API Key, and rebuild to unlock real-time tactical AI analysis!"
        }

        val prompt = if (status == "UPCOMING") {
            "You are a professional World Cup soccer analyst. Provide a brief, highly engaging, 3-sentence prediction and tactical battleground for the upcoming match: $team1 vs $team2 (Group $group) to be held at $venue. Be direct, exciting, and professional."
        } else {
            "You are a professional World Cup soccer analyst. Provide a brief, highly engaging, 3-sentence recap/tactical analysis for this match: $team1 vs $team2. Current status is $status with score: $score. Discuss key match momentum and implications on their World Cup journey in Group $group."
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            
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
            val request = Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Request failed: $errorMsg, code ${response.code}")
                    if (response.code == 400 && errorMsg.contains("API key not valid")) {
                        return@withContext "Invalid API Key. Please verify your GEMINI_API_KEY in the AI Studio Secrets panel."
                    }
                    return@withContext "Unable to query Google AI Studio. (HTTP Error Code ${response.code})"
                }

                val bodyStr = response.body?.string() ?: return@withContext "Received response without text."
                val jsonResponse = JSONObject(bodyStr)
                val candidates = jsonResponse.optJSONArray("candidates")
                val firstCandidate = candidates?.optJSONObject(0)
                val content = firstCandidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)
                val text = firstPart?.optString("text")

                if (!text.isNullOrBlank()) {
                    text.trim()
                } else {
                    "No analysis generated. Check model status."
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini", e)
            "Could not connect to Google AI Studio. Please verify network access and GEMINI_API_KEY. Details: ${e.localizedMessage ?: ""}"
        }
    }
}
