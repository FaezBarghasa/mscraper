package com.example.core

import com.example.model.Track
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader

class GeminiPlaylistGenerator {
    
    suspend fun generatePlaylist(mood: String): Pair<String, List<Track>> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty()) {
            return@withContext Pair("API KEY MISSING", emptyList())
        }
        
        val prompt = """
            You are a cyberpunk AI DJ. Generate a playlist of 5 real songs that match this mood/activity: "$mood".
            Return ONLY a JSON object with this exact structure, no markdown formatting or extra text:
            {
                "themeName": "A cool cyberpunk neon themed name for the playlist",
                "tracks": [
                    { "title": "Song Title", "artist": "Artist Name", "duration": "3:45" }
                ]
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=${apiKey}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true
            
            connection.outputStream.use { os ->
                val input = jsonPayload.toString().toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val responseJson = JSONObject(responseString)
                
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val content = candidates.getJSONObject(0).optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val text = parts.getJSONObject(0).optString("text")
                        
                        // Parse the actual text response (which should be JSON)
                        val cleanText = text.replace("```json", "").replace("```", "").trim()
                        val resultObj = JSONObject(cleanText)
                        
                        val themeName = resultObj.optString("themeName", "NEON DRIFTER")
                        val tracksArray = resultObj.optJSONArray("tracks")
                        
                        val trackList = mutableListOf<Track>()
                        if (tracksArray != null) {
                            for (i in 0 until tracksArray.length()) {
                                val trackObj = tracksArray.getJSONObject(i)
                                trackList.add(
                                    Track(
                                        id = "gemini_${System.currentTimeMillis()}_$i",
                                        title = trackObj.optString("title"),
                                        artist = trackObj.optString("artist"),
                                        duration = trackObj.optString("duration", "3:00"),
                                        imageUrl = "",
                                        filePath = ""
                                    )
                                )
                            }
                        }
                        return@withContext Pair(themeName, trackList)
                    }
                }
            }
            Pair("NEON DRIFTER", getFallbackTracks())
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("ERROR PROTOCOL", getFallbackTracks())
        }
    }
    
    private fun getFallbackTracks(): List<Track> {
        return listOf(
            Track("fb1", "Nightcall", "Kavinsky", "4:19", ""),
            Track("fb2", "Tech Noir", "Gunship", "4:57", "")
        )
    }
}
